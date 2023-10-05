package com.bloxbean.cardano.client.example.timelock;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.Block;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.script.*;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.client.util.Tuple;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.ChangeOutputAdjustments.adjustChangeOutput;
import static com.bloxbean.cardano.client.function.helper.FeeCalculators.feeCalculator;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example : Create a enterprise address(no staking part) from a Time-lock Native script which is created using a regular account's key as verification key
 * - Transfer fund to the newly generated script address
 * - After unlocked slot no, transfer fund from script address to another address with signer as original payment account.
 */
public class TimeLockScriptWithRegularAddress extends BaseTest {
    String paymentAccMnemonic = "couple pulse brother awake panther garden elder scheme erase bone close estate token receive blossom squirrel report field pioneer project behind abstract enable legend";
    long targetUnlockSlot = 55111682;

    @Test
    public void run() throws Exception {
        TimeLockScriptWithRegularAddress timeLockScriptWithRegularAddress = new TimeLockScriptWithRegularAddress();

        //Transfer fund to time-lock script address
        timeLockScriptWithRegularAddress.transferToScriptAddress();

        //Transfer fund from time-lock script address to another address
        timeLockScriptWithRegularAddress.transferFromScriptAddress();
    }

    public Tuple<String, NativeScript> createScriptAddress() throws Exception {
        Account account = new Account(Networks.testnet(), paymentAccMnemonic);

        //Create Payment part
        ScriptPubkey scriptPubkey = ScriptPubkey.create(VerificationKey.create(account.publicKeyBytes()));
        RequireTimeAfter requireTimeAfter = new RequireTimeAfter(targetUnlockSlot);

        ScriptAll scriptAll = new ScriptAll()
                .addScript(requireTimeAfter)
                .addScript(new ScriptAtLeast(1).addScript(scriptPubkey));

        //Address with only payment part
        Address entAddress = AddressProvider.getEntAddress(scriptAll, Networks.testnet());
        System.out.println(entAddress.toBech32());

        return new Tuple(entAddress.toBech32(), scriptAll);

    }

    public void transferToScriptAddress() throws Exception {
        Account sender = new Account(Networks.testnet(), paymentAccMnemonic);
        String senderAddress = sender.baseAddress();

        Tuple<String, NativeScript> scriptTuple = createScriptAddress();
        String scriptAddress = scriptTuple._1;
        NativeScript script = scriptTuple._2;

        System.out.println(JsonUtil.getPrettyJson(script));

        Output output = Output.builder()
                .address(scriptAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(6))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(senderAddress, senderAddress))
                .andThen(feeCalculator(senderAddress, 1))
                .andThen(adjustChangeOutput(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(sender));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    public void transferFromScriptAddress() throws Exception {
        Account signingAcc = new Account(Networks.testnet(), paymentAccMnemonic);
        Tuple<String, NativeScript> scriptTuple = createScriptAddress();
        String scriptAddress = scriptTuple._1;
        NativeScript script = scriptTuple._2;

        System.out.println(JsonUtil.getPrettyJson(script));

        String receiverAddress = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";

        Output output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1.1))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(scriptAddress, scriptAddress))
                .andThen((txBuilderContext, transaction) -> { //Add script witness and validation start interval
                    //Set validity start interval. Should be after target slot. Without that it will not work
                    transaction.getBody().setValidityStartInterval(targetUnlockSlot + 1);

                    //Set script witness
                    transaction.getWitnessSet().setNativeScripts(List.of(script));
                })
                .andThen(feeCalculator(scriptAddress, 1))
                .andThen(adjustChangeOutput(scriptAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(signingAcc));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    protected long getCurrentSlot() throws ApiException {
        Block block = blockService.getLatestBlock().getValue();
        long slot = block.getSlot();
        return slot;
    }

    public void getSlot() throws ApiException {
        System.out.println("Expected slot : " + targetUnlockSlot);
        System.out.println("Current slot: " + getCurrentSlot());
    }

}
