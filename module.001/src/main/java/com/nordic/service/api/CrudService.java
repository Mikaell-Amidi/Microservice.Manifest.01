package com.nordic.service.api;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.Optional;

public interface CrudService<R> {

    Optional<R> save(R r);

    Optional<R> update(R r);

    Optional<Page<R>> query(Map<String, Object> input, Pageable pageable);

    Optional<R> inquiry(Long id);

    Boolean delete(Long id);
}
