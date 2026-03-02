package com.bloxbean.cardano.client.example.tutorial;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.util.JsonUtil;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Optional;

/**
 * Base class for tutorial examples configured for Yaci DevKit.
 *
 * <p>Before running any tutorial example, ensure that Yaci DevKit is running
 * on localhost with the default ports (API on 8080, Admin on 10000).</p>
 *
 * <p>Start Yaci DevKit with:
 * <pre>
 *   yaci-cli:> create-node -o --start
 * </pre>
 * </p>
 *
 * @see <a href="http://localhost:3000/docs">Cardano Client Lib Documentation</a>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TutorialBase {

    // Yaci DevKit API URL (Blockfrost-compatible)
    protected static final String YACI_API_URL = "http://localhost:8080/api/v1/";

    // Default mnemonic for Yaci DevKit pre-funded accounts
    protected static final String DEFAULT_MNEMONIC =
            "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    protected BackendService backendService;

    // Pre-funded accounts from Yaci DevKit
    protected Account account1;
    protected Account account2;
    protected Account account3;

    protected String address1;
    protected String address2;
    protected String address3;

    @BeforeAll
    void checkDevKitAvailability() {
        try {
            java.net.URL url = new java.net.URL(YACI_API_URL + "blocks/latest");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            Assumptions.assumeTrue(code >= 200 && code < 500,
                "Yaci DevKit not available at " + YACI_API_URL);
        } catch (Exception e) {
            Assumptions.assumeTrue(false,
                "Yaci DevKit not available at " + YACI_API_URL + ": " + e.getMessage());
        }
    }

    public TutorialBase() {
        backendService = new BFBackendService(YACI_API_URL, "Dummy");

        account1 = new Account(Networks.testnet(), DEFAULT_MNEMONIC, 0);
        account2 = new Account(Networks.testnet(), DEFAULT_MNEMONIC, 1);
        account3 = new Account(Networks.testnet(), DEFAULT_MNEMONIC, 2);

        address1 = account1.baseAddress();
        address2 = account2.baseAddress();
        address3 = account3.baseAddress();
    }

    /**
     * Wait for a transaction to be confirmed on chain.
     */
    protected void waitForTransaction(Result<String> result) {
        if (!result.isSuccessful()) {
            System.err.println("Transaction submission failed: " + result.getResponse());
            return;
        }

        String txHash = result.getValue();
        System.out.println("Waiting for transaction " + txHash + " to be confirmed...");

        try {
            int count = 0;
            while (count < 60) {
                Result<TransactionContent> txnResult =
                        backendService.getTransactionService().getTransaction(txHash);
                if (txnResult.isSuccessful()) {
                    System.out.println("Transaction confirmed: " + txHash);
                    System.out.println(JsonUtil.getPrettyJson(txnResult.getValue()));
                    return;
                }
                count++;
                Thread.sleep(1000);
            }
            System.err.println("Transaction not confirmed after 60 attempts: " + txHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if a UTXO is available at an address.
     */
    protected boolean checkIfUtxoAvailable(String txHash, String address) {
        int count = 0;
        while (count < 30) {
            List<Utxo> utxos = new DefaultUtxoSupplier(backendService.getUtxoService()).getAll(address);
            Optional<Utxo> utxo = utxos.stream()
                    .filter(u -> u.getTxHash().equals(txHash))
                    .findFirst();
            if (utxo.isPresent()) {
                System.out.println("UTXO found from tx " + txHash);
                return true;
            }
            count++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
