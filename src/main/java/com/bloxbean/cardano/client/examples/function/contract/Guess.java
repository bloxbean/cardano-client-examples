package com.bloxbean.cardano.client.examples.function.contract;

import com.bloxbean.cardano.client.plutus.annotation.Constr;
import com.bloxbean.cardano.client.plutus.annotation.PlutusField;

@Constr
class Guess {
    @PlutusField
    Integer number;

    public Guess(int number) {
        this.number = number;
    }
}
