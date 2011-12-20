package org.elasticsearch.action.suggest;

import org.elasticsearch.action.support.nodes.NodeOperationRequest;

public class NodeSuggestRefreshRequest extends NodeOperationRequest {

    public NodeSuggestRefreshRequest() {}

    public NodeSuggestRefreshRequest(String nodeId) {
        super(nodeId);
    }
}
