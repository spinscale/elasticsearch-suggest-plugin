package org.elasticsearch.action.suggest;

import java.util.concurrent.atomic.AtomicReferenceArray;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.action.index.IndexAction;
import org.elasticsearch.action.support.nodes.TransportNodesOperationAction;
import org.elasticsearch.cluster.ClusterName;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.service.NodeService;
import org.elasticsearch.service.suggest.Suggester;
import org.elasticsearch.threadpool.ThreadPool;
import org.elasticsearch.transport.TransportService;

public class TransportNodesSuggestRefreshAction extends TransportNodesOperationAction<NodesSuggestRefreshRequest, NodesSuggestRefreshResponse, NodeSuggestRefreshRequest, NodeSuggestRefreshResponse> {

    private Suggester suggester;
    private NodeService nodeService;

    @Inject public TransportNodesSuggestRefreshAction(Settings settings,
            ClusterName clusterName, ThreadPool threadPool, ClusterService clusterService,
            TransportService transportService, NodeService nodeService, Suggester suggester) {
        super(settings, clusterName, threadPool, clusterService, transportService);
        this.suggester = suggester;
        this.nodeService = nodeService;
    }

    @Override
    protected String transportAction() {
        return "/indices/suggest/refresh";
    }

    @Override
    protected String executor() {
        return ThreadPool.Names.INDEX;
    }

    @Override
    protected NodesSuggestRefreshRequest newRequest() {
        return new NodesSuggestRefreshRequest();
    }

    @Override
    protected NodesSuggestRefreshResponse newResponse(NodesSuggestRefreshRequest request, AtomicReferenceArray nodesResponses) {
        // Why should I collect the responses here?
        for (int i = 0; i < nodesResponses.length(); i++) {
            Object resp = nodesResponses.get(i);
            if (resp instanceof NodeSuggestRefreshResponse) {
                NodeSuggestRefreshResponse response = (NodeSuggestRefreshResponse) resp;
                logger.trace("Got refresh response from [{}]", response.node().id());
            } else {
                logger.trace("Got some response from [{}]", resp);
            }
        }

        return new NodesSuggestRefreshResponse();
    }

    @Override
    protected NodeSuggestRefreshRequest newNodeRequest() {
        return new NodeSuggestRefreshRequest();
    }

    @Override
    protected NodeSuggestRefreshRequest newNodeRequest(String nodeId,
            NodesSuggestRefreshRequest request) {
        return new NodeSuggestRefreshRequest(nodeId);
    }

    @Override
    protected NodeSuggestRefreshResponse newNodeResponse() {
        return new NodeSuggestRefreshResponse(nodeService.info().getNode());
    }

    @Override
    protected NodeSuggestRefreshResponse nodeOperation(
            NodeSuggestRefreshRequest request) throws ElasticSearchException {
        logger.trace("TransportNodesSuggestRefreshAction.nodeOperation() called");
        suggester.update();
        return new NodeSuggestRefreshResponse(nodeService.info().getNode());
    }

    @Override
    protected boolean accumulateExceptions() {
        return false;
    }

}
