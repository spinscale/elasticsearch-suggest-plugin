package org.elasticsearch.plugin.suggest;

import java.util.Collection;

import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.module.suggest.SuggestModule;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;
import org.elasticsearch.rest.action.suggest.RefreshSuggestAction;
import org.elasticsearch.rest.action.suggest.RestSuggestAction;
import org.elasticsearch.service.suggest.SuggestService;

public class SuggestPlugin extends AbstractPlugin {

    private final Settings settings;

    @Inject
    public SuggestPlugin(Settings settings) {
        this.settings = settings;
    }

    @Override
    public String name() {
        return "suggest";
    }

    @Override
    public String description() {
        return "Suggest Plugin";
    }

    public void onModule(RestModule restModule) {
        restModule.addRestAction(RestSuggestAction.class);
        restModule.addRestAction(RefreshSuggestAction.class);
    }

    @SuppressWarnings("rawtypes")
    @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();

        if (!settings.getAsBoolean("node.client", false)) {
            services.add(SuggestService.class);
        }
        return services;
    }

    @Override public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();

        if (!settings.getAsBoolean("node.client", false)) {
            modules.add(SuggestModule.class);
        }
        return modules;
    }

}
