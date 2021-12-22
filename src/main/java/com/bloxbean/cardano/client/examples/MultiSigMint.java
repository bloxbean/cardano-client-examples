package com.bloxbean.cardano.client.examples;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.Map;
import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.backend.api.helper.UtxoSelectionStrategy;
import com.bloxbean.cardano.client.backend.api.helper.impl.DefaultUtxoSelectionStrategyImpl;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.backend.model.TransactionContent;
import com.bloxbean.cardano.client.backend.model.Utxo;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.KeyGenUtil;
import com.bloxbean.cardano.client.crypto.SecretKey;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.metadata.cbor.CBORMetadata;
import com.bloxbean.cardano.client.transaction.TransactionSigner;
import com.bloxbean.cardano.client.transaction.model.TransactionDetailsParams;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import com.bloxbean.cardano.client.util.AssetUtil;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import com.bloxbean.cardano.client.util.Tuple;

import java.math.BigInteger;
import java.util.*;

import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static com.bloxbean.cardano.client.common.CardanoConstants.ONE_ADA;

//Multi-sig - Sign with Java backend + Sign with Nami frontend
public class MultiSigMint extends BaseTest {

    public void createTransaction() throws ApiException, CborSerializationException, CborDeserializationException {
        String senderAddress = "addr_test1qr25tw2qpyfvpnmdxrc4ythrl3zq8dpk8ggwxxd230xndquvjw44r906qaszsqktacayq37zlavvft875g8afrd6mazsgjc6gz";

        String receiver = "addr_test1qpdgydcegwlmqenfcqc6y5x3wyzs0e8xml7t9k3jj76g5w9kw68k4h5aegz0ugmnplwg7yfqwc6p55hskq0zfpmpjf0s266wgd";
        //For Mint
        String keyCbor = "5820c393da4a70c478b4b7eb06d92aed6e78a2f704a82a7bf24704f58edc3c886c55";
        SecretKey skey = new SecretKey();
        skey.setCborHex(keyCbor);
        VerificationKey vkey = KeyGenUtil.getPublicKeyFromPrivateKey(skey);

        ScriptPubkey scriptPubkey = ScriptPubkey.create(vkey);

        long ttl = blockService.getLastestBlock().getValue().getSlot() + 20000;

        TransactionDetailsParams detailsParams = TransactionDetailsParams.builder().ttl(ttl).build();

        MultiAsset multiAsset = new MultiAsset();
        multiAsset.setPolicyId(scriptPubkey.getPolicyId());
        Asset asset = new Asset("zaaa", BigInteger.valueOf(500));
        Asset asset1 = new Asset("qhjjjj", BigInteger.valueOf(3000));
        Asset asset2 = new Asset("bbbbb", BigInteger.valueOf(1000));
        multiAsset.getAssets().add(asset);
        multiAsset.getAssets().add(asset1);
        multiAsset.getAssets().add(asset2);

        List<MultiAsset> multiAssetList = Collections.singletonList(multiAsset);

        BigInteger amountToTransfer = ONE_ADA.multiply(BigInteger.valueOf(4));


        Utxo collateral = Utxo.builder()
                .txHash("fd877f4434e94ef0786574da8f88336c21a4459d0ad92bab104a212cab73f86a")
                .outputIndex(0).build();

        //Find required utxos
        UtxoSelectionStrategy utxoSelectionStrategy = new DefaultUtxoSelectionStrategyImpl(utxoService);
        List<Utxo> utxos = utxoSelectionStrategy.selectUtxos(senderAddress, LOVELACE,
                amountToTransfer.add(ONE_ADA.multiply(BigInteger.valueOf(2))), Collections.singleton(collateral)); //transfer amount + 2 ADA to cover fee and min ada

        //Create single TxnOutput for the sender
        TransactionOutput change = TransactionOutput
                .builder()
                .address(senderAddress)
                .value(Value.builder().coin(BigInteger.ZERO)
                        .multiAssets(new ArrayList<>())
                        .build())
                .build();

        //Inputs
        List<TransactionInput> inputs = new ArrayList<>();
        for (Utxo utxo : utxos) {
            TransactionInput input = TransactionInput.builder()
                    .transactionId(utxo.getTxHash())
                    .index(utxo.getOutputIndex()).build();
            inputs.add(input);

            copyUtxoValuesToChangeOutput(change, utxo);
        }

        //Outputs
        TransactionOutput output = TransactionOutput
                .builder()
                .address(receiver)
                .value(new Value(amountToTransfer, null))
                .build();


        BigInteger fee = BigInteger.valueOf(200000); //hard coded fee
        BigInteger minAmountInMintOutput = ONE_ADA.multiply(BigInteger.valueOf(2));

        //Deduct fee + minCost in a MA output
        BigInteger remainingAmount = change.getValue().getCoin().subtract(amountToTransfer.add(fee).add(minAmountInMintOutput));
        change.getValue().setCoin(remainingAmount); //deduct requirement amt (fee + min amount)

        TransactionOutput mintedTransactionOutput = new TransactionOutput();
        mintedTransactionOutput.setAddress(senderAddress);
        Value value = Value.builder()
                .coin(minAmountInMintOutput)
                .multiAssets(new ArrayList<>())
                .build();
        mintedTransactionOutput.setValue(value);
        for (MultiAsset ma : multiAssetList) {
            mintedTransactionOutput.getValue().getMultiAssets().add(ma);
        }
        List<TransactionOutput> outputs = Arrays.asList(output, change, mintedTransactionOutput);

        CBORMetadata metadata = new CBORMetadata();
        metadata.put(BigInteger.valueOf(10000001), "mint Test");

        TransactionBody body = TransactionBody.builder()
                .inputs(inputs)
                .outputs(outputs)
                .fee(fee)
                .ttl(detailsParams.getTtl())
                //.validityStartInterval(detailsParams.getValidityStartInterval())
                .mint(multiAssetList)
                .build();


        TransactionWitnessSet transactionWitnessSet = new TransactionWitnessSet();
        transactionWitnessSet.getNativeScripts().add(scriptPubkey);

        Transaction transaction = Transaction.builder()
                .body(body)
//                .witnessSet(transactionWitnessSet)
                .metadata(metadata)
                .build();

        String txnHex = transaction.serializeToHex();

        //Get the output. This txnHex is sent to Nami for signing
        System.out.println(txnHex);

    }

    void assembleWitness() throws CborSerializationException, CborDeserializationException, CborException, ApiException {
        //Txn hex from previous method. Send this to Nami for signing
        String txnHex = "84a600828258203af17b86821d4216271d79ba2865ea08fc42d45eefe2e659ca4b0d0603b5868902825820b6f2deef26470b684911dc25a9061e87153bc7a43278cc36f4edfb525109521f010183825839005a82371943bfb06669c031a250d1710507e4e6dffcb2da3297b48a38b6768f6ade9dca04fe23730fdc8f112076341a52f0b01e248761925f1a003d090082583900d545b9400912c0cf6d30f1522ee3fc4403b4363a10e319aa8bcd36838c93ab5195fa07602802cbee3a4047c2ff58c4acfea20fd48dbadf45821a375fe603a1581c329728f73683fe04364631c27a7912538c116d802416ca1eaf2d7a96ab4262621a000249f0437a63641a0003d09044616161611a00055730447a61616119157c447a7a7a7a1a000249f04561616161611a000557304562626262621a000343f0457172657463191770457a646263641927104671686a6a6a6a191770467a78616263641a000249f082583900d545b9400912c0cf6d30f1522ee3fc4403b4363a10e319aa8bcd36838c93ab5195fa07602802cbee3a4047c2ff58c4acfea20fd48dbadf45821a001e8480a1581c329728f73683fe04364631c27a7912538c116d802416ca1eaf2d7a96a3447a6161611901f44562626262621903e84671686a6a6a6a190bb8021a00030d40031a02bb55430758209b327fa4d4c0d75048590f8cc9b353c436aacf153090241a5291baed8583b86309a1581c329728f73683fe04364631c27a7912538c116d802416ca1eaf2d7a96a3447a6161611901f44562626262621903e84671686a6a6a6a190bb8a0f5a11a00989681696d696e742054657374";

        //Copied the below from browser console after Nami sign
        String namiWitnessSetCbor = "a10081825820720f3255e00946db45e823ca03d7300ccd0f488a0b5008f47740b6f5c5186aad58403bbc41d62247b7132ba32fcd6126e050144e85265aa37ff95723477f48db5261731b8cb7c630eb806f6c968d9e8b40ea0c0a8e55c84d828e5a6029252122950c";

        //Same policy as in method1
        String keyCbor = "5820c393da4a70c478b4b7eb06d92aed6e78a2f704a82a7bf24704f58edc3c886c55";
        SecretKey skey = new SecretKey();
        skey.setCborHex(keyCbor);
        VerificationKey vkey = KeyGenUtil.getPublicKeyFromPrivateKey(skey);
        ScriptPubkey scriptPubkey = ScriptPubkey.create(vkey);

        //De-serialize original txn hash
        Transaction txn = Transaction.deserialize(HexUtil.decodeHexString(txnHex));

        //Decode Nami's witness cbor
        List<DataItem> dis = CborDecoder.decode(HexUtil.decodeHexString(namiWitnessSetCbor));
        Map witnessMap = (Map)dis.get(0);
        TransactionWitnessSet transactionWitnessSet = TransactionWitnessSet.deserialize(witnessMap);

        //Add native script
        transactionWitnessSet.setNativeScripts(new ArrayList<>());
        transactionWitnessSet.getNativeScripts().add(scriptPubkey);

        //Set witness
        txn.setWitnessSet(transactionWitnessSet);

        //Need to do this if  latest version of cardano-client-lib
//       txn.setValid(true);
        String signedTxnHash = TransactionSigner.INSTANCE.sign(txn, skey).serializeToHex();
        System.out.println(txn.serializeToHex());
//        String signedTxnHash = CardanoJNAUtil.signWithSecretKey(txn.serializeToHex(), HexUtil.encodeHexString(skey.getBytes()));

        Transaction finalTxn = Transaction.deserialize(HexUtil.decodeHexString(signedTxnHash));

        String senderMnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account sender = new Account(Networks.testnet(), senderMnemonic);

        Result<String> result = transactionService.submitTransaction(HexUtil.decodeHexString(signedTxnHash));

        waitForTransactionHash(result);
        System.out.println(result);
    }

    protected void waitForTransactionHash(Result<String> result) {
        try {
            if (result.isSuccessful()) { //Wait for transaction to be mined
                int count = 0;
                while (count < 180) {
                    Result<TransactionContent> txnResult = transactionService.getTransaction(result.getValue());
                    if (txnResult.isSuccessful()) {
                        System.out.println(JsonUtil.getPrettyJson(txnResult.getValue()));
                        break;
                    } else {
                        System.out.println("Waiting for transaction to be processed ....");
                    }

                    count++;
                    Thread.sleep(2000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Copy utxo content to TransactionOutput
     *
     * @param changeOutput
     * @param utxo
     */
    protected void copyUtxoValuesToChangeOutput(TransactionOutput changeOutput, Utxo utxo) {
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
        if (multiAssets != null && multiAssets.size() > 0) {
            multiAssets.forEach(ma -> {
                if (ma.getAssets() == null || ma.getAssets().size() == 0)
                    markedForRemoval.add(ma);
            });

            if (markedForRemoval != null && !markedForRemoval.isEmpty()) multiAssets.removeAll(markedForRemoval);
        }
    }

    public static void main(String[] args) throws CborDeserializationException, CborSerializationException, ApiException, CborException {
        new MultiSigMint().createTransaction();
        //new MultiSigMint().assembleWitness();

    }
}
