package com.bloxbean.cardano.client.example.tutorial.core;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.bip32.HdKeyPair;
import com.bloxbean.cardano.client.crypto.bip39.Words;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.hdwallet.Wallet;
import org.junit.jupiter.api.Test;

/**
 * HD Wallet API examples — wallet creation, restoration, and address management.
 *
 * <p>The HD Wallet API provides hierarchical deterministic wallet support following
 * BIP-39/BIP-44/CIP-1852 standards. It supports multiple accounts, address indexes,
 * and key derivation paths.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating wallets with different mnemonic lengths</li>
 *   <li>Restoring wallets from mnemonic</li>
 *   <li>Generating addresses at different indexes</li>
 *   <li>Multi-account management</li>
 *   <li>Key management and access</li>
 *   <li>Privacy-enhanced address generation</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/core/hd-wallet-api">HD Wallet API Documentation</a>
 */
public class HdWalletApiExample {

    /**
     * Create wallets with different mnemonic lengths.
     */
    @Test
    public void createWallets() {
        // Default: 24-word mnemonic
        Wallet wallet24 = Wallet.create(Networks.testnet());
        System.out.println("24-word mnemonic: " + wallet24.getMnemonic());
        System.out.println("Base address: " + wallet24.getBaseAddress(0).toBech32());

        // 15-word mnemonic
        Wallet wallet15 = Wallet.create(Networks.testnet(), Words.FIFTEEN);
        System.out.println("15-word mnemonic: " + wallet15.getMnemonic());
        System.out.println("Base address: " + wallet15.getBaseAddress(0).toBech32());

        // 12-word mnemonic
        Wallet wallet12 = Wallet.create(Networks.testnet(), Words.TWELVE);
        System.out.println("12-word mnemonic: " + wallet12.getMnemonic());
        System.out.println("Base address: " + wallet12.getBaseAddress(0).toBech32());
    }

    /**
     * Restore a wallet from mnemonic.
     */
    @Test
    public void restoreWallet() {
        // Create a wallet and save the mnemonic
        Wallet original = Wallet.create(Networks.testnet());
        String mnemonic = original.getMnemonic();
        String originalAddress = original.getBaseAddress(0).toBech32();
        System.out.println("Original address: " + originalAddress);

        // Restore from mnemonic
        Wallet restored = Wallet.createFromMnemonic(Networks.testnet(), mnemonic);
        String restoredAddress = restored.getBaseAddress(0).toBech32();
        System.out.println("Restored address: " + restoredAddress);
        System.out.println("Addresses match: " + originalAddress.equals(restoredAddress));

        // Restore with specific account number
        Wallet restoredAccount1 = Wallet.createFromMnemonic(Networks.testnet(), mnemonic, 1);
        System.out.println("Account 1 address: " + restoredAccount1.getBaseAddress(0).toBech32());
    }

    /**
     * Generate addresses at different indexes.
     */
    @Test
    public void addressGeneration() {
        Wallet wallet = Wallet.create(Networks.testnet());

        System.out.println("Base addresses:");
        for (int i = 0; i < 5; i++) {
            System.out.println("  Index " + i + ": " + wallet.getBaseAddress(i).toBech32());
        }

        System.out.println("Enterprise addresses:");
        for (int i = 0; i < 3; i++) {
            System.out.println("  Index " + i + ": " + wallet.getEntAddress(i).toBech32());
        }
    }

    /**
     * Manage multiple accounts within a wallet.
     */
    @Test
    public void multiAccountManagement() {
        Wallet wallet = Wallet.create(Networks.testnet());
        String mnemonic = wallet.getMnemonic();

        // Account 0 (default)
        Wallet account0 = Wallet.createFromMnemonic(Networks.testnet(), mnemonic, 0);
        System.out.println("Account 0, Index 0: " + account0.getBaseAddress(0).toBech32());

        // Account 1
        Wallet account1 = Wallet.createFromMnemonic(Networks.testnet(), mnemonic, 1);
        System.out.println("Account 1, Index 0: " + account1.getBaseAddress(0).toBech32());

        // Account 2
        Wallet account2 = Wallet.createFromMnemonic(Networks.testnet(), mnemonic, 2);
        System.out.println("Account 2, Index 0: " + account2.getBaseAddress(0).toBech32());

        // Get Account objects from wallet for transactions
        Account acc = account0.getAccountAtIndex(0);
        System.out.println("Account object address: " + acc.baseAddress());
    }

    /**
     * Access wallet keys.
     */
    @Test
    public void keyManagement() {
        Wallet wallet = Wallet.create(Networks.testnet());

        // Root key pair
        HdKeyPair rootKeyPair = wallet.getRootKeyPair().orElse(null);
        if (rootKeyPair != null) {
            System.out.println("Root public key: " + HexUtil.encodeHexString(rootKeyPair.getPublicKey().getKeyData()));
        }

        // Stake address (returns String directly)
        System.out.println("Stake address: " + wallet.getStakeAddress());
    }

    /**
     * Privacy-enhanced address generation — use a fresh address per transaction.
     */
    @Test
    public void privacyEnhancedAddresses() {
        Wallet wallet = Wallet.create(Networks.testnet());

        System.out.println("Privacy-enhanced pattern: Use a fresh address per transaction");
        for (int i = 0; i < 3; i++) {
            String freshAddress = wallet.getBaseAddress(i).toBech32();
            System.out.println("  Transaction " + (i + 1) + ": " + freshAddress);
        }
    }

    public static void main(String[] args) {
        HdWalletApiExample example = new HdWalletApiExample();

        System.out.println("=== HD Wallet API Examples ===\n");

        System.out.println("--- Create Wallets ---");
        example.createWallets();

        System.out.println("\n--- Restore Wallet ---");
        example.restoreWallet();

        System.out.println("\n--- Address Generation ---");
        example.addressGeneration();

        System.out.println("\n--- Multi-Account Management ---");
        example.multiAccountManagement();

        System.out.println("\n--- Key Management ---");
        example.keyManagement();

        System.out.println("\n--- Privacy-Enhanced Addresses ---");
        example.privacyEnhancedAddresses();
    }
}
