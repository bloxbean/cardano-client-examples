package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.api.MinAdaCalculator;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Amount;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.api.model.Utxo;
import com.bloxbean.cardano.client.api.util.PolicyUtil;
import com.bloxbean.cardano.client.backend.api.BackendService;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.example.Constant;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.helper.OutputMergers;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;

public class P2PNFTTransfer {
    String party1Mnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
    Account party1 = new Account(Networks.testnet(), party1Mnemonic);
    String party1Addr = party1.baseAddress();

    String party2Mnemonic = "essence pilot click armor alpha noise mixture soldier able advice multiply inject ticket pride airport uncover honey desert curtain sun true toast valve half";
    Account party2 = new Account(Networks.testnet(), party2Mnemonic);
    String party2Addr = party2.baseAddress();

    BackendService backendService = new BFBackendService(Constants.BLOCKFROST_PREPROD_URL, Constant.BF_PROJECT_KEY);

    private Policy policy;
    Asset asset;
    BigInteger NFT_PRICE = adaToLovelace(10);

    public void testP2PTransaction() throws Exception {
        //create NFT in sender's wallet
        setupNTFs();

        //calculate min ADA required when there's a single NFT in the UTXO
        //This is required to calculate the min ADA required to send the NFT to receiver
        //Party-2 needs to pay nft price + min ADA to party-1
        BigInteger minAda = calculateMinAdaInUtxo();

        //Sender Part - Party1
        //create P2P transaction
        Tx party1Tx = new Tx()
                .payToAddress(party2Addr, Amount.asset(policy.getPolicyId(), asset.getName(), 1))
                .from(party1Addr);

        Tx party2Tx = new Tx()
                .payToAddress(party1Addr, Amount.lovelace(NFT_PRICE.add(minAda)))
                .from(party2Addr);

        Transaction transaction = new QuickTxBuilder(backendService)
                .compose(party1Tx, party2Tx)
                .feePayer(party2Addr)
                .preBalanceTx((context, tx) -> {
                    OutputMergers.mergeOutputsForAddress(party1Addr).apply(context, tx);
                    OutputMergers.mergeOutputsForAddress(party2Addr).apply(context, tx);
                })
                .withSigner(SignerProviders.signerFrom(party1)) //signer for party1
                .buildAndSign();

        //Serialize this transaction and send it to receiver
        String serializedTx = transaction.serializeToHex();

        //Reciver Part - Party2
        //Deserialize the transaction
        Transaction deserializedTx = Transaction.deserialize(HexUtil.decodeHexString(serializedTx));
        Transaction finalTx = party2.sign(deserializedTx);

        //Submit the final signed transaction
        Result<String> result = backendService.getTransactionService().submitTransaction(finalTx.serialize());
        System.out.println(result);

        if (result.isSuccessful())
            checkIfUtxoAvailable(result.getValue(), party2Addr);
    }

    private BigInteger calculateMinAdaInUtxo() throws ApiException, CborSerializationException {
        MinAdaCalculator minAdaCalculator = new MinAdaCalculator(backendService.getEpochService().getProtocolParameters().getValue());
        BigInteger minAda = minAdaCalculator.calculateMinAda(TransactionOutput.builder()
                .address(party2Addr)
                .value(Value.builder()
                        .multiAssets(List.of(
                                MultiAsset.builder()
                                        .policyId(policy.getPolicyId())
                                        .assets(
                                                List.of(
                                                        new Asset(asset.getName(), new BigInteger("1"))
                                                )
                                        ).build()
                        ))
                        .build()).build());

        return minAda;
    }

    private void setupNTFs() throws Exception {
        policy = PolicyUtil.createMultiSigScriptAtLeastPolicy("policy", 1, 1);
        asset = new Asset(UUID.randomUUID().toString().substring(0, 5), new BigInteger("1"));

        Tx tx = new Tx()
                .mintAssets(policy.getPolicyScript(), asset, party1Addr)
                .from(party1Addr);
        Result<String> result = new QuickTxBuilder(backendService)
                .compose(tx)
                .withSigner(SignerProviders.signerFrom(party1))
                .withSigner(SignerProviders.signerFrom(policy))
                .completeAndWait(System.out::println);

        System.out.println(result);
        if (result.isSuccessful())
            checkIfUtxoAvailable(result.getValue(), party1Addr);
    }

    protected void checkIfUtxoAvailable(String txHash, String address) {
        Optional<Utxo> utxo = Optional.empty();
        int count = 0;
        while (utxo.isEmpty()) {
            if (count++ >= 20)
                break;
            List<Utxo> utxos = new DefaultUtxoSupplier(backendService.getUtxoService()).getAll(address);
            utxo = utxos.stream().filter(u -> u.getTxHash().equals(txHash))
                    .findFirst();
            System.out.println("Try to get new output... txhash: " + txHash);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

    public static void main(String[] args) throws Exception {
        P2PNFTTransfer p2PTest = new P2PNFTTransfer();
        p2PTest.testP2PTransaction();
    }
}
