package com.nordic.persistence.director;


import com.nordic.persistence.OperandBuilder;
import com.nordic.persistence.QueryBuilder;
import com.nordic.persistence.context.QueryContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.ObjectUtils;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Subquery;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@RequiredArgsConstructor
public class QueryDirector<R> implements QueryBuilder<R> {

    private final EntityManagerFactory factory;
    private Boolean rolledBack = false;
    private RuntimeException exception;
    private QueryContext<R> queryCache;

    @Override
    public QueryBuilder<R> initiate(Class<R> type) {
        queryCache = new QueryContext<R>(factory.createEntityManager()).initiate(type);
        rolledBack = false;
        return this;
    }

    @Override
    public QueryBuilder<R> initiate(Class<R> type, EntityManager session) {
        queryCache = new QueryContext<R>(session).initiate(type);
        rolledBack = false;
        return this;
    }

    @Override
    public QueryBuilder<R> setPageable(Pageable pageable) {
        return dispatcher(o -> o.setPageable(pageable));
    }

    @Override
    public QueryBuilder<R> join(Map<String, Object> map, String router) {
        return dispatcher(o -> o.join(map, router));
    }

    @Override
    public <T> QueryBuilder<R> join(Class<T> type, String key, String right) {
        return dispatcher(o -> o.join(type, key, right));
    }

    @Override
    public QueryBuilder<R> restrict(Map<String, Object> map) {
        return dispatcher(o -> o.restrict(map));
    }

    @Override
    public <T> QueryBuilder<R> compound(String field, Subquery<T> child) {
        return dispatcher(o -> o.compound(field, child));
    }

    @Override
    public <T> QueryBuilder<R> restrict(String key, T value) {
        return Objects.isNull(value) ? this : dispatcher(o -> o.restrict(key, value, true));
    }

    @Override
    public <T> QueryBuilder<R> restrict(String key, Subquery<T> query) {
        return dispatcher(o -> o.restrict(key, query));
    }

    @Override
    public QueryBuilder<R> restrict(String key, Long value, QueryContext.ComparisonOperand operand) {
        return Objects.isNull(value) ? this : dispatcher(o -> o.restrict(key, value, operand));
    }

    @Override
    public <T> QueryBuilder<R> negate(String key, T value) {
        return Objects.isNull(value) ? this : dispatcher(o -> o.restrict(key, value, false));
    }

    @Override
    public <T> QueryBuilder<R> restrict(String key, T[] values) {
        return ObjectUtils.isEmpty(values) ? this : dispatcher(o -> o.restrict(key, values));
    }

    @Override
    public QueryBuilder<R> textSearch(List<String> searchFields, String text) {
        return Objects.isNull(text) ? this : dispatcher(o -> queryCache.textSearch(searchFields, text));
    }

    @Override
    public <T> QueryBuilder<R> append(String key, T value) {
        return Objects.isNull(value) ? this : dispatcher(o -> queryCache.append(key, value));
    }

    @Override
    public <T> QueryBuilder<R> contains(String key, T value) {
        return dispatcher(o -> queryCache.contains(key, value));
    }

    @Override
    public QueryBuilder<R> groupRestrict() {
        return dispatcher(o -> queryCache.groupRestrict());
    }

    @Override
    public <V, T> Subquery<V> querySession(Class<V> select, String r, String agg, String key, String rs, T[] vs) {
        return extract(() -> queryCache.query(select, r, agg, key, rs, vs));
    }

    @Override
    public <V> QueryBuilder<R> subQuery(Class<V> type) {
        return dispatcher(o -> o.subQuery(type));
    }

    @Override
    public Subquery<?> adoptSubQuery(String field) {
        return extract(() -> queryCache.query(field));
    }

    @Override
    public <T> TupleDirector<R, T> tupleOperand(Class<R> leftOperand, Class<T> rightOperand) {
        return extract(() -> queryCache.tupleOperand(leftOperand, rightOperand));
    }

    @Override
    public CriteriaBuilder criteriaBuilder() {
        return extract(() -> queryCache.criteriaBuilder());
    }

    @Override
    public <T> OperandBuilder<R, T> subQueryOperand(Class<T> type) {
        return extract(() -> new OperandDirector<>(this, type));
    }

    @Override
    public Page<R> queryAsPage() {
        return extract(() -> queryCache.queryAsPage());
    }

    @Override
    public List<R> queryAsList() {
        return extract(() -> queryCache.queryAsList());
    }

    @Override
    public List<R> queryAsListSession() {
        return extract(() -> queryCache.queryAsListLiveSession());
    }

    @Override
    public <T> List<T> queryAsList(Function<R, T> mapper) {
        return extract(() -> queryCache.queryAsList(mapper));
    }

    @Override
    public R queryAsSingleton() {
        return extract(() -> queryCache.queryAsSingleton());
    }

    @Override
    public R queryAsSingletonSession() {
        return extract(() -> queryCache.queryAsSingletonSession());
    }

    private QueryBuilder<R> dispatcher(Consumer<QueryContext<R>> action) {
        try {
            if (!rolledBack)
                action.accept(queryCache);
        } catch (RuntimeException e) {
            exception = e;
            rolledBack = true;
            queryCache.clear();
            log.error(e.getLocalizedMessage(), e);
        }
        return this;
    }

    private <T> T extract(Supplier<T> supplier) {
        if (rolledBack)
            throw exception;
        try {
            return supplier.get();
        } catch (RuntimeException e) {
            queryCache.clear();
            exception = e;
            rolledBack = true;
        }
        throw exception;
    }
}
