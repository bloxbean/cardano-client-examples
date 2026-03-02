package com.bloxbean.cardano.client.example.tutorial.txplan;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.quicktx.serialization.TxPlan;
import com.bloxbean.cardano.client.quicktx.signing.DefaultSignerRegistry;
import com.bloxbean.cardano.client.quicktx.signing.SignerRegistry;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

/**
 * TxPlan Advanced Usage — Signer Registry, multi-tx plans, dynamic configuration, TxFlow integration.
 *
 * <p>Covers:
 * <ul>
 *   <li>SignerRegistry — keep private keys out of YAML files</li>
 *   <li>Registry references in Java and YAML</li>
 *   <li>Multiple transactions in a single YAML plan</li>
 *   <li>Dynamic configuration for different environments</li>
 *   <li>TxPlan with TxFlow for YAML-defined multi-step flows</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txplan/advanced-usage">TxPlan Advanced Usage Documentation</a>
 */
public class TxPlanAdvancedExample extends TutorialBase {

    /**
     * Use SignerRegistry to keep private keys out of YAML.
     *
     * <p>Senders, fee payers, and signers are referenced by URI-style identifiers
     * that the registry resolves at runtime.</p>
     */
    @Test
    @Order(1)
    public void signerRegistry() {
        // Create a signer registry
        SignerRegistry registry = new DefaultSignerRegistry()
                .addAccount("account://sender", account1);

        // Use registry references in Java
        Tx tx = new Tx()
                .fromRef("account://sender")
                .payToAddress(address2, Amount.ada(5));

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        Result<String> result = builder
                .compose(tx)
                .withSignerRegistry(registry)
                .feePayerRef("account://sender")
                .withSignerRef("account://sender", "payment")
                .completeAndWait(System.out::println);

        System.out.println("Signer registry result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    /**
     * Use registry references in YAML.
     */
    @Test
    @Order(2)
    public void signerRegistryWithYaml() {
        SignerRegistry registry = new DefaultSignerRegistry()
                .addAccount("account://ops", account1);

        String yamlPlan = "version: 1.1\n" +
                "variables:\n" +
                "  receiver: " + address2 + "\n" +
                "  amount: '5000000'\n" +
                "context:\n" +
                "  fee_payer_ref: account://ops\n" +
                "  signers:\n" +
                "    - ref: account://ops\n" +
                "      scope: payment\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from_ref: account://ops\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: ${receiver}\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '${amount}'\n";

        TxPlan plan = TxPlan.from(yamlPlan);

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        Result<String> result = builder
                .compose(plan, registry)
                .completeAndWait(System.out::println);

        System.out.println("YAML registry result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    /**
     * Multiple payment intents in a single YAML plan.
     *
     * <p>Note: Each {@code tx} entry in a YAML plan must have a unique {@code from} address.
     * To send multiple payments from the same address, use multiple intents in a single tx.</p>
     */
    @Test
    @Order(3)
    public void multiplePayments() {
        String yamlPlan = "version: 1.0\n" +
                "variables:\n" +
                "  treasury: " + address1 + "\n" +
                "context:\n" +
                "  fee_payer: ${treasury}\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from: ${treasury}\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: " + address2 + "\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '5000000'\n" +
                "        - type: payment\n" +
                "          address: " + address3 + "\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '3000000'\n";

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        TxPlan plan = TxPlan.from(yamlPlan);

        Result<String> result = builder.compose(plan)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Multi-payment result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    /**
     * Dynamic configuration — load a base plan and customize for different environments.
     *
     * <p>Variables are resolved during YAML deserialization ({@code TxPlan.from()}).
     * To customize for different environments, set the variable values in the YAML
     * before calling {@code from()}.</p>
     */
    @Test
    @Order(4)
    public void dynamicConfiguration() {
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

        // Customize for environment before parsing
        String yamlPlan = String.format(yamlTemplate, address2, "7000000");
        TxPlan plan = TxPlan.from(yamlPlan);

        QuickTxBuilder builder = new QuickTxBuilder(backendService);
        Result<String> result = builder.compose(plan)
                .withSigner(SignerProviders.signerFrom(account1))
                .completeAndWait(System.out::println);

        System.out.println("Dynamic config result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getValue());
        }
    }

    /**
     * TxPlan with TxFlow — YAML-defined transactions in multi-step flows.
     */
    @Test
    @Order(5)
    public void txPlanWithTxFlow() {
        String depositYaml = "version: 1.0\n" +
                "variables:\n" +
                "  receiver: " + address2 + "\n" +
                "context:\n" +
                "  fee_payer_ref: account://sender\n" +
                "  signers:\n" +
                "    - ref: account://sender\n" +
                "      scope: payment\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from_ref: account://sender\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: ${receiver}\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '5000000'\n";

        String releaseYaml = "version: 1.0\n" +
                "variables:\n" +
                "  receiver: " + address3 + "\n" +
                "context:\n" +
                "  fee_payer_ref: account://sender\n" +
                "  signers:\n" +
                "    - ref: account://sender\n" +
                "      scope: payment\n" +
                "transaction:\n" +
                "  - tx:\n" +
                "      from_ref: account://sender\n" +
                "      intents:\n" +
                "        - type: payment\n" +
                "          address: ${receiver}\n" +
                "          amounts:\n" +
                "            - unit: lovelace\n" +
                "              quantity: '3000000'\n";

        TxPlan depositPlan = TxPlan.from(depositYaml);
        TxPlan releasePlan = TxPlan.from(releaseYaml);

        // Build flow with YAML-defined steps
        TxFlow flow = TxFlow.builder("yaml-flow")
                .addStep(FlowStep.builder("deposit")
                        .withTxPlan(depositPlan)
                        .build())
                .addStep(FlowStep.builder("release")
                        .dependsOn("deposit")
                        .withTxPlan(releasePlan)
                        .build())
                .build();

        SignerRegistry registry = new DefaultSignerRegistry()
                .addAccount("account://sender", account1);

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withSignerRegistry(registry);

        FlowResult result = executor.executeSync(flow);

        System.out.println("TxPlan + TxFlow result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        } else {
            System.out.println("Flow status: " + result.getStatus());
            result.getFailedStep().ifPresent(sr ->
                    System.out.println("  Failed step '" + sr.getStepId() + "': " +
                            (sr.getError() != null ? sr.getError().getMessage() : "unknown error")));
        }
    }

    public static void main(String[] args) {
        TxPlanAdvancedExample example = new TxPlanAdvancedExample();

        System.out.println("=== TxPlan Advanced Examples ===\n");

        System.out.println("--- Signer Registry ---");
        example.signerRegistry();

        System.out.println("\n--- Signer Registry with YAML ---");
        example.signerRegistryWithYaml();

        System.out.println("\n--- Multiple Payments ---");
        example.multiplePayments();

        System.out.println("\n--- Dynamic Configuration ---");
        example.dynamicConfiguration();

        System.out.println("\n--- TxPlan with TxFlow ---");
        example.txPlanWithTxFlow();
    }
}
