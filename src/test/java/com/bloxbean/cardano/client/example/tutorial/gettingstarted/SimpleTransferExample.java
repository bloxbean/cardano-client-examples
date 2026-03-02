package com.bloxbean.cardano.client.example.tutorial.gettingstarted;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import org.junit.jupiter.api.Test;

/**
 * Simple ADA Transfer — the fundamental Cardano transaction.
 *
 * <p>This example demonstrates how to send ADA from one account to multiple
 * receivers using the QuickTx API. It includes CIP-20 message metadata
 * to attach a human-readable description to the transaction.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating a QuickTxBuilder with a backend service</li>
 *   <li>Building a transaction with multiple outputs</li>
 *   <li>Attaching CIP-20 message metadata</li>
 *   <li>Signing and submitting the transaction</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/gettingstarted/simple-transfer">Simple Transfer Documentation</a>
 */
public class SimpleTransferExample extends TutorialBase {

    /**
     * Send ADA to two receivers with a CIP-20 message.
     *
     * <p>Uses account1 as the sender, account2 and account3 as receivers.
     * Attaches a human-readable message metadata (CIP-20, label 674).</p>
     */
    @Test
    public void simpleAdaTransfer() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create a CIP20 message metadata
        MessageMetadata metadata = MessageMetadata.create()
                .add("First transfer transaction");

        // Define transaction: send 10 ADA to account2, 20 ADA to account3
        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(10))
                .payToAddress(address3, Amount.ada(20))
                .attachMetadata(metadata)
                .from(address1);

        // Build, sign, submit and wait for confirmation
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Transfer result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    public static void main(String[] args) {
        SimpleTransferExample example = new SimpleTransferExample();

        System.out.println("=== Simple ADA Transfer Examples ===\n");

        System.out.println("--- Simple ADA Transfer ---");
        example.simpleAdaTransfer();
    }
}
