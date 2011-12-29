package org.elasticsearch.module.suggest;

import org.elasticsearch.action.suggest.TransportNodesSuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.service.suggest.Suggester;

public class SuggestModule extends AbstractModule {

    @Override protected void configure() {
        bind(TransportSuggestAction.class).asEagerSingleton();
        bind(TransportNodesSuggestRefreshAction.class).asEagerSingleton();
        bind(Suggester.class).asEagerSingleton();
    }
}
