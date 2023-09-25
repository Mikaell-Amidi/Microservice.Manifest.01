package com.nordic.persistence.director;


import com.nordic.persistence.OperandBuilder;
import com.nordic.persistence.QueryBuilder;
import org.springframework.util.ObjectUtils;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;


public class OperandDirector<R, T> implements OperandBuilder<R, T> {

    private final Root<T> root;
    private final Subquery<T> query;
    private final QueryBuilder<R> queryBuilder;
    private final CriteriaBuilder criteriaBuilder;
    private final List<Predicate> predicates = new ArrayList<>();
    private OperandDirector<R, ?> parent;

    public OperandDirector(QueryBuilder<R> queryBuilder, Class<T> type) {
        this.queryBuilder = queryBuilder;
        criteriaBuilder = queryBuilder.criteriaBuilder();
        query = criteriaBuilder.createQuery(type).subquery(type);
        root = query.from(type);
    }

    public <V> OperandDirector(QueryBuilder<R> queryBuilder, OperandDirector<R, V> parent, Class<T> type) {
        this.queryBuilder = queryBuilder;
        this.parent = parent;
        criteriaBuilder = queryBuilder.criteriaBuilder();
        query = criteriaBuilder.createQuery(type).subquery(type);
        root = query.from(type);
    }

    @Override
    public OperandBuilder<R, T> restrict(String key, String value) {
        predicates.add(criteriaBuilder.equal(root.get(key), value));
        return this;
    }

    @Override
    public OperandBuilder<R, T> restrict(String key, Long value) {
        predicates.add(criteriaBuilder.equal(root.get(key), value));
        return this;
    }

    @Override
    public <V> OperandBuilder<R, T> restrict(String field, Subquery<V> child) {
        predicates.add(criteriaBuilder.and(root.get(field).in(child)));
        return this;
    }

    @Override
    public <I> OperandBuilder<R, T> restrict(String key, I[] values) {
        if (!ObjectUtils.isEmpty(values))
            predicates.add(in(key, values));
        return this;
    }

    @Override
    public OperandBuilder<R, T> tuple(String field) {
        query.select(root.get(field)).where(criteriaBuilder.and(predicates.toArray(new Predicate[]{})));
        return this;
    }

    @Override
    public OperandBuilder<R, ?> operandFuse(String field) {
        OperandBuilder<R, ?> result = parent.restrict(field, query);
        this.clear();
        return result;
    }

    @Override
    public QueryBuilder<R> queryFuse(String field) {
        QueryBuilder<R> result = queryBuilder.compound(field, query);
        this.clear();
        return result;
    }

    @Override
    public <V> OperandBuilder<R, V> subQueryOperand(Class<V> type) {
        return new OperandDirector<>(this.queryBuilder, this, type);
    }

    private <T, V> Predicate in(String key, T[] values) {
        return root.get(key).in(values);
    }

    private void clear() {
        predicates.clear();
    }
}
