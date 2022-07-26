package com.bloxbean.cardano.client.example.function.minting;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.AuxDataProviders;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.metadata.Metadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataList;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.util.PolicyUtil;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MintTokenTest extends BaseTest {

    @Test
    public void run() throws AddressExcepion, CborSerializationException, ApiException {
        new MintTokenTest().mintToken();
    }

    public void mintToken() throws CborSerializationException, ApiException, AddressExcepion {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);
        String senderAddress = sender.baseAddress();

        String receiverAddress = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";

        Policy policy = PolicyUtil.createMultiSigScriptAllPolicy("policy-1", 1);

        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policy.getPolicyId());
        Asset asset = new Asset("TestCoin", BigInteger.valueOf(50000));
        multiAsset.getAssets().add(asset);

        //Metadata
        CBORMetadataMap tokenInfoMap
                = new CBORMetadataMap()
                .put("token", "Test Token")
                .put("symbol", "TTOK");

        CBORMetadataList tagList
                = new CBORMetadataList()
                .add("tag1")
                .add("tag2");

        Metadata metadata = new CBORMetadata()
                .put(new BigInteger("670001"), tokenInfoMap)
                .put(new BigInteger("670002"), tagList);

        Output output = Output.builder()
                .address(receiverAddress)
                .policyId(policy.getPolicyId())
                .assetName(asset.getName())
                .qty(BigInteger.valueOf(50000))
                .build();

        TxBuilder txBuilder = output.mintOutputBuilder()
                .buildInputs(InputBuilders.createFromSender(senderAddress, senderAddress))
                .andThen(MintCreators.mintCreator(policy.getPolicyScript(), multiAsset))
                .andThen(AuxDataProviders.metadataProvider(metadata))
                .andThen(BalanceTxBuilders.balanceTx(senderAddress, 2));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(sender).andThen(signerFrom(policy)));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);

    }
}
