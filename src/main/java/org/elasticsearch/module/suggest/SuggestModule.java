package org.elasticsearch.module.suggest;

import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportNodesSuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
import org.elasticsearch.service.suggest.Suggester;

public class SuggestModule extends AbstractModule {

    @Override protected void configure() {
        bind(TransportSuggestAction.class).asEagerSingleton();
        bind(TransportNodesSuggestRefreshAction.class).asEagerSingleton();
        bind(Suggester.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder =
            MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(SuggestAction.INSTANCE).to(TransportSuggestAction.class).asEagerSingleton();
        transportActionsBinder.addBinding(SuggestRefreshAction.INSTANCE).to(TransportNodesSuggestRefreshAction.class).asEagerSingleton();
    }
}
