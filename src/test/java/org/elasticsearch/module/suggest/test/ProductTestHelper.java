package org.elasticsearch.module.suggest.test;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.elasticsearch.action.admin.indices.refresh.RefreshRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.RandomStringGenerator;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.collect.Maps;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.node.Node;
import org.junit.Assert;

public class ProductTestHelper {

    static void indexProducts(List<Map<String, Object>> products, Node node) throws Exception {
        indexProducts(products, "products", node);
    }

    static void indexProducts(List<Map<String, Object>> products, String index, Node node) throws Exception {
        long currentCount = getCurrentDocumentCount(index, node);
        BulkRequest bulkRequest = new BulkRequest();
        for (Map<String, Object> product : products) {
            IndexRequest indexRequest = new IndexRequest(index, "product", (String)product.get("ProductId"));
            indexRequest.source(product);
            bulkRequest.add(indexRequest);
        }
        BulkResponse response = node.client().bulk(bulkRequest).actionGet();
        if (response.hasFailures()) {
            Assert.fail("Error in creating products: " + response.buildFailureMessage());
        }

        refreshIndex(index, node);
        assertDocumentCountAfterIndexing(index, products.size() + currentCount, node);
    }

    public static List<Map<String, Object>> createProducts(int count) throws Exception {
        List<Map<String, Object>> products = Lists.newArrayList();

        for (int i = 0 ; i < count; i++) {
            Map<String, Object> product = Maps.newHashMap();
            product.put("ProductName", RandomStringGenerator.randomAlphabetic(10));
            product.put("ProductId", i + "_" + RandomStringGenerator.randomAlphanumeric(10));
            products.add(product);
        }

        return products;
    }

    public static void refreshIndex(String index, Node node) throws ExecutionException, InterruptedException {
        node.client().admin().indices().refresh(new RefreshRequest(index)).get();
    }

    public static void assertDocumentCountAfterIndexing(String index, long expectedDocumentCount, Node node) throws Exception {
        assertThat(getCurrentDocumentCount(index, node), is(expectedDocumentCount));
    }

    public static long getCurrentDocumentCount(String index, Node node) {
        return node.client().prepareCount(index).setQuery(QueryBuilders.matchAllQuery()).execute().actionGet(2000).count();
    }

}
