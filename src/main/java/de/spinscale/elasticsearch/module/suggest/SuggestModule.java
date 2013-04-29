package de.spinscale.elasticsearch.module.suggest;

import de.spinscale.elasticsearch.action.suggest.statistics.SuggestStatisticsAction;
import de.spinscale.elasticsearch.action.suggest.statistics.TransportSuggestStatisticsAction;
import de.spinscale.elasticsearch.action.suggest.suggest.SuggestAction;
import de.spinscale.elasticsearch.action.suggest.refresh.TransportSuggestRefreshAction;
import org.elasticsearch.action.GenericAction;
import de.spinscale.elasticsearch.action.suggest.refresh.SuggestRefreshAction;
import de.spinscale.elasticsearch.action.suggest.suggest.TransportSuggestAction;
import org.elasticsearch.action.support.TransportAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;

public class SuggestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(TransportSuggestAction.class).asEagerSingleton();
        bind(TransportSuggestRefreshAction.class).asEagerSingleton();
        bind(TransportSuggestStatisticsAction.class).asEagerSingleton();

        MapBinder<GenericAction, TransportAction> transportActionsBinder =
            MapBinder.newMapBinder(binder(), GenericAction.class, TransportAction.class);

        transportActionsBinder.addBinding(SuggestAction.INSTANCE).to(TransportSuggestAction.class).asEagerSingleton();
        transportActionsBinder.addBinding(SuggestRefreshAction.INSTANCE).to(TransportSuggestRefreshAction.class).asEagerSingleton();
        transportActionsBinder.addBinding(SuggestStatisticsAction.INSTANCE).to(TransportSuggestStatisticsAction.class).asEagerSingleton();

        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(SuggestAction.NAME).toInstance(SuggestAction.INSTANCE);
        actionsBinder.addBinding(SuggestRefreshAction.NAME).toInstance(SuggestRefreshAction.INSTANCE);
        actionsBinder.addBinding(SuggestStatisticsAction.NAME).toInstance(SuggestStatisticsAction.INSTANCE);
    }
}
