package de.spinscale.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;
import java.util.concurrent.Callable;

public class NodeTestHelper {

    public static void createIndexWithMapping(String index, Client client) throws IOException {
        IndicesExistsResponse existsResponse = client.admin().indices().prepareExists(index).execute().actionGet();
        if (existsResponse.isExists()) {
            client.admin().indices().prepareDelete(index).execute().actionGet();
        }
        String settings = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/product.json"));
        CreateIndexResponse createIndexResponse = client.admin().indices().prepareCreate(index)
                // TODO randomize
                // TODO randomize replicas
                .setSettings(ImmutableSettings.settingsBuilder().put("index.number_of_shards", 1))
                .setSource(settings).execute().actionGet();
        assertThat(createIndexResponse.isAcknowledged(), is(true));
        client.admin().cluster().prepareHealth(index).setWaitForGreenStatus().execute().actionGet();
    }
}
