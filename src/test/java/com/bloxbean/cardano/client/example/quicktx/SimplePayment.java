package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import org.junit.jupiter.api.Test;

/**
 * Simple payments from two accounts
 */
public class SimplePayment extends QuickTxBaseTest {

    @Test
    public void transfer() {
        Tx tx1 = new Tx()
                .payToAddress(receiver1Addr, Amount.ada(1.5))
                .payToAddress(receiver2Addr, Amount.ada(2.5))
                .attachMetadata(MessageMetadata.create().add("This is a test message 2"))
                .from(sender1Addr);

        Tx tx2 = new Tx()
                .payToAddress(receiver2Addr, Amount.ada(4.5))
                .from(sender2Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder
                .compose(tx1, tx2)
                .feePayer(sender1Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender2))
                .completeAndWait(System.out::println);
    }

}
