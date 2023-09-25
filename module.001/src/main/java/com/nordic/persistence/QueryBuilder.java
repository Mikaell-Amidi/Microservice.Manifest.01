package com.nordic.persistence;


import com.nordic.persistence.context.QueryContext;
import com.nordic.persistence.director.TupleDirector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Subquery;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface QueryBuilder<R> {

    QueryBuilder<R> initiate(Class<R> type);

    QueryBuilder<R> initiate(Class<R> type, EntityManager session);

    QueryBuilder<R> setPageable(Pageable pageable);

    QueryBuilder<R> join(Map<String, Object> map, String router);

    <T> QueryBuilder<R> join(Class<T> type, String left, String right);

    QueryBuilder<R> textSearch(List<String> searchFields, String text);

    <T> QueryBuilder<R> append(String key, T value);

    <T> QueryBuilder<R> groupRestrict();

    <T> QueryBuilder<R> contains(String key, T value);

    QueryBuilder<R> restrict(Map<String, Object> map);

    <T> QueryBuilder<R> restrict(String key, Subquery<T> query);

    <T> QueryBuilder<R> restrict(String key, T value);

    <T> QueryBuilder<R> restrict(String key, T[] values);

    QueryBuilder<R> restrict(String key, Long value, QueryContext.ComparisonOperand operand);

    <T> QueryBuilder<R> negate(String key, T value);

    <T> QueryBuilder<R> compound(String field, Subquery<T> child);

    <V> QueryBuilder<R> subQuery(Class<V> type);

    <V, T> Subquery<V> querySession(Class<V> select, String rout, String aggregate, String key, String restrict, T[] t);

    Subquery<?> adoptSubQuery(String field);

    <T> TupleDirector<R, T> tupleOperand(Class<R> leftOperand, Class<T> rightOperand);

    <T> OperandBuilder<R, T> subQueryOperand(Class<T> type);

    CriteriaBuilder criteriaBuilder();

    Page<R> queryAsPage();

    List<R> queryAsList();

    List<R> queryAsListSession();

    <T> List<T> queryAsList(Function<R, T> mapper);

    R queryAsSingleton();

    R queryAsSingletonSession();
}
