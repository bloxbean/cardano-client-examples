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
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.util.PolicyUtil;

import java.math.BigInteger;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.AuxDataProviders.metadataProvider;
import static com.bloxbean.cardano.client.function.helper.ChangeOutputAdjustments.adjustChangeOutput;
import static com.bloxbean.cardano.client.function.helper.FeeCalculators.feeCalculator;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.MintCreators.mintCreator;
import static com.bloxbean.cardano.client.function.helper.OutputBuilders.createFromMintOutput;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;

//One sender -- 3 Receivers (1 NFT receiver + 1 NFT receiver + 1 Ada receiver)
public class MulitpleMintNFTsWithRefund extends BaseTest {

    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException {
        new MulitpleMintNFTsWithRefund().mintToken();
        System.exit(1);
    }

    public void mintToken() throws CborSerializationException, ApiException, AddressExcepion {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);
        String senderAddress = sender.baseAddress();

        String receiver1 = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";
        String receiver2 = "addr_test1qq9f6hzuwmqpe3p9h90z2rtgs0v0lsq9ln5f79fjyec7eclg7v88q9als70uzkdh5k6hw20uuwqfz477znfp5v4rga2s3ysgxu";
        String receiver3 = "addr_test1qqqvjp4ffcdqg3fmx0k8rwamnn06wp8e575zcv8d0m3tjn2mmexsnkxp7az774522ce4h3qs4tjp9rxjjm46qf339d9sk33rqn";

        Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("policy-1", 1);

        //Multi asset and NFT metadata
        //NFT-1
        MultiAsset multiAsset1 = new MultiAsset();
        multiAsset1.setPolicyId(policy.getPolicyId());
        Asset asset = new Asset("TestNFT", BigInteger.valueOf(1));
        multiAsset1.getAssets().add(asset);

        NFT nft1 = NFT.create()
                .assetName(asset.getName())
                .name(asset.getName())
                .image("ipfs://Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4")
                .mediaType("image/png")
                .addFile(NFTFile.create()
                        .name("file-1")
                        .mediaType("image/png")
                        .src("ipfs/Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4"))
                .description("This is a test NFT");

        //NFT-2
        MultiAsset multiAsset2 = new MultiAsset();
        multiAsset2.setPolicyId(policy.getPolicyId());
        Asset asset2 = new Asset("TestNFT2", BigInteger.valueOf(1));
        multiAsset2.getAssets().add(asset2);

        NFT nft2 = NFT.create()
                .assetName(asset2.getName())
                .name(asset2.getName())
                .image("ipfs://Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4")
                .mediaType("image/png")
                .addFile(NFTFile.create()
                        .name("file-1")
                        .mediaType("image/png")
                        .src("ipfs/Qmcv6hwtmdVumrNeb42R1KmCEWdYWGcqNgs17Y3hj6CkP4"))
                .description("This is a test NFT");

        NFTMetadata nftMetadata = NFTMetadata.create()
                .version("1.0")
                .addNFT(policy.getPolicyId(), nft1)
                .addNFT(policy.getPolicyId(), nft2);

        //Define outputs
        //Output using TransactionOutput
        TransactionOutput mintOutput1 = TransactionOutput.builder()
                .address(receiver1)
                .value(Value.builder().coin(adaToLovelace(2))
                        .multiAssets(List.of(multiAsset1)).build()).build();

        TransactionOutput mintOutput2 = TransactionOutput.builder()
                .address(receiver2)
                .value(Value.builder().coin(adaToLovelace(3))
                        .multiAssets(List.of(multiAsset2)).build()).build();

        //Output using new Output class
        Output output3 = Output.builder()
                .address(receiver3)
                .assetName(LOVELACE)
                .qty(adaToLovelace(4.5)).build();

        MultiAsset mergeMultiAsset = multiAsset1.plus(multiAsset2);

        //Create TxBuilder function
        TxBuilder txBuilder =
                createFromMintOutput(mintOutput1)
                        .and(createFromMintOutput(mintOutput2))
                        .and(createFromMintOutput(output3))
                        .buildInputs(createFromSender(senderAddress, senderAddress))
                        .andThen(mintCreator(policy.getPolicyScript(), mergeMultiAsset))
                        .andThen(metadataProvider(nftMetadata))
                        .andThen(feeCalculator(senderAddress, 2))
                        .andThen(adjustChangeOutput(senderAddress, 2)); //any adjustment in change output

        //Build and sign transaction
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
