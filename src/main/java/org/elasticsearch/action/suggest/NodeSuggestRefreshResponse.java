package org.elasticsearch.action.suggest;

import org.elasticsearch.action.support.nodes.NodeOperationResponse;
import org.elasticsearch.cluster.node.DiscoveryNode;

public class NodeSuggestRefreshResponse extends NodeOperationResponse {

    protected NodeSuggestRefreshResponse() {
    }

    protected NodeSuggestRefreshResponse(DiscoveryNode node) {
        super(node);
    }

}
