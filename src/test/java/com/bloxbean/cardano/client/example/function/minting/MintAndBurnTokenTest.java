package com.bloxbean.cardano.client.example.function.minting;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.MintCreators;
import com.bloxbean.cardano.client.transaction.spec.Asset;
import com.bloxbean.cardano.client.transaction.spec.MultiAsset;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.TransactionOutput;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static com.bloxbean.cardano.client.function.helper.SignerProviders.signerFrom;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MintAndBurnTokenTest extends BaseTest {
    private String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
    private final Account sender;
    private final String senderAddress;

    //policy script specific fields
    private SecretKey secretKey;
    private ScriptPubkey scriptPubkey;
    private String tokenName = "BurnTokenTest";

    public MintAndBurnTokenTest() {
        sender = new Account(Networks.testnet(), senderMnemonic);
        senderAddress = sender.baseAddress();

        secretKey = new SecretKey("5820bb93a58be2457d3e2cd6faf07af24b4be10a58f08b9a478ccfcb790aaa64e363");
        scriptPubkey = ScriptPubkey.create(new VerificationKey("58205fd196d870a6cd5422967092be93243c5bca7bfffc81c795f0cfa50be20dae9c"));
    }

    @Test
    public void run() throws AddressExcepion, CborSerializationException, ApiException {
        MintAndBurnTokenTest mintAndBurnTokenTest = new MintAndBurnTokenTest();
//      mintAndBurnTokenTest.mintToken();
        mintAndBurnTokenTest.burnToken();
    }

    public void mintToken() throws CborSerializationException, ApiException {
        String policyId = scriptPubkey.getPolicyId();

        BigInteger noOfTokensToMint = new BigInteger("100000");
        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policyId);
        Asset asset = new Asset(tokenName, noOfTokensToMint);
        multiAsset.getAssets().add(asset);

        Output output = Output.builder()
                .address(senderAddress)
                .policyId(policyId)
                .assetName(asset.getName())
                .qty(noOfTokensToMint)
                .build();

        TxBuilder txBuilder = output.mintOutputBuilder()
                .buildInputs(InputBuilders.createFromSender(senderAddress, senderAddress))
                .andThen(MintCreators.mintCreator(scriptPubkey, multiAsset))
                .andThen(BalanceTxBuilders.balanceTx(senderAddress, 2));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(sender).andThen(signerFrom(secretKey)));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Mint Transaction Id: " + result.getValue());
        else
            System.out.println("Mint Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    public void burnToken() throws CborSerializationException, ApiException, AddressExcepion {
        String policyId = scriptPubkey.getPolicyId();

        BigInteger noOfTokensToBurn = new BigInteger("3");

        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policyId);
        Asset asset = new Asset(tokenName, noOfTokensToBurn.negate()); //set negative number to burn
        multiAsset.getAssets().add(asset);

        //Create an output with receiver addr as sender with burn token amount
        //This is just a dummy output which helps in utxo selection. But the output will be discarded later
        Output output = Output.builder()
                .address(senderAddress)
                .policyId(policyId)
                .assetName(asset.getName())
                .qty(noOfTokensToBurn)
                .build();

        TxBuilder txBuilder = output.outputBuilder()
                .buildInputs(InputBuilders.createFromSender(senderAddress, senderAddress))
                .andThen(MintCreators.mintCreator(scriptPubkey, multiAsset))
                .andThen((context, transaction) -> {
                    //Discard the first output which is our dummy output. But the min ada value from first output
                    //needs to be added to the changeoutput to balance the transaction.
                    TransactionOutput firstTxOuput = transaction.getBody().getOutputs().get(0); //output with burn token
                    TransactionOutput changeTxOuput = transaction.getBody().getOutputs().get(1);

                    //Discard first txOuputput
                    transaction.getBody().getOutputs().remove(firstTxOuput);
                    //Add ada value from first output to changeoutput
                    changeTxOuput.getValue()
                            .setCoin(changeTxOuput.getValue().getCoin().add(firstTxOuput.getValue().getCoin()));
                })
                .andThen(BalanceTxBuilders.balanceTx(senderAddress, 2));

        Transaction signedTransaction = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, signerFrom(sender).andThen(signerFrom(secretKey)));

        Result<String> result = transactionService.submitTransaction(signedTransaction.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            System.out.println("Burn Transaction Id: " + result.getValue());
        else
            System.out.println("Burn Transaction failed: " + result);

        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }
}
