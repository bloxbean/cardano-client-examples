package com.bloxbean.cardano.client.example.tutorial.unifiedtx;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import org.junit.jupiter.api.Test;

/**
 * Unified Tx API examples — ScriptTx merged into Tx.
 *
 * <p>In 0.8.0, all {@code ScriptTx} operations are now available directly on {@code Tx}.
 * The {@code ScriptTx} class is deprecated and will be removed in a future release.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Simple ADA transfer using the unified Tx API</li>
 *   <li>Mixed operations in a single Tx (native payment + multiple outputs)</li>
 * </ul>
 *
 * <p>For script-related examples (collectFrom, mintAsset, attachSpendingValidator),
 * a deployed Plutus script is required. See the Smart Contract Calls tutorial.</p>
 *
 * @see <a href="http://localhost:3000/docs/preview/unified-tx">Unified Tx API Documentation</a>
 */
public class UnifiedTxExample extends TutorialBase {

    /**
     * Simple ADA transfer using the unified Tx API.
     *
     * <p>Demonstrates the basic usage pattern: create a Tx, compose it
     * with QuickTxBuilder, sign, and submit.</p>
     */
    @Test
    public void simpleTransfer() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(10))
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Simple transfer result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Mixed operations in one transaction — combining multiple payments.
     *
     * <p>In 0.8.0, you can freely combine native payments, script inputs,
     * Plutus minting, and staking operations in a single {@code Tx} object.</p>
     */
    @Test
    public void mixedOperations() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Combine multiple payments in a single Tx
        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(5))
                .payToAddress(address3, Amount.ada(3))
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Mixed operations result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    public static void main(String[] args) {
        UnifiedTxExample example = new UnifiedTxExample();

        System.out.println("=== Unified Tx API Examples ===\n");

        System.out.println("--- Simple Transfer ---");
        example.simpleTransfer();

        System.out.println("\n--- Mixed Operations ---");
        example.mixedOperations();
    }
}
