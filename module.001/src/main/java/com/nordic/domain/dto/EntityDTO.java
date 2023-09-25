package com.nordic.domain.dto;


import lombok.*;
import lombok.experimental.Accessors;

import javax.validation.constraints.Size;

@Getter
@Setter
@EqualsAndHashCode
@Builder
@ToString(callSuper = true)
@Accessors
public class EntityDTO {

    private Long id;

    @Size(max = 6)
    private String field;
}
