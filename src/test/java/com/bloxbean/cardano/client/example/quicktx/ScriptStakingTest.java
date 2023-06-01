package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusScript;
import com.bloxbean.cardano.client.plutus.util.PlutusUtil;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.ScriptTx;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.util.JsonUtil;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ScriptStakingTest extends QuickTxBaseTest {

    String aikenCompiledCode1 = "581801000032223253330043370e00290010a4c2c6eb40095cd1"; //redeemer = 1
    PlutusScript plutusScript1 = PlutusUtil.getPlutusV2Script(aikenCompiledCode1);

    String aikenCompileCode2 = "581801000032223253330043370e00290020a4c2c6eb40095cd1"; //redeemer = 2
    PlutusScript plutusScript2 = PlutusUtil.getPlutusV2Script(aikenCompileCode2);

    String scriptStakeAddress1 = AddressProvider.getRewardAddress(plutusScript1, Networks.testnet()).toBech32();
    String scriptStakeAddress2 = AddressProvider.getRewardAddress(plutusScript2, Networks.testnet()).toBech32();

    @Test
    @Order(1)
    void scriptStakeAddress_registration() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .registerStakeAddress(scriptStakeAddress1)
                .registerStakeAddress(scriptStakeAddress2)
                .attachMetadata(MessageMetadata.create().add("This is a script stake registration tx"))
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withTxInspector((txn) -> System.out.println(JsonUtil.getPrettyJson(txn)))
                .completeAndWait(msg -> System.out.println(msg));

        System.out.println(result);
        assertTrue(result.isSuccessful());

        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }

    @Test
    @Order(2)
    void scriptStakeAddress_deRegistration() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        ScriptTx tx = new ScriptTx()
                .deregisterStakeAddress(scriptStakeAddress1, BigIntPlutusData.of(1))
                .deregisterStakeAddress(scriptStakeAddress2, BigIntPlutusData.of(2))
                .attachMetadata(MessageMetadata.create().add("This is a script stake address deregistration tx"))
                .attachCertificateValidator(plutusScript1)
                .attachCertificateValidator(plutusScript2);

        Result<String> result = quickTxBuilder.compose(tx)
                .feePayer(sender1Addr)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withTxInspector((txn) -> System.out.println(JsonUtil.getPrettyJson(txn)))
                .completeAndWait(msg -> System.out.println(msg));

        System.out.println(result);
        assertTrue(result.isSuccessful());

        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }
}
