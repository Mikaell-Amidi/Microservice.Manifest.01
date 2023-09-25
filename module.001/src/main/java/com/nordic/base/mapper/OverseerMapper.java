package com.nordic.base.mapper;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

public interface OverseerMapper<D, E> extends BaseMapper<D, E> {

    BinaryOperator<E> constraintUpdate(D dto);

    Predicate<E> constraintRemove(D dto);

    default Function<E, BinaryOperator<E>> functionalUpdate() {
        return o -> constraintUpdate(dtoProjector(o));
    }

    default Function<E, Predicate<E>> constraintRemoveFunction() {
        return o -> constraintRemove(dtoProjector(o));
    }
}
