package com.nordic.presentation.rest.resource;


import com.nordic.base.extractor.ExtractorUtils;
import com.nordic.domain.dto.EntityDTO;
import com.nordic.presentation.rest.api.TPRController;
import com.nordic.service.api.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;


@RestController
@RequiredArgsConstructor
public class TPRControllerImpl implements TPRController {
    private final CrudService<EntityDTO> service;
    private final Predicate<EntityDTO> createPredicate = o -> Objects.isNull(o.getId());
    private final Predicate<EntityDTO> updatePredicate = o -> Objects.nonNull(o.getId());

    @Override
    public ResponseEntity<EntityDTO> createMKSHeader(EntityDTO mksHeaderDto) {
        return createPredicate.test(mksHeaderDto)
                ? ExtractorUtils.create(service.save(mksHeaderDto))
                : ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @Override
    public ResponseEntity<EntityDTO> updateMKSHeader(EntityDTO mksHeaderDto) {
        return updatePredicate.test(mksHeaderDto)
                ? ExtractorUtils.update(service.update(mksHeaderDto))
                : ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @Override
    public ResponseEntity<EntityDTO> inquiryMKSHeaders(Long id) {
        return ExtractorUtils.inquiry(service.inquiry(id));
    }

    @Override
    public ResponseEntity<Page<EntityDTO>> queryMKSHeaders
            (Map<String, Object> map, Integer page, Integer size, String params) {
        return ExtractorUtils.query(service.query(map, PageRequest.of(page, size)));
    }

    @Override
    public ResponseEntity<Void> deleteMKSHeader(Long id) {
        return ExtractorUtils.delete(service.delete(id));
    }
}
