package com.bloxbean.cardano.client.example.derivation;


import com.bloxbean.cardano.client.account.Account;
import com.bloxbean.cardano.client.address.Address;
import com.bloxbean.cardano.client.address.AddressProvider;
import com.bloxbean.cardano.client.common.model.Networks;
import com.bloxbean.cardano.client.crypto.Bech32;
import com.bloxbean.cardano.client.crypto.bip32.HdKeyGenerator;
import com.bloxbean.cardano.client.crypto.bip32.key.HdPublicKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class demonstrates how to derive multiple addresses (base and enterprise) from a given account level public key.
 * Derivation path according to CIP1852  m / purpose' / coin_type' / account' / role / index . Example: m / 1852' / 1815' / 0' / 0 / 0
 * As keys at role/index level are not hardened, they can be derived from the account level public key.
*/
public class AddressDerivationFromPublicKey {


    /**
     * Derive multiple addresses (base and enterprise) from a given account level public key.
     * Get the account level public key from your wallet. In Eternl wallet, you can get the account public key from "Wallet & Account Settings".
     * The following account pub key is taken from Eternl wallet.
     *
     * In the following method, we derive 20 base addresses and 20 enterprise addresses from the given wallet's account public key (account = 0)
     * @return
     */
    public List<String> deriveFromPublicKey() {

        // Account Key:0 (m/1852'/1815'/0')
        String accoutPubKey = "xpub1ytx7lt4rs28wehx47gt6xkyl8tphjy9lznpjh7wy3jpdnuveegq4sstrxvvhl0elm9aulpzjhfkvfjtvncp3xnstd6ypa2m9my9kl9cmprz00";
        byte[] accountPubKeyBytes = Bech32.decode(accoutPubKey).data;
        HdPublicKey accountKey = HdPublicKey.fromBytes(accountPubKeyBytes);

        HdKeyGenerator hdKeyGenerator = new HdKeyGenerator();

        //Derive Stake Key and Address
        //role = 2, staking key (m/1852'/1815'/0'/2/0)
        HdPublicKey stakeRolePublicKey = hdKeyGenerator.getChildPublicKey(accountKey, 2);
        HdPublicKey stakePublicKey = hdKeyGenerator.getChildPublicKey(stakeRolePublicKey, 0);
        Address stakeAddress = AddressProvider.getRewardAddress(stakePublicKey, Networks.testnet());

        System.out.println("Account Public Key: " + accoutPubKey);
        System.out.println("Stake Address: " + stakeAddress.toBech32());
        System.out.println("------------------------------------------------");

        //Derive base/ent addresses from account key at index 0 to 19
        //Role=0
        HdPublicKey role0Key = hdKeyGenerator.getChildPublicKey(accountKey, 0);

        //(m/1852'/1815'/0'/0/i) where i=0 to 19
        List<String> addresses = new ArrayList<>();
        for (int i=0; i<20; i++) {
            HdPublicKey indexKey = hdKeyGenerator.getChildPublicKey(role0Key, i);
            Address indexAddress = AddressProvider.getBaseAddress(indexKey, stakePublicKey, Networks.testnet());
            Address entAddress = AddressProvider.getEntAddress(indexKey, Networks.testnet());

            System.out.println("Derivation Path: m/1852'/1815'/0'/" + i);
            System.out.println("BaseAddress:" + indexAddress.toBech32());
            System.out.println("Enterprise Address: " + entAddress.toBech32());
            System.out.println("------------------------------------------------");

            addresses.add(indexAddress.toBech32());
        }

        return addresses;
    }

    //Test method to verify derived addresses from account key with mnemonic derived addresses
    @Test
    public void verifyAccountKeyAddressWithMnemonicDerivedAddresses() {
        String originalMnemonic = "skull imitate fatal brain extra stem dress until patrol yellow rule mobile case acoustic invest raw second vacuum that average huge artwork dilemma claw";

        var accountKeyDerivedAddresses = deriveFromPublicKey();

        List<String> addresses = new ArrayList<>();
        for (int i=0; i<20; i++) {
            Account account = new Account(Networks.testnet(), originalMnemonic, i);
            addresses.add(account.baseAddress());
        }

        assertThat(addresses).containsAll(accountKeyDerivedAddresses);

    }

    public static void main(String[] args) {
        AddressDerivationFromPublicKey publicKeyDerivation = new AddressDerivationFromPublicKey();
        publicKeyDerivation.deriveFromPublicKey();
    }
}
