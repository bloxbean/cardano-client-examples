package com.bloxbean.cardano.client.examples;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.api.model.Result;
import com.bloxbean.cardano.client.backend.model.Block;
import com.bloxbean.cardano.client.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockServiceTest extends BaseTest {
    Logger LOG = LoggerFactory.getLogger(BaseTest.class);

    public BlockServiceTest() {
        super();
    }

    public void fetchLatestBlock() throws ApiException {
        Result<Block> blockResult = blockService.getLatestBlock();

        System.out.println(JsonUtil.getPrettyJson(blockResult.getValue()));
        LOG.info("Block fetched");
    }

    public static void main(String[] args) throws ApiException {
        new BlockServiceTest().fetchLatestBlock();
        System.exit(1);
    }
}
