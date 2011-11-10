package org.elasticsearch.plugin.suggest;

import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.suggest.SuggestAction.SuggestAction;
import org.elasticsearch.service.SuggestService;


public class SuggestPlugin extends AbstractPlugin {

    @Inject public SuggestPlugin() {
    }

    public String name() {
        return "suggest";
    }

    public String description() {
        return "Suggest Plugin";
    }

    @Override public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(SuggestAction.class);
        }
    }

    @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(SuggestService.class);
        return services;
    }

}
