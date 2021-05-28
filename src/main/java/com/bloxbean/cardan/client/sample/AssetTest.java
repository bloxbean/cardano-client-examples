package com.bloxbean.cardan.client.sample;

import com.bloxbean.cardano.client.backend.api.AssetService;
import com.bloxbean.cardano.client.backend.exception.ApiException;
import com.bloxbean.cardano.client.backend.model.Asset;
import com.bloxbean.cardano.client.backend.model.Result;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.client.util.JsonUtil;

import java.nio.charset.StandardCharsets;

public class AssetTest extends BaseTest {

    public AssetTest() {
        super();
    }

    public void getAssets() throws ApiException {
        String policyId = "d11b0562dcac7042636c9dbb44897b38675da0d613d30f98a541a290";
        String assetName = HexUtil.encodeHexString("TestCoin".getBytes(StandardCharsets.UTF_8));

        Result<Asset> asset = assetService.getAsset(policyId + assetName);

        System.out.println(JsonUtil.getPrettyJson(asset.getValue()));
    }

    public static void main(String[] args) throws ApiException {
        AssetTest assetTest = new AssetTest();
        for(int i=0; i< 30;i++) {
            assetTest.getAssets();
        }
        System.exit(1);
    }
}
