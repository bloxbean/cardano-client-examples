package com.bloxbean.cardano.client.example.tutorial.mpftrie;

import com.bloxbean.cardano.vds.core.api.StorageMode;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbMptSession;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbNodeStore;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbStateTrees;
import org.junit.jupiter.api.Test;
import org.rocksdb.FlushOptions;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * MpfTrie Production Guide — batch operations, session-based batching, graceful shutdown.
 *
 * <p>Covers:
 * <ul>
 *   <li>WriteBatch for atomic multi-operation writes</li>
 *   <li>Session-based batching with try-with-resources</li>
 *   <li>Thread safety considerations</li>
 *   <li>Graceful shutdown</li>
 * </ul>
 *
 * <p>Note: This example uses local RocksDB storage and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/preview/mpftrie/production-guide">Production Guide Documentation</a>
 */
public class MpfTrieProductionGuideExample {

    /**
     * Use RocksDB WriteBatch for atomic multi-operation writes.
     */
    @Test
    public void writeBatchOperations() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-batch");

        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);
        RocksDbNodeStore nodeStore = stateTrees.nodeStore();
        MpfTrie trie = new MpfTrie(nodeStore);

        try (WriteBatch batch = new WriteBatch();
             WriteOptions writeOpts = new WriteOptions()) {

            nodeStore.withBatch(batch, () -> {
                // All operations go into the batch
                trie.put("key1".getBytes(), "value1".getBytes());
                trie.put("key2".getBytes(), "value2".getBytes());
                trie.put("key3".getBytes(), "value3".getBytes());
                return null;
            });

            // Atomic commit
            stateTrees.db().write(writeOpts, batch);
        }

        byte[] rootHash = trie.getRootHash();

        // Verify all writes succeeded atomically
        System.out.println("key1: " + new String(trie.get("key1".getBytes())));
        System.out.println("key2: " + new String(trie.get("key2".getBytes())));
        System.out.println("key3: " + new String(trie.get("key3".getBytes())));

        stateTrees.close();
    }

    /**
     * Session-based batching using RocksDbMptSession.
     *
     * <p>The session wraps write operations in a WriteBatch for atomicity.</p>
     */
    @Test
    public void sessionBasedBatching() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-session");

        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);
        MpfTrie trie = new MpfTrie(stateTrees.nodeStore());

        try (RocksDbMptSession session = RocksDbMptSession.of(stateTrees)) {
            // Multiple operations batched
            session.write(() -> {
                trie.put("alice".getBytes(), "100".getBytes());
                trie.put("bob".getBytes(), "200".getBytes());
                trie.put("charlie".getBytes(), "300".getBytes());
            });
        }

        // Save the root as a new version
        stateTrees.putRootWithRefcount(trie.getRootHash());

        // Verify data
        System.out.println("alice: " + new String(trie.get("alice".getBytes())));
        System.out.println("bob: " + new String(trie.get("bob".getBytes())));
        System.out.println("charlie: " + new String(trie.get("charlie".getBytes())));

        stateTrees.close();
    }

    /**
     * Thread safety: create separate MpfTrie instances per thread sharing the same store.
     *
     * <p>MpfTrie is NOT thread-safe — use separate instances per thread,
     * all sharing the same RocksDbNodeStore (which is thread-safe for reads).</p>
     */
    @Test
    public void threadSafety() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-threads");

        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);
        RocksDbNodeStore sharedNodeStore = stateTrees.nodeStore();

        // Initial data
        MpfTrie mainTrie = new MpfTrie(sharedNodeStore);
        mainTrie.put("shared-key".getBytes(), "initial".getBytes());
        stateTrees.putRootWithRefcount(mainTrie.getRootHash());

        byte[] rootHash = mainTrie.getRootHash();

        // Each thread should create its own MpfTrie instance
        Thread reader1 = new Thread(() -> {
            MpfTrie threadTrie = new MpfTrie(sharedNodeStore, rootHash);
            byte[] value = threadTrie.get("shared-key".getBytes());
            System.out.println("Reader 1: " + new String(value));
        });

        Thread reader2 = new Thread(() -> {
            MpfTrie threadTrie = new MpfTrie(sharedNodeStore, rootHash);
            byte[] value = threadTrie.get("shared-key".getBytes());
            System.out.println("Reader 2: " + new String(value));
        });

        reader1.start();
        reader2.start();
        reader1.join();
        reader2.join();

        stateTrees.close();
    }

    /**
     * Graceful shutdown — flush writes and close in correct order.
     */
    @Test
    public void gracefulShutdown() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-shutdown");

        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);
        MpfTrie trie = new MpfTrie(stateTrees.nodeStore());

        trie.put("important-data".getBytes(), "must-persist".getBytes());
        stateTrees.putRootWithRefcount(trie.getRootHash());

        // Ensure all writes are flushed
        stateTrees.db().flush(new FlushOptions().setWaitForFlush(true));
        System.out.println("All writes flushed to disk");

        // Close in correct order
        stateTrees.close();
        System.out.println("StateTrees closed gracefully");
    }

    public static void main(String[] args) throws Exception {
        MpfTrieProductionGuideExample example = new MpfTrieProductionGuideExample();

        System.out.println("=== MpfTrie Production Guide Examples ===\n");

        System.out.println("--- WriteBatch Operations ---");
        example.writeBatchOperations();

        System.out.println("\n--- Session-Based Batching ---");
        example.sessionBasedBatching();

        System.out.println("\n--- Thread Safety ---");
        example.threadSafety();

        System.out.println("\n--- Graceful Shutdown ---");
        example.gracefulShutdown();
    }
}
