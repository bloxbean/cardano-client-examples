package com.bloxbean.cardano.client.example.tutorial.standards;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.util.HexUtil;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

/**
 * CIP-20 Message Metadata examples — creating and attaching transaction messages.
 *
 * <p>CIP-20 defines a standard for attaching human-readable messages to Cardano
 * transactions using metadata label 674. Messages are stored as string arrays
 * and can be used for payment descriptions, invoices, and audit trails.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating message metadata (standalone)</li>
 *   <li>Attaching messages to transactions (requires Yaci DevKit)</li>
 *   <li>Retrieving messages from metadata</li>
 *   <li>Merging message metadata with other metadata</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/apis/standards/cip20-api">CIP-20 API Documentation</a>
 */
public class Cip20MessageMetadataExample extends TutorialBase {

    /**
     * Create CIP-20 message metadata (standalone — no blockchain needed).
     */
    @Test
    public void createMessageMetadata() throws Exception {
        MessageMetadata messageMetadata = MessageMetadata.create()
                .add("Payment for services rendered")
                .add("Invoice #12345")
                .add("Thank you for your business!");

        byte[] serialized = messageMetadata.serialize();
        System.out.println("Message metadata CBOR: " + HexUtil.encodeHexString(serialized));
        System.out.println("Message metadata size: " + serialized.length + " bytes");
    }

    /**
     * Attach CIP-20 messages to a transaction.
     *
     * <p>Requires Yaci DevKit running.</p>
     */
    @Test
    public void attachToTransaction() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        MessageMetadata metadata = MessageMetadata.create()
                .add("Payment from Account 1 to Account 2")
                .add("Amount: 5 ADA");

        Tx tx = new Tx()
                .payToAddress(address2, Amount.ada(5))
                .attachMetadata(metadata)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Transaction with message: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        } else {
            System.err.println("Failed: " + result.getResponse());
        }
    }

    /**
     * Retrieve messages from a MessageMetadata object.
     */
    @Test
    public void retrieveMessages() {
        MessageMetadata messageMetadata = MessageMetadata.create()
                .add("Line 1: Payment description")
                .add("Line 2: Reference number ABC-123")
                .add("Line 3: Thank you");

        List<String> messages = messageMetadata.getMessages();

        System.out.println("Retrieved " + messages.size() + " messages:");
        for (int i = 0; i < messages.size(); i++) {
            System.out.println("  [" + i + "] " + messages.get(i));
        }
    }

    /**
     * Merge CIP-20 messages with other custom metadata.
     */
    @Test
    public void mergeWithOtherMetadata() throws Exception {
        // Create message metadata (label 674)
        MessageMetadata messageMetadata = MessageMetadata.create()
                .add("Transaction with custom data");

        // Create custom metadata at different labels
        CBORMetadata customMetadata = new CBORMetadata();
        customMetadata.put(BigInteger.valueOf(100), "Application data");
        customMetadata.put(BigInteger.valueOf(200), "Additional context");

        // Merge
        customMetadata.merge(messageMetadata);

        byte[] serialized = customMetadata.serialize();
        System.out.println("Merged metadata (CIP-20 + custom): " + HexUtil.encodeHexString(serialized));
        System.out.println("Merged metadata size: " + serialized.length + " bytes");
    }

    public static void main(String[] args) throws Exception {
        Cip20MessageMetadataExample example = new Cip20MessageMetadataExample();

        System.out.println("=== CIP-20 Message Metadata Examples ===\n");

        System.out.println("--- Create Message Metadata (standalone) ---");
        example.createMessageMetadata();

        System.out.println("\n--- Attach to Transaction (Yaci DevKit) ---");
        example.attachToTransaction();

        System.out.println("\n--- Retrieve Messages ---");
        example.retrieveMessages();

        System.out.println("\n--- Merge with Other Metadata ---");
        example.mergeWithOtherMetadata();
    }
}
