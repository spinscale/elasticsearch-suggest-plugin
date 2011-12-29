package org.elasticsearch.plugin.suggest;

import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.module.suggest.SuggestModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.suggest.RefreshSuggestAction;
import org.elasticsearch.rest.action.suggest.SuggestAction;
import org.elasticsearch.service.suggest.SuggestService;

public class SuggestPlugin extends AbstractPlugin {

    public String name() {
        return "suggest";
    }

    public String description() {
        return "Suggest Plugin";
    }

    @Override public void processModule(Module module) {
        if (module instanceof RestModule) {
            ((RestModule) module).addRestAction(SuggestAction.class);
            ((RestModule) module).addRestAction(RefreshSuggestAction.class);
        }
    }

    @SuppressWarnings("rawtypes")
    @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();
        services.add(SuggestService.class);
        return services;
    }

    @Override public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(SuggestModule.class);
        return modules;
    }

}
