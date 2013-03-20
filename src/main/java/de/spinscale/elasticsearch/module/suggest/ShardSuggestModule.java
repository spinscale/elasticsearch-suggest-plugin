package de.spinscale.elasticsearch.module.suggest;

import org.elasticsearch.common.inject.AbstractModule;
import de.spinscale.elasticsearch.service.suggest.ShardSuggestService;


public class ShardSuggestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ShardSuggestService.class).asEagerSingleton();
    }

}
