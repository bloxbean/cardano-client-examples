package com.bloxbean.cardano.client.example.tutorial.gettingstarted;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.util.AssetUtil;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

/**
 * Token Distribution — minting and distributing native tokens.
 *
 * <p>This example demonstrates how to mint a native token using a native script policy
 * and then distribute it to multiple addresses. This is a common pattern for token
 * airdrops, reward distributions, and initial token distribution.</p>
 *
 * <p>The doc site example reads recipients from a CSV file. This tutorial instead uses
 * a programmatic list for simplicity and mints the token first (since Yaci DevKit
 * starts with no native tokens).</p>
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Creating a native script policy from a verification key</li>
 *   <li>Minting a native token</li>
 *   <li>Distributing tokens to multiple addresses in a single transaction</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/gettingstarted/tokens-distribution">Token Distribution Documentation</a>
 */
public class TokenDistributionExample extends TutorialBase {

    private String policyId;
    private String unit;
    private ScriptPubkey policy;

    /**
     * Mint a test token using a native script policy.
     *
     * <p>Creates a ScriptPubkey policy from account1's verification key,
     * mints 1,000,000 tokens to account1.</p>
     */
    @Test
    @Order(1)
    public void mintTestToken() throws Exception {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Create a native script policy from account1's verification key
        VerificationKey vk = VerificationKey.create(account1.publicKeyBytes());
        policy = ScriptPubkey.create(vk);
        policyId = policy.getPolicyId();

        // Define the asset
        String assetName = "TestToken";
        Asset asset = new Asset(assetName, BigInteger.valueOf(1_000_000));
        unit = AssetUtil.getUnit(policyId, assetName);

        System.out.println("Policy ID: " + policyId);
        System.out.println("Asset name: " + assetName);
        System.out.println("Unit: " + unit);

        // Mint tokens to account1
        Tx mintTx = new Tx()
                .mintAssets(policy, asset, address1)
                .from(address1);

        Result<String> result = quickTxBuilder.compose(mintTx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Mint result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Mint tx hash: " + result.getValue());
        } else {
            System.err.println("Failed to mint: " + result.getResponse());
        }
    }

    /**
     * Distribute tokens to multiple addresses in a single transaction.
     *
     * <p>Sends tokens from account1 to account2 and account3. In production,
     * the recipient list could come from a CSV file or database.</p>
     */
    @Test
    @Order(2)
    public void distributeTokens() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);

        // Build transaction with multiple token payments
        // (In production, these would come from a CSV file or other data source)
        Tx tx = new Tx()
                .payToAddress(address2, Amount.asset(unit, BigInteger.valueOf(250_000)))
                .payToAddress(address3, Amount.asset(unit, BigInteger.valueOf(150_000)))
                .from(address1);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Distribution result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Distribution tx hash: " + result.getValue());
        } else {
            System.err.println("Failed to distribute: " + result.getResponse());
        }
    }

    public static void main(String[] args) throws Exception {
        TokenDistributionExample example = new TokenDistributionExample();

        System.out.println("=== Token Distribution Examples ===\n");

        System.out.println("--- Mint Test Token ---");
        example.mintTestToken();

        System.out.println("\n--- Distribute Tokens ---");
        example.distributeTokens();
    }
}
