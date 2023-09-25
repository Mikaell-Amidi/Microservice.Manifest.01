package com.nordic.persistence.context;


import com.nordic.persistence.director.TupleDirector;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.IteratorUtils;
import org.hibernate.query.criteria.internal.OrderImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class QueryContext<R> {

    private final EntityManager entityManager;
    private final List<String> routers = new ArrayList<>();
    private final List<Predicate> groupPredicate = new ArrayList<>();
    private final List<Predicate> andPredicates = new ArrayList<>();
    private final List<Predicate> orPredicates = new ArrayList<>();
    private Root<R> root;
    private Class<R> type;
    private Pageable pageable;
    private Subquery<?> subQuery;
    private CriteriaQuery<R> criteriaQuery;
    private CriteriaBuilder criteriaBuilder;

    public QueryContext<R> initiate(Class<R> type) {
        criteriaBuilder = entityManager.getCriteriaBuilder();
        andPredicates.clear();
        orPredicates.clear();
        groupPredicate.clear();
        routers.clear();
        criteriaQuery = criteriaBuilder.createQuery(type);
        root = criteriaQuery.from(type);
        this.type = type;
        return this;
    }

    public <V> void subQuery(Class<V> type) {
        subQuery = criteriaQuery.subquery(type);
        root = subQuery.from(this.type);
    }

    public void setPageable(Pageable pageable) {
        this.pageable = pageable;
    }

    public void join(Map<String, Object> map, String router) {
        this.routers.add(router);
        Join<?, ?> join = root.join(router, JoinType.LEFT);
        map.forEach(
                (u, v) -> andPredicates.add(join.on(criteriaBuilder.equal(join.get(u), v)).getOn()));
    }

    public <T> void join(String key, T value, String router) {
        this.routers.add(router);
        Join<?, ?> join = root.join(router, JoinType.LEFT);
        andPredicates.add(join.on(criteriaBuilder.equal(join.get(key), value)).getOn());
    }

    public <T> void join(Class<T> type, String left, String right) {
        Join<R, T> join = root.join(left, JoinType.LEFT);
        join.on(criteriaBuilder.equal(root.get(left), join.get(right)));
        criteriaQuery.select(root);
    }

    public void restrict(Map<String, Object> map) {
        map.forEach(
                (u, v) -> andPredicates
                        .add(criteriaBuilder.equal(root.get(u), v)));
    }

    public <T> void compound(String field, Subquery<T> child) {
        andPredicates.add(criteriaBuilder.and(root.get(field).in(child)));
    }

    public void groupRestrict() {
        orPredicates.add(criteriaBuilder.and(groupPredicate.toArray(new Predicate[]{})));
        groupPredicate.clear();
    }

    public <T> void append(String key, T value) {
        groupPredicate.add(criteriaBuilder.and(criteriaBuilder.equal(root.get(key), value)));
    }

    public <T> void contains(String key, T value) {
        andPredicates.add(criteriaBuilder.like(root.get(key), "%" + value + "%"));
    }

    public <T> void restrict(String key, T[] values) {
        andPredicates.add(in(key, values, this.root));
    }

    public void restrict(String key, Long value, ComparisonOperand operand) {
        this.comparison(key, value, operand);
    }

    public <T> void restrict(String key, T value, boolean equal) {
        andPredicates.add(equal
                ? criteriaBuilder.equal(root.get(key), value) : criteriaBuilder.notEqual(root.get(key), value));
    }

    public <T> void restrict(String key, Subquery<T> query) {
        andPredicates.add(root.get(key).in(query));
    }

    public void textSearch(List<String> searchFields, String text) {
        insensitiveTextSearch(searchFields, text);
    }

    public <T> TupleDirector<R, T> tupleOperand(Class<R> leftOperand, Class<T> rightOperand) {
        return new TupleDirector<R, T>(entityManager).bind(leftOperand, rightOperand);
    }

    public <V, T> Subquery<V> query(Class<V> select, String router, String agg, String key, String restrict, T[] vs) {
        Subquery<V> query = this.criteriaQuery.subquery(select);
        Root<R> root = query.from(type);
        Join<R, ?> join = root.join(router, JoinType.LEFT);
        Subquery<V> result = query.select(root.get(router).get(key).as(select))
                .where(join.on(criteriaBuilder.greaterThan(root.get(agg), 0)).getOn(), in(restrict, vs, root));
        routers.clear();
        orPredicates.clear();
        andPredicates.clear();
        groupPredicate.clear();
        return result;
    }

    public Subquery<?> query(String field) {
        subQuery = orPredicates.isEmpty() ?
                subQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{}))).select(root.get(field)) :
                andPredicates.isEmpty() ?
                        subQuery.where(criteriaBuilder.or(orPredicates.toArray(new Predicate[]{})))
                                .select(root.get(field)) :
                        subQuery.where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})),
                                criteriaBuilder.or(orPredicates.toArray(new Predicate[]{}))).select(root.get(field));
        this.clear();
        return subQuery;
    }

    public CriteriaBuilder criteriaBuilder() {
        return this.criteriaBuilder;
    }

    public Page<R> queryAsPage() {
        criteriaQuery.orderBy(IteratorUtils.toList(this.pageable.getSort().iterator()).stream()
                .map(o -> new OrderImpl(root.get(o.getProperty()), o.isAscending())).collect(Collectors.toList()));
        Page<R> result = PageableExecutionUtils
                .getPage(adoptTypedQuery()
                        .setFirstResult((int) pageable.getOffset())
                        .setMaxResults(pageable.getPageSize()).getResultList(), this.pageable, this::count);
        this.clear();
        return result;
    }

    public List<R> queryAsList() {
        List<R> result = adoptTypedQuery().getResultList();
        this.clear();
        return result;
    }

    public <T> List<T> queryAsList(Function<R, T> mapper) {
        List<T> result = adoptTypedQuery().getResultList().stream().map(mapper).collect(Collectors.toList());
        this.clear();
        return result;
    }

    public List<R> queryAsListLiveSession() {
        List<R> result = adoptTypedQuery().getResultList();
        routers.clear();
        orPredicates.clear();
        andPredicates.clear();
        groupPredicate.clear();
        return result;
    }

    public R queryAsSingleton() {
        R result = null;
        try {
            result = adoptTypedQuery().getSingleResult();
        } catch (NoResultException ignored) {

        }
        this.clear();
        return result;
    }

    public R queryAsSingletonSession() {
        R result = null;
        try {
            result = adoptTypedQuery().getSingleResult();
        } catch (NoResultException ignored) {

        }
        routers.clear();
        orPredicates.clear();
        andPredicates.clear();
        groupPredicate.clear();
        return result;
    }

    public void clear() {
        routers.clear();
        orPredicates.clear();
        andPredicates.clear();
        groupPredicate.clear();
        if (entityManager.isOpen())
            entityManager.close();
    }

    private TypedQuery<R> adoptTypedQuery() {
        return orPredicates.isEmpty() ?
                entityManager.createQuery(criteriaQuery
                        .where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})))) :
                andPredicates.isEmpty() ?
                        entityManager.createQuery(criteriaQuery
                                .where(criteriaBuilder.or(orPredicates.toArray(new Predicate[]{})))) :
                        entityManager.createQuery(criteriaQuery
                                .where(criteriaBuilder.and(andPredicates.toArray(new Predicate[]{})),
                                        criteriaBuilder.or(orPredicates.toArray(new Predicate[]{}))));
    }

    private void comparison(String key, Long value, ComparisonOperand operand) {
        andPredicates.add(operand.equals(ComparisonOperand.EQUAL)
                ? criteriaBuilder
                .equal(root.get(key), value) :
                operand.equals(ComparisonOperand.EQUAL_MORE)
                        ? criteriaBuilder.greaterThanOrEqualTo(root.get(key), value)
                        : criteriaBuilder.lessThanOrEqualTo(root.get(key), value));
    }

    private <T, V> Predicate in(String key, T[] values, Root<V> root) {
        return root.get(key).in(values);
    }

    private void insensitiveTextSearch(List<String> searchFields, String text) {
        searchFields
                .stream()
                .map(o -> criteriaBuilder
                        .like(criteriaBuilder.lower(root.get(o)), "%" + text.toLowerCase(Locale.ROOT) + "%"))
                .forEach(orPredicates::add);
    }

    private Long count() {
        CriteriaQuery<Long> countCriteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<R> countRoot = countCriteriaQuery.from(type);

        routers.forEach(o -> countRoot.join(o, JoinType.LEFT));

        return orPredicates.isEmpty() ?
                entityManager
                        .createQuery(countCriteriaQuery.select(criteriaBuilder
                                        .count(countRoot))
                                .where(criteriaBuilder
                                        .and(andPredicates.toArray(new Predicate[]{})))).getSingleResult() :
                andPredicates.isEmpty() ?
                        entityManager.createQuery(countCriteriaQuery.select(criteriaBuilder.count(countRoot))
                                .where(criteriaBuilder
                                        .or(orPredicates.toArray(new Predicate[]{})))).getSingleResult() :
                        entityManager
                                .createQuery(countCriteriaQuery.select(criteriaBuilder
                                                .count(countRoot))
                                        .where(criteriaBuilder
                                                        .and(andPredicates.toArray(new Predicate[]{})),
                                                criteriaBuilder
                                                        .or(orPredicates
                                                                .toArray(new Predicate[]{})))).getSingleResult();
    }

    public enum ComparisonOperand {
        EQUAL, EQUAL_MORE, EQUAL_LESS
    }

}
