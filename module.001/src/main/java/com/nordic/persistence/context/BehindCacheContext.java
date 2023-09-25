package com.nordic.persistence.context;

import lombok.RequiredArgsConstructor;

import javax.persistence.*;
import javax.persistence.criteria.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.*;


@RequiredArgsConstructor
public class BehindCacheContext<R, V> {

    private final List<V> parents = new ArrayList<>();
    private final Map<V, String> types = new HashMap<>();
    private final List<V> references = new ArrayList<>();
    private final List<Query> queries = new ArrayList<>();
    private final Map<V, V> referenceMap = new HashMap<>();
    private Boolean pessimistic;
    private EntityTransaction transaction;
    private CriteriaBuilder criteriaBuilder;
    private Consumer<EntityManager> pessimisticLocker;
    private EntityManager entityManager;

    public BehindCacheContext<R, V> instantiate(EntityManagerFactory factory) {
        entityManager = factory.createEntityManager();
        return contextBuilder(entityManager);
    }

    private BehindCacheContext<R, V> contextBuilder(EntityManager entityManager) {
        criteriaBuilder = entityManager.getCriteriaBuilder();
        transaction = entityManager.getTransaction();
        references.clear();
        parents.clear();
        referenceMap.clear();
        queries.clear();
        types.clear();
        pessimistic = false;
        return this;
    }

    public <T, S extends Number> Aggregate<T, S> aggregate(Class<T> tableType, Class<S> type) {
        return new Aggregate<>(tableType, type);
    }

    public <T> boolean removeCommit(Class<T> type, V v, Function<T, Predicate<T>> function) {
        T t = entityManager.find(type, v);
        return !Objects.isNull(t) && function.apply(t).test(t) && removeCommit(t);
    }

    public <T> T detach(Class<T> type, T managed, BinaryOperator<T> map, Function<T, V> extractor) {
        managed = map.apply(entityManager.find(type, extractor.apply(managed)), managed);
        this.commit();
        return managed;
    }

    public <T> void persist(T t) {
        entityManager.persist(t);
    }

    public <T> void merge(Class<T> type, T managed, BinaryOperator<T> map, Function<T, V> extractor) {
        T t = entityManager.find(type, extractor.apply(managed));
        if (Objects.isNull(t)) entityManager.persist(managed);
        else map.apply(t, managed);
    }

    public <T> void persist(List<T> list) {
        list.forEach(t -> entityManager.persist(t));
    }

    public <T> void persistFlush(Class<?> type, Function<T, V> ke, UnaryOperator<T> ee, List<T> list) {
        list.forEach(i -> {
            references.add(ke.apply(i));
            types.put(ke.apply(i), type.getSimpleName());
            entityManager.persist(ee.apply(i));
        });
        flush();
        for (int i = 0; i < references.size(); i++) {
            referenceMap.put(references.get(i), ke.apply(list.get(i)));
        }
        references.clear();
    }

    public <T> void parentPersistFlush(Class<?> type, Function<T, V> ke, Function<T, V> pe,
                                       UnaryOperator<T> ee, BiConsumer<T, V> bc, List<T> list) {
        list.forEach(i -> {
            references.add(ke.apply(i));
            types.put(ke.apply(i), type.getSimpleName());
            parents.add(pe.apply(i));
            entityManager.persist(ee.apply(i));
        });
        flush();
        for (int i = 0; i < references.size(); i++) {
            referenceMap.put(references.get(i), ke.apply(list.get(i)));
        }
        for (int i = 0; i < references.size(); i++) {
            if (Objects.nonNull(parents.get(i))) {
                bc.accept(list.get(i), referenceMap.getOrDefault(parents.get(i), parents.get(i)));
                entityManager.merge(list.get(i));
            }
        }
        references.clear();
        parents.clear();
    }

    public <T> void pessimisticLock(Class<T> type, V v, LockModeType mode) {
        pessimistic = true;
        this.pessimisticLocker = o -> o.find(type, v, mode);
    }

    public <T, Y> void bulkAppend(Class<T> type, Subquery<?> query, String pointer, String key, Y y) {
        CriteriaUpdate<T> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(type);
        Root<T> updateRoot = criteriaUpdate.from(type);
        Path<String> path = updateRoot.get(pointer);
        criteriaUpdate.set(path, criteriaBuilder.concat(path, y.toString()));
        queries.add(entityManager.createQuery(criteriaUpdate.where(updateRoot.get(key).in(query))));
    }

    public <T, Y> void bulkReduce(Class<T> type, Subquery<?> query, String pointer, String key, Y y) {
        CriteriaUpdate<T> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(type);
        Root<T> updateRoot = criteriaUpdate.from(type);
        Path<String> path = updateRoot.get(pointer);
        criteriaUpdate.set(path, criteriaBuilder.function("REPLACE",
                String.class, path, criteriaBuilder.literal(y), criteriaBuilder.literal("")));
        queries.add(entityManager.createQuery(criteriaUpdate.where(updateRoot.get(key).in(query))));
    }

    public <T, Y> void bulkUpdate(Class<T> type, Subquery<?> query, String key, Map<String, Y> map) {
        CriteriaUpdate<T> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(type);
        Root<T> updateRoot = criteriaUpdate.from(type);
        map.forEach(criteriaUpdate::set);
        queries.add(entityManager.createQuery(criteriaUpdate.where(updateRoot.get(key).in(query))));
    }

    public <T, S extends Number> void bulkUpdate(Class<T> updateType, String pointer, String key, S s, V v) {
        CriteriaUpdate<T> criteriaUpdate = criteriaBuilder.createCriteriaUpdate(updateType);
        Root<T> updateRoot = criteriaUpdate.from(updateType);
        criteriaUpdate.set(updateRoot.get(pointer), s);
        queries.add(entityManager.createQuery(criteriaUpdate.where(criteriaBuilder.equal(updateRoot.get(key), v))));
    }

    public <T> void update(Class<T> type, UnaryOperator<T> operator, V v) {
        this.safeDetachMerge(entityManager.find(type, v), operator);
    }

    public <T> void update(T detach, T merge, BinaryOperator<T> map) {
        map.apply(detach, merge);
    }

    public <T> void update(Class<T> type, T managed, BinaryOperator<T> map, Function<T, V> extractor) {
        map.apply(entityManager.find(type, extractor.apply(managed)), managed);
    }

    public <T> void update(Class<T> type, List<T> list, Function<T, BinaryOperator<T>> map, Function<T, V> extractor) {
        list.stream().filter(i -> Objects.nonNull(entityManager.find(type, extractor.apply(i))))
                .forEach(i -> map.apply(i).apply(entityManager.find(type, extractor.apply(i)), i));
    }

    public <T> void update(Class<T> type, BinaryOperator<T> map, Function<T, V> extractor, List<T> list) {
        list.forEach(i -> map.apply(entityManager.find(type, extractor.apply(i)), i));
    }

    public <T> void remove(Class<T> type, V v, Predicate<T> predicate) {
        T t = entityManager.find(type, v);
        if (predicate.test(t))
            entityManager.remove(t);
    }

    public <T> void remove(Class<T> type, List<V> list, Function<T, Predicate<T>> function) {
        list.forEach(i -> {
            T t = entityManager.find(type, i);
            if (function.apply(t).test(t))
                entityManager.remove(t);
        });
    }

    public <T> void bulkRemove(Class<T> type, List<V> list, String field) {
        CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(type);
        Root<T> deleteRoot = criteriaDelete.from(type);
        queries.add(entityManager.createQuery(criteriaDelete.where(deleteRoot.get(field).in(list))));
    }

    public <T> void bulkRemove(Class<T> type, V v, String field) {
        CriteriaDelete<T> criteriaDelete = criteriaBuilder.createCriteriaDelete(type);
        Root<T> deleteRoot = criteriaDelete.from(type);
        queries.add(entityManager.createQuery(criteriaDelete.where(criteriaBuilder.equal(deleteRoot.get(field), v))));
    }

    public <T, Y> void orphanRemoval(Class<T> type, Class<Y> orphan, List<V> list, String key) {
        CriteriaDelete<Y> criteriaDelete = criteriaBuilder.createCriteriaDelete(orphan);
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(type);

        Root<Y> deleteRoot = criteriaDelete.from(orphan);
        Root<T> queryRoot = criteriaQuery.from(type);

        criteriaQuery.select(queryRoot.get(key));

        queries.add(entityManager.createQuery(criteriaDelete
                .where(deleteRoot.in(entityManager.createQuery(criteriaQuery
                        .where(queryRoot.get("id").in(list))).getResultList()))));
    }

    public <T> void safeDetachMerge(T managed, UnaryOperator<T> operator) {
        if (Objects.nonNull(managed)) {
            entityManager.detach(managed);
            entityManager.merge(operator.apply(managed));
        }
    }

    public void concurrencyControl() {
        if (!transaction.isActive() && entityManager.isOpen())
            transaction.begin();
        if (pessimistic)
            pessimisticLocker.accept(this.entityManager);
    }

    public void commit() {
        try {
            this.concurrencyControl();
            queries.forEach(Query::executeUpdate);
            transaction.commit();
            entityManager.close();
        } catch (Exception exception) {
            if (entityManager.isOpen()) {
                if (transaction.isActive())
                    transaction.rollback();
                entityManager.close();
            }
            throw exception;
        }
    }

    public void flush() {
        try {
            this.concurrencyControl();
            entityManager.flush();
        } catch (Exception exception) {
            if (entityManager.isOpen()) {
                if (transaction.isActive())
                    transaction.rollback();
                entityManager.close();
            }
            throw exception;
        }
    }

    public void clear() {
        references.clear();
        parents.clear();
        referenceMap.clear();
        queries.clear();
        types.clear();
        pessimistic = false;
        if (entityManager.isOpen()) {
            if (transaction.isActive())
                transaction.rollback();
            entityManager.close();
        }
    }

    public V referencing(V v) {
        return Objects.isNull(v) ? null : this.referenceMap.getOrDefault(v, v);
    }

    public Map<V, V> references() {
        return this.referenceMap;
    }

    public Map<V, String> types() {
        return this.types;
    }

    public EntityManager getEntityManager() {
        return this.entityManager;
    }

    private <T> boolean removeCommit(T t) {
        entityManager.remove(t);
        this.commit();
        this.clear();
        return true;
    }

    public class Aggregate<T, S extends Number> {
        private final Root<?> root;
        private final CriteriaQuery<S> criteria;
        private final CriteriaBuilder criteriaBuilder;

        public Aggregate(Class<T> tableType, Class<S> type) {
            this.criteriaBuilder = BehindCacheContext.this.criteriaBuilder;
            this.criteria = criteriaBuilder.createQuery(type);
            this.root = criteria.from(tableType);
        }

        public Aggregate<T, S> aggregate(String field) {
            criteria.select(criteriaBuilder.sum(root.get(field)));
            return this;
        }

        public <I> Aggregate<T, S> restrict(String field, I i) {
            criteria.where(criteriaBuilder.equal(root.get(field), i));
            return this;
        }

        public S perform() {
            return entityManager.createQuery(criteria).getSingleResult();
        }
    }
}
