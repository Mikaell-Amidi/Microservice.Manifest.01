package com.nordic.service.topology;


import com.nordic.domain.dto.RuleDTO;
import com.nordic.domain.model.Rule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public final class Topology<T> {

    private final KStreamConfig config;
    private final KClusterService clusterService;
    private final Mapper<RuleDTO, Rule> mapper;
    private final JpaRepository<Rule, Long> dynamicRepository;
    private final PlainGSONMapper<List<T>, Map<String, Object>> kMessageMapper;

    public void execute() {
        new StreamDirector<>(config.extract())
                .add(kStream ->
                        kStream.filter((key, value) -> Objects.nonNull(key) && key.equals("key"))
                                .transform(KStreamValueDecorator::new)
                                .foreach((key, value) ->
                                        dynamicRepository.findAll().stream()
                                                .filter(rule -> mapper.filterApplicable(rule, value))
                                                .findFirst().map(mapper::builder)
                                                .map(builder -> {
                                                    try {
                                                        return builder
                                                                .attachment(kMessageMapper.paths(value))
                                                                .dynamicData(kMessageMapper.data(value))
                                                                .execute();
                                                    } catch (RuntimeException e) {
                                                        builder.clearContext();
                                                        clusterService
                                                                .publish(SomeEntity.builder().date(new Date())
                                                                        .message(new KeyValue<>(key, value)).build());
                                                        return new Response();
                                                    }
                                                })
                                ))
                .operate();
    }

    @PostConstruct
    public void lunch() {
        this.execute();
    }
}
