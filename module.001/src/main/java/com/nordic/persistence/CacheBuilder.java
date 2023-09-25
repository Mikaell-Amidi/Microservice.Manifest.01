package com.nordic.persistence;


import com.nordic.persistence.context.BehindCacheContext;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.criteria.Subquery;
import java.util.List;
import java.util.Map;
import java.util.function.*;

public interface CacheBuilder<R, V> {

    <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list, String field);

    <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list);

    <T> CacheBuilder<R, V> remove(Class<T> type, List<V> list, Function<T, Predicate<T>> function);

    <T> CacheBuilder<R, V> remove(Class<T> type, V v, String field);

    <T, Y> CacheBuilder<R, V> cascade(Class<T> type, Class<Y> orphan, List<V> list, String key);

    <T, S extends Number> CacheBuilder<R, V> bulkUpdate(Class<T> type, String pointer, String key, S s, V v);

    <T, Y> CacheBuilder<R, V> bulkUpdate(Class<T> type, Subquery<?> query, String key, Map<String, Y> map);

    <T> CacheBuilder<R, V> update(Class<T> type, UnaryOperator<T> operator, V v);

    <T> CacheBuilder<R, V> update(T detach, T merge, BinaryOperator<T> map);

    <T> CacheBuilder<R, V> update(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor);

    <T> CacheBuilder<R, V> update(Class<T> type, List<T> t, Function<T, BinaryOperator<T>> s, Function<T, V> x);

    <T, Y> CacheBuilder<R, V> append(Class<T> type, Subquery<?> query, String pointer, String key, Y y);

    <T, Y> CacheBuilder<R, V> reduce(Class<T> type, Subquery<?> query, String pointer, String key, Y y);

    <T> CacheBuilder<R, V> merge(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor);

    <T> CacheBuilder<R, V> persist(T t);

    <T> CacheBuilder<R, V> persist(List<T> list);

    <T> CacheBuilder<R, V> persist(Class<?> type, Function<T, V> ke, UnaryOperator<T> ee, List<T> list);

    <T> CacheBuilder<R, V> persist
            (Class<?> t, Function<T, V> ke, Function<T, V> pe, UnaryOperator<T> ee, BiConsumer<T, V> bc, List<T> l);

    <T> CacheBuilder<R, V> pessimisticLock(Class<T> type, V v, LockModeType mode);

    CacheBuilder<R, V> initiateTransaction();

    CacheBuilder<R, V> flush();

    CacheBuilder<R, V> commit();

    <T, S extends Number> BehindCacheContext<R, V>.Aggregate<T, S> aggregate(Class<T> tableType, Class<S> type);

    <T> T detach(Class<T> type, T t, BinaryOperator<T> map, Function<T, V> extractor);

    <T> boolean remove(Class<T> type, V v, Function<T, Predicate<T>> function);

    EntityManager session();

    Map<V, V> references();

    Map<V, String> types();

    V referencing(V v);

    <E extends RuntimeException> void halt(E e);

    void clear();

}
