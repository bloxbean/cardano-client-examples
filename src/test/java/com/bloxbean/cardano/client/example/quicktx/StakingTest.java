package com.bloxbean.cardano.client.example.quicktx;

import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.cip.cip20.MessageMetadata;
import com.bloxbean.cardano.client.function.helper.SignerProviders;
import com.bloxbean.cardano.client.quicktx.QuickTxBuilder;
import com.bloxbean.cardano.client.quicktx.Tx;
import com.bloxbean.cardano.client.util.JsonUtil;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StakingTest extends QuickTxBaseTest {

    @Test
    void stakeAddressRegistration_onlyRegistration() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .registerStakeAddress(sender1Addr)
                .attachMetadata(MessageMetadata.create().add("This is a stake registration tx"))
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withTxInspector((txn) -> System.out.println(JsonUtil.getPrettyJson(txn)))
                .completeAndWait(msg -> System.out.println(msg));

        System.out.println(result);
        assertTrue(result.isSuccessful());
    }

    @Test
    @Order(4)
    void stakeAddressDeRegistration_onlyRegistration() {
        QuickTxBuilder quickTxBuilder = new QuickTxBuilder(backendService);
        Tx tx = new Tx()
                .deregisterStakeAddress(AddressProvider.getStakeAddress(new Address(sender1Addr)))
                .attachMetadata(MessageMetadata.create().add("This is a stake deregistration tx"))
                .from(sender1Addr);

        Result<String> result = quickTxBuilder.compose(tx)
                .withSigner(SignerProviders.signerFrom(sender1))
                .withSigner(SignerProviders.stakeKeySignerFrom(sender1))
                .withTxInspector((txn) -> System.out.println(JsonUtil.getPrettyJson(txn)))
                .completeAndWait(msg -> System.out.println(msg));

        System.out.println(result);
        assertTrue(result.isSuccessful());
    }
}
