package com.bloxbean.cardano.client.example;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;
import org.junit.jupiter.api.Test;

public class AccountGen {

    @Test
    public static void genAccount() {
        String mnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account account = new Account(Networks.testnet(), mnemonic);

        String baseAddress = account.baseAddress();
        System.out.println(baseAddress);
    }

    public static void main(String[] args) {
        genAccount();
    }
}
