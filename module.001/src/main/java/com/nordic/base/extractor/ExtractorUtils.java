package com.nordic.base.extractor;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ExtractorUtils {

    public static <V> ResponseEntity<V> create(Optional<V> opt) {
        return opt
                .map(o -> ResponseEntity.status(HttpStatus.CREATED).body(o))
                .orElse(ResponseEntity.status(HttpStatus.CONFLICT).build());
    }

    public static <V> ResponseEntity<V> update(Optional<V> opt) {
        return opt
                .map(o -> ResponseEntity.status(HttpStatus.OK).body(o))
                .orElse(ResponseEntity.status(HttpStatus.NOT_MODIFIED).build());
    }

    public static <V> ResponseEntity<V> inquiry(Optional<V> opt) {
        return opt.map(o -> ResponseEntity.status(HttpStatus.OK).body(o))
                .orElse(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

    public static <V> ResponseEntity<Page<V>> query(Optional<Page<V>> opt) {
        return opt
                .map(vs -> ResponseEntity.status(HttpStatus.OK).body(vs)).orElseGet(
                        () -> ResponseEntity.status(HttpStatus.OK)
                                .body(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 5), 0)));
    }

    public static <V> ResponseEntity<V> delete(Boolean isDeleted) {
        return isDeleted ? ResponseEntity.status(HttpStatus.NO_CONTENT)
                .build() : ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }


    public static <U, V> ResponseEntity<V> operandSafe(Function<U, Optional<V>> extractor, U u, HttpStatus status) {
        return extractor.apply(u).map(o -> ResponseEntity.status(HttpStatus.OK).body(o))
                .orElse(ResponseEntity.status(status).build());
    }


    public static <V> Optional<Page<V>> paginate(List<V> list, Pageable pageable) {
        int size = list.size();
        return list.isEmpty() ? Optional.empty() :
                pageable.getOffset() >= size ? Optional.of(new PageImpl<V>(new ArrayList<>(), pageable, size)) :
                        Optional.of(new PageImpl<V>(
                                list.subList((int) pageable.getOffset(), Math
                                        .min((int) pageable.getOffset() + pageable.getPageSize(),
                                                size)), pageable, size));


    }
}
