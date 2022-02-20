package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.model.PaymentTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.TransactionWitnessSet;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAtLeast;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.HexUtil;

import java.math.BigInteger;
import java.util.Arrays;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.common.CardanoConstants.ONE_ADA;

/**
 * Example to transfer ADA from a multisig script address.
 * This example uses high-level api to build transaction and then handles witnesses.
 */
public class MultiWitnessWithPaymentTransaction extends BaseTest {

    public void transfer() throws ApiException, AddressExcepion, CborSerializationException, CborDeserializationException {
        //String multisigScriptAddr = "addr_test1wzchaw4vxmmpws44ffh99eqzmlg6wr3swg36pqug8xn20ygxgqher";
        String multisigScriptAddr = AddressService.getInstance().getEntAddress(getMultisigScript(), Networks.testnet()).toBech32();

        String receiverAddress = "addr_test1qr2y2yf2lwj0xn2nrhmyqe26t52twp06cp4lm2r62undytvj5ggkj79y993ds6645ewwfus90su92j554u2294wtm93s25m8cz";

        BigInteger amountToTransfer = ONE_ADA.multiply(BigInteger.valueOf(7));

        //Create a payment transaction
        //Sender is a ReadOnlyAccount.
        //ReadOnlyAccount extends Account. Only needs to override baseAddress() for now. This will be simplified in future release.
        PaymentTransaction paymentTransaction =
                PaymentTransaction.builder()
                        .sender(new ReadOnlyAccount(multisigScriptAddr))
                        .receiver(receiverAddress)
                        .amount(amountToTransfer)
                        .unit(LOVELACE)
                        .build();

        TransactionDetailsParams detailsParams =
                TransactionDetailsParams.builder()
                        .ttl(getTtl())
                        .build();

        ProtocolParams protocolParams = epochService.getProtocolParameters().getValue();

        //Select utxos and build transaction using UtxoTransactionBuilder
        Transaction transaction = utxoTransactionBuilder.buildTransaction(Arrays.asList(paymentTransaction), detailsParams, null, protocolParams);

        //Add script witness
        TransactionWitnessSet transactionWitnessSet = new TransactionWitnessSet();
        transactionWitnessSet.setNativeScripts(Arrays.asList(getMultisigScript()));
        transaction.setWitnessSet(transactionWitnessSet);

        //Set some default fee for exact fee calculation. This will be replace by actual fee later
        //Set this after transaction build by utxoTransaction Builder
        transaction.getBody().setFee(BigInteger.valueOf(17000000));

        //Signers
        String signerAcc1Mnemonic = "around submit turtle canvas friend remind push vehicle debate drop blouse piece obvious crane tone avoid aspect power milk eye brand cradle tide wrist";
        String signerAcc2Mnemonic = "prison glide olympic diamond rib payment crucial ski vintage example dinner matrix cruise upper antenna surge drink divorce brother half figure skate jar stand";

        Account signer1 = new Account(Networks.testnet(), signerAcc1Mnemonic);
        Account signer2 = new Account(Networks.testnet(), signerAcc2Mnemonic);

        //Sign the transaction. so that we get the actual size of the transaction to calculate the fee
        Transaction signTxn = signer1.sign(transaction); //cbor encoded bytes in Hex format
        signTxn = signer2.sign(signTxn);

        //Calculate final fee
        BigInteger fee = feeCalculationService.calculateFee(signTxn);

        //Update fee in transaction body
        transaction.getBody().setFee(fee);

        //Final change amount after fee calculation
        //Find change output and the update fee and change amount
        for (TransactionOutput output : transaction.getBody().getOutputs()) {
            if (output.getAddress().equals(multisigScriptAddr)) { //find change ouput.
                BigInteger finalChangeAmt = output.getValue().getCoin().subtract(fee); //substract fee
                output.getValue().setCoin(finalChangeAmt);
            }
        }

        //TODO Sign the transaction by signer1 to add witness and then sign the signed transaction again by signer2 to append signer2's witness
        //String finalSignedTxn = signTransactionOneAfterAnother(transaction, signer1, signer2);

        //TODO Uncomment if want to sign independently and then assembly. Orignal transaction can be serialized and distribute to signing party
        String finalSignedTxn = signOriginalTransactionBySigner1AndSigner2AndThenAssemble(transaction, signer1, signer2);

        Result<String> result = transactionService.submitTransaction(HexUtil.decodeHexString(finalSignedTxn));

        System.out.println(result);
        if (result.isSuccessful())
            waitForTransactionHash(result);
        else
            System.out.println("Transaction failed : " + result);
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

    /**
     * Sign the transaction first by signer1 and then sign the signed transactioin by signer2
     *
     * @param transaction
     * @param signer1
     * @param signer2
     * @return
     * @throws CborSerializationException
     */
    private Transaction signTransactionOneAfterAnother(Transaction transaction, Account signer1, Account signer2) throws CborSerializationException {
        Transaction signedTxn = signer1.sign(transaction);
        signedTxn = signer2.sign(signedTxn);

        return signedTxn;
    }

    private String signOriginalTransactionBySigner1AndSigner2AndThenAssemble(Transaction transaction, Account signer1, Account signer2)
            throws CborSerializationException, CborDeserializationException {
        //Signer 1 sign the transaction
        Transaction signer1Txn = signer1.sign(transaction);

        //Signer 2 sign the transaction
        Transaction signer2Txn = signer2.sign(transaction);

        //Get witness from signer1 signed txn and signer2 signed transaction and add to witnesses
        //transaction.setValid(true); //Set the transaction validity to true.
        transaction.getWitnessSet().getVkeyWitnesses().add(signer1Txn.getWitnessSet().getVkeyWitnesses().get(0));
        transaction.getWitnessSet().getVkeyWitnesses().add(signer2Txn.getWitnessSet().getVkeyWitnesses().get(0));

        return transaction.serializeToHex();
    }

    class ReadOnlyAccount extends Account {
        private String address;

        public ReadOnlyAccount(String address) {
            this.address = address;
        }

        @Override
        public String baseAddress() {
            return address;
        }
    }

    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException, CborDeserializationException {
        MultiWitnessWithPaymentTransaction multiWitnessWithPaymentTransaction = new MultiWitnessWithPaymentTransaction();
        multiWitnessWithPaymentTransaction.transfer();
    }
}
