package com.bloxbean.cardano.client.example.timelock;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.model.Block;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.function.*;
import com.bloxbean.cardano.client.function.helper.AuxDataProviders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.metadata.Metadata;
import com.bloxbean.cardano.client.spec.Script;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.cert.StakeCredential;
import com.bloxbean.cardano.client.transaction.spec.cert.StakeDelegation;
import com.bloxbean.cardano.client.transaction.spec.cert.StakePoolId;
import com.bloxbean.cardano.client.transaction.spec.cert.StakeRegistration;
import com.bloxbean.cardano.client.transaction.spec.script.*;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.client.util.Triple;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.function.helper.ChangeOutputAdjustments.adjustChangeOutput;
import static com.bloxbean.cardano.client.function.helper.FeeCalculators.feeCalculator;
import static com.bloxbean.cardano.client.function.helper.InputBuilders.createFromSender;
import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Example: Create a base address which consists of
 *  - payment part from a time-lock native script using regular account's key as verification key
 *  - script part from a multi-sig native script created from verification keys of 3 different accounts
 *
 * - Transfer fund to the newly created base address.
 * - Register the stake key using one of the staking account
 * - Delegate to a pool using one of the staking account
 * - Transfer fund from the newly created based address using original payment account after target unlock slot.
 */
public class TimeLockPaymentScriptWithRegularAddressWithMultisigStakingCredential extends BaseTest {
    String paymentAccMnemonic = "aisle into aware where broken during title heart sample salt kid betray income faint allow poverty exhibit present rifle extend yellow economy jungle weird";

    String stakeAccMnemonic1 = "sibling dynamic tobacco already noise magic name name fiber blind clever control woman motion puzzle wealth valley edit child early dice invest party siren";
    String stakeAccMnemonic2 = "athlete edit vault heart race beauty shy desk rally bleak wrist phrase toast expand pull broccoli phone twin unveil flock dutch raven attitude sleep";
    String stakeAccMnemonic3 = "canvas sight sample gentle pipe reject movie goose trim game shield hello easy note unique exotic weather sunny mansion boat order omit smart atom";

    Account paymentAccount;

    Account stakeAccount1;
    Account stakeAccount2;
    Account stakeAccount3;

    long targetUnlockSlot = 55111682;

    @Test
    public void run() throws Exception {
        TimeLockPaymentScriptWithRegularAddressWithMultisigStakingCredential timeLockScriptWithRegularAddressWithStaking
                = new TimeLockPaymentScriptWithRegularAddressWithMultisigStakingCredential();

        //#### Transfer from the regular payment address to a timelock script address (with payment address's key hash)
        timeLockScriptWithRegularAddressWithStaking.transferToScriptBaseAddress();

        //#### Register multi-sig staking key
        timeLockScriptWithRegularAddressWithStaking.registerStakeKey();

        //#### Delegate to a pool using multi-sign staking credential
        timeLockScriptWithRegularAddressWithStaking.delegateToPool();

        //#### Transfer fund from the timelock script address to another address after timelock
        timeLockScriptWithRegularAddressWithStaking.transferFromScriptBaseAddress();
    }

    public TimeLockPaymentScriptWithRegularAddressWithMultisigStakingCredential() {
        paymentAccount = new Account(Networks.testnet(), paymentAccMnemonic);
        stakeAccount1 = new Account(Networks.testnet(), stakeAccMnemonic1);
        stakeAccount2 = new Account(Networks.testnet(), stakeAccMnemonic2);
        stakeAccount3 = new Account(Networks.testnet(), stakeAccMnemonic3);
    }

    public Triple<String, Script, Script> createScriptAddress() throws Exception {
        paymentAccount = new Account(Networks.testnet(), paymentAccMnemonic);

        //Create Payment part
        ScriptPubkey scriptPubkey = ScriptPubkey.create(VerificationKey.create(paymentAccount.publicKeyBytes()));
        RequireTimeAfter requireTimeAfter = new RequireTimeAfter(targetUnlockSlot);

        Script paymentScript = new ScriptAll()
                .addScript(requireTimeAfter)
                .addScript(new ScriptAtLeast(1).addScript(scriptPubkey));

        //Create a multi-sig staking credential with 3 keys (from regular accounts) and at least 2 required signatures
        NativeScript stakeScript1 = ScriptPubkey.create(VerificationKey.create(stakeAccount1.publicKeyBytes()));
        NativeScript stakeScript2 = ScriptPubkey.create(VerificationKey.create(stakeAccount2.publicKeyBytes()));
        NativeScript stakeScript3 = ScriptPubkey.create(VerificationKey.create(stakeAccount3.publicKeyBytes()));

        Script stakingScript = new ScriptAtLeast(2)
                .addScript(stakeScript1)
                .addScript(stakeScript2)
                .addScript(stakeScript3);
        Address baseAddress = AddressProvider.getBaseAddress(paymentScript, stakingScript, Networks.testnet());
        System.out.println(baseAddress.toBech32());

        return new Triple<>(baseAddress.toBech32(), paymentScript, stakingScript);
    }

    public void registerStakeKey() throws Exception {
        Triple<String, Script, Script> triple = createScriptAddress();
        NativeScript stakingScript = (NativeScript) triple._3;

        String feePayingAddress = stakeAccount1.baseAddress();

        //protocol params -- Get deposit amount
        String depositStr = backendService.getEpochService().getProtocolParameters().getValue().getKeyDeposit();
        BigInteger deposit = new BigInteger(depositStr);

        //Find utxo for fee from one of the stake cred account. Fee will be paid
        UtxoSelectionStrategy selectionStrategy = new DefaultUtxoSelectionStrategyImpl(new DefaultUtxoSupplier(backendService.getUtxoService()));
        List<Utxo> utxos = selectionStrategy.selectUtxos(feePayingAddress, LOVELACE, deposit.add(adaToLovelace(2)), Collections.EMPTY_SET);

        if (utxos == null || utxos.size() == 0)
            throw new RuntimeException("No utxo found");

        //-- Stake key registration
        StakeRegistration stakeRegistration = new StakeRegistration(StakeCredential.fromScript(stakingScript));

        TxOutputBuilder txOutBuilder = (context, outputs) -> {
        };

        TxBuilder builder = txOutBuilder
                .buildInputs(InputBuilders.createFromUtxos(utxos, feePayingAddress)) //Fee and deposit will be paid by payment acc
                .andThen((context, txn) -> {
                    txn.getBody().getCerts().add(stakeRegistration);
                })
                .andThen((context, txn) -> { //Update change output
                    //Remove stake registration deposit amount from change output
                    txn.getBody().getOutputs().stream().filter(transactionOutput -> feePayingAddress.equals(transactionOutput.getAddress()))
                            .findFirst()
                            .ifPresent(transactionOutput -> transactionOutput.getValue().setCoin(transactionOutput.getValue().getCoin().subtract(deposit)));
                })
                .andThen(feeCalculator(feePayingAddress, 1))
                .andThen(adjustChangeOutput(feePayingAddress, 1));

        TxSigner signer = SignerProviders.signerFrom(stakeAccount1);

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(builder, signer);

        Result<String> result = backendService.getTransactionService().submitTransaction(signedTransaction.serialize());

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    void delegateToPool() throws Exception {
        Triple<String, Script, Script> triple = createScriptAddress();
        String baseAddress = triple._1;
        Script paymentScript = triple._2;
        NativeScript stakingScript = (NativeScript) triple._3;

        String feePayingAddress = stakeAccount1.baseAddress();

        UtxoSelectionStrategy selectionStrategy = new DefaultUtxoSelectionStrategyImpl(new DefaultUtxoSupplier(backendService.getUtxoService()));
        List<Utxo> utxos = selectionStrategy.selectUtxos(feePayingAddress, LOVELACE, adaToLovelace(2), Collections.EMPTY_SET);

        if (utxos == null || utxos.size() == 0)
            throw new RuntimeException("No utxo found");

        //-- Stake key delegation
        StakePoolId stakePoolId = StakePoolId.fromBech32PoolId("pool18yslg3q320jex6gsmetukxvzm7a20qd90wsll9anlkrfua38flr");
        StakeDelegation stakeDelegation = new StakeDelegation(StakeCredential.fromScript(stakingScript), stakePoolId);
        Metadata metadata = MessageMetadata.create()
                .add("Stake Delegation using cardano-client-lib")
                .add("https://github.com/bloxbean/cardano-client-lib");

        TxOutputBuilder txOutBuilder = (context, outputs) -> {
        };

        TxBuilder builder = txOutBuilder
                .buildInputs(InputBuilders.createFromUtxos(utxos, feePayingAddress))
                .andThen((context, txn) -> {
                    txn.getBody().getCerts().add(stakeDelegation);
                })
                .andThen((txBuilderContext, txn) -> {
                    //Add staking key script to witnessset
                    txn.getWitnessSet().setNativeScripts(List.of(stakingScript));
                })
                .andThen(AuxDataProviders.metadataProvider(metadata))
                .andThen(feeCalculator(feePayingAddress, 2))
                .andThen(adjustChangeOutput(feePayingAddress, 2));

        TxSigner signer = SignerProviders.signerFrom(stakeAccount1)
                .andThen(signerFrom(stakeAccount2));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(builder, signer);

        Result<String> result = backendService.getTransactionService().submitTransaction(signedTransaction.serialize());

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    public void transferToScriptBaseAddress() throws Exception {
        String senderAddress = paymentAccount.baseAddress();

        Triple<String, Script, Script> scriptTuple = createScriptAddress();
        String scriptAddress = scriptTuple._1;

        Output output = Output.builder()
                .address(scriptAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(30))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(senderAddress, senderAddress))
                .andThen(feeCalculator(senderAddress, 1))
                .andThen(adjustChangeOutput(senderAddress, 1));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(paymentAccount));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    public void transferFromScriptBaseAddress() throws Exception {
        Account signingAcc = new Account(Networks.testnet(), paymentAccMnemonic);
        Triple<String, Script, Script> scriptTuple = createScriptAddress();
        String scriptAddress = scriptTuple._1;
        NativeScript script = (NativeScript)scriptTuple._2;

        System.out.println(JsonUtil.getPrettyJson(script));

        String receiverAddress = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";

        Output output = Output.builder()
                .address(receiverAddress)
                .assetName(LOVELACE)
                .qty(adaToLovelace(1.1))
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(createFromSender(scriptAddress, scriptAddress))
                .andThen((txBuilderContext, transaction) -> { //Add script witness and current slot
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

    private long getCurrentSlot() throws ApiException {
        Block block = blockService.getLatestBlock().getValue();
        long slot = block.getSlot();
        return slot;
    }

    public void getSlot() throws ApiException {
        System.out.println("Expected slot : " + targetUnlockSlot);
        System.out.println("Current slot: " + getCurrentSlot());
    }
}
