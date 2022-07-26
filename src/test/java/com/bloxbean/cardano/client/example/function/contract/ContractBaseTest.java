package com.bloxbean.cardano.client.example.function.contract;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.exception.ApiRuntimeException;
import com.bloxbean.cardano.client.api.helper.model.TransactionResult;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.model.Block;
import com.bloxbean.cardano.client.coinselection.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.coinselection.UtxoSelector;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.coinselection.impl.DefaultUtxoSelector;
import com.bloxbean.cardano.client.config.Configuration;
import com.bloxbean.cardano.client.example.BaseTest;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.helper.ScriptUtxoFinders;
import com.bloxbean.cardano.client.transaction.model.PaymentTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.util.Tuple;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.common.CardanoConstants.ONE_ADA;

public class ContractBaseTest extends BaseTest {

    protected long getTtl() {
        try {
            Block block = blockService.getLatestBlock().getValue();
            long slot = block.getSlot();
            return slot + 2000;
        } catch (Exception e) {
            throw new ApiRuntimeException(e);
        }
    }

    //Setup collateral if not there
    protected Tuple<String, Integer> collateralSetup(Account sender) throws Exception {

        //Get collateral
        String collateral = "ab44e0f5faf56154cc33e757c9d98a60666346179d5a7a0b9d77734c23c42082";
        int collateralIndex = 0;

        Tuple<String, Integer> collateralTuple = checkCollateral(sender, collateral, collateralIndex);
        if (collateralTuple == null) {
            System.out.println("Collateral cannot be found or created. " + collateral);
            return null;
        }
        collateral = collateralTuple._1;
        collateralIndex = collateralTuple._2;

        return new Tuple<>(collateral, collateralIndex);
    }

    //Find utxo by datum. If not available, send a payment to script utxo.
    protected Tuple<Utxo, BigInteger> getScriptUtxo(Account sender, String scriptAddress, Object datum, Tuple<String, Integer> collateral) throws Exception {
        Optional<Utxo> utxoOptional = ScriptUtxoFinders.findFirstByDatumHashUsingDatum(new DefaultUtxoSupplier(backendService.getUtxoService()), scriptAddress, datum);
        //Start contract transaction to claim fund
        if (!utxoOptional.isPresent()) {
            System.out.println("No utxo found...Let's transfer some Ada to script address");
            boolean paymentSuccessful = transferToContractAddress(sender, scriptAddress, adaToLovelace(5),
                    Configuration.INSTANCE.getPlutusObjectConverter().toPlutusData(datum).getDatumHash(), collateral._1, collateral._2);
            utxoOptional = ScriptUtxoFinders.findFirstByDatumHashUsingDatum(new DefaultUtxoSupplier(backendService.getUtxoService()), scriptAddress, datum);

            if (!paymentSuccessful)
                throw new RuntimeException("Payment to script address failed");
        }

        Utxo scriptUtxo = utxoOptional.get();
        BigInteger claimableAmt = utxoOptional.get().getAmount().stream().filter(amount -> LOVELACE.equals(amount.getUnit()))
                .findFirst()
                .map(amount -> amount.getQuantity())
                .orElseGet(() -> BigInteger.ZERO);

        return new Tuple<Utxo, BigInteger>(scriptUtxo, claimableAmt);
    }

    protected Tuple<String, Integer> checkCollateral(Account sender, final String collateralUtxoHash, final int collateralIndex) throws ApiException, AddressExcepion, CborSerializationException {
        List<Utxo> utxos = utxoService.getUtxos(sender.baseAddress(), 100, 1).getValue(); //Check 1st page 100 utxos
        Optional<Utxo> collateralUtxoOption = utxos.stream().filter(utxo -> utxo.getTxHash().equals(collateralUtxoHash))
                .findAny();

        if (collateralUtxoOption.isPresent()) {//Collateral present
            System.out.println("--- Collateral utxo still there");
            return new Tuple(collateralUtxoHash, collateralIndex);
        } else {

            Utxo randomCollateral = getRandomUtxoForCollateral(sender.baseAddress());
            if (randomCollateral != null) {
                System.out.println("Found random collateral ---");
                return new Tuple<>(randomCollateral.getTxHash(), randomCollateral.getOutputIndex());
            } else {
                System.out.println("*** Collateral utxo not found");

                //Transfer to self to create collateral utxo
                BigInteger collateralAmt = BigInteger.valueOf(8000000L);
                transferFund(sender, sender.baseAddress(), collateralAmt, null, null, null);

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
                    System.out.println("Collateral cannot be created");
                    return null;
                }

                Utxo collateral = collateralUtxoOption.get();
                String colUtxoHash = collateral.getTxHash();
                int colIndex = collateral.getOutputIndex();

                return new Tuple(colUtxoHash, colIndex);
            }
        }
    }

    protected boolean transferFund(Account sender, String recevingAddress, BigInteger amount, String datumHash, String collateralUtxoHash, Integer collateralIndex) throws CborSerializationException, AddressExcepion, ApiException {

        //Ignore collateral utxos
        Set ignoreUtxos = new HashSet();
        if (collateralUtxoHash != null) {
            Utxo collateralUtxo = Utxo.builder()
                    .txHash(collateralUtxoHash)
                    .outputIndex(collateralIndex)
                    .build();
            ignoreUtxos.add(collateralUtxo);
        }

        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
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

            waitForTransactionHash(resultWithTxId);
        } else {
            System.out.println(result);
        }

        return result.isSuccessful();
    }

    protected boolean transferToContractAddress(Account sender, String scriptAddress, BigInteger amount, String datumHash,
                                                String collateralTxHash, int collateralIndex) throws CborSerializationException, AddressExcepion, ApiException {

        Utxo collateralUtxo = Utxo.builder()
                .txHash(collateralTxHash)
                .outputIndex(collateralIndex)
                .build();
        Set ignoreUtxos = new HashSet();
        ignoreUtxos.add(collateralUtxo);

        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoSupplier);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(sender.baseAddress(), LOVELACE, amount.add(ONE_ADA), ignoreUtxos); //One ada extra buffer for fee

        PaymentTransaction paymentTransaction =
                PaymentTransaction.builder()
                        .sender(sender)
                        .receiver(scriptAddress)
                        .amount(amount)
                        .unit(LOVELACE)
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

            waitForTransactionHash(resultWithTxId);
        } else {
            System.out.println(result);
        }

        return result.isSuccessful();
    }

    protected Utxo getRandomUtxoForCollateral(String address) throws ApiException {
        UtxoSelector utxoSelector = new DefaultUtxoSelector(utxoSupplier);
        //Find 5 > utxo > 10 ada
        Optional<Utxo> optional = utxoSelector.findFirst(address, u -> {
            if (u.getAmount().size() == 1
                    && u.getAmount().get(0).getQuantity().compareTo(adaToLovelace(5)) == 1
                    && u.getAmount().get(0).getQuantity().compareTo(adaToLovelace(10)) == -1)
                return true;
            else
                return false;
        });

        return optional.orElse(null);
    }
}
