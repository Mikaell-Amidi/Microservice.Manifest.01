package com.nordic.domain.dto;

import lombok.*;
import lombok.experimental.Accessors;

import java.util.Map;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@ToString(callSuper = true)
@Accessors
public class RuleDTO {
    private Long id;

    private Map<String, Object> field;
}
