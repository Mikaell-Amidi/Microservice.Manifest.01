package com.nordic.persistence.director;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public class TupleDirector<R, T> {

    private final CriteriaQuery<Tuple> tupleQuery;
    private final EntityManager entityManager;
    private final CriteriaBuilder criteriaBuilder;
    private final List<Predicate> predicates = new ArrayList<>();
    private Root<R> leftRoot;
    private Root<T> rightRoot;
    private String operandRouter;
    private Boolean operandSwitch = false;
    private List<String> routers;
    private Pageable pageable;

    public TupleDirector(EntityManager entityManager) {
        this.entityManager = entityManager;
        this.criteriaBuilder = entityManager.getCriteriaBuilder();
        this.tupleQuery = criteriaBuilder.createTupleQuery();
    }

    public TupleDirector<R, T> bind(Class<R> leftOperand, Class<T> rightOperand) {
        this.leftRoot = tupleQuery.from(leftOperand);
        this.rightRoot = tupleQuery.from(rightOperand);
        this.predicates.clear();
        this.routers = new ArrayList<>();
        return this;
    }

    public TupleDirector<R, T> fuse(String leftRoute, String rightRouter) {
        this.predicates.add(criteriaBuilder.equal(this.leftRoot.get(leftRoute), this.rightRoot.get(rightRouter)));
        this.operandRouter = rightRouter;
        return this;
    }

    public TupleDirector<R, T> select(BiFunction<Root<R>, Root<T>, List<Selection<?>>> selector) {
        List<Selection<?>> selections = selector.apply(leftRoot, rightRoot);
        selections.add(rightRoot.join(operandRouter, JoinType.LEFT));
        tupleQuery.multiselect(selections);
        return this;
    }

    public TupleDirector<R, T> operandSwitch() {
        this.operandSwitch = true;
        return this;
    }

    public TupleDirector<R, T> join(Map<String, Object> map, String router) {
        addRouters(router);
        Join<?, ?> join = operandSwitch ? rightRoot.join(router, JoinType.LEFT) : leftRoot.join(router, JoinType.LEFT);
        map.forEach(
                (u, v) -> predicates
                        .add(join.on(criteriaBuilder.equal(join.get(u), v)).getOn()));
        return this;
    }

    public TupleDirector<R, T> restrict(Map<String, Object> map) {
        map.forEach(
                (u, v) -> predicates
                        .add(criteriaBuilder.equal(operandSwitch ? rightRoot.get(u) : leftRoot.get(u), v)));
        return this;
    }

    public <V> TupleDirector<R, T> restrict(String key, V value) {
        predicates.add(criteriaBuilder.equal(operandSwitch ? rightRoot.get(key) : leftRoot.get(key), value));
        return this;
    }

    public <V> TupleDirector<R, T> restrict(String key, V[] values) {
        return Objects.isNull(values) ? this : traversValues(key, values);
    }

    public TupleDirector<R, T> setPageable(Pageable pageable) {
        this.pageable = pageable;
        return this;
    }

    public TypedQuery<Tuple> adoptTypedQuery() {
        return entityManager.createQuery(tupleQuery
                .where(criteriaBuilder.and(predicates.toArray(new Predicate[]{}))));
    }

    public List<Tuple> queryAsList() {
        return adoptTypedQuery().getResultList();
    }

    public Page<Tuple> queryAsPage() {
        return new PageImpl<>(
                adoptTypedQuery()
                        .setFirstResult((int) pageable.getOffset())
                        .setMaxResults(pageable.getPageSize()).getResultList(), pageable, count());
    }

    private <V> TupleDirector<R, T> traversValues(String key, V[] values) {
        List<Predicate> intersectionPredicates = new ArrayList<>();
        for (V v : values) {
            intersectionPredicates.add(
                    criteriaBuilder.equal(operandSwitch ? rightRoot.get(key) : leftRoot.get(key), v));
        }
        if (!intersectionPredicates.isEmpty())
            predicates.add(criteriaBuilder.or(intersectionPredicates.toArray(new Predicate[]{})));
        return this;
    }

    private Long count() {
        CriteriaQuery<Long> countCriteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<R> countRoot = countCriteriaQuery.from(leftRoot.getModel());
        routers.forEach(o -> countRoot.join(o, JoinType.LEFT));

        return entityManager
                .createQuery(countCriteriaQuery.select(criteriaBuilder
                        .count(countRoot)).where(predicates.toArray(new Predicate[]{}))).getSingleResult();
    }

    private void addRouters(String router) {
        if (!operandSwitch) this.routers.add(router);
    }
}
