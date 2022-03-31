package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.common.CardanoConstants.ONE_ADA;

public class TransferFromMultisigScriptAddress extends BaseTest {

    public TransferFromMultisigScriptAddress() {

    }

    public void transfer() throws ApiException, CborSerializationException, CborDeserializationException, IOException {
        String multisigScriptAddr = "addr_test1wzchaw4vxmmpws44ffh99eqzmlg6wr3swg36pqug8xn20ygxgqher";
        String receiverAddress = "addr_test1qr2y2yf2lwj0xn2nrhmyqe26t52twp06cp4lm2r62undytvj5ggkj79y993ds6645ewwfus90su92j554u2294wtm93s25m8cz";

        BigInteger amountToTransfer = ONE_ADA.multiply(BigInteger.valueOf(7));

        //Find required utxos
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(multisigScriptAddr, LOVELACE,
                amountToTransfer.add(ONE_ADA.multiply(BigInteger.valueOf(2))), Collections.EMPTY_SET); //transfer amount + 2 ADA to cover fee and min ada

        //Find utxos first and then create inputs
        List<TransactionInput> inputs = new ArrayList<>();
        for (Utxo utxo : utxos) {
            TransactionInput input = TransactionInput.builder()
                    .transactionId(utxo.getTxHash())
                    .index(utxo.getOutputIndex()).build();
            inputs.add(input);
        }

        TransactionOutput output = TransactionOutput
                .builder()
                .address(receiverAddress)
                .value(new Value(amountToTransfer, null))
                .build();

        TransactionOutput change = TransactionOutput
                .builder()
                .address(multisigScriptAddr)
                .value(Value.builder()
                        .coin(BigInteger.ZERO).build()
                )
                .build();
        utxos.forEach(utxo -> copyUtxoValuesToChangeOutput(change, utxo));

        List<TransactionOutput> outputs = Arrays.asList(output, change);

        //Create the transaction body with dummy fee
        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .ttl(getTtl())
                .fee(BigInteger.valueOf(170000))
                .build();

        //script file
        //TODO Not able to deserialize from file through ObjectMapper as there is no default constructor in ScriptAtLeast class.
        // This will be fixed in future release
//        InputStream ins = this.getClass().getClassLoader().getResourceAsStream("multisig.script");
//        ScriptAtLeast multiSigScript = objectMapper.readValue(ins, ScriptAtLeast.class);

        ScriptAtLeast multiSigScript = getMultisigScript();
        TransactionWitnessSet witnessSet = new TransactionWitnessSet();
        witnessSet.getNativeScripts().add(multiSigScript);

        Transaction transaction = Transaction.builder()
                .body(body)
                .witnessSet(witnessSet)
                .build();

        //Signers
        String signerAcc1Mnemonic = "around submit turtle canvas friend remind push vehicle debate drop blouse piece obvious crane tone avoid aspect power milk eye brand cradle tide wrist";
        String signerAcc2Mnemonic = "prison glide olympic diamond rib payment crucial ski vintage example dinner matrix cruise upper antenna surge drink divorce brother half figure skate jar stand";

        Account signer1 = new Account(Networks.testnet(), signerAcc1Mnemonic);
        Account signer2 = new Account(Networks.testnet(), signerAcc2Mnemonic);

        //Sign the transaction. so that we get the actual size of the transaction to calculate the fee
        Transaction signTxn = signer1.sign(transaction); //cbor encoded bytes in Hex format
        signTxn = signer2.sign(signTxn);

        BigInteger fee = feeCalculationService.calculateFee(signTxn);

        //Update fee in transaction body
        body.setFee(fee);

        //Final change amount after amountToTransfer + fee
        BigInteger finalChangeAmt = change.getValue().getCoin().subtract(amountToTransfer.add(fee));
        change.getValue().setCoin(finalChangeAmt);

        //TODO Sign the transaction by signer1 to add witness and then sign the signed transaction again by signer2 to append signer2's witness
        String finalSignedTxn = signTransactionOneAfterAnother(transaction, signer1, signer2);

        //TODO Uncomment if want to sign independently and then assemble. Orignal transaction can be serialized and distribute to signing party
//        String finalSignedTxn = signOriginalTransactionBySigner1AndSigner2AndThenAssemble(transaction, signer1, signer2);

        Result<String> result = transactionService.submitTransaction(HexUtil.decodeHexString(finalSignedTxn));

        System.out.println(result);
        if (result.isSuccessful())
            waitForTransactionHash(result);
        else
            System.out.println("Transaction failed : " + result);
    }

    /**
     * Sign the transaction first by signer1 and then sign the signed transactioin by signer2
     *
     * @param transaction
     * @param signer1
     * @param signer2
     * @return
     * @throws CborSerializationException
     */
    private String signTransactionOneAfterAnother(Transaction transaction, Account signer1, Account signer2) throws CborSerializationException {
        Transaction signedTxn = signer1.sign(transaction);
        signedTxn = signer2.sign(signedTxn);

        return signedTxn.serializeToHex();
    }

    private String signOriginalTransactionBySigner1AndSigner2AndThenAssemble(Transaction transaction, Account signer1, Account signer2)
            throws CborSerializationException, CborDeserializationException {
        //Signer 1 sign the transaction
        Transaction signer1Txn = signer1.sign(transaction);

        //Signer 2 sign the transaction
        Transaction signer2Txn = signer2.sign(transaction);

        //Get witness from signer1 signed txn and signer2 signed transaction and add to witnesses
        transaction.setValid(true); //Set the transaction validity to true.
        transaction.getWitnessSet().getVkeyWitnesses().add(signer1Txn.getWitnessSet().getVkeyWitnesses().get(0));
        transaction.getWitnessSet().getVkeyWitnesses().add(signer2Txn.getWitnessSet().getVkeyWitnesses().get(0));

        return transaction.serializeToHex();
    }

    public ScriptAtLeast getMultisigScript() {
        ScriptPubkey key1 = new ScriptPubkey();
        key1.setKeyHash("74cfebcf5e97474d7b89c862d7ee7cff22efbb032d4133a1b84cbdcd");

        ScriptPubkey key2 = new ScriptPubkey();
        key2.setKeyHash("710ee487dbbcdb59b5841a00d1029a56a407c722b3081c02470b516d");

        ScriptPubkey key3 = new ScriptPubkey();
        key3.setKeyHash("beed26382ec96254a6714928c3c5bb8227abecbbb095cfeab9fb2dd1");

        ScriptAtLeast scriptAtLeast = new ScriptAtLeast(2);
        scriptAtLeast.addScript(key1)
                .addScript(key2)
                .addScript(key3);

        return scriptAtLeast;
    }

    public static void main(String[] args) throws CborDeserializationException, CborSerializationException, ApiException, IOException {
        TransferFromMultisigScriptAddress transferFromMultisigScriptAddress = new TransferFromMultisigScriptAddress();
        transferFromMultisigScriptAddress.transfer();
    }
}
