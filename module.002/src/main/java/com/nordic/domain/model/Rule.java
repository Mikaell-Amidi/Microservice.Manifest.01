package com.nordic.domain.model;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

import javax.persistence.*;
import java.util.HashMap;
import java.util.Map;

@Entity
@Getter
@Builder
@Cacheable
@NoArgsConstructor
@Table(name = "rule")
@Accessors(chain = true)
@ToString(callSuper = true)
@EqualsAndHashCode(doNotUseGetters = true)
@TypeDef(name = "json", typeClass = JsonType.class)
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Rule {

    @Id
    @SequenceGenerator(name = "sequenceGenerator")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    private Long id;

    @Type(type = "json")
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> field = new HashMap<>();

}
