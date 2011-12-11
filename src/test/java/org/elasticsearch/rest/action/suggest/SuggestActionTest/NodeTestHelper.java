package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.exists.IndicesExistsResponse;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

public class NodeTestHelper {

    public static Node createNode(String clusterName, int numberOfShards) throws IOException {
        Builder settingsBuilder = ImmutableSettings.settingsBuilder();
        String config = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/elasticsearch.yml"));
        settingsBuilder = settingsBuilder.loadFromSource(config);

        settingsBuilder.put("gateway.type", "none");
        settingsBuilder.put("cluster.name", clusterName);
        settingsBuilder.put("index.number_of_shards", numberOfShards);
        settingsBuilder.put("index.number_of_replicas", 0);

        LogConfigurator.configure(settingsBuilder.build());

        Node node = NodeBuilder.nodeBuilder().settings(settingsBuilder.build()).node();

        IndicesExistsResponse existsResponse = node.client().admin().indices().prepareExists("products").execute().actionGet();
        if (!existsResponse.exists()) {
            String mapping = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/product.json"));
            node.client().admin().indices().prepareCreate("products").execute().actionGet();
            node.client().admin().indices().preparePutMapping("products").setType("product").setSource(mapping).execute().actionGet();
        }

        return node;
    }

}
