package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.api.DefaultTransactionProcessor;
import com.bloxbean.cardano.client.backend.api.DefaultUtxoSupplier;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.transaction.spec.governance.Anchor;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;
import org.junit.jupiter.api.Test;

import static com.bloxbean.cardano.client.common.ADAConversionUtil.adaToLovelace;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GovTxTest extends QuickTxBaseTest {
    @Test
    void registerDrep() throws Exception {
        var protocolParam = backendService.getEpochService().getProtocolParameters().getValue();
        protocolParam.setDrepDeposit(adaToLovelace(500));
        protocolParam.setGovActionDeposit(adaToLovelace(100000));

        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(new DefaultUtxoSupplier(backendService.getUtxoService()),
                () -> protocolParam,
                new DefaultTransactionProcessor(backendService.getTransactionService())
                );


        var anchor = new Anchor("https://pages.bloxbean.com/cardano-stake/bloxbean-pool.json",
                HexUtil.decodeHexString("bafef700c0039a2efb056a665b3a8bcd94f8670b88d659f7f3db68340f6f0937"));

        Tx drepRegTx = new Tx()
                .registerDRep(sender1, anchor)
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(drepRegTx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withSigner(SignerProviders.signerFrom(sender1.drepHdKeyPair()))
                .withTxInspector(transaction -> {
                    System.out.println(JsonUtil.getPrettyJson(transaction));
                })
                .completeAndWait(System.out::println);

        System.out.println("DRepId : " + sender1.drepId());


        System.out.println(result);
        assertTrue(result.isSuccessful());

        checkIfUtxoAvailable(result.getValue(), sender1Addr);
    }
}
