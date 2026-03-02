package com.bloxbean.cardano.client.example.tutorial.txplan;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.quicktx.serialization.TxPlan;
import org.junit.jupiter.api.Test;

/**
 * TxPlan Overview — YAML-based transaction definitions with variable substitution.
 *
 * <p>TxPlan provides a configuration-driven approach to defining Cardano transactions.
 * Instead of writing Java code for every transaction, define transactions in YAML
 * with variable substitution, context properties, and signer registry references.</p>
 *
 * <p>Key benefits:
 * <ul>
 *   <li>Configuration-driven — define transactions in YAML files</li>
 *   <li>Variable resolution — use {@code ${variable}} placeholders</li>
 *   <li>Context properties — centralize fee payer, collateral, validity</li>
 *   <li>No extra dependency — TxPlan is part of the quicktx module</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txplan/overview">TxPlan Overview Documentation</a>
 */
public class TxPlanOverviewExample extends TutorialBase {

    /**
     * Create a TxPlan from a Tx object and serialize to YAML.
     */
    @Test
    public void createFromTxObject() {
        Tx tx = new Tx()
                .from(address1)
                .payToAddress(address2, Amount.ada(5));

        TxPlan plan = TxPlan.from(tx)
                .feePayer(address1)
                .validTo(2000L);

        // Serialize to YAML
        String yaml = plan.toYaml();
        System.out.println("TxPlan as YAML:\n" + yaml);
    }

    /**
     * Create a TxPlan from a YAML string and execute it.
     */
    @Test
    public void executeFromYaml() {
        String yamlPlan = "version: 1.0\n" +
                "variables:\n" +
                "  sender: " + address1 + "\n" +
                "  receiver: " + address2 + "\n" +
                "context:\n" +
                "  fee_payer: ${sender}\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from: ${sender}\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: ${receiver}\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '5000000'\n";

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        TxPlan plan = TxPlan.from(yamlPlan);

        Result<String> result = builder.compose(plan)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("YAML execution result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    /**
     * Use variables in TxPlan for dynamic values.
     *
     * <p>Variables are resolved during YAML deserialization ({@code TxPlan.from()}).
     * To override variable values, set them in the YAML string before calling {@code from()}.</p>
     */
    @Test
    public void withVariables() {
        // YAML template with variable placeholders
        String yamlTemplate = "version: 1.0\n" +
                "variables:\n" +
                "  sender: " + address1 + "\n" +
                "  receiver: %s\n" +
                "  amount: '%s'\n" +
                "context:\n" +
                "  fee_payer: ${sender}\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from: ${sender}\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: ${receiver}\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '${amount}'\n";

        // Set dynamic values before parsing
        String yamlPlan = String.format(yamlTemplate, address3, "10000000");

        TxPlan plan = TxPlan.from(yamlPlan);

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        Result<String> result = builder.compose(plan)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("With variables result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    public static void main(String[] args) {
        TxPlanOverviewExample example = new TxPlanOverviewExample();

        System.out.println("=== TxPlan Overview Examples ===\n");

        System.out.println("--- Create from Tx Object ---");
        example.createFromTxObject();

        System.out.println("\n--- Execute from YAML ---");
        example.executeFromYaml();

        System.out.println("\n--- With Variables ---");
        example.withVariables();
    }
}
