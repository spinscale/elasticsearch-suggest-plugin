package org.elasticsearch.module.suggest;

import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.service.suggest.Suggester;

public class SuggestModule extends AbstractModule {

    @Override protected void configure() {
        bind(Suggester.class).asEagerSingleton();

    }
}
