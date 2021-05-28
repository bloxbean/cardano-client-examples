package com.bloxbean.cardan.client.sample;

import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.common.model.Networks;

public class AccountGen {

    public static void genAccount() {
//        Account account = new Account(Networks.testnet());
//        String baseAddress = account.baseAddress();
//
//        System.out.println(baseAddress);
//        System.out.println(account.mnemonic());

        String mnemonic = "kit color frog trick speak employ suit sort bomb goddess jewel primary spoil fade person useless measure manage warfare reduce few scrub beyond era";
        Account account = new Account(Networks.testnet(), mnemonic);

        String baseAddress = account.baseAddress();
        System.out.println(baseAddress);
    }

    public static void main(String[] args) {
        genAccount();
    }
}
