package com.nordic.persistence;

import co.streamx.fluent.JPA.FluentQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FluentQueryBuilder {

    <R, T extends FluentQuery> List<R> queryAsList(T t, Class<R> type);

    <R, T extends FluentQuery> Page<R> queryAsPage(T query, T countQuery, Pageable pageable, Class<R> type);
}
