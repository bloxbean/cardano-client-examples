package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.transaction.model.MintTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;

import java.math.BigInteger;
import java.util.Arrays;

public class MintTokenPolicyWithRegularAccountTest extends BaseTest {

    public void mintToken() throws CborSerializationException, ApiException, AddressExcepion {
        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);

        String scriptAccountMnemonic = "episode same use wreck force already grief spike kiss host magic spoon cry lecture tuition style old detect session creek champion cry service exchange";
        Account scriptAccount = new Account(Networks.testnet(), scriptAccountMnemonic);

        byte[] prvKeyBytes = scriptAccount.privateKeyBytes();
        byte[] pubKeyBytes = scriptAccount.publicKeyBytes();

        SecretKey sk = SecretKey.create(prvKeyBytes);
        VerificationKey vkey = VerificationKey.create(pubKeyBytes);

        ScriptPubkey scriptPubkey = ScriptPubkey.create(vkey);
        String policyId = scriptPubkey.getPolicyId();

        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policyId);

        Asset asset = new Asset("token_12", BigInteger.valueOf(6000));
        multiAsset.getAssets().add(asset);

        MintTransaction mintTransaction = MintTransaction.builder()
                .sender(sender)
//                .receiver(receiver)
                .mintAssets(Arrays.asList(multiAsset))
                .policyScript(scriptPubkey)
                .policyKeys(Arrays.asList(sk))
                .build();

        TransactionDetailsParams detailsParams =
                TransactionDetailsParams.builder()
                        .ttl(getTtl())
                        .build();

        //Calculate fee
        BigInteger fee
                = feeCalculationService.calculateFee(mintTransaction, detailsParams, null);
        mintTransaction.setFee(fee);

        Result<String> result = transactionHelperService.mintToken(mintTransaction,
                TransactionDetailsParams.builder().ttl(getTtl()).build());

        System.out.println(result);

        if(result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        waitForTransaction(result);

    }

    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException {
        new MintTokenPolicyWithRegularAccountTest().mintToken();
        System.exit(1);
    }
}
