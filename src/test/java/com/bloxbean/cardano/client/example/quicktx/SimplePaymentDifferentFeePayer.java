package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import org.junit.jupiter.api.Test;

/**
 * Simple payments from two accounts
 */
public class SimplePaymentDifferentFeePayer extends QuickTxBaseTest {

    @Test
    public void transfer() {
        Tx tx1 = new Tx()
                .payToAddress(receiver1Addr, Amount.ada(1.5))
                .attachMetadata(MessageMetadata.create().add("This is a test message 2"))
                .from(sender1Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder
                .compose(tx1)
                .feePayer(sender2Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender2))
                .completeAndWait(System.out::println);
    }

}
