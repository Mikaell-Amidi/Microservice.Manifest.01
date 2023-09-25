package com.nordic.service.resource;

import com.nordic.base.mapper.OverseerMapper;
import com.nordic.domain.dto.EntityDTO;
import com.nordic.domain.model.Entity;
import com.nordic.persistence.factory.PersistenceFactory;
import com.nordic.service.api.CrudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManagerFactory;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EntityService implements CrudService<EntityDTO> {

    private final OverseerMapper<EntityDTO, Entity> mapper;
    private final JpaRepository<Entity, Long> dynamicRepository;
    private final PersistenceFactory<EntityManagerFactory> factory;

    @Override
    public Optional<EntityDTO> save(EntityDTO dto) {
        return Optional.of(dynamicRepository.saveAndFlush(mapper.entityProjector(dto))).map(mapper::dtoProjector);
    }

    @Override
    public Optional<EntityDTO> update(EntityDTO dto) {
        return Optional.of(factory.cacheBuilder().initiateTransaction()
                .detach(Entity.class, mapper.entityProjector(dto), dto, Entity::getId)).map(mapper::dtoProjector);
    }

    @Override
    public Optional<Page<EntityDTO>> query(Map<String, Object> input, Pageable pageable) {
        return mapper
                .paginator(factory.queryBuilder(Entity.class)
                        .restrict(mapper.queryMapAdopter(input).build())
                        .textSearch(extractor.token(),
                                extractor.extractor(input)).setPageable(pageable).queryAsPage());
    }

    @Override
    public Optional<EntityDTO> inquiry(Long id) {
        return dynamicRepository.findById(id).map(mapper::dtoProjector);
    }

    @Override
    public Boolean delete(Long id) {
        return inquiry(id)
                .map(o -> {
                    dynamicRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }

}
