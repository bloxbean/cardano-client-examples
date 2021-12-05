package com.bloxbean.cardano.client.examples;

import co.nstant.in.cbor.CborException;
import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.api.helper.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.backend.api.helper.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.backend.api.helper.model.TransactionResult;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.model.Utxo;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataList;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadataMap;
import com.bloxbean.cardano.client.transaction.model.PaymentTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.transaction.util.CostModelConstants;
import com.bloxbean.cardano.client.transaction.util.ScriptDataHashGenerator;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

public class AlwaysSuccessContractCall extends BaseTest {
    private final String senderMnemonic;
    private final Account sender;
    private String collateralUtxoHash;
    private int collateralIndex;
    private final BigInteger scriptAmt;
    private final String scriptAddress;

    PlutusScript contractScript = PlutusScript.builder()
            .type("PlutusScriptV1")
            .cborHex("4d01000033222220051200120011")
            .build();

    public AlwaysSuccessContractCall() {
        senderMnemonic = "company coast prison denial unknown design paper engage sadness employ phone cherry thunder chimney vapor cake lock afraid frequent myself engage lumber between tip";
        sender = new Account(Networks.testnet(), senderMnemonic);

        collateralUtxoHash = "eb789399004def74334c1d8950206dd4c51c36d19aa567b47ff8dd99e1b0cfbc";
        collateralIndex = 0;

        scriptAmt = new BigInteger("2479280");
        scriptAddress = "addr_test1wpnlxv2xv9a9ucvnvzqakwepzl9ltx7jzgm53av2e9ncv4sysemm8";
    }

    public void transferFundAndCallContract() throws CborSerializationException, AddressExcepion, ApiException, CborDeserializationException, CborException {

        /********************************************
         1. First transfer fund to script address
         ********************************************/

        //Creating random datum for transfer transaction to script address
        Random rand = new Random();
        int randInt = rand.nextInt();
        PlutusData plutusData = new BigIntPlutusData(BigInteger.valueOf(randInt));
        String datumHash = plutusData.getDatumHash();

        boolean paymentSuccessful = transferFund(sender, scriptAddress, scriptAmt, datumHash);
        if (!paymentSuccessful)
            throw new RuntimeException("Payment to script address failed");

        /********************************************
         * 2. Check if collateral utxo is still there.
         * If it has already been spent, send 5 ADA to your own address to create a collateral utxo
         ********************************************/
         checkCollateral();

        /********************************************
         3. Start contract transaction to claim fund
         ********************************************/

        //Find our utxo by datumHash from script address.
        //Set ignoreUtxosWithDatumHash == true
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoService);
        utxoSelectionStrategy.setIgnoreUtxosWithDatumHash(false);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(scriptAddress, LOVELACE, BigInteger.valueOf(1), datumHash, Collections.EMPTY_SET);

        if (utxos.size() == 0) {
            System.out.println("No script utxo found for datumhash : " + datumHash);
            return;
        }
        Utxo inputScriptUtxo = utxos.get(0);

        //Create inputs using script utxo
        List<TransactionInput> inputs = Arrays.asList(
                TransactionInput.builder()
                        .transactionId(inputScriptUtxo.getTxHash())
                        .index(inputScriptUtxo.getOutputIndex()).build()
        );

        //Create input for collateral utxo
        TransactionInput collateralInput = TransactionInput.builder()
                .transactionId(collateralUtxoHash)
                .index(collateralIndex).build();

        //Create change output
        TransactionOutput change = TransactionOutput
                .builder()
                .address(sender.baseAddress())
                .value(new Value(scriptAmt, null)) //Actual amount will be set after fee estimation
                .build();
        List<TransactionOutput> outputs = Arrays.asList(change);

        //Create the transaction body with dummy fee
        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .collateral(Arrays.asList(collateralInput))
                .fee(BigInteger.valueOf(170000)) //Dummy fee
                .ttl(getTtl())
                .networkId(NetworkId.TESTNET)
                .build();

        //Create Redeemer
        //ExUnits are hardcoded for now
        Redeemer redeemer = Redeemer.builder()
                .tag(RedeemerTag.Spend)
                .data(plutusData)
                .index(BigInteger.valueOf(0))
                .exUnits(ExUnits.builder()
                        .mem(BigInteger.valueOf(1700))
                        .steps(BigInteger.valueOf(476468)).build()
                ).build();

        //Add witnesses
        TransactionWitnessSet transactionWitnessSet = new TransactionWitnessSet();
        transactionWitnessSet.setPlutusScripts(Arrays.asList(contractScript));
        transactionWitnessSet.setPlutusDataList(Arrays.asList(plutusData));
        transactionWitnessSet.setRedeemers(Arrays.asList(redeemer));

        //Calculate script data hash from redeemer and datum
        byte[] scriptDataHash = ScriptDataHashGenerator.generate(Arrays.asList(redeemer),
                Arrays.asList(plutusData), CostModelConstants.LANGUAGE_VIEWS);
        body.setScriptDataHash(scriptDataHash);

        //Create AuxiliaryData (Metadata + ...)
        CBORMetadata cborMetadata = new CBORMetadata();
        CBORMetadataMap metadataMap = new CBORMetadataMap();
        CBORMetadataList metadataList = new CBORMetadataList();
        metadataList.add("Contract call");
        metadataMap.put("msg", metadataList);
        cborMetadata.put(new BigInteger("674"), metadataMap);

        AuxiliaryData auxiliaryData = AuxiliaryData.builder()
                .metadata(cborMetadata)
                // .plutusScripts(Arrays.asList(contractScript)) //Optional
                .build();

        //Create transaction
        Transaction transaction = Transaction.builder()
                .body(body)
                .witnessSet(transactionWitnessSet)
                .auxiliaryData(auxiliaryData)
                .build();

        //Sign transaction for fee calculation
        String signTxnHashForFeeCalculation = sender.sign(transaction);

        //Calculate base fee
        BigInteger baseFee = feeCalculationService.calculateFee(HexUtil.decodeHexString(signTxnHashForFeeCalculation));
        //Calculate script fee based on ExUnits
        BigInteger scriptFee = feeCalculationService.calculateScriptFee(Arrays.asList(redeemer.getExUnits()));
        //Total fee = base fee + script fee
        BigInteger totalFee = baseFee.add(scriptFee);
        System.out.println("Total Fee ----- " + totalFee);

        //Update change amount in transaction based on fee
        BigInteger changeAmt = scriptAmt.subtract(totalFee);
        change.getValue().setCoin(changeAmt);
        body.setFee(totalFee);

        //Sign the updated final transaction
        String signTxnHash = sender.sign(transaction);
        byte[] signTxnBytes = HexUtil.decodeHexString(signTxnHash);

        //Submit transaction to the network
        Result<String> result = transactionService.submitTransaction(signTxnBytes);
        System.out.println(result);

        if (result.isSuccessful()) {
            waitForTransactionId(result);
        } else {
            System.out.println("Contract transaction failed.");
        }
    }

    private void checkCollateral() throws ApiException, AddressExcepion, CborSerializationException {
        List<Utxo> utxos = utxoService.getUtxos(sender.baseAddress(), 100, 1).getValue(); //Check 1st page 100 utxos
        Optional<Utxo> collateralUtxoOption = utxos.stream().filter(utxo -> utxo.getTxHash().equals(collateralUtxoHash))
                .findAny();

        if (collateralUtxoOption.isPresent()) {//Collateral present
            System.out.println("--- Collateral utxo still there");
            return;
        } else {
            System.out.println("*** Collateral utxo not found");

            //Transfer to self to create collateral utxo
            BigInteger collateralAmt = BigInteger.valueOf(5000000L);
            transferFund(sender, sender.baseAddress(), collateralAmt, null);

            //Find collateral utxo again
            utxos = utxoService.getUtxos(sender.baseAddress(), 100, 1).getValue();
            collateralUtxoOption = utxos.stream().filter(utxo -> {
                if (utxo.getAmount().size() == 1 //Assumption: 1 Amount means, only LOVELACE
                        && LOVELACE.equals(utxo.getAmount().get(0).getUnit())
                        && collateralAmt.equals(utxo.getAmount().get(0).getQuantity()))
                    return true;
                else
                    return false;
            }).findFirst();

            if (!collateralUtxoOption.isPresent()) {
                System.out.println("Collateral cannot be crated");
                return;
            }

            Utxo collateral = collateralUtxoOption.get();
            collateralUtxoHash = collateral.getTxHash();
            collateralIndex = collateral.getOutputIndex();
        }
    }

    private boolean transferFund(Account sender, String recevingAddress, BigInteger amount, String datumHash) throws CborSerializationException, AddressExcepion, ApiException {

        Utxo collateralUtxo = Utxo.builder()
                .txHash(collateralUtxoHash)
                .outputIndex(collateralIndex)
                .build();
        Set ignoreUtxos = new HashSet();
        ignoreUtxos.add(collateralUtxo);

        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoService);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(sender.baseAddress(), LOVELACE, amount, ignoreUtxos);

        PaymentTransaction paymentTransaction =
                PaymentTransaction.builder()
                        .sender(sender)
                        .receiver(recevingAddress)
                        .amount(amount)
                        .unit("lovelace")
                        .datumHash(datumHash)
                        .utxosToInclude(utxos)
                        .build();

        BigInteger fee = feeCalculationService.calculateFee(paymentTransaction, TransactionDetailsParams.builder().ttl(getTtl()).build(), null);
        paymentTransaction.setFee(fee);

        Result<TransactionResult> result = transactionHelperService.transfer(paymentTransaction, TransactionDetailsParams.builder().ttl(getTtl()).build());
        if (result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        if (result.isSuccessful()) {
            Result<String> resultWithTxId = Result.success(result.getResponse()).code(result.code())
                    .withValue(result.getValue().getTransactionId());

            waitForTransactionId(resultWithTxId);
        } else {
            System.out.println(result);
        }

        return result.isSuccessful();
    }

    private void waitForTransactionId(Result<String> result) {
        try {
            if (result.isSuccessful()) { //Wait for transaction to be added
                int count = 0;
                while (count < 60) {
                    Result<TransactionContent> txnResult = transactionService.getTransaction(result.getValue());
                    if (txnResult.isSuccessful()) {
                        System.out.println(JsonUtil.getPrettyJson(txnResult.getValue()));
                        break;
                    } else {
                        System.out.println("Waiting for transaction to be included in a block ....");
                    }

                    count++;
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws CborException, AddressExcepion, CborDeserializationException, CborSerializationException, ApiException {
        AlwaysSuccessContractCall alwaysSuccessContractCall = new AlwaysSuccessContractCall();
        alwaysSuccessContractCall.transferFundAndCallContract();
        System.exit(0);
    }
}
