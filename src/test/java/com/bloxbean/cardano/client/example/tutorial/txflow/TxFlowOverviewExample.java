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
 * TxFlow Overview — multi-step transaction orchestration.
 *
 * <p>TxFlow automates multi-transaction workflows by:
 * <ul>
 *   <li>Tracking which UTXOs were produced by each transaction</li>
 *   <li>Waiting for confirmations before building the next transaction</li>
 *   <li>Handling chain reorganizations that invalidate previous transactions</li>
 *   <li>Implementing retry logic for transient network failures</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/overview">TxFlow Overview Documentation</a>
 */
public class TxFlowOverviewExample extends TutorialBase {

    /**
     * Minimal two-step flow: send ADA, then send more ADA.
     *
     * <p>The second step declares a dependency on the first step, so the executor
     * ensures the first transaction is confirmed before building the second.</p>
     */
    @Test
    public void minimalTwoStepFlow() {
        // Define a two-step flow
        TxFlow flow = TxFlow.builder("simple-transfer")
                .withDescription("Send ADA in two steps")
                .addStep(FlowStep.builder("send-ada-1")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(10))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .addStep(FlowStep.builder("send-ada-2")
                        .dependsOn("send-ada-1")  // Uses UTXOs from step 1
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address3, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        // Execute synchronously
        FlowExecutor executor = FlowExecutor.create(backendService);
        FlowResult result = executor.executeSync(flow);

        if (result.isSuccessful()) {
            System.out.println("Flow completed successfully!");
            System.out.println("All tx hashes: " + result.getTransactionHashes());
            System.out.println("Duration: " + result.getDuration());
        } else {
            System.err.println("Flow failed: " + result.getError().getMessage());
            result.getFailedStep().ifPresent(step ->
                    System.err.println("Failed at step: " + step.getStepId()));
        }
    }

    public static void main(String[] args) {
        TxFlowOverviewExample example = new TxFlowOverviewExample();

        System.out.println("=== TxFlow Overview Examples ===\n");

        System.out.println("--- Minimal Two-Step Flow ---");
        example.minimalTwoStepFlow();
    }
}
