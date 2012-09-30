package org.elasticsearch.rest.action.suggest.SuggestActionTest;

import static org.elasticsearch.rest.action.suggest.SuggestActionTest.NodeTestHelper.*;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.IOException;

import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.client.action.suggest.SuggestRequestBuilder;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.node.Node;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class TransportClientTest {

    private final String clusterName = "SuggestIntegrationTestCluster_" + Math.random();
    private Node node;
    private TransportClient client;

    @Before
    public void startElasticSearch() throws IOException {
        node = createNode(clusterName, 1);
    }

    @After
    public void shutdownElasticsearch() throws Exception {
        if (client != null) client.close();
        if (node != null) node.close();
    }

    @Test
    public void transportClientShouldStartWithoutException() throws Exception {
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).put("node.client", true).build();
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));
        assertThat(client.listedNodes().size(), is(1));
    }

    @Ignore("Broken, must be working without spitting exceptions or NPE")
    @Test
    public void suggestQueryWithTransportClientShouldWork() throws Exception {
        Settings settings = ImmutableSettings.settingsBuilder().put("cluster.name", clusterName).put("node.client", true).build();
        client = new TransportClient(settings).addTransportAddress(new InetSocketTransportAddress("localhost", 9300));

        SuggestRequestBuilder builder = new SuggestRequestBuilder(client).setIndices("foo").field("foo").term("bar");
        SuggestResponse response = builder.execute().actionGet();
        assertThat(response.suggestions(), hasSize(1));

    }
}
