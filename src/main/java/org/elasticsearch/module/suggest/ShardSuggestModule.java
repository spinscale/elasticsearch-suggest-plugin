package org.elasticsearch.module.suggest;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.service.suggest.ShardSuggestService;


public class ShardSuggestModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ShardSuggestService.class).asEagerSingleton();
    }

}
