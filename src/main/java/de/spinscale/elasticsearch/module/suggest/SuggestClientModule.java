package de.spinscale.elasticsearch.module.suggest;

import de.spinscale.elasticsearch.action.suggest.SuggestAction;
import org.elasticsearch.action.GenericAction;
import de.spinscale.elasticsearch.action.suggest.SuggestRefreshAction;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.multibindings.MapBinder;


public class SuggestClientModule extends AbstractModule {

    @Override
    protected void configure() {
        @SuppressWarnings("rawtypes")
        MapBinder<String, GenericAction> actionsBinder = MapBinder.newMapBinder(binder(), String.class, GenericAction.class);
        actionsBinder.addBinding(SuggestAction.NAME).toInstance(SuggestAction.INSTANCE);
        actionsBinder.addBinding(SuggestRefreshAction.NAME).toInstance(SuggestRefreshAction.INSTANCE);
    }
}
