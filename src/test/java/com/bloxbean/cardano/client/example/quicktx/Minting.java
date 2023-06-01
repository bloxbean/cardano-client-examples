package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.util.PolicyUtil;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.Policy;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Minting extends QuickTxBaseTest {

    //This is an example of simple minting by one account
    @Test
    void minting() throws CborSerializationException {
        Policy policy = PolicyUtil.createMultiSigScriptAtLeastPolicy("test_policy", 1, 1);
        String assetName = "MyAsset";
        BigInteger qty = BigInteger.valueOf(1000);

        Tx tx = new Tx()
                .mintAssets(policy.getPolicyScript(), new Asset(assetName, qty), sender1.baseAddress())
                .attachMetadata(MessageMetadata.create().add("Minting tx"))
                .from(sender1.baseAddress());

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(policy))
                .complete();

        System.out.println(result);
        waitForTransactionHash(result);
    }

    //This is a composed transaction with minting and regular transfer
    @Test
    void minting_withTransfer() throws CborSerializationException {
        Policy policy = PolicyUtil.createMultiSigScriptAtLeastPolicy("test_policy", 1, 1);
        String assetName = "MyAsset";
        BigInteger qty = BigInteger.valueOf(2000);

        Tx tx1 = new Tx()
                .payToAddress(receiver1Addr, Amount.ada(1.5))
                .mintAssets(policy.getPolicyScript(), new Asset(assetName, qty), receiver2Addr)
                .attachMetadata(MessageMetadata.create().add("Minting tx"))
                .from(sender1Addr);

        Tx tx2 = new Tx()
                .payToAddress(receiver2Addr, new Amount(LOVELACE, adaToLovelace(2.13)))
                .from(sender2Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(tx1, tx2)
                .feePayer(sender1.baseAddress())
                .withSigner(SignerProviders.signerFrom(sender1)
                        .andThen(SignerProviders.signerFrom(sender2)))
                .withSigner(SignerProviders.signerFrom(policy))
                .additionalSignersCount(1) //As we have composed TxSigners from 2 signers, we need to add 1 additional signer,
                // as it's hard to determine how many signers are in the composed TxSigner. Alternatively, just keep adding
                //different signers with withSigner() method call.
                .completeAndWait(System.out::println);

        System.out.println(result);
        assertTrue(result.isSuccessful());
    }
}
