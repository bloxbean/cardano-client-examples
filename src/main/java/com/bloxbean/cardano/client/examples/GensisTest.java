package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.Genesis;

public class GensisTest extends BaseTest {

    public void getGenesis() throws ApiException {
        Result<Genesis> result = networkInfoService.getNetworkInfo();
        System.out.println(result.getValue());
    }

    public static void main(String[] args) throws ApiException {
        new GensisTest().getGenesis();
    }
}
