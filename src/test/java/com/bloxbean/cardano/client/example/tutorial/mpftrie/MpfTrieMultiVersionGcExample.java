package com.bloxbean.cardano.client.example.tutorial.mpftrie;

import com.bloxbean.cardano.vds.core.api.StorageMode;
import com.bloxbean.cardano.vds.mpf.MpfTrie;
import com.bloxbean.cardano.vds.mpf.rocksdb.RocksDbStateTrees;
import com.bloxbean.cardano.vds.mpf.rocksdb.gc.GcManager;
import com.bloxbean.cardano.vds.mpf.rocksdb.gc.GcOptions;
import com.bloxbean.cardano.vds.mpf.rocksdb.gc.GcReport;
import com.bloxbean.cardano.vds.mpf.rocksdb.gc.RetentionPolicy;
import com.bloxbean.cardano.vds.mpf.rocksdb.gc.strategy.OnDiskMarkSweepStrategy;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Multi-Version and Garbage Collection — RocksDbStateTrees versioning, GC strategies.
 *
 * <p>{@code RocksDbStateTrees} provides multi-version support, allowing versioned snapshots
 * of the trie with commit/rollback capabilities.</p>
 *
 * <p>Garbage collection strategies:
 * <ul>
 *   <li>Mark-Sweep GC — marks reachable nodes from retained roots, sweeps unreachable ones</li>
 * </ul>
 *
 * <p>Retention policies:
 * <ul>
 *   <li>{@code keepLatestN(n)} — keep the latest N versions</li>
 *   <li>{@code keepVersions(collection)} — keep specific versions</li>
 *   <li>{@code keepRange(from, to)} — keep a range of versions</li>
 * </ul>
 *
 * <p>Note: This example uses local RocksDB storage and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/preview/mpftrie/multi-version-gc">Multi-Version &amp; GC Documentation</a>
 */
public class MpfTrieMultiVersionGcExample {

    /**
     * Demonstrate versioning with RocksDbStateTrees.
     *
     * <p>Each call to {@code putRootWithRefcount()} creates a new version, and you can
     * roll back to any previous version by loading the root hash for that version.</p>
     */
    @Test
    public void versioning() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-versioning");

        // Enable versioning
        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);

        // Get node store and create MpfTrie
        MpfTrie trie = new MpfTrie(stateTrees.nodeStore());

        // Version 1
        trie.put("alice".getBytes(), "100".getBytes());
        long v1 = stateTrees.putRootWithRefcount(trie.getRootHash());
        System.out.println("Version 1: " + v1);
        System.out.println("  Alice balance: " + new String(trie.get("alice".getBytes())));

        // Version 2
        trie.put("alice".getBytes(), "150".getBytes());
        long v2 = stateTrees.putRootWithRefcount(trie.getRootHash());
        System.out.println("Version 2: " + v2);
        System.out.println("  Alice balance: " + new String(trie.get("alice".getBytes())));

        // Version 3
        trie.put("alice".getBytes(), "200".getBytes());
        trie.put("bob".getBytes(), "50".getBytes());
        long v3 = stateTrees.putRootWithRefcount(trie.getRootHash());
        System.out.println("Version 3: " + v3);
        System.out.println("  Alice balance: " + new String(trie.get("alice".getBytes())));
        System.out.println("  Bob balance: " + new String(trie.get("bob".getBytes())));

        // Rollback to version 1
        byte[] rootV1 = stateTrees.rootsIndex().get(v1);
        trie.setRootHash(rootV1);

        byte[] value = trie.get("alice".getBytes());
        System.out.println("\nAfter rollback to v1:");
        System.out.println("  Alice balance: " + new String(value));  // Returns "100"

        // Rollback to version 2
        byte[] rootV2 = stateTrees.rootsIndex().get(v2);
        trie.setRootHash(rootV2);
        System.out.println("After rollback to v2:");
        System.out.println("  Alice balance: " + new String(trie.get("alice".getBytes())));  // Returns "150"

        stateTrees.close();
    }

    /**
     * Run mark-sweep garbage collection to reclaim space from stale nodes.
     */
    @Test
    public void garbageCollection() throws Exception {
        Path dataDir = Files.createTempDirectory("mpf-gc");

        RocksDbStateTrees stateTrees = new RocksDbStateTrees(dataDir.toString(), StorageMode.MULTI_VERSION);
        MpfTrie trie = new MpfTrie(stateTrees.nodeStore());

        // Create multiple versions with overlapping data
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 100; j++) {
                trie.put(("key-" + j).getBytes(), ("value-v" + i + "-" + j).getBytes());
            }
            long version = stateTrees.putRootWithRefcount(trie.getRootHash());
            System.out.println("Committed version " + version);
        }

        // Set up GC
        GcManager gcManager = new GcManager(
                stateTrees.nodeStore(),
                stateTrees.rootsIndex()
        );

        GcOptions options = new GcOptions();
        options.deleteBatchSize = 10_000;
        options.progress = deleted -> System.out.println("  Deleted so far: " + deleted);

        // Run GC keeping only the latest 2 versions
        RetentionPolicy policy = RetentionPolicy.keepLatestN(2);

        System.out.println("\nRunning mark-sweep GC (keeping latest 2 versions)...");
        GcReport report = gcManager.runSync(new OnDiskMarkSweepStrategy(), policy, options);

        System.out.println("GC Report:");
        System.out.println("  Marked (reachable): " + report.marked);
        System.out.println("  Deleted: " + report.deleted);
        System.out.println("  Total processed: " + report.total);
        System.out.println("  Duration: " + report.durationMillis + "ms");

        stateTrees.close();
    }

    /**
     * Demonstrate different retention policies.
     */
    @Test
    public void retentionPolicies() {
        System.out.println("Available retention policies:");

        // Keep latest N versions
        RetentionPolicy keepLatest = RetentionPolicy.keepLatestN(10);
        System.out.println("  keepLatestN(10) - keeps the 10 most recent versions");

        // Keep specific versions
        RetentionPolicy keepSpecific = RetentionPolicy.keepVersions(java.util.List.of(1L, 5L, 10L));
        System.out.println("  keepVersions({1, 5, 10}) - keeps only specified versions");

        // Keep a range of versions
        RetentionPolicy keepRange = RetentionPolicy.keepRange(5, 15);
        System.out.println("  keepRange(5, 15) - keeps versions 5 through 15");
    }

    public static void main(String[] args) throws Exception {
        MpfTrieMultiVersionGcExample example = new MpfTrieMultiVersionGcExample();

        System.out.println("=== MpfTrie Multi-Version & GC Examples ===\n");

        System.out.println("--- Versioning ---");
        example.versioning();

        System.out.println("\n--- Garbage Collection ---");
        example.garbageCollection();

        System.out.println("\n--- Retention Policies ---");
        example.retentionPolicies();
    }
}
