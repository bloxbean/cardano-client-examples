package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.ScriptUtxoFinders;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusV2Script;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple script tx example, which
 * 1. locks funds to a script address
 * 2. unlocks funds from the script address
 */
public class AlwaysTrueScriptTest extends QuickTxBaseTest {

    @Test
    void alwaysTrueScript() throws ApiException, InterruptedException {
        PlutusV2Script plutusScript = PlutusV2Script.builder()
                .type("PlutusScriptV2")
                .cborHex("49480100002221200101")
                .build();

        String scriptAddress = AddressProvider.getEntAddress(plutusScript, Networks.testnet()).toBech32();
        BigInteger scriptAmt = new BigInteger("2479280");

        Random rand = new Random();
        int randInt = rand.nextInt();
        BigIntPlutusData plutusData =  new BigIntPlutusData(BigInteger.valueOf(randInt)); //any random number

        System.out.println("Trying to lock fund --------");
        lockFund(scriptAddress, scriptAmt, plutusData);

        System.out.println("Trying to unlock fund --------");
        //Script tx
        Optional<Utxo> optionalUtxo  = ScriptUtxoFinders.findFirstByInlineDatum(utxoSupplier, scriptAddress, plutusData);
        ScriptTx scriptTx = new ScriptTx()
                .collectFrom(optionalUtxo.get(), plutusData)
                .payToAddress(receiver1Addr, Amount.lovelace(scriptAmt))
                .attachSpendingValidator(plutusScript);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result1 = quickTxBuilder.compose(scriptTx)
                .feePayer(sender1Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .completeAndWait(System.out::println);

        System.out.println(result1.getResponse());
        assertTrue(result1.isSuccessful());
    }

    private void lockFund(String scriptAddress, BigInteger scriptAmt, BigIntPlutusData plutusData) {
        Tx tx = new Tx();
        tx.payToContract(scriptAddress, Amount.lovelace(scriptAmt), plutusData)
                .from(sender2Addr);

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender2))
                .completeAndWait(System.out::println);

        System.out.println(result.getResponse());
        checkIfUtxoAvailable(result.getValue(), scriptAddress);
    }
}
