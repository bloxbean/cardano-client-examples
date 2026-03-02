package com.bloxbean.cardano.client.example.tutorial.mpftrie;

import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbNodeStore;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

/**
 * MpfTrie Overview — Merkle Patricia Forestry with RocksDB.
 *
 * <p>MpfTrie provides cryptographically verifiable data structures compatible with
 * <a href="https://github.com/aiken-lang/merkle-patricia-forestry">Aiken's on-chain proof
 * verification</a>. Data is stored off-chain in a trie and verified on-chain using Plutus scripts.</p>
 *
 * <p>Key features:
 * <ul>
 *   <li>RocksDB persistence — high-performance embedded database</li>
 *   <li>Aiken compatible — proofs can be verified by Aiken's on-chain MPF library</li>
 *   <li>Automatic key hashing — Blake2b-256</li>
 * </ul>
 *
 * <p>Note: This example uses local RocksDB storage and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/preview/mpftrie/overview">MpfTrie Overview Documentation</a>
 */
public class MpfTrieOverviewExample {

    /**
     * Basic usage: create a trie, store data, retrieve data, get root hash.
     */
    @Test
    public void basicUsage() throws Exception {
        Path dataDir = Files.createTempDirectory("data_mpf");

        // Initialize RocksDB-backed node store
        RocksDbNodeStore nodeStore = new RocksDbNodeStore("data/mpf");

        // Create trie (Blake2b-256 hashing, Cardano/Aiken compatible)
        MpfTrie trie = new MpfTrie(nodeStore);

        // Store data (keys are automatically hashed)
        trie.put("alice".getBytes(), "balance:100".getBytes());
        trie.put("bob".getBytes(), "balance:200".getBytes());

        // Get root hash for on-chain verification
        byte[] rootHash = trie.getRootHash();
        System.out.println("Root hash: " + bytesToHex(rootHash));

        // Retrieve data
        byte[] aliceValue = trie.get("alice".getBytes());
        System.out.println("Alice: " + new String(aliceValue));

        byte[] bobValue = trie.get("bob".getBytes());
        System.out.println("Bob: " + new String(bobValue));

        // Delete data
        trie.delete("bob".getBytes());
        byte[] deletedValue = trie.get("bob".getBytes());
        System.out.println("Bob after delete: " + (deletedValue == null ? "null" : new String(deletedValue)));

        // Root hash changes after modification
        byte[] newRootHash = trie.getRootHash();
        System.out.println("New root hash: " + bytesToHex(newRootHash));
        System.out.println("Root hash changed: " + !Arrays.equals(rootHash, newRootHash));

        // Cleanup
        nodeStore.close();
    }

    /**
     * Load an existing trie with a known root hash.
     */
    @Test
    public void loadExistingTrie() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-load");

        // Create and populate a trie
        RocksDbNodeStore nodeStore = new RocksDbNodeStore(dataDir.toString());

        MpfTrie trie = new MpfTrie(nodeStore);
        trie.put("alice".getBytes(), "balance:100".getBytes());
        trie.put("bob".getBytes(), "balance:200".getBytes());

        byte[] savedRootHash = trie.getRootHash();
        System.out.println("Saved root hash: " + bytesToHex(savedRootHash));

        // Load existing trie with known root hash
        MpfTrie loadedTrie = new MpfTrie(nodeStore, savedRootHash);

        byte[] value = loadedTrie.get("alice".getBytes());
        System.out.println("Loaded alice value: " + new String(value));

        nodeStore.close();
    }

    /**
     * Generate Plutus-compatible proofs for on-chain verification with Aiken.
     */
    @Test
    public void proofGeneration() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-proof");

        RocksDbNodeStore nodeStore = new RocksDbNodeStore(dataDir.toString());
        MpfTrie trie = new MpfTrie(nodeStore);

        // Store data
        trie.put("account123".getBytes(), "verified_user".getBytes());

        // Generate proof for on-chain verification
        Optional<ListPlutusData> proof = trie.getProofPlutusData("account123".getBytes());

        if (proof.isPresent()) {
            System.out.println("Proof generated successfully!");
            System.out.println("Proof data: " + proof.get());
            System.out.println("Root hash: " + bytesToHex(trie.getRootHash()));
            System.out.println("The proof can be included as a redeemer or datum in a Cardano transaction");
        } else {
            System.out.println("No proof available (key not found)");
        }

        // Wire format proof (CBOR bytes)
        Optional<byte[]> wireProof = trie.getProofWire("account123".getBytes());
        wireProof.ifPresent(bytes ->
                System.out.println("Wire proof size: " + bytes.length + " bytes"));

        nodeStore.close();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        MpfTrieOverviewExample example = new MpfTrieOverviewExample();

        System.out.println("=== MpfTrie Overview Examples ===\n");

        System.out.println("--- Basic Usage ---");
        example.basicUsage();

        System.out.println("\n--- Load Existing Trie ---");
        example.loadExistingTrie();

        System.out.println("\n--- Proof Generation for Aiken ---");
        example.proofGeneration();
    }
}
