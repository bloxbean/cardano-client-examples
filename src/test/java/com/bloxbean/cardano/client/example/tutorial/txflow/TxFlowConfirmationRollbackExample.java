package com.bloxbean.cardano.client.example.tutorial.txflow;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.ConfirmationConfig;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.exec.RollbackStrategy;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import org.junit.jupiter.api.Test;

/**
 * Confirmation and Rollback — confirmation tracking, status lifecycle, and rollback strategies.
 *
 * <p>Confirmation status lifecycle:
 * <pre>
 * SUBMITTED -> IN_BLOCK -> CONFIRMED
 *                 |
 *            ROLLED_BACK
 * </pre>
 *
 * <p>Rollback strategies:
 * <ul>
 *   <li><b>FAIL_IMMEDIATELY (Default):</b> Safest — application decides how to handle failure</li>
 *   <li><b>NOTIFY_ONLY:</b> Continue monitoring, transaction may be re-included</li>
 *   <li><b>REBUILD_FROM_FAILED:</b> Rebuild the rolled-back step with fresh UTXOs</li>
 *   <li><b>REBUILD_ENTIRE_FLOW:</b> Re-execute the entire flow from step 1</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/confirmation-rollback">Confirmation &amp; Rollback Documentation</a>
 */
public class TxFlowConfirmationRollbackExample extends TutorialBase {

    /**
     * Execute a flow with devnet confirmation tracking.
     *
     * <p>Uses {@code ConfirmationConfig.devnet()} preset:
     * <ul>
     *   <li>minConfirmations: 3</li>
     *   <li>checkInterval: 1s</li>
     *   <li>timeout: 5min</li>
     * </ul>
     */
    @Test
    public void withDevnetConfirmation() {
        TxFlow flow = TxFlow.builder("confirmation-demo")
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withConfirmationConfig(ConfirmationConfig.devnet());

        FlowResult result = executor.executeSync(flow);

        System.out.println("With confirmation tracking result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hash: " + result.getTransactionHashes());
            System.out.println("Duration (with confirmations): " + result.getDuration());
        }
    }

    /**
     * Execute a flow with confirmation tracking and REBUILD_ENTIRE_FLOW rollback strategy.
     *
     * <p>If a chain reorganization invalidates a transaction, the executor
     * will automatically re-execute the entire flow from step 1.</p>
     */
    @Test
    public void withRollbackStrategy() {
        TxFlow flow = TxFlow.builder("rollback-demo")
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

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withConfirmationConfig(ConfirmationConfig.devnet())
                .withRollbackStrategy(RollbackStrategy.REBUILD_ENTIRE_FLOW);

        FlowResult result = executor.executeSync(flow);

        System.out.println("With rollback strategy result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Configure a custom ConfirmationConfig.
     */
    @Test
    public void customConfirmationConfig() {
        ConfirmationConfig config = ConfirmationConfig.builder()
                .minConfirmations(3)
                .checkInterval(java.time.Duration.ofSeconds(2))
                .timeout(java.time.Duration.ofMinutes(5))
                .maxRollbackRetries(3)
                .build();

        System.out.println("Custom ConfirmationConfig created:");
        System.out.println("  minConfirmations: " + config.getMinConfirmations());
        System.out.println("  checkInterval: " + config.getCheckInterval());
        System.out.println("  timeout: " + config.getTimeout());
        System.out.println("  maxRollbackRetries: " + config.getMaxRollbackRetries());
    }

    public static void main(String[] args) {
        TxFlowConfirmationRollbackExample example = new TxFlowConfirmationRollbackExample();

        System.out.println("=== Confirmation & Rollback Examples ===\n");

        System.out.println("--- Custom Confirmation Config ---");
        example.customConfirmationConfig();

        System.out.println("\n--- With Devnet Confirmation ---");
        example.withDevnetConfirmation();

        System.out.println("\n--- With Rollback Strategy ---");
        example.withRollbackStrategy();
    }
}
