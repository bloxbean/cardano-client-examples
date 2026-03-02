package com.bloxbean.cardano.client.example.tutorial.txflow;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.txflow.ChainingMode;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import org.junit.jupiter.api.Test;

/**
 * Chaining Modes — SEQUENTIAL, PIPELINED, and BATCH execution strategies.
 *
 * <p>The chaining mode controls how transactions are submitted and confirmed
 * relative to each other:
 * <ul>
 *   <li><b>SEQUENTIAL (Default):</b> Each step waits for confirmation before the next begins.
 *       Safest option for production.</li>
 *   <li><b>PIPELINED:</b> All transactions are submitted without waiting for confirmations.
 *       Faster execution, multiple transactions possible in the same block.</li>
 *   <li><b>BATCH:</b> All transactions are built and signed first, then submitted in rapid
 *       succession. Highest likelihood of same-block inclusion.</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/chaining-modes">Chaining Modes Documentation</a>
 */
public class TxFlowChainingModesExample extends TutorialBase {

    /**
     * Execute a flow using SEQUENTIAL chaining mode (default).
     */
    @Test
    public void sequentialMode() {
        TxFlow flow = buildTwoStepFlow("sequential-flow");

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withChainingMode(ChainingMode.SEQUENTIAL);

        FlowResult result = executor.executeSync(flow);

        System.out.println("SEQUENTIAL result: " + result.isSuccessful());
        System.out.println("Duration: " + result.getDuration());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Execute a flow using PIPELINED chaining mode.
     *
     * <p>Transactions are submitted without waiting for confirmations between steps.
     * This enables multiple transactions in the same block for faster execution.</p>
     */
    @Test
    public void pipelinedMode() {
        TxFlow flow = buildTwoStepFlow("pipelined-flow");

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withChainingMode(ChainingMode.PIPELINED);

        FlowResult result = executor.executeSync(flow);

        System.out.println("PIPELINED result: " + result.isSuccessful());
        System.out.println("Duration: " + result.getDuration());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Execute a flow using BATCH chaining mode.
     *
     * <p>All transactions are built and signed first, then submitted in rapid succession.
     * Highest likelihood of same-block inclusion. Best for devnets and fast networks.</p>
     */
    @Test
    public void batchMode() {
        TxFlow flow = buildTwoStepFlow("batch-flow");

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withChainingMode(ChainingMode.BATCH);

        FlowResult result = executor.executeSync(flow);

        System.out.println("BATCH result: " + result.isSuccessful());
        System.out.println("Duration: " + result.getDuration());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    private TxFlow buildTwoStepFlow(String flowId) {
        return TxFlow.builder(flowId)
                .withDescription("Two-step flow for chaining mode demo")
                .addStep(FlowStep.builder("step-1")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .addStep(FlowStep.builder("step-2")
                        .dependsOn("step-1")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address3, Amount.ada(3))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();
    }

    public static void main(String[] args) {
        TxFlowChainingModesExample example = new TxFlowChainingModesExample();

        System.out.println("=== Chaining Modes Examples ===\n");

        System.out.println("--- SEQUENTIAL Mode ---");
        example.sequentialMode();

        System.out.println("\n--- PIPELINED Mode ---");
        example.pipelinedMode();

        System.out.println("\n--- BATCH Mode ---");
        example.batchMode();
    }
}
