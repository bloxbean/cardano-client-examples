package com.bloxbean.cardano.client.examples.function.minting;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.cip.cip25.NFT;
import com.bloxbean.cardano.client.cip.cip25.NFTFile;
import com.bloxbean.cardano.client.cip.cip25.NFTMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.examples.BaseTest;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.util.PolicyUtil;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.client.function.helper.AuxDataProviders.metadataProvider;
import static com.bloxbean.cardano.client.function.helper.ChangeOutputAdjustments.adjustChangeOutput;
import static com.bloxbean.cardano.client.function.helper.FeeCalculators.feeCalculator;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.MintCreators.mintCreator;
import static com.bloxbean.cardano.client.function.helper.OutputBuilders.createFromMintOutput;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

public class MintTokenNFT extends BaseTest {

    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException {
        new MintTokenNFT().mintToken();
        System.exit(1);
    }

    public void mintToken() throws CborSerializationException, ApiException, AddressExcepion {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);
        String senderAddress = sender.baseAddress();

        String receiverAddress = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";

        Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("policy-1", 1);

        //Multi asset and NFT metadata
        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policy.getPolicyId());
        Asset asset = new Asset("TestNFT", BigInteger.valueOf(1));
        multiAsset.getAssets().add(asset);

        NFT nft = NFT.create()
                .assetName(asset.getName())
                .name(asset.getName())
                .image("ipfs://Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4")
                .mediaType("image/png")
                .addFile(NFTFile.create()
                        .name("file-1")
                        .mediaType("image/png")
                        .src("ipfs/Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4"))
                .description("This is a test NFT");

        NFTMetadata nftMetadata = NFTMetadata.create()
                .version("1.0")
                .addNFT(policy.getPolicyId(), nft);

        Value value = Value.builder()
                .coin(BigInteger.ZERO)
                .multiAssets(List.of(multiAsset)).build();

        TransactionOutput mintOutput = TransactionOutput.builder()
                .address(receiverAddress)
                .value(value).build();

        TxBuilder txBuilder =
                createFromMintOutput(mintOutput)
                        .buildInputs(createFromSender(senderAddress, senderAddress))
                        .andThen(mintCreator(policy.getPolicyScript(), multiAsset))
                        .andThen(metadataProvider(nftMetadata))
                        .andThen(feeCalculator(senderAddress, 2))
                        .andThen(adjustChangeOutput(senderAddress, 2));

        Transaction signedTransaction = TxBuilderContext.init(backendService)
                .buildAndSign(txBuilder, signerFrom(sender).andThen(signerFrom(policy)));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        waitForTransactionHash(result);

    }
}
