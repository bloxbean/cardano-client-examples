package com.bloxbean.cardano.client.example.tutorial.core;

import com.bloxbean.cardano.client.metadata.Metadata;
import com.bloxbean.cardano.client.metadata.MetadataBuilder;
import com.bloxbean.cardano.client.metadata.MetadataList;
import com.bloxbean.cardano.client.metadata.MetadataMap;
import com.bloxbean.cardano.client.util.HexUtil;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

/**
 * Metadata API examples — creating, structuring, and serializing transaction metadata.
 *
 * <p>The Metadata API provides utilities for building CBOR-encoded transaction metadata
 * that can be attached to Cardano transactions. It supports maps, lists, strings, integers,
 * and byte arrays.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating basic key-value metadata</li>
 *   <li>Building structured metadata with maps and lists</li>
 *   <li>Serialization to CBOR and JSON</li>
 *   <li>JSON to metadata conversion</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/core/metadata-api">Metadata API Documentation</a>
 */
public class MetadataApiExample {

    /**
     * Create basic metadata with simple key-value pairs.
     */
    @Test
    public void basicMetadata() throws Exception {
        Metadata metadata = MetadataBuilder.createMetadata()
                .put(BigInteger.ONE, "Hello Cardano")
                .put(BigInteger.valueOf(2), BigInteger.valueOf(100))
                .put(BigInteger.valueOf(3), "metadata".getBytes());

        System.out.println("Basic metadata:");
        byte[] serialized = metadata.serialize();
        System.out.println("CBOR hex: " + HexUtil.encodeHexString(serialized));
    }

    /**
     * Build structured metadata with nested maps and lists.
     */
    @Test
    public void structuredMetadata() throws Exception {
        // Create a map
        MetadataMap productMap = MetadataBuilder.createMap()
                .put("code", "PROD-800")
                .put("slno", "SL20000039484")
                .put("price", BigInteger.valueOf(5000));

        // Create a list
        MetadataList tagList = MetadataBuilder.createList()
                .add("laptop")
                .add("computer")
                .add("electronics");

        // Build the metadata with nested structures
        Metadata metadata = MetadataBuilder.createMetadata()
                .put(BigInteger.valueOf(100), productMap)
                .put(BigInteger.valueOf(200), tagList)
                .put(BigInteger.valueOf(300), "Order placed");

        byte[] serialized = metadata.serialize();
        System.out.println("Structured metadata:");
        System.out.println("CBOR hex: " + HexUtil.encodeHexString(serialized));
    }

    /**
     * Serialize metadata to CBOR bytes and convert to JSON.
     */
    @Test
    public void metadataSerialization() throws Exception {
        Metadata metadata = MetadataBuilder.createMetadata()
                .put(BigInteger.valueOf(674), "Hello from CCL");

        // Serialize to CBOR
        byte[] cbor = metadata.serialize();
        System.out.println("CBOR bytes: " + HexUtil.encodeHexString(cbor));
        System.out.println("CBOR size: " + cbor.length + " bytes");
    }

    /**
     * Build application-specific metadata structures.
     */
    @Test
    public void applicationMetadata() throws Exception {
        // Example: Build a product catalog entry
        MetadataMap catalogEntry = MetadataBuilder.createMap()
                .put("name", "Cardano Developer Kit")
                .put("version", "2.0")
                .put("author", "Cardano Community");

        MetadataList features = MetadataBuilder.createList()
                .add("transaction building")
                .add("smart contracts")
                .add("NFT minting");

        MetadataMap fullEntry = MetadataBuilder.createMap()
                .put("product", catalogEntry)
                .put("features", features)
                .put("timestamp", BigInteger.valueOf(System.currentTimeMillis()));

        Metadata metadata = MetadataBuilder.createMetadata()
                .put(BigInteger.valueOf(1000), fullEntry);

        byte[] serialized = metadata.serialize();
        System.out.println("Application metadata:");
        System.out.println("CBOR hex: " + HexUtil.encodeHexString(serialized));
        System.out.println("CBOR size: " + serialized.length + " bytes");
    }

    public static void main(String[] args) throws Exception {
        MetadataApiExample example = new MetadataApiExample();

        System.out.println("=== Metadata API Examples ===\n");

        System.out.println("--- Basic Metadata ---");
        example.basicMetadata();

        System.out.println("\n--- Structured Metadata ---");
        example.structuredMetadata();

        System.out.println("\n--- Metadata Serialization ---");
        example.metadataSerialization();

        System.out.println("\n--- Application Metadata ---");
        example.applicationMetadata();
    }
}
