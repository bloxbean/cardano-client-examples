package com.bloxbean.cardano.client.example;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.AddressService;
import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.VerificationKey;
import com.bloxbean.cardano.client.exception.CborSerializationException;
import com.bloxbean.cardano.client.function.Output;
import com.bloxbean.cardano.client.function.TxBuilder;
import com.bloxbean.cardano.client.function.TxBuilderContext;
import com.bloxbean.cardano.client.function.TxSigner;
import com.bloxbean.cardano.client.function.helper.AuxDataProviders;
import com.bloxbean.cardano.client.function.helper.BalanceTxBuilders;
import com.bloxbean.cardano.client.function.helper.InputBuilders;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.transaction.spec.Transaction;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptAll;
import com.bloxbean.cardano.client.transaction.spec.script.ScriptPubkey;
import org.junit.jupiter.api.Test;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static com.bloxbean.cardano.client.common.CardanoConstants.LOVELACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultisigMultiPartyDistribution extends BaseTest {
    private String mnemonicA = "day witness total swallow soul digital teach all emotion gate fly toward gentle worry bicycle nurse proud milk search lucky twelve trap elder bitter";
    private String mnemonicB = "spice addict bubble city truly cage debris pig smile away enhance cereal dice cotton nominee now alcohol health exclude depend stumble glide observe donate";
    private String mnemonicC = "method payment either organ brave orphan zero dismiss more avoid abuse budget end radar ship drink input neutral leisure glad sentence mobile skin begin";

    private String addressD = "addr_test1qqtxrmaw9ljc67jup0p3qa98hs6taaqh7vm7j5wrp5xdkwn27lrqsdpyfnhjrnlfx0as3nqz5ex94lfsj6u5lrll5yvqpn6rt8";
    private String addressE = "addr_test1qqc23365wjfapsly5drw2md9paynamw7hs4nnm8k545gcwjrc7ugd6mcsgfc29qyak4dvtjmss8xewfaeal77jdrnxpsujk2h5";
    private String addressF = "addr_test1qpk54sjd6xj823c2d6kmps84jacr933ymq92pf0xnh5lnt27wqtc8xnkda520vnwmgalzl3guxghyy9q2q07grgrm28sh9gld4";

    private Account senderA;
    private Account senderB;
    private Account senderC;

    private String nativeScriptAddress;
    private ScriptAll scriptAll;

    public MultisigMultiPartyDistribution() throws CborSerializationException {
        senderA = new Account(Networks.testnet(), mnemonicA);
        senderB = new Account(Networks.testnet(), mnemonicB);
        senderC = new Account(Networks.testnet(), mnemonicC);

        //Create native script with ScriptAll
        ScriptPubkey scriptPubkeyA = ScriptPubkey.create(VerificationKey.create(senderA.publicKeyBytes()));
        ScriptPubkey scriptPubkeyB = ScriptPubkey.create(VerificationKey.create(senderB.publicKeyBytes()));
        ScriptPubkey scriptPubkeyC = ScriptPubkey.create(VerificationKey.create(senderC.publicKeyBytes()));

        scriptAll = new ScriptAll();
        scriptAll.addScript(scriptPubkeyA);
        scriptAll.addScript(scriptPubkeyB);
        scriptAll.addScript(scriptPubkeyC);

        System.out.println(scriptAll);
        nativeScriptAddress = AddressService.getInstance().getEntAddress(scriptAll, Networks.testnet()).toBech32();
        System.out.println("Multisig address : " + nativeScriptAddress);
    }

    public void transfer() throws CborSerializationException, ApiException {

        //Define outputs
        //Output for D
        Output outputD = Output.builder()
                .address(addressD)
                .assetName(LOVELACE)
                .qty(adaToLovelace(2)).build();

        //Output for E
        Output outputE = Output.builder()
                .address(addressE)
                .assetName(LOVELACE)
                .qty(adaToLovelace(3)).build();

        //Output for F
        Output outputF = Output.builder()
                .address(addressF)
                .assetName(LOVELACE)
                .qty(adaToLovelace(4)).build();

        //Define Tx
        TxBuilder txBuilder = outputD.outputBuilder()
                .and(outputE.outputBuilder())
                .and(outputF.outputBuilder())
                .buildInputs(InputBuilders.createFromSender(nativeScriptAddress, nativeScriptAddress))
                .andThen(AuxDataProviders.metadataProvider(MessageMetadata.create().add("Multisig script txn (A,B,C) ---> (D,E,F) using CCL")))
                .andThen((context, transaction) -> {
                    //Add script to the witness. Need to find a way to inject through another helper later instead of manual step
                    transaction.getWitnessSet().getNativeScripts().add(scriptAll);
                })
                .andThen(BalanceTxBuilders.balanceTx(nativeScriptAddress, 3));

        //Define singers
        TxSigner txSigner = SignerProviders.signerFrom(senderA, senderB, senderC);

        //Build and sign multisig txn
        Transaction signedTxn = TxBuilderContext.init(utxoSupplier, protocolParamsSupplier)
                .buildAndSign(txBuilder, txSigner);

        Result<String> result = transactionService.submitTransaction(signedTxn.serialize());

        System.out.println(result);
        assertTrue(result.isSuccessful());
        waitForTransactionHash(result);
    }

    @Test
    public void run() throws Exception {
       MultisigMultiPartyDistribution multisigMultiPartyDistribution = new MultisigMultiPartyDistribution();
       multisigMultiPartyDistribution.transfer();
    }
}
