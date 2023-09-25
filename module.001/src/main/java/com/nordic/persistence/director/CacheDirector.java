package com.nordic.persistence.director;


import com.nordic.persistence.CacheBuilder;
import com.nordic.persistence.context.BehindCacheContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.criteria.Subquery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.*;

@Slf4j
@RequiredArgsConstructor
public class CacheDirector<R, V> implements CacheBuilder<R, V> {

    private final EntityManagerFactory factory;
    private Boolean rolledBack = false;
    private RuntimeException exception;
    private BehindCacheContext<R, V> context;

    @Override
    public CacheDirector<R, V> initiateTransaction() {
        this.context = new BehindCacheContext<R, V>().instantiate(this.factory);
        rolledBack = false;
        return this;
    }

    @Override
    public <T> CacheBuilder<R, V>
    persist(Class<?> t, Function<T, V> ke, Function<T, V> pe, UnaryOperator<T> ee, BiConsumer<T, V> bc, List<T> l) {
        return l.isEmpty() ? this : dispatcher(o -> o.parentPersistFlush(t, ke, pe, ee, bc, l));
    }

    @Override
    public <T> CacheBuilder<R, V> persist(Class<?> type, Function<T, V> ke, UnaryOperator<T> ee, List<T> list) {
        return list.isEmpty() ? this : dispatcher(o -> o.persistFlush(type, ke, ee, list));
    }

    @Override
    public <T> CacheBuilder<R, V> persist(T t) {
        return dispatcher(o -> o.persist(t));
    }

    @Override
    public <T> CacheBuilder<R, V> merge(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor) {
        return dispatcher(o -> o.merge(type, t, map, extractor));
    }

    @Override
    public <T> CacheBuilder<R, V> persist(List<T> list) {
        return dispatcher(o -> o.persist(list));
    }

    @Override
    public <T> CacheBuilder<R, V> pessimisticLock(Class<T> type, V v, LockModeType mode) {
        return dispatcher(o -> o.pessimisticLock(type, v, mode));
    }

    @Override
    public <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list) {
        return list.isEmpty() ? this : dispatcher(o -> o.bulkRemove(type, list, "id"));
    }

    @Override
    public <T> boolean remove(Class<T> type, V v, Function<T, Predicate<T>> function) {
        return extract(() -> context.removeCommit(type, v, function));
    }

    @Override
    public <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list, Function<T, Predicate<T>> function) {
        return list.isEmpty() ? this : dispatcher(o -> o.remove(type, list, function));
    }

    @Override
    public <T> CacheBuilder<R, V> remove(Class<T> type, V v, String field) {
        return Objects.isNull(v) ? this : dispatcher(o -> o.bulkRemove(type, v, field));
    }

    @Override
    public <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list, String field) {
        return list.isEmpty() ? this : dispatcher(o -> o.bulkRemove(type, list, field));
    }

    @Override
    public <T, Y> CacheBuilder<R, V> cascade(Class<T> type, Class<Y> orphan, List<V> list, String key) {
        return list.isEmpty() ? this : dispatcher(o -> o.orphanRemoval(type, orphan, list, key));
    }

    @Override
    public <T, Y> CacheBuilder<R, V> append(Class<T> type, Subquery<?> query, String pointer, String key, Y y) {
        return query.getRestriction().
                getExpressions().isEmpty() ? this : dispatcher(o -> o.bulkAppend(type, query, pointer, key, y));
    }

    @Override
    public <T, Y> CacheBuilder<R, V> reduce(Class<T> type, Subquery<?> query, String pointer, String key, Y y) {
        return query.getRestriction().
                getExpressions().isEmpty() ? this : dispatcher(o -> o.bulkReduce(type, query, pointer, key, y));
    }

    @Override
    public <T, S extends Number> CacheBuilder<R, V> bulkUpdate(Class<T> type, String p, String k, S s, V v) {
        return dispatcher(o -> o.bulkUpdate(type, p, k, s, v));
    }

    @Override
    public <T, Y> CacheBuilder<R, V> bulkUpdate(Class<T> type, Subquery<?> query, String k, Map<String, Y> map) {
        return query.getRestriction().
                getExpressions().isEmpty() ? this : dispatcher(o -> o.bulkUpdate(type, query, k, map));
    }

    @Override
    public <T> CacheBuilder<R, V> update(T detach, T merge, BinaryOperator<T> map) {
        return Objects.isNull(detach) ? this : dispatcher(o -> o.update(detach, merge, map));
    }

    @Override
    public <T> CacheBuilder<R, V> update(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor) {
        return Objects.isNull(t) ? this : dispatcher(o -> o.update(type, t, map, extractor));
    }

    @Override
    public <T> CacheBuilder<R, V>
    update(Class<T> type, List<T> list, Function<T, BinaryOperator<T>> operator, Function<T, V> extractor) {
        return list.isEmpty() ? this : dispatcher(o -> o.update(type, list, operator, extractor));
    }

    @Override
    public <T> CacheBuilder<R, V> update(Class<T> type, UnaryOperator<T> operator, V v) {
        dispatcher(o -> o.update(type, operator, v));
        return this;
    }

    @Override
    public CacheBuilder<R, V> flush() {
        return dispatcher(BehindCacheContext::flush);
    }

    @Override
    public EntityManager session() {
        return extract(() -> context.getEntityManager());
    }

    @Override
    public V referencing(V v) {
        return extract(() -> context.referencing(v));
    }

    @Override
    public Map<V, V> references() {
        return extract(() -> context.references());
    }

    @Override
    public Map<V, String> types() {
        return extract(() -> context.types());
    }

    @Override
    public <E extends RuntimeException> void halt(E exception) {
        dispatcher(BehindCacheContext::clear);
        rolledBack = true;
        throw exception;
    }

    @Override
    public CacheBuilder<R, V> commit() {
        if (rolledBack)
            throw exception;
        dispatcher(BehindCacheContext::commit);
        if (rolledBack)
            throw exception;
        return this;
    }

    @Override
    public <T, S extends Number> BehindCacheContext<R, V>.Aggregate<T, S> aggregate(Class<T> tableType, Class<S> type) {
        return extract(() -> context.aggregate(tableType, type));
    }

    @Override
    public <T> T detach(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor) {
        T detached = extract(() -> context.detach(type, t, map, extractor));
        this.clear();
        return detached;
    }

    @Override
    public void clear() {
        this.dispatcher(BehindCacheContext::clear);
    }

    private CacheBuilder<R, V> dispatcher(Consumer<BehindCacheContext<R, V>> action) {
        try {
            if (!rolledBack)
                action.accept(context);
        } catch (RuntimeException e) {
            context.clear();
            rolledBack = true;
            exception = e;
            log.error(e.getLocalizedMessage(), e);
        }
        return this;
    }

    private <T> T extract(Supplier<T> supplier) {
        if (rolledBack)
            throw exception;
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            context.clear();
            exception = e;
            rolledBack = true;
        }
        throw exception;
    }

}
