package com.bloxbean.cardano.client.example.tutorial.quicktx;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

/**
 * QuickTx API examples — the comprehensive transaction builder.
 *
 * <p>The QuickTx API provides a declarative way to build, sign, and submit Cardano
 * transactions. It supports simple payments, native token minting, metadata,
 * and composing multiple transactions together.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Simple ADA payment to multiple addresses</li>
 *   <li>Composing multiple transactions with a shared fee payer</li>
 *   <li>Native token minting with a native script policy</li>
 *   <li>Attaching transaction metadata</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/apis/transaction/quicktx-api">QuickTx API Documentation</a>
 */
public class QuickTxApiExample extends TutorialBase {

    /**
     * Simple ADA payment to multiple addresses.
     */
    @Test
    public void simplePayment() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(1.5))
                .payToAddress(address3, Amount.ada(2.5))
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Simple payment result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Compose multiple transactions with a shared fee payer.
     *
     * <p>Two senders each create a Tx, composed together with a single fee payer.</p>
     */
    @Test
    public void multipleTransactions() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        Tx tx1 = new Tx()
                .payToAddress(address3, Amount.ada(3))
                .from(address1);

        Tx tx2 = new Tx()
                .payToAddress(address3, Amount.ada(2))
                .from(address2);

        Result<String> result = quickTxBuilder
                .compose(tx1, tx2)
                .feePayer(address1)
                .withSigner(SignerProviders.signerFrom(account1))
                .withSigner(SignerProviders.signerFrom(account2))
                .completeAndWait(System.out::println);

        System.out.println("Multiple transactions result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Mint a native token using a native script policy.
     */
    @Test
    public void nativeTokenMinting() throws Exception {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create a policy from account1's verification key
        VerificationKey vk = VerificationKey.create(account1.publicKeyBytes());
        ScriptPubkey policy = ScriptPubkey.create(vk);

        // Define the asset
        Asset asset = new Asset("QuickTxToken", BigInteger.valueOf(10_000));

        // Mint and send to account1
        Tx mintTx = new Tx()
                .mintAssets(policy, asset, address1)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(mintTx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Mint result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
            System.out.println("Policy ID: " + policy.getPolicyId());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Attach metadata to a transaction.
     */
    @Test
    public void metadataAttachment() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        MessageMetadata metadata = MessageMetadata.create()
                .add("Transaction with metadata")
                .add("From QuickTx API tutorial");

        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(2))
                .attachMetadata(metadata)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Metadata attachment result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    public static void main(String[] args) throws Exception {
        QuickTxApiExample example = new QuickTxApiExample();

        System.out.println("=== QuickTx API Examples ===\n");

        System.out.println("--- Simple Payment ---");
        example.simplePayment();

        System.out.println("\n--- Multiple Transactions ---");
        example.multipleTransactions();

        System.out.println("\n--- Native Token Minting ---");
        example.nativeTokenMinting();

        System.out.println("\n--- Metadata Attachment ---");
        example.metadataAttachment();
    }
}
