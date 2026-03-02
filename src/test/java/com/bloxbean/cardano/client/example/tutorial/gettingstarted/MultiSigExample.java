package com.bloxbean.cardano.client.example.tutorial.gettingstarted;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.Keys;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * Multi-sig Transfer — using native scripts for multi-signature transactions.
 *
 * <p>This example demonstrates how to create a 2-of-3 multi-signature native script,
 * derive its script address, fund it, and then claim from it with the required
 * number of signatures.</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating verification keys from accounts and generated key pairs</li>
 *   <li>Building a ScriptAtLeast (2-of-3) native script</li>
 *   <li>Deriving a script address from a native script</li>
 *   <li>Funding the script address</li>
 *   <li>Claiming from the script address with 2 of 3 signatures</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/gettingstarted/multisig-quickstart">Multi-sig Transfer Documentation</a>
 */
public class MultiSigExample extends TutorialBase {

    private ScriptAtLeast scriptAtLeast;
    private SecretKey thirdSecretKey;
    private String scriptAddress;

    /**
     * Create a 2-of-3 multi-sig native script and derive its address.
     *
     * <p>Uses account1 and account2 from TutorialBase, plus a generated key pair
     * for the third signer.</p>
     */
    @Test
    @Order(1)
    public void createMultiSigScript() throws Exception {
        // Generate a third key pair
        Keys keys = KeyGenUtil.generateKey();
        VerificationKey thirdVk = keys.getVkey();
        thirdSecretKey = keys.getSkey();
        System.out.println("Generated third key pair");

        // Derive verification keys for account1 and account2
        VerificationKey account1Vk = VerificationKey.create(account1.publicKeyBytes());
        VerificationKey account2Vk = VerificationKey.create(account2.publicKeyBytes());

        // Create native script with type=sig for each verification key
        ScriptPubkey scriptPubkey1 = ScriptPubkey.create(account1Vk);
        ScriptPubkey scriptPubkey2 = ScriptPubkey.create(account2Vk);
        ScriptPubkey scriptPubkey3 = ScriptPubkey.create(thirdVk);

        // Create multi-sig script: at least 2 of 3 signatures required
        scriptAtLeast = new ScriptAtLeast(2)
                .addScript(scriptPubkey1)
                .addScript(scriptPubkey2)
                .addScript(scriptPubkey3);

        // Derive the script address
        scriptAddress = AddressProvider.getEntAddress(scriptAtLeast, Networks.testnet()).toBech32();

        System.out.println("Script address: " + scriptAddress);
        System.out.println("Policy ID: " + scriptAtLeast.getPolicyId());
    }

    /**
     * Fund the script address and then claim from it.
     *
     * <p>Phase 1: Send ADA from account1 to the script address.
     * Phase 2: Claim from the script address back to account1 and account2,
     * signing with account1 and the third key (2 of 3).</p>
     */
    @Test
    @Order(2)
    public void fundAndClaimFromScript() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Phase 1: Fund the script address
        System.out.println("Phase 1: Funding script address...");
        Tx fundTx = new Tx()
                .payToAddress(scriptAddress, Amount.ada(100))
                .from(address1);

        Result<String> fundResult = quickTxBuilder.compose(fundTx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Fund result: " + fundResult.isSuccessful());
        if (!fundResult.isSuccessful()) {
            System.err.println("Failed to fund: " + fundResult.getResponse());
            return;
        }
        System.out.println("Fund tx hash: " + fundResult.getValue());

        // Wait for UTXO to be available
        checkIfUtxoAvailable(fundResult.getValue(), scriptAddress);

        // Phase 2: Claim from script address
        System.out.println("\nPhase 2: Claiming from script address...");
        Tx claimTx = new Tx()
                .payToAddress(address1, Amount.ada(25))
                .payToAddress(address2, Amount.ada(25))
                .attachNativeScript(scriptAtLeast)
                .from(scriptAddress);

        // Sign with account1 + third key (2 of 3 required)
        Result<String> claimResult = quickTxBuilder.compose(claimTx)
                .withSigner(SignerProviders.signerFrom(account1))
                .withSigner(SignerProviders.signerFrom(thirdSecretKey))
                .completeAndWait(System.out::println);

        System.out.println("Claim result: " + claimResult.isSuccessful());
        if (claimResult.isSuccessful()) {
            System.out.println("Claim tx hash: " + claimResult.getValue());
        } else {
            System.err.println("Failed to claim: " + claimResult.getResponse());
        }
    }

    public static void main(String[] args) throws Exception {
        MultiSigExample example = new MultiSigExample();

        System.out.println("=== Multi-sig Transfer Examples ===\n");

        System.out.println("--- Create Multi-sig Script ---");
        example.createMultiSigScript();

        System.out.println("\n--- Fund and Claim from Script ---");
        example.fundAndClaimFromScript();
    }
}
