package org.elasticsearch.module.suggest;

import org.elasticsearch.action.GenericAction;
import org.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.action.suggest.TransportSuggestAction;
import org.elasticsearch.action.suggest.TransportSuggestRefreshAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;
public class SuggestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportSuggestAction.class).asEagerSingleton();
        bind(TransportSuggestRefreshAction.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder =
            MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(SuggestAction.INSTANCE).to(TransportSuggestAction.class).asEagerSingleton();
        transportActionsBinder.addBinding(SuggestRefreshAction.INSTANCE).to(TransportSuggestRefreshAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(SuggestAction.NAME).toInstance(SuggestAction.INSTANCE);
        actionsBinder.addBinding(SuggestRefreshAction.NAME).toInstance(SuggestRefreshAction.INSTANCE);
    }
}
