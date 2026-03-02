package com.bloxbean.cardano.client.example.tutorial.core;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.address.Credential;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;

import org.junit.jupiter.api.Test;

import java.util.Optional;

/**
 * Address API examples — address creation, validation, and credential extraction.
 *
 * <p>The Address API provides utilities for creating, validating, and inspecting
 * Cardano addresses. It supports base, enterprise, reward (stake), and script addresses.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating addresses from accounts</li>
 *   <li>Generating addresses from credentials</li>
 *   <li>Stake address derivation</li>
 *   <li>Address validation</li>
 *   <li>Credential extraction from addresses</li>
 *   <li>Address type and network inspection</li>
 * </ul>
 *
 * <p>Note: This example is standalone and does not require Yaci DevKit.</p>
 *
 * @see <a href="http://localhost:3000/docs/apis/core/address-api">Address API Documentation</a>
 */
public class AddressApiExample {

    /**
     * Create addresses from an Account and from Bech32 strings.
     */
    @Test
    public void createAddresses() {
        Account account = new Account(Networks.testnet());

        // From an account
        String baseAddr = account.baseAddress();
        String entAddr = account.enterpriseAddress();
        String stakeAddr = account.stakeAddress();

        System.out.println("Base address:       " + baseAddr);
        System.out.println("Enterprise address: " + entAddr);
        System.out.println("Stake address:      " + stakeAddr);

        // From a Bech32 string
        Address address = new Address(baseAddr);
        System.out.println("Address from bech32: " + address.toBech32());
        System.out.println("Address bytes: " + HexUtil.encodeHexString(address.getBytes()));
    }

    /**
     * Generate addresses from payment and stake credentials.
     */
    @Test
    public void addressFromCredentials() throws Exception {
        Account account = new Account(Networks.testnet());

        // Extract credentials from account
        String paymentKeyHash = KeyGenUtil.getKeyHash(VerificationKey.create(account.publicKeyBytes()));
        String stakeKeyHash = KeyGenUtil.getKeyHash(VerificationKey.create(account.stakeHdKeyPair().getPublicKey().getKeyData()));

        Credential paymentCred = Credential.fromKey(paymentKeyHash);
        Credential stakeCred = Credential.fromKey(stakeKeyHash);

        // Generate different address types from credentials
        Address baseAddress = AddressProvider.getBaseAddress(paymentCred, stakeCred, Networks.testnet());
        Address entAddress = AddressProvider.getEntAddress(paymentCred, Networks.testnet());
        Address rewardAddress = AddressProvider.getRewardAddress(stakeCred, Networks.testnet());

        System.out.println("Base address from credentials:       " + baseAddress.toBech32());
        System.out.println("Enterprise address from credentials: " + entAddress.toBech32());
        System.out.println("Reward address from credentials:     " + rewardAddress.toBech32());

        // Verify it matches the account-derived address
        System.out.println("Matches account base address: " + baseAddress.toBech32().equals(account.baseAddress()));
    }

    /**
     * Derive a stake address from a base address.
     */
    @Test
    public void stakeAddressDerivation() {
        Account account = new Account(Networks.testnet());
        Address baseAddress = new Address(account.baseAddress());

        // Derive stake address from base address
        Address stakeAddress = AddressProvider.getStakeAddress(baseAddress);
        System.out.println("Base address:    " + baseAddress.toBech32());
        System.out.println("Stake address:   " + stakeAddress.toBech32());
        System.out.println("Matches account: " + stakeAddress.toBech32().equals(account.stakeAddress()));
    }

    /**
     * Validate address format and network.
     */
    @Test
    public void addressValidation() {
        Account account = new Account(Networks.testnet());
        String validAddress = account.baseAddress();
        String invalidAddress = "addr_test1invalid_address_format";

        System.out.println("Valid address check: " + isValidAddress(validAddress));
        System.out.println("Invalid address check: " + isValidAddress(invalidAddress));
    }

    private boolean isValidAddress(String addr) {
        try {
            new Address(addr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract payment and delegation credentials from an address.
     */
    @Test
    public void credentialExtraction() {
        Account account = new Account(Networks.testnet());
        Address address = new Address(account.baseAddress());

        // Extract payment credential
        Optional<Credential> paymentCred = address.getPaymentCredential();
        paymentCred.ifPresent(cred -> {
            System.out.println("Payment credential type: " + cred.getType());
            System.out.println("Payment credential hash: " + HexUtil.encodeHexString(cred.getBytes()));
        });

        // Extract delegation credential
        Optional<Credential> delegationCred = address.getDelegationCredential();
        delegationCred.ifPresent(cred -> {
            System.out.println("Delegation credential type: " + cred.getType());
            System.out.println("Delegation credential hash: " + HexUtil.encodeHexString(cred.getBytes()));
        });

        // Enterprise address has no delegation credential
        Address entAddress = new Address(account.enterpriseAddress());
        System.out.println("Enterprise has delegation cred: " + entAddress.getDelegationCredential().isPresent());
    }

    /**
     * Inspect address type, network, and other properties.
     */
    @Test
    public void addressInfo() {
        Account account = new Account(Networks.testnet());
        Address address = new Address(account.baseAddress());

        System.out.println("Address type: " + address.getAddressType());
        System.out.println("Network: " + address.getNetwork());
        System.out.println("Prefix: " + address.getPrefix());
        System.out.println("Is pub key in payment part: " + address.isPubKeyHashInPaymentPart());
        System.out.println("Is stake key in delegation part: " + address.isStakeKeyHashInDelegationPart());
    }

    /**
     * Generate a script address from a native multi-sig script.
     */
    @Test
    public void scriptAddress() throws Exception {
        Account account1 = new Account(Networks.testnet());
        Account account2 = new Account(Networks.testnet());

        // Create verification keys
        VerificationKey vk1 = VerificationKey.create(account1.publicKeyBytes());
        VerificationKey vk2 = VerificationKey.create(account2.publicKeyBytes());

        // Create a 1-of-2 multi-sig script
        ScriptPubkey sp1 = ScriptPubkey.create(vk1);
        ScriptPubkey sp2 = ScriptPubkey.create(vk2);
        ScriptAtLeast script = new ScriptAtLeast(1)
                .addScript(sp1)
                .addScript(sp2);

        // Derive the script address
        String scriptAddress = AddressProvider.getEntAddress(script, Networks.testnet()).toBech32();
        System.out.println("Script address: " + scriptAddress);
        System.out.println("Policy ID: " + script.getPolicyId());
    }

    public static void main(String[] args) throws Exception {
        AddressApiExample example = new AddressApiExample();

        System.out.println("=== Address API Examples ===\n");

        System.out.println("--- Create Addresses ---");
        example.createAddresses();

        System.out.println("\n--- Address from Credentials ---");
        example.addressFromCredentials();

        System.out.println("\n--- Stake Address Derivation ---");
        example.stakeAddressDerivation();

        System.out.println("\n--- Address Validation ---");
        example.addressValidation();

        System.out.println("\n--- Credential Extraction ---");
        example.credentialExtraction();

        System.out.println("\n--- Address Info ---");
        example.addressInfo();

        System.out.println("\n--- Script Address ---");
        example.scriptAddress();
    }
}
