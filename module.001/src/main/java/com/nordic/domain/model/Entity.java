package com.nordic.domain.model;


import lombok.*;
import lombok.experimental.Accessors;

import javax.persistence.*;


@Getter
@Setter
@javax.persistence.Entity
@Builder
@NoArgsConstructor
@Accessors(chain = true)
@Table(name = "entity")
@ToString(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Entity extends Auditable {

    @Id
    @SequenceGenerator(name = "entity_generator", sequenceName = "entity_id_gen", allocationSize = 5)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "entity_generator")
    private Long id;

    @Column(name = "field")
    private String field;
}
