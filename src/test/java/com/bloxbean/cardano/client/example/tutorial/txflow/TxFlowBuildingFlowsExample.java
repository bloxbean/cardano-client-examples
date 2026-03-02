package com.bloxbean.cardano.client.example.tutorial.txflow;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import org.junit.jupiter.api.Test;

/**
 * Building Flows — TxFlow and FlowStep builder API, step dependencies, and UTXO chaining.
 *
 * <p>Demonstrates:
 * <ul>
 *   <li>TxFlow builder with description and variables</li>
 *   <li>FlowStep builder with TxContext</li>
 *   <li>Step dependency strategies: ALL, INDEX, CHANGE, FILTER</li>
 *   <li>Flow validation</li>
 *   <li>YAML serialization</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/building-flows">Building Flows Documentation</a>
 */
public class TxFlowBuildingFlowsExample extends TutorialBase {

    /**
     * Build a flow with variables, description, and step dependencies.
     */
    @Test
    public void flowWithBuilderApi() {
        // Build a flow using the builder API
        FlowStep step1 = FlowStep.builder("deposit")
                .withDescription("Send deposit ADA")
                .withTxContext(builder -> builder
                        .compose(new Tx()
                                .payToAddress(address2, Amount.ada(20))
                                .from(address1))
                        .withSigner(SignerProviders.signerFrom(account1)))
                .build();

        FlowStep step2 = FlowStep.builder("payment")
                .withDescription("Send payment using change from deposit")
                .dependsOn("deposit")  // ALL outputs from "deposit"
                .withTxContext(builder -> builder
                        .compose(new Tx()
                                .payToAddress(address3, Amount.ada(5))
                                .from(address1))
                        .withSigner(SignerProviders.signerFrom(account1)))
                .build();

        TxFlow flow = TxFlow.builder("deposit-and-pay")
                .withDescription("Deposit then pay")
                .addVariable("amount", 20_000_000L)
                .addVariable("receiver", address2)
                .addStep(step1)
                .addStep(step2)
                .build();

        System.out.println("Flow built: " + flow.getId());
        System.out.println("Steps: " + flow.getSteps().size());

        // Validate the flow
        TxFlow.ValidationResult validation = flow.validate();
        if (validation.isValid()) {
            System.out.println("Flow is valid!");
        } else {
            System.err.println("Flow validation errors: " + validation.getErrors());
        }

        // Execute the flow
        FlowExecutor executor = FlowExecutor.create(backendService);
        FlowResult result = executor.executeSync(flow);

        System.out.println("Flow result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Demonstrate different step dependency strategies.
     */
    @Test
    public void dependencyStrategies() {
        TxFlow flow = TxFlow.builder("dependency-demo")
                .withDescription("Demo dependency strategies")
                // Step 1: Initial payment
                .addStep(FlowStep.builder("initial-payment")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(10))
                                        .payToAddress(address3, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())

                // Step 2: Depends on all outputs from step 1
                .addStep(FlowStep.builder("use-change")
                        .dependsOn("initial-payment")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(2))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())

                .build();

        System.out.println("Flow with dependency strategies built");

        TxFlow.ValidationResult validation = flow.validate();
        System.out.println("Valid: " + validation.isValid());

        FlowExecutor executor = FlowExecutor.create(backendService);
        FlowResult result = executor.executeSync(flow);

        System.out.println("Result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Serialize a flow to YAML and deserialize it back.
     */
    @Test
    public void yamlSerialization() {
        TxFlow flow = TxFlow.builder("yaml-demo")
                .withDescription("YAML serialization demo")
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        // Serialize to YAML
        String yaml = flow.toYaml();
        System.out.println("Flow as YAML:\n" + yaml);

        // Deserialize from YAML
        TxFlow restored = TxFlow.fromYaml(yaml);
        System.out.println("Restored flow ID: " + restored.getId());
        System.out.println("Restored steps: " + restored.getSteps().size());
    }

    public static void main(String[] args) {
        TxFlowBuildingFlowsExample example = new TxFlowBuildingFlowsExample();

        System.out.println("=== Building Flows Examples ===\n");

        System.out.println("--- Flow with Builder API ---");
        example.flowWithBuilderApi();

        System.out.println("\n--- Dependency Strategies ---");
        example.dependencyStrategies();

        System.out.println("\n--- YAML Serialization ---");
        example.yamlSerialization();
    }
}
