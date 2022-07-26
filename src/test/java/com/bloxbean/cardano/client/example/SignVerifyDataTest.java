package com.bloxbean.cardano.client.example;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.cip.cip30.CIP30DataSigner;
import com.bloxbean.cardano.client.cip.cip30.DataSignature;
import com.bloxbean.cardano.client.common.model.Networks;
import org.junit.jupiter.api.Test;

public class SignVerifyDataTest {

    @Test
    public void run() throws Exception {
        new SignVerifyDataTest().verifyNamiSignature();
//        new SignVerifyDataTest().signAndVerify();
    }

    public void verifyNamiSignature() throws Exception {
        String json = "{\n" +
                "  \"signature\" : \"845846a2012767616464726573735839003175d03902583e82037438cc86732f6e539f803f9a8b2d4ee164b9d0c77e617030631811f60a1f8a8be26d65a57ff71825b336cc6b76361da166686173686564f44b48656c6c6f20576f726c64584036c2151e1230364b0bf9e40cb65dbdca4c5decf4187e3c5511945d410ea59a1e733b5e68178c234979053ed75b0226ba826fb951c5a79fabf10bddcabda8dc05\",\n" +
                "  \"key\" : \"a4010103272006215820a5f73966e73d0bb9eadc75c5857eafd054a0202d716ac6dde00303ee9c0019e3\"\n" +
                "}";

        //Load signature
        DataSignature dataSignature = DataSignature.from(json);
        //verify
        boolean verified = CIP30DataSigner.INSTANCE.verify(dataSignature);

        System.out.println("Verification Status: " + verified);
    }

    public void signAndVerify() throws Exception {
        String mnemonic = "nice orient enjoy teach jump office alert inquiry apart unaware seat tumble unveil device have bullet morning eyebrow time image embody divide version uniform";
        Account account = new Account(Networks.testnet(), mnemonic);

        byte[] payload = "Hello".getBytes();

        //Sign
        Address address = new Address(account.baseAddress());
        DataSignature dataSignature = CIP30DataSigner.INSTANCE.signData(address.getBytes(), payload, account);

        //Verify
        boolean verified = CIP30DataSigner.INSTANCE.verify(dataSignature);

        System.out.println("Verification status: " + verified);
    }
}
