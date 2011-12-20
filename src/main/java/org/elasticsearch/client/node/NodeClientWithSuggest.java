package org.elasticsearch.client.node;

import org.elasticsearch.action.ActionFuture;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.TransportBulkAction;
import org.elasticsearch.action.count.TransportCountAction;
import org.elasticsearch.action.delete.TransportDeleteAction;
import org.elasticsearch.action.deletebyquery.TransportDeleteByQueryAction;
import org.elasticsearch.action.get.TransportGetAction;
import org.elasticsearch.action.get.TransportMultiGetAction;
import org.elasticsearch.action.index.TransportIndexAction;
import org.elasticsearch.action.mlt.TransportMoreLikeThisAction;
import org.elasticsearch.action.percolate.TransportPercolateAction;
import org.elasticsearch.action.search.TransportSearchAction;
import org.elasticsearch.action.search.TransportSearchScrollAction;
import org.elasticsearch.action.suggest.NodesSuggestRefreshRequest;
import org.elasticsearch.action.suggest.NodesSuggestRefreshResponse;
import org.elasticsearch.action.suggest.SuggestRequest;
import org.elasticsearch.action.suggest.SuggestResponse;
import org.elasticsearch.action.suggest.TransportNodesSuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.threadpool.ThreadPool;

public class NodeClientWithSuggest extends NodeClient {

    private TransportSuggestAction suggestAction;
    private TransportNodesSuggestRefreshAction suggestRefreshAction;

    @Inject public NodeClientWithSuggest(Settings settings, ThreadPool threadPool,
            NodeAdminClient admin, TransportIndexAction indexAction,
            TransportDeleteAction deleteAction, TransportBulkAction bulkAction,
            TransportDeleteByQueryAction deleteByQueryAction,
            TransportGetAction getAction,
            TransportMultiGetAction multiGetAction,
            TransportCountAction countAction,
            TransportSearchAction searchAction,
            TransportSearchScrollAction searchScrollAction,
            TransportMoreLikeThisAction moreLikeThisAction,
            TransportPercolateAction percolateAction,
            TransportSuggestAction suggestAction,
            TransportNodesSuggestRefreshAction suggestRefreshAction) {
        super(settings, threadPool, admin, indexAction, deleteAction, bulkAction,
                deleteByQueryAction, getAction, multiGetAction, countAction,
                searchAction, searchScrollAction, moreLikeThisAction, percolateAction);
        this.suggestAction = suggestAction;
        this.suggestRefreshAction = suggestRefreshAction;
    }

    public ActionFuture<SuggestResponse> suggest(SuggestRequest request) {
        return suggestAction.execute(request);
    }

    public void suggest(SuggestRequest request, ActionListener<SuggestResponse> listener) {
        suggestAction.execute(request, listener);
    }

    public ActionFuture<NodesSuggestRefreshResponse> suggestRefresh(NodesSuggestRefreshRequest request) {
        return suggestRefreshAction.execute(request);
    }

    public void suggestRefresh(NodesSuggestRefreshRequest request, ActionListener<NodesSuggestRefreshResponse> listener) {
        suggestRefreshAction.execute(request, listener);
    }

}
