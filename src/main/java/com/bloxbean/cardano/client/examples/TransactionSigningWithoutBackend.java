package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.*;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

//Sample : How to build and sign a Transaction manually
public class TransactionSigningWithoutBackend {

    public static void createAndSignTransaction() throws CborSerializationException, CborDeserializationException {

        String senderMnemonic = "deny anchor replace squirrel type early local kitten dinner burst afford room hub cool diary buyer believe frequent evoke churn process pupil exotic notice";
        Account sender = new Account(Networks.testnet(), senderMnemonic);
        String receiverAddress = "addr_test1qr2y2yf2lwj0xn2nrhmyqe26t52twp06cp4lm2r62undytvj5ggkj79y993ds6645ewwfus90su92j554u2294wtm93s25m8cz";

        //Find utxos first and then create inputs
        List<TransactionInput> inputs = Arrays.asList(
                TransactionInput.builder()
                        .transactionId("2a95e941761fa6187d0eaeec3ea0a8f68f439ec806ebb0e4550e640e8e0d189c")
                        .index(0).build()
        );

        TransactionOutput output = TransactionOutput
                .builder()
                .address(receiverAddress)
                .value(new Value(BigInteger.valueOf(20000000L), null))
                .build();

        TransactionOutput change = TransactionOutput
                .builder()
                .address(sender.baseAddress())
                .value(new Value(BigInteger.valueOf(400000000L), null))
                .build();

        List<TransactionOutput> outputs = Arrays.asList(output, change);

        //Create the transaction body with dummy fee
        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .ttl(230000003)
                .fee(BigInteger.valueOf(170000))
                .build();

        Transaction transaction = Transaction.builder()
                .body(body)
                .build();

        //Sign the transaction. so that we get the actual size of the transaction to calculate the fee
        Transaction signTxn = sender.sign(transaction); //cbor encoded bytes in Hex format

        //Calculate fees
        byte[] signTxnBytes = signTxn.serialize();
        //Current protocol parameters in Cardano
        Integer minFeeA = 44;
        Integer minFeeB = 155381;
        BigInteger estimatedFee = BigInteger.valueOf((minFeeA * signTxnBytes.length) + minFeeB);

        //Now set the actual fee
        transaction.getBody().setFee(estimatedFee);

        //Sign the final transaction with correct fee
        signTxn = sender.sign(transaction); //cbor encoded bytes in Hex
        byte[] signedCBorBytes = signTxn.serialize();

        System.out.println(signTxn);
        System.out.println(signedCBorBytes.length);

        //You can also deserialize the txn from the cbor bytes
        Transaction txn = Transaction.deserialize(signedCBorBytes);

        //Submit signedCBorBytes to Cardano Node

    }

    public static void main(String[] args) throws CborSerializationException, CborDeserializationException {
        createAndSignTransaction();
    }
}
