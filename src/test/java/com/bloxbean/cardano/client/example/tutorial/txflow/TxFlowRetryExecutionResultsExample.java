package com.bloxbean.cardano.client.example.tutorial.txflow;

import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.example.tutorial.TutorialBase;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.txflow.BackoffStrategy;
import com.bloxbean.cardano.client.txflow.FlowStep;
import com.bloxbean.cardano.client.txflow.RetryPolicy;
import com.bloxbean.cardano.client.txflow.TxFlow;
import com.bloxbean.cardano.client.txflow.exec.FlowExecutor;
import com.bloxbean.cardano.client.txflow.exec.FlowHandle;
import com.bloxbean.cardano.client.txflow.result.FlowResult;
import com.bloxbean.cardano.client.txflow.result.FlowStepResult;

import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * Retry, Execution and Results — RetryPolicy, FlowExecutor, sync/async execution, FlowResult.
 *
 * <p>Retry policies handle transient failures (network errors, timeouts). They are distinct
 * from rollback handling which deals with chain reorganizations.</p>
 *
 * <p>Backoff strategies:
 * <ul>
 *   <li>FIXED: constant delay between retries</li>
 *   <li>LINEAR: delay increases linearly</li>
 *   <li>EXPONENTIAL (default): delay doubles each attempt</li>
 * </ul>
 *
 * @see <a href="http://localhost:3000/docs/preview/txflow/retry-execution-results">Retry, Execution &amp; Results Documentation</a>
 */
public class TxFlowRetryExecutionResultsExample extends TutorialBase {

    /**
     * Configure a custom retry policy for the flow executor.
     */
    @Test
    public void retryPolicyConfiguration() {
        // Custom retry policy
        RetryPolicy policy = RetryPolicy.builder()
                .maxAttempts(5)
                .backoffStrategy(BackoffStrategy.EXPONENTIAL)
                .initialDelay(Duration.ofSeconds(2))
                .maxDelay(Duration.ofSeconds(60))
                .retryOnTimeout(true)
                .retryOnNetworkError(true)
                .build();

        System.out.println("Custom RetryPolicy created:");
        System.out.println("  maxAttempts: " + policy.getMaxAttempts());
        System.out.println("  backoffStrategy: " + policy.getBackoffStrategy());

        // Factory methods
        RetryPolicy defaults = RetryPolicy.defaults();
        RetryPolicy noRetry = RetryPolicy.noRetry();
        System.out.println("Defaults maxAttempts: " + defaults.getMaxAttempts());
        System.out.println("NoRetry maxAttempts: " + noRetry.getMaxAttempts());
    }

    /**
     * Synchronous execution with full FlowResult inspection.
     */
    @Test
    public void syncExecutionWithResults() {
        TxFlow flow = TxFlow.builder("sync-demo")
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        FlowExecutor executor = FlowExecutor.create(backendService)
                .withDefaultRetryPolicy(RetryPolicy.defaults());

        FlowResult result = executor.executeSync(flow);

        // Inspect FlowResult
        System.out.println("Status: " + result.getStatus());
        System.out.println("Successful: " + result.isSuccessful());
        System.out.println("Duration: " + result.getDuration());
        System.out.println("Completed steps: " + result.getCompletedStepCount() + "/" + result.getTotalStepCount());
        System.out.println("Tx hashes: " + result.getTransactionHashes());

        // Inspect individual step results
        for (FlowStepResult stepResult : result.getStepResults()) {
            System.out.println("  Step '" + stepResult.getStepId() + "': " +
                    stepResult.getStatus() + " - tx: " + stepResult.getTransactionHash());
        }
    }

    /**
     * Asynchronous execution using FlowHandle for non-blocking monitoring.
     */
    @Test
    public void asyncExecution() throws Exception {
        TxFlow flow = TxFlow.builder("async-demo")
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(3))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        FlowExecutor executor = FlowExecutor.create(backendService);

        // Execute asynchronously - returns immediately
        FlowHandle handle = executor.execute(flow);

        // Monitor progress
        while (handle.isRunning()) {
            System.out.printf("Progress: %d/%d (step: %s)%n",
                    handle.getCompletedStepCount(),
                    handle.getTotalStepCount(),
                    handle.getCurrentStepId().orElse("none"));
            Thread.sleep(1000);
        }

        // Get the result
        FlowResult result = handle.await();
        System.out.println("Async result: " + result.isSuccessful());
        if (result.isSuccessful()) {
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Error handling — inspecting failed flows and individual step results.
     */
    @Test
    public void errorHandling() {
        TxFlow flow = TxFlow.builder("error-handling-demo")
                .addStep(FlowStep.builder("send-ada")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .build())
                .build();

        FlowExecutor executor = FlowExecutor.create(backendService);
        FlowResult result = executor.executeSync(flow);

        if (result.isFailed()) {
            // Identify which step failed
            result.getFailedStep().ifPresent(failedStep -> {
                System.err.printf("Step '%s' failed: %s%n",
                        failedStep.getStepId(),
                        failedStep.getError().getMessage());
            });

            // Check which steps succeeded before the failure
            for (FlowStepResult step : result.getStepResults()) {
                if (step.isSuccessful()) {
                    System.out.printf("Step '%s' succeeded: %s%n",
                            step.getStepId(), step.getTransactionHash());
                }
            }
        } else {
            System.out.println("Flow succeeded (no error to demonstrate)");
            System.out.println("Tx hashes: " + result.getTransactionHashes());
        }
    }

    /**
     * Per-step retry policy override.
     */
    @Test
    public void perStepRetryPolicy() {
        TxFlow flow = TxFlow.builder("per-step-retry")
                .addStep(FlowStep.builder("critical-step")
                        .withTxContext(builder -> builder
                                .compose(new Tx()
                                        .payToAddress(address2, Amount.ada(5))
                                        .from(address1))
                                .withSigner(SignerProviders.signerFrom(account1)))
                        .withRetryPolicy(RetryPolicy.builder()
                                .maxAttempts(10)
                                .initialDelay(Duration.ofSeconds(5))
                                .build())
                        .build())
                .build();

        // Default policy for the executor, but the step overrides it
        FlowExecutor executor = FlowExecutor.create(backendService)
                .withDefaultRetryPolicy(RetryPolicy.defaults());

        FlowResult result = executor.executeSync(flow);
        System.out.println("Per-step retry result: " + result.isSuccessful());
    }

    public static void main(String[] args) throws Exception {
        TxFlowRetryExecutionResultsExample example = new TxFlowRetryExecutionResultsExample();

        System.out.println("=== Retry, Execution & Results Examples ===\n");

        System.out.println("--- Retry Policy Configuration ---");
        example.retryPolicyConfiguration();

        System.out.println("\n--- Synchronous Execution with Results ---");
        example.syncExecutionWithResults();

        System.out.println("\n--- Async Execution ---");
        example.asyncExecution();

        System.out.println("\n--- Error Handling ---");
        example.errorHandling();

        System.out.println("\n--- Per-Step Retry Policy ---");
        example.perStepRetryPolicy();
    }
}
