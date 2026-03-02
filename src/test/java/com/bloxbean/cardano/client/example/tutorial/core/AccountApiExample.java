package com.bloxbean.cardano.client.example.tutorial.core;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.bip32.HdKeyPair;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

/**
 * Account API examples — account creation, address derivation, key management.
 *
 * <p>The {@code Account} class provides a simple abstraction for creating and
 * managing Cardano accounts, including CIP-1852 compatible address derivation,
 * BIP-39 mnemonic generation, key management, and transaction signing.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating accounts with factory methods and constructors</li>
 *   <li>Address generation (base, enterprise, stake, change)</li>
 *   <li>Key management (payment, stake, root keys)</li>
 *   <li>DRep and governance key access</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/core/account-api">Account API Documentation</a>
 */
public class AccountApiExample {

    /**
     * Create accounts using factory methods and constructors.
     */
    @Test
    public void createAccounts() {
        // Generate new account with random mnemonic
        Account newAccount = new Account(Networks.testnet());
        System.out.println("Generated mnemonic: " + newAccount.mnemonic());
        System.out.println("Base address: " + newAccount.baseAddress());

        // Create mainnet account
        Account mainnetAccount = new Account(Networks.mainnet());
        System.out.println("Mainnet address: " + mainnetAccount.baseAddress());

        // Recommended: Factory method from mnemonic
        String mnemonic = newAccount.mnemonic();
        Account restored = Account.createFromMnemonic(Networks.testnet(), mnemonic);
        System.out.println("Restored address: " + restored.baseAddress());
        System.out.println("Addresses match: " + newAccount.baseAddress().equals(restored.baseAddress()));

        // With specific account and address index (m/1852'/1815'/1/0/2)
        Account indexedAccount = Account.createFromMnemonic(Networks.testnet(), mnemonic, 1, 2);
        System.out.println("Indexed account address: " + indexedAccount.baseAddress());
    }

    /**
     * Generate different types of Cardano addresses from an account.
     */
    @Test
    public void addressGeneration() {
        Account account = new Account(Networks.testnet());

        String baseAddress = account.baseAddress();
        String enterpriseAddress = account.enterpriseAddress();
        String stakeAddress = account.stakeAddress();
        String changeAddress = account.changeAddress();

        System.out.println("Base address:       " + baseAddress);
        System.out.println("Enterprise address: " + enterpriseAddress);
        System.out.println("Stake address:      " + stakeAddress);
        System.out.println("Change address:     " + changeAddress);
    }

    /**
     * Access payment, stake, and root key pairs from an account.
     */
    @Test
    public void keyManagement() {
        Account account = new Account(Networks.testnet());

        // Payment keys
        byte[] privateKey = account.privateKeyBytes();
        byte[] publicKey = account.publicKeyBytes();
        System.out.println("Payment private key: " + HexUtil.encodeHexString(privateKey));
        System.out.println("Payment public key:  " + HexUtil.encodeHexString(publicKey));

        // HD key pairs
        HdKeyPair paymentKeyPair = account.hdKeyPair();
        HdKeyPair stakeKeyPair = account.stakeHdKeyPair();
        HdKeyPair changeKeyPair = account.changeHdKeyPair();

        System.out.println("Payment key pair: " + HexUtil.encodeHexString(paymentKeyPair.getPublicKey().getKeyData()));
        System.out.println("Stake key pair:   " + HexUtil.encodeHexString(stakeKeyPair.getPublicKey().getKeyData()));
        System.out.println("Change key pair:  " + HexUtil.encodeHexString(changeKeyPair.getPublicKey().getKeyData()));

        // Root key pair (available when created from mnemonic)
        HdKeyPair rootKeyPair = account.getRootKeyPair().orElse(null);
        System.out.println("Root key pair:    " + (rootKeyPair != null ? "available" : "not available"));

        // Mnemonic
        System.out.println("Mnemonic: " + account.mnemonic());
    }

    /**
     * Access DRep and Constitutional Committee keys for governance.
     */
    @Test
    public void drepAndGovernanceKeys() {
        Account account = new Account(Networks.testnet());

        // DRep keys (CIP-1852 derivation)
        HdKeyPair drepKeyPair = account.drepHdKeyPair();
        System.out.println("DRep public key: " + HexUtil.encodeHexString(drepKeyPair.getPublicKey().getKeyData()));
        System.out.println("DRep ID (CIP-129): " + account.drepId());
        System.out.println("DRep credential: " + HexUtil.encodeHexString(account.drepCredential().getBytes()));

        // Constitutional Committee cold keys
        HdKeyPair coldKeyPair = account.committeeColdKeyPair();
        System.out.println("Committee cold key: " + HexUtil.encodeHexString(coldKeyPair.getPublicKey().getKeyData()));

        // Constitutional Committee hot keys
        HdKeyPair hotKeyPair = account.committeeHotKeyPair();
        System.out.println("Committee hot key: " + HexUtil.encodeHexString(hotKeyPair.getPublicKey().getKeyData()));
    }

    public static void main(String[] args) {
        AccountApiExample example = new AccountApiExample();

        System.out.println("=== Account API Examples ===\n");

        System.out.println("--- Create Accounts ---");
        example.createAccounts();

        System.out.println("\n--- Address Generation ---");
        example.addressGeneration();

        System.out.println("\n--- Key Management ---");
        example.keyManagement();

        System.out.println("\n--- DRep & Governance Keys ---");
        example.drepAndGovernanceKeys();
    }
}
