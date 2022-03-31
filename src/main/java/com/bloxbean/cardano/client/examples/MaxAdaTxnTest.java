package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.spec.*;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

public class MaxAdaTxnTest extends BaseTest {

    public void sendGroupTxn() throws ApiException, CborSerializationException {
        String senderMnemonic = "enjoy slender universe autumn tragic horse matrix welcome mandate goose fitness problem effort solid remain present razor conduct acquire rule select sorry awesome diet";
        Account sender = new Account(Networks.testnet(), senderMnemonic);

        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(sender.baseAddress(), LOVELACE, adaToLovelace(1000), Collections.emptySet());

        List<TransactionInput> inputs = new ArrayList<>();
        BigInteger totalInuputLoveLace = BigInteger.ZERO;
        for (Utxo utxo : utxos) {
            inputs.add(TransactionInput.builder()
                    .transactionId(utxo.getTxHash())
                    .index(utxo.getOutputIndex()).build());

            Amount amount = utxo.getAmount().stream().filter(amt -> LOVELACE.equals(amt.getUnit())).findFirst().get();
            totalInuputLoveLace = totalInuputLoveLace.add(amount.getQuantity());
        }

        TransactionOutput change = TransactionOutput
                .builder()
                .address(sender.baseAddress())
                .value(new Value(BigInteger.ZERO, new ArrayList<>()))
                .build();
        utxos.stream().forEach(utxo -> copyUtxoValuesToChangeOutput(change, utxo));

        List<TransactionOutput> outputs = new ArrayList<>();
        outputs.add(change);

        int index = 1;
        BigInteger totalTransferAmt = BigInteger.ZERO;
        while (true) {
            Account receiverAcc = new Account(Networks.testnet(), senderMnemonic, index++);
            BigInteger amtToTransfer = adaToLovelace(1.5);

            TransactionOutput output = TransactionOutput
                    .builder()
                    .address(receiverAcc.enterpriseAddress())
                    .value(new Value(amtToTransfer, new ArrayList<>()))
                    .build();

            outputs.add(output);

            byte[] size = createTxnAndSignForSizeCheck(sender, inputs, outputs);
            if (size.length < 16384) { //16kb
                totalTransferAmt = totalTransferAmt.add(amtToTransfer);
                continue;
            } else {
                System.out.println("No of transfers >> " + --index);
                outputs.remove(output);
                break;
            }
        }

        //Create the transaction body with dummy fee
        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .ttl(getTtl())
                .fee(BigInteger.valueOf(170000))
                .build();

        Transaction transaction = Transaction.builder()
                .body(body)
                .build();
        //Sign the transaction. so that we get the actual size of the transaction to calculate the fee
        Transaction signTxn = sender.sign(transaction); //cbor encoded bytes in Hex format

        BigInteger fee = feeCalculationService.calculateFee(signTxn);

        //Final change amount
        BigInteger changeAmt = totalInuputLoveLace.subtract(totalTransferAmt).subtract(fee);
        body.setFee(fee);
        change.getValue().setCoin(changeAmt);

        signTxn = sender.sign(transaction);

        Result<String> result = transactionService.submitTransaction(signTxn.serialize());

        if (result.isSuccessful()) {
            System.out.println(result);
            waitForTransactionHash(result);
        } else {
            System.out.println("Failed : " + result);
        }
    }

    //Dummy txn for size check
    private byte[] createTxnAndSignForSizeCheck(Account sender, List<TransactionInput> inputs, List<TransactionOutput> outputs) throws CborSerializationException {
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
        Transaction signTxn = sender.sign(transaction); //cbor encoded bytes in Hex format

        byte[] size = signTxn.serialize();
        return size;
    }

    public static void main(String[] args) throws CborSerializationException, ApiException {
        new MaxAdaTxnTest().sendGroupTxn();
    }
}
