package com.bloxbean.cardano.client.example;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.Genesis;
import org.junit.jupiter.api.Test;

public class GensisTest extends BaseTest {

    @Test
    public void getGenesis() throws ApiException {
        Result<Genesis> result = networkInfoService.getNetworkInfo();
        System.out.println(result.getValue());
    }

    public static void main(String[] args) throws ApiException {
        new GensisTest().getGenesis();
    }
}
