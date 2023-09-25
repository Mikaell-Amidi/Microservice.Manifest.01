package com.nordic.persistence.context;


import co.streamx.fluent.JPA.FluentQuery;
import com.nordic.persistence.FluentQueryBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.math.BigInteger;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FluentQueryContext implements FluentQueryBuilder {

    private final EntityManager entityManager;

    @Override
    public <R, T extends FluentQuery> List<R> queryAsList(T t, Class<R> type) {
        TypedQuery<R> query = t.createQuery(entityManager, type);
        return query.getResultList();
    }

    @Override
    public <R, T extends FluentQuery> Page<R> queryAsPage(T query, T countQuery, Pageable pageable, Class<R> type) {
        return PageableExecutionUtils
                .getPage(query.createQuery(entityManager, type)
                        .setFirstResult((int) pageable.getOffset())
                        .setMaxResults(pageable.getPageSize()).getResultList(), pageable, () -> count(countQuery));
    }

    private <R, T extends FluentQuery> Long count(T t) {
        return ((BigInteger) t.createQuery(entityManager).getSingleResult()).longValue();
    }
}
