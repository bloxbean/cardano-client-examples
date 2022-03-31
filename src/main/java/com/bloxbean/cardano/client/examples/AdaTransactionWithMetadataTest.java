package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.helper.model.TransactionResult;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.metadata.Metadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataList;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap;
import com.bloxbean.cardano.client.transaction.model.PaymentTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

public class AdaTransactionWithMetadataTest extends BaseTest {

    public void transfer() throws ApiException, AddressExcepion, CborSerializationException {

        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);

        String receiver = "addr_test1qqwpl7h3g84mhr36wpetk904p7fchx2vst0z696lxk8ujsjyruqwmlsm344gfux3nsj6njyzj3ppvrqtt36cp9xyydzqzumz82";

        CBORMetadataMap productDetailsMap
                = new CBORMetadataMap()
                .put("code", "PROD-800")
                .put("slno", "SL20000039484");

        CBORMetadataList tagList
                = new CBORMetadataList()
                .add("laptop")
                .add("computer");

        Metadata metadata = new CBORMetadata()
                .put(new BigInteger("670001"), productDetailsMap)
                .put(new BigInteger("670002"), tagList);

        PaymentTransaction paymentTransaction =
                PaymentTransaction.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .amount(BigInteger.valueOf(20000000))
                        .unit(LOVELACE)
                        .build();

        long ttl = blockService.getLatestBlock().getValue().getSlot() + 1000;
        TransactionDetailsParams detailsParams =
                TransactionDetailsParams.builder()
                        .ttl(ttl)
                        .build();

        BigInteger fee = feeCalculationService.calculateFee(paymentTransaction, detailsParams, metadata);
        paymentTransaction.setFee(fee);

        Result<TransactionResult> result
                = transactionHelperService.transfer(paymentTransaction, detailsParams, metadata);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        waitForTransaction(result);
    }


    public static void main(String[] args) throws AddressExcepion, ApiException, CborSerializationException {
        new AdaTransactionWithMetadataTest().transfer();
        System.exit(1);
    }
}
