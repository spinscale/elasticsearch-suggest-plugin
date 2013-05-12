package de.spinscale.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.common.logging.log4j.LogConfigurator;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.ImmutableSettings.Builder;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import java.io.IOException;
import java.util.concurrent.Callable;

public class NodeTestHelper {

    public static Callable<Node> createNode(final String clusterName, final String nodeName, final int numberOfShards) throws IOException {
        return new Callable<Node>() {

            @Override
            public Node call() throws Exception {
                Builder settingsBuilder = ImmutableSettings.settingsBuilder();
                String config = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/elasticsearch.yml"));
                settingsBuilder = settingsBuilder.loadFromSource(config);

                settingsBuilder.put("cluster.name", clusterName);
                settingsBuilder.put("node.name", nodeName);
                settingsBuilder.put("index.number_of_shards", numberOfShards);
                settingsBuilder.put("index.number_of_replicas", 0);

                LogConfigurator.configure(settingsBuilder.build());

                return NodeBuilder.nodeBuilder().settings(settingsBuilder.build()).node();
            }
        };
    }

    public static Callable<Void> stopNode(final Node node) {
        return new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                node.client().close();
                node.close();
                return null;
            }
        };
    }

    public static void createIndexWithMapping(String index, Node node) throws IOException {
        IndicesExistsResponse existsResponse = node.client().admin().indices().prepareExists(index).execute().actionGet();
        if (existsResponse.isExists()) {
            node.client().admin().indices().prepareDelete(index).execute().actionGet();
        }
        String settings = IOUtils.toString(NodeTestHelper.class.getResourceAsStream("/product.json"));
        CreateIndexResponse createIndexResponse = node.client().admin().indices().prepareCreate(index).setSource(settings).execute().actionGet();
        assertThat(createIndexResponse.isAcknowledged(), is(true));
        node.client().admin().cluster().prepareHealth(index).setWaitForGreenStatus().execute().actionGet();
    }
}
