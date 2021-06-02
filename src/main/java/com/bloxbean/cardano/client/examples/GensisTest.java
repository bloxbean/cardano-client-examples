package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Genesis;
import com.bloxbean.cardano.client.backend.model.Result;

public class GensisTest extends BaseTest {

    public void getGenesis() throws ApiException {
        Result<Genesis> result = networkInfoService.getNetworkInfo();
        System.out.println(result.getValue());
    }

    public static void main(String[] args) throws ApiException {
        new GensisTest().getGenesis();
    }
}
