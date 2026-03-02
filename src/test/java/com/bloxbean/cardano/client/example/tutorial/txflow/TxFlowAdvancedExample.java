package com.bloxbean.cardano.client.example.tutorial.txflow;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.txflow.ChainingMode;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.RetryPolicy;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.ConfirmationConfig;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.exec.FlowHandle;
import com.bloxbean.cardano.client.txflow.exec.FlowListener;
import com.bloxbean.cardano.client.txflow.exec.RollbackStrategy;
import com.bloxbean.cardano.client.txflow.exec.registry.FlowLifecycleListener;
import com.bloxbean.cardano.client.txflow.exec.registry.FlowRegistry;
import com.bloxbean.cardano.client.txflow.exec.registry.InMemoryFlowRegistry;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import com.bloxbean.cardano.client.txflow.result.FlowStatus;
import org.junit.jupiter.api.Test;

/**
 * Advanced Topics — FlowListener, FlowRegistry, virtual threads.
 *
 * <p>Covers:
 * <ul>
 *   <li>FlowListener for lifecycle event callbacks (logging, metrics, alerting)</li>
 *   <li>Composite listeners for multiple concerns</li>
 *   <li>FlowRegistry for centralized in-memory flow tracking</li>
 *   <li>FlowLifecycleListener for registry-level events</li>
 *   <li>Virtual threads with Java 21+</li>
 * </ul>
 *
 * <p>Note: Spring Boot integration and FlowStateStore (persistence) are conceptual patterns
 * demonstrated in the documentation but are not standalone-runnable in this example.</p>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/advanced">Advanced Topics Documentation</a>
 */
public class TxFlowAdvancedExample extends TutorialBase {

    /**
     * Execute a flow with a logging FlowListener.
     *
     * <p>FlowListener provides callbacks for flow, step, and transaction
     * lifecycle events.</p>
     */
    @Test
    public void withFlowListener() {
        FlowListener loggingListener = new FlowListener() {
            @Override
            public void onFlowStarted(TxFlow flow) {
                System.out.println("[LISTENER] Flow started: " + flow.getId());
            }

            @Override
            public void onFlowCompleted(TxFlow flow, FlowResult result) {
                System.out.println("[LISTENER] Flow completed: " + flow.getId()
                        + " with " + result.getTransactionHashes().size() + " transactions");
            }

            @Override
            public void onFlowFailed(TxFlow flow, FlowResult result) {
                System.out.println("[LISTENER] Flow failed: " + flow.getId()
                        + " - " + (result.getError() != null ? result.getError().getMessage() : "unknown"));
            }

            @Override
            public void onTransactionSubmitted(FlowStep step, String txHash) {
                System.out.println("[LISTENER] Step '" + step.getId() + "' submitted tx: " + txHash);
            }
        };

        TxFlow flow = buildSimpleFlow("listener-demo");

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withListener(loggingListener);

        FlowResult result = executor.executeSync(flow);
        System.out.println("Result: " + result.isSuccessful());
    }

    /**
     * Use FlowRegistry for centralized flow tracking.
     *
     * <p>FlowRegistry provides in-memory tracking of all active flows.</p>
     */
    @Test
    public void withFlowRegistry() {
        FlowRegistry registry = new InMemoryFlowRegistry();

        // Add lifecycle listener for registry-level events
        registry.addLifecycleListener(new FlowLifecycleListener() {
            @Override
            public void onFlowRegistered(String flowId, FlowHandle handle) {
                System.out.println("[REGISTRY] Flow registered: " + flowId);
            }

            @Override
            public void onFlowCompleted(String flowId, FlowHandle handle, FlowResult result) {
                System.out.println("[REGISTRY] Flow completed: " + flowId
                        + " status=" + result.getStatus());
            }

            @Override
            public void onFlowStatusChanged(String flowId, FlowHandle handle,
                                            FlowStatus oldStatus, FlowStatus newStatus) {
                System.out.println("[REGISTRY] Flow " + flowId
                        + " status: " + oldStatus + " -> " + newStatus);
            }
        });

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withRegistry(registry);

        TxFlow flow = buildSimpleFlow("registry-demo");
        FlowResult result = executor.executeSync(flow);

        // Query the registry
        System.out.println("Active flows: " + registry.activeCount());
        System.out.println("Total flows: " + registry.size());
        System.out.println("Result: " + result.isSuccessful());
    }

    /**
     * Composite listener: combine multiple listeners for different concerns.
     */
    @Test
    public void compositeListener() {
        FlowListener listener1 = new FlowListener() {
            @Override
            public void onFlowStarted(TxFlow flow) {
                System.out.println("[METRICS] Flow started: " + flow.getId());
            }
        };

        FlowListener listener2 = new FlowListener() {
            @Override
            public void onFlowCompleted(TxFlow flow, FlowResult result) {
                System.out.println("[AUDIT] Flow completed: " + flow.getId());
            }
        };

        FlowListener combined = FlowListener.composite(listener1, listener2);

        TxFlow flow = buildSimpleFlow("composite-demo");
        FlowExecutor executor = FlowExecutor.create(backendService)
                .withListener(combined);

        FlowResult result = executor.executeSync(flow);
        System.out.println("Result: " + result.isSuccessful());
    }

    /**
     * Full executor configuration with all options.
     */
    @Test
    public void fullExecutorConfiguration() {
        FlowExecutor executor = FlowExecutor.create(backendService)
                .withChainingMode(ChainingMode.SEQUENTIAL)
                .withConfirmationConfig(ConfirmationConfig.devnet())
                .withRollbackStrategy(RollbackStrategy.REBUILD_ENTIRE_FLOW)
                .withDefaultRetryPolicy(RetryPolicy.defaults())
                .withListener(new FlowListener() {
                    @Override
                    public void onFlowStarted(TxFlow flow) {
                        System.out.println("Flow started: " + flow.getId());
                    }

                    @Override
                    public void onFlowCompleted(TxFlow flow, FlowResult result) {
                        System.out.println("Flow completed: " + flow.getId());
                    }
                })
                .withRegistry(new InMemoryFlowRegistry())
                .withTxInspector(tx -> System.out.println("Built transaction: " + tx));

        TxFlow flow = buildSimpleFlow("full-config-demo");
        FlowResult result = executor.executeSync(flow);
        System.out.println("Full config result: " + result.isSuccessful());
    }

    private TxFlow buildSimpleFlow(String flowId) {
        return TxFlow.builder(flowId)
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();
    }

    public static void main(String[] args) {
        TxFlowAdvancedExample example = new TxFlowAdvancedExample();

        System.out.println("=== TxFlow Advanced Examples ===\n");

        System.out.println("--- With FlowListener ---");
        example.withFlowListener();

        System.out.println("\n--- With FlowRegistry ---");
        example.withFlowRegistry();

        System.out.println("\n--- Composite Listener ---");
        example.compositeListener();

        System.out.println("\n--- Full Executor Configuration ---");
        example.fullExecutorConfiguration();
    }
}
