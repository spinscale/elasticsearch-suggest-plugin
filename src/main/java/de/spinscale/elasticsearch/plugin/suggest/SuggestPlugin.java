package de.spinscale.elasticsearch.plugin.suggest;

import de.spinscale.elasticsearch.module.suggest.ShardSuggestModule;
import de.spinscale.elasticsearch.module.suggest.SuggestClientModule;
import de.spinscale.elasticsearch.module.suggest.SuggestModule;
import de.spinscale.elasticsearch.rest.action.suggest.RestRefreshSuggestAction;
import de.spinscale.elasticsearch.rest.action.suggest.RestStatisticsAction;
import de.spinscale.elasticsearch.rest.action.suggest.RestSuggestAction;
import de.spinscale.elasticsearch.service.suggest.SuggestService;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.Version;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.common.component.LifecycleComponent;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Module;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.plugins.AbstractPlugin;
import org.elasticsearch.rest.RestModule;

import java.util.Collection;
import java.util.Locale;

public class SuggestPlugin extends AbstractPlugin {

    private final Settings settings;

    @Inject
    public SuggestPlugin(Settings settings) {
        this.settings = settings;

        // Check if the plugin is newer than elasticsearch
        // First failure, if the versions dont match
        // Second failure: if the Version specified in before() does not yet exist, therefore catching Throwable
        try {
            if (Version.CURRENT.before(Version.V_0_90_3)) {
                throw new Exception();
            }
        } catch (Throwable e) {
            String error = String.format(Locale.ROOT, "The elasticsearch suggest plugin needs a newer version of elasticsearch than %s", Version.CURRENT);
            throw new ElasticsearchException(error);
        }
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
        restModule.addRestAction(RestRefreshSuggestAction.class);
        restModule.addRestAction(RestStatisticsAction.class);
    }

    @SuppressWarnings("rawtypes")
    @Override public Collection<Class<? extends LifecycleComponent>> services() {
        Collection<Class<? extends LifecycleComponent>> services = Lists.newArrayList();

        if (!isClient()) {
            services.add(SuggestService.class);
        }
        return services;
    }

    @Override public Collection<Class<? extends Module>> modules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        if (isClient()) {
            modules.add(SuggestClientModule.class);
        } else {
            modules.add(SuggestModule.class);
        }
        return modules;
    }

    @Override
    public Collection<Class<? extends Module>> shardModules() {
        Collection<Class<? extends Module>> modules = Lists.newArrayList();
        modules.add(ShardSuggestModule.class);
        return modules;
    }

    private boolean isClient() {
        return settings.getAsBoolean("node.client", false);
    }
}
