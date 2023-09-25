package com.nordic.persistence;

import javax.persistence.criteria.Subquery;

public interface OperandBuilder<R, T> {

    <V> OperandBuilder<R, V> subQueryOperand(Class<V> type);

    <V> OperandBuilder<R, T> restrict(String field, Subquery<V> child);

    <V> OperandBuilder<R, T> restrict(String key, V[] values);

    OperandBuilder<R, T> restrict(String key, String value);

    OperandBuilder<R, T> restrict(String key, Long value);

    OperandBuilder<R, T> tuple(String field);

    OperandBuilder<R, ?> operandFuse(String field);

    QueryBuilder<R> queryFuse(String field);

}
