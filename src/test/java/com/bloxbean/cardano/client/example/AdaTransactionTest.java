package com.bloxbean.cardano.client.example;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.helper.model.TransactionResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.model.PaymentTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AdaTransactionTest extends BaseTest {

    @Test
    public void transfer() throws ApiException, AddressExcepion, CborSerializationException {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);

        String receiver = "addr_test1qzs3037mcmh77wpq6katwk24v5eq6qk085jnu8uhrduzhf4zl94kwevfv9hpuz4hc0p5vjye3q45umskhdyhwj6ptzrq8tjm23";

        PaymentTransaction paymentTransaction =
                PaymentTransaction.builder()
                        .sender(sender)
                        .receiver(receiver)
                        .amount(BigInteger.valueOf(2500000))
                        .unit(LOVELACE)
                        .build();

        long ttl = blockService.getLatestBlock().getValue().getSlot() + 1000;
        TransactionDetailsParams detailsParams =
                TransactionDetailsParams.builder()
                        .ttl(ttl)
                        .build();

        BigInteger fee = feeCalculationService.calculateFee(paymentTransaction, detailsParams
                , null);

        paymentTransaction.setFee(fee);

        Result<TransactionResult> result = transactionHelperService.transfer(paymentTransaction, detailsParams);

        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransaction(result);
    }

}
