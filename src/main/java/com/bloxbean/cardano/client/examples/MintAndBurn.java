package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.api.helper.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.backend.api.helper.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.backend.api.helper.model.TransactionResult;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.ProtocolParams;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.backend.model.Utxo;
import com.bloxbean.cardano.client.common.MinAdaCalculator;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.Keys;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.AddressExcepion;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.jna.CardanoJNAUtil;
import com.bloxbean.cardano.client.metadata.Metadata;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.transaction.model.MintTransaction;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.AssetUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.client.util.Tuple;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;

/**
 * Mint and Burn Cardano native token. This example tries to mint a new token and then burn the same token.
 * Mint is done using Cardano-client-lib's high-level api MintTransaction
 * Burn is done using Cardano-client-lib's low-level api like Transaction, TransactionInput/Output
 */
public class MintAndBurn extends BaseTest {
    private String senderMnemonic = "beach real isolate vacuum hire run report give sister mask trip flavor video until season boring naive glare zoo resist pave demand salmon resource";
    private Account senderAcc;
    String senderAddress;
    String receiver;

    public MintAndBurn() {
         senderAcc = new Account(Networks.testnet(), senderMnemonic);
         senderAddress = senderAcc.baseAddress();
         receiver = senderAcc.baseAddress();
    }

    /**
     * First mint token and then burn
     * @throws CborSerializationException
     * @throws ApiException
     * @throws IOException
     * @throws CborDeserializationException
     * @throws AddressExcepion
     */
    public void mintAndBurnToken() throws CborSerializationException, ApiException, IOException, CborDeserializationException, AddressExcepion {
        //InputStream isp = MintAndBurn.class.getResourceAsStream("/Test.script");
        //ObjectMapper objectMapper = new ObjectMapper();
        //ScriptPubkey scriptPubkey = objectMapper.readValue(isp, ScriptPubkey.class);
        //InputStream iss = MintAndBurn.class.getResourceAsStream("/Test.skey");
        //SecretKey skey = objectMapper.readValue(iss, SecretKey.class);

        Keys keys = KeyGenUtil.generateKey();
        VerificationKey vkey = keys.getVkey();
        SecretKey skey = keys.getSkey();

        ScriptPubkey scriptPubkey = ScriptPubkey.create(vkey);

        String policyId = scriptPubkey.getPolicyId();
        System.out.println(scriptPubkey.toString());

        String assetName = "Test";

        //Mint
        mintToken(scriptPubkey, skey, policyId, assetName, BigInteger.valueOf(1000));

        //Burn token
        burnToken(scriptPubkey, skey, policyId, assetName, BigInteger.valueOf(-300)); //Pass a negative value to burn
    }

    private void mintToken(ScriptPubkey scriptPubkey, SecretKey skey, String policyId, String assetName, BigInteger quantity) throws AddressExcepion, CborSerializationException, ApiException {
        MultiAsset multiAsset = MultiAsset.builder()
                .policyId(policyId)
                .assets(Arrays.asList(new Asset(assetName, quantity)))
                .build();

        //Metadata
        Metadata metadata = new CBORMetadata()
                .put(new BigInteger("10290929293383"), "Some metadata");

        MintTransaction paymentTransaction =
                MintTransaction.builder()
                        .sender(senderAcc)
                        .receiver(receiver)
                        .mintAssets(Arrays.asList(multiAsset))
                        .policyScript(scriptPubkey)
                        .policyKeys(Arrays.asList(skey))
                        .build();

        TransactionDetailsParams detailsParams1 = TransactionDetailsParams.builder().ttl(getTtl()).build();
        BigInteger fee = feeCalculationService.calculateFee(paymentTransaction, detailsParams1, metadata);
        paymentTransaction.setFee(fee);

        TransactionDetailsParams detailsParams = TransactionDetailsParams.builder().ttl(getTtl()).build();
        Result<TransactionResult> result = transactionHelperService.mintToken(paymentTransaction, detailsParams, metadata);

        System.out.println("Request: \n" + JsonUtil.getPrettyJson(paymentTransaction));
        if(result.isSuccessful())
            System.out.println("Transaction Id: " + result.getValue());
        else
            System.out.println("Transaction failed: " + result);

        waitForTransaction(result);
    }

    private void burnToken(ScriptPubkey scriptPubkey, SecretKey skey, String policyId, String assetName, BigInteger noToBurn) throws ApiException, CborSerializationException, CborDeserializationException {

        System.out.println("Start token burn -------------------------------------");
        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(policyId);
        multiAsset.getAssets().add(new Asset(assetName, noToBurn)); //noToBurn is negative

        //Get assetId
        String assetNameInHex = HexUtil.encodeHexString(assetName.getBytes(StandardCharsets.UTF_8));
        String unit = policyId + assetNameInHex;

        //Get utxos for asset (unit)
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoService);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(senderAddress, unit, noToBurn.abs(), Collections.EMPTY_SET);

        //Create inputs
        List<TransactionInput> inputs = new ArrayList<>();
        utxos.forEach(utxo -> {
            TransactionInput transactionInput = TransactionInput.builder()
                    .transactionId(utxo.getTxHash())
                    .index(utxo.getOutputIndex())
                    .build();

            inputs.add(transactionInput);
        });

        //Create outputs
        List<TransactionOutput> outputs = new ArrayList<>();
        TransactionOutput transactionOutput = TransactionOutput.builder()
                .address(senderAddress)
                .value(Value.builder()
                        .multiAssets(new ArrayList<>())
                        .coin(BigInteger.ZERO)
                        .build())
                .build();

        //Copy selected utxos content to transactionoutput
        utxos.forEach(utxo -> copyUtxoValuesToChangeOutput(transactionOutput, utxo));

        //Update asset value. Deduct burn amount
        transactionOutput.getValue().getMultiAssets()
                .stream().filter(mulAsset -> mulAsset.getPolicyId().equals(policyId))
                .forEach(ma -> {
                    Optional<Asset> assetOptional = ma.getAssets().stream().filter(ast ->
                            ast.getName().equals(HexUtil.encodeHexString(assetName.getBytes(StandardCharsets.UTF_8), true)))
                            .findFirst();
                    if(assetOptional.isPresent()) {
                        Asset asset = assetOptional.get();
                        asset.setValue(asset.getValue().add(noToBurn));
                    }
                });
        outputs.add(transactionOutput);

        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .mint(Arrays.asList(multiAsset))
                .ttl(getTtl())
                .fee(BigInteger.valueOf(170000)) //dummy fee to calculate actual fee
                .build();

        //Add script witness
        TransactionWitnessSet transactionWitnessSet = TransactionWitnessSet.builder()
                .nativeScripts(Arrays.asList(scriptPubkey))
                .build();

        Transaction transaction = Transaction.builder()
                .body(body)
                .witnessSet(transactionWitnessSet)
                .build();

        calculateEstimatedFeeAndMinAdaRequirementAndUpdateTxnOutput(skey, utxoSelectionStrategy, utxos, transaction);

        byte[] signedCBorBytes = signTransactionWithSenderAndSecretKey(skey, transaction).serialize();

        Result<String> response = transactionService.submitTransaction(signedCBorBytes);
        System.out.println(response);

        System.out.println("Request: \n" + JsonUtil.getPrettyJson(transaction));
        if(response.isSuccessful())
            System.out.println("Transaction Id: " + response.getValue());
        else
            System.out.println("Transaction failed: " + response);

        waitForTransactionHash(response);
    }

    private void calculateEstimatedFeeAndMinAdaRequirementAndUpdateTxnOutput(SecretKey skey, UtxoSelectionStrategy utxoSelectionStrategy,
                                                                             List<Utxo> utxos, Transaction transaction) throws ApiException, CborSerializationException, CborDeserializationException {
        List<TransactionInput> inputs = transaction.getBody().getInputs();
        TransactionOutput transactionOutput = transaction.getBody().getOutputs().get(0);

        //Calculate fee with signed transaction
        BigInteger estimatedFee = feeCalculationService.calculateFee(signTransactionWithSenderAndSecretKey(skey, transaction));

        //Check if min-ada is there in transaction output. If not, get some additional utxos
        ProtocolParams protocolParams = epochService.getProtocolParameters().getValue();
        MinAdaCalculator minAdaCalculator = new MinAdaCalculator(protocolParams);
        BigInteger minAda = minAdaCalculator.calculateMinAda(transactionOutput);

        Set<Utxo> utxosToExclude = new HashSet<>();
        utxosToExclude.addAll(utxos);

        //Check if not enough lovelace in the transaction output. Get some additional utxos and recalculate fee again and iterate
        while(minAda.compareTo(transactionOutput.getValue().getCoin().subtract(estimatedFee)) == 1) {
            //Get some additional utxos
            BigInteger reqAdditionalLovelace = minAda.subtract(transactionOutput.getValue().getCoin().subtract(estimatedFee));
            List<Utxo> additionalUtxos = utxoSelectionStrategy.selectUtxos(senderAddress, LOVELACE, reqAdditionalLovelace, utxosToExclude);
            if(additionalUtxos.size() > 0) {
                additionalUtxos.forEach(utxo -> {
                    TransactionInput transactionInput = TransactionInput.builder()
                            .transactionId(utxo.getTxHash())
                            .index(utxo.getOutputIndex())
                            .build();
                    inputs.add(transactionInput);

                    copyUtxoValuesToChangeOutput(transactionOutput, utxo);
                });
                utxosToExclude.addAll(additionalUtxos);

                //Calculate fee again as new utxos were added
                estimatedFee = feeCalculationService.calculateFee(signTransactionWithSenderAndSecretKey(skey, transaction));
            }
            minAda = minAdaCalculator.calculateMinAda(transactionOutput);
        }

        //Set final estimated fee and lovelace amount in output
        transaction.getBody().setFee(estimatedFee);
        transactionOutput.getValue().setCoin(transactionOutput.getValue().getCoin().subtract(estimatedFee));
    }

    /**
     * Return a new copy of signed transaction.
     * @param skey
     * @param transaction
     * @return
     * @throws CborSerializationException
     */
    private Transaction signTransactionWithSenderAndSecretKey(SecretKey skey, Transaction transaction)
            throws CborSerializationException, CborDeserializationException {
        //sign with sender account
        String signTxnHash = senderAcc.sign(transaction);

        //sign with secret key
        signTxnHash = CardanoJNAUtil.signWithSecretKey(signTxnHash, HexUtil.encodeHexString(skey.getBytes()));

        //Get cbor bytes
        byte[] signedCBorBytes = HexUtil.decodeHexString(signTxnHash);
        return Transaction.deserialize(signedCBorBytes);
    }

    /**
     * Copy utxo content to TransactionOutput
     * @param changeOutput
     * @param utxo
     */
    private void copyUtxoValuesToChangeOutput(TransactionOutput changeOutput, Utxo utxo) {
        utxo.getAmount().forEach(utxoAmt -> { //For each amt in utxo
            String utxoUnit = utxoAmt.getUnit();
            BigInteger utxoQty = utxoAmt.getQuantity();
            if (utxoUnit.equals(LOVELACE)) {
                BigInteger existingCoin = changeOutput.getValue().getCoin();
                if (existingCoin == null) existingCoin = BigInteger.ZERO;
                changeOutput.getValue().setCoin(existingCoin.add(utxoQty));
            } else {
                Tuple<String, String> policyIdAssetName = AssetUtil.getPolicyIdAndAssetName(utxoUnit);

                //Find if the policy id is available
                Optional<MultiAsset> multiAssetOptional =
                        changeOutput.getValue().getMultiAssets().stream().filter(ma -> policyIdAssetName._1.equals(ma.getPolicyId())).findFirst();
                if (multiAssetOptional.isPresent()) {
                    Optional<Asset> assetOptional = multiAssetOptional.get().getAssets().stream()
                            .filter(ast -> policyIdAssetName._2.equals(ast.getName()))
                            .findFirst();
                    if (assetOptional.isPresent()) {
                        BigInteger changeVal = assetOptional.get().getValue().add(utxoQty);
                        assetOptional.get().setValue(changeVal);
                    } else {
                        Asset asset = new Asset(policyIdAssetName._2, utxoQty);
                        multiAssetOptional.get().getAssets().add(asset);
                    }
                } else {
                    Asset asset = new Asset(policyIdAssetName._2, utxoQty);
                    MultiAsset multiAsset = new MultiAsset(policyIdAssetName._1, new ArrayList<>(Arrays.asList(asset)));
                    changeOutput.getValue().getMultiAssets().add(multiAsset);
                }
            }
        });

        //Remove any empty MultiAssets
        List<MultiAsset> multiAssets = changeOutput.getValue().getMultiAssets();
        List<MultiAsset> markedForRemoval = new ArrayList<>();
        if(multiAssets != null && multiAssets.size() > 0) {
            multiAssets.forEach(ma -> {
                if(ma.getAssets() == null || ma.getAssets().size() == 0)
                    markedForRemoval.add(ma);
            });

            multiAssets.removeAll(markedForRemoval);
        }
    }

    public static void main(String[] args) throws AddressExcepion, CborSerializationException, ApiException, IOException, CborDeserializationException {
        new MintAndBurn().mintAndBurnToken();
        System.exit(1);
    }
}
