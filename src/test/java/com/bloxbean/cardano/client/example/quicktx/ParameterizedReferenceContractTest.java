package com.bloxbean.cardano.client.example.quicktx;

/**
 * This test demonstrates how to work with parameterized Plutus scripts and reference scripts on Cardano.
 *
 * The test performs the following operations:
 * 1. Takes a pre-compiled Aiken/Plutus smart contract and applies parameters to it
 * 2. Deploys the parameterized script as a reference script in a UTXO
 * 3. Locks funds at the script address (without including the script in the transaction)
 * 4. Unlocks funds from the script address using the reference script (avoiding script inclusion in spending transaction)
 *
 * Key concepts demonstrated:
 * - Parameterized scripts: Applying runtime parameters to compiled Plutus code
 * - Reference scripts: Storing scripts on-chain in UTXOs for reuse without including them in every transaction
 * - Script locking/unlocking: Sending funds to and redeeming from script addresses
 * - CBOR encoding: Working with single vs double-encoded CBOR hex representations
 *
 * This approach reduces transaction size and fees by storing the script once and referencing it in future transactions.
 */

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.UtxoSupplier;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.blueprint.PlutusBlueprintUtil;
import com.bloxbean.cardano.client.plutus.blueprint.model.PlutusVersion;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.util.JsonUtil;
import org.junit.jupiter.api.Test;
import scalus.bloxbean.ScalusScriptUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParameterizedReferenceContractTest {

    private String aikenCompiledCode = "589001010032323232323223225333004323232323253330093370e900118051baa0011323232533300c3370e900018069baa005132325333011301300213371e6eb8c048c040dd50038060b1bae3011001300e375400a2c601e6020004601c00260166ea800458c030c034008c02c004c02c008c024004c018dd50008a4c26cac6eb80055cd2ab9d5573caae7d5d0aba201";

    private BackendService backendService = new BFBackendService("http://localhost:8080/api/v1/", "dummy key");
    private String mnemonic = "test test test test test test test test test test test test test test test test test test test test test test test sauce";

    Account account = new Account(Networks.testnet(), mnemonic);
    String accountAddr = account.baseAddress();
    QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
    UtxoSupplier utxoSupplier = new DefaultUtxoSupplier(backendService.getUtxoService());

    @Test
    public void deployLockAndUnlock() {
        // Deploy reference utxos
        String msg = System.currentTimeMillis() + "";

        // Apply parameter to the compiled code using AikenScriptUtil
        // AikenScriptUtil's applyParamToScript method takes single encoded cbor hex
        // and returns single encoded cbor hex. So, we need to use PlutusBlueprintUtil to convert it to PlutusScript
        // which contains double encoded cbor hex
        /*
        ListPlutusData params = ListPlutusData.of(BytesPlutusData.of(msg));
        String compiledCode = AikenScriptUtil.applyParamToScript(params, aikenCompiledCode);
        // convert Aiken compiled code to PlutusScript
        PlutusScript script = PlutusBlueprintUtil.getPlutusScriptFromCompiledCode(compiledCode, PlutusVersion.v3);
        */

        // Apply parameter to the compiled code using ScalusUtil
        ListPlutusData params = ListPlutusData.of(BytesPlutusData.of(msg));
        PlutusScript paramScript = PlutusBlueprintUtil.getPlutusScriptFromCompiledCode(aikenCompiledCode, PlutusVersion.v3);

        // Scalus util starts
        // Scalus's Util's applyParamToScript method expects double encoded cbor hex
        // So convert the aiken compiled code to PlutusScript which contains double encoded cbor hex
        String finalScriptCborHex = ScalusScriptUtils.applyParamsToScript(paramScript.getCborHex(), params);

        // Create PlutusScript from finalScriptCborHex as it's already double encoded
        PlutusScript script = PlutusV3Script.builder()
                .cborHex(finalScriptCborHex)
                .build();
        // Scalus util ends

        String scriptAddress = AddressProvider.getEntAddress(script, Networks.testnet()).toBech32();

        /* Create Utxo with Reference Script */
        // Create a reference script UTXO
        Tx refTx = new Tx()
                .payToAddress(scriptAddress, Amount.ada(1), script)
                .from(accountAddr);

        var refTxResult = quickTxBuilder.compose(refTx)
                .withSigner(SignerProviders.signerFrom(account))
                .completeAndWait(msg1 -> System.out.println(msg1));

        System.out.println("Reference Script UTXO creation refTxResult: " + refTxResult);
        assertTrue(refTxResult.isSuccessful());

        /* Lock some fund with the reference script */
        Tx scriptLockTx = new Tx()
                .payToAddress(scriptAddress, Amount.ada(2))
                .from(accountAddr);

        var lockResult = quickTxBuilder.compose(scriptLockTx)
                .withSigner(SignerProviders.signerFrom(account))
                .completeAndWait(msg1 -> System.out.println(msg1));

        System.out.println("Script Lock Tx refTxResult: " + lockResult);
        assertTrue(lockResult.isSuccessful());

        /* Unlock fund from script address using reference script */
        var scriptLockedUtxos = utxoSupplier.getTxOutput(lockResult.getValue(), 0).get();
        var redeemer = ConstrPlutusData.of(0, BytesPlutusData.of(msg));
        ScriptTx scriptUnlockTx = new ScriptTx()
                .collectFrom(scriptLockedUtxos, redeemer)
                .payToAddress(accountAddr, Amount.ada(1))
                .readFrom(refTxResult.getValue(), 0)
                .withChangeAddress(accountAddr);

        var unlockResult = quickTxBuilder.compose(scriptUnlockTx)
                .withSigner(SignerProviders.signerFrom(account))
                .feePayer(accountAddr)
                .withTxInspector(transaction -> {
                    System.out.println(JsonUtil.getPrettyJson(transaction));
                })
                .ignoreScriptCostEvaluationError(false)
                .completeAndWait(msg1 -> System.out.println(msg1));

        System.out.println("Script Unlock Tx refTxResult: " + unlockResult);

        assertTrue(unlockResult.isSuccessful());
    }

}
