package com.nordic.base.mapper;


import org.springframework.data.domain.Page;

import java.util.Map;
import java.util.Optional;

public interface BaseMapper<D, E> {

    E entityProjector(D dto);

    D dtoProjector(E entity);

    MapBuilder queryMapAdopter(Map<String, Object> map);

    default Optional<Page<D>> paginator(Page<E> page) {
        return Optional.of(page.map(this::dtoProjector));
    }
}
