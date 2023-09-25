package com.nordic.persistence.factory;


import com.nordic.base.descriptor.factory.Factory;
import com.nordic.base.descriptor.factory.SupplierFactory;
import com.nordic.persistence.CacheBuilder;
import com.nordic.persistence.QueryBuilder;
import com.nordic.persistence.director.CacheDirector;
import com.nordic.persistence.director.QueryDirector;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class PersistenceFactory<V extends EntityManagerFactory> {

    private final V secondCache;

    public <T> QueryBuilder<T> queryBuilder(Class<T> type) {
        Factory<QueryBuilder<T>> factory = new SupplierFactory<>();
        return factory.instantiate(() -> new QueryDirector<T>(secondCache).initiate(type));
    }

    public <T> QueryBuilder<T> queryBuilder(Class<T> type, EntityManager session) {
        Factory<QueryBuilder<T>> factory = new SupplierFactory<>();
        return factory.instantiate(() -> new QueryDirector<T>(secondCache).initiate(type, session));
    }

    public <T> CacheBuilder<T, Long> cacheBuilder() {
        Factory<CacheBuilder<T, Long>> factory = new SupplierFactory<>();
        return factory.instantiate(() -> new CacheDirector<>(secondCache));
    }
}
