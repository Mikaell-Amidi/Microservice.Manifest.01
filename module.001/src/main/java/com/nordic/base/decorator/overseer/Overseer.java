package com.nordic.base.decorator.overseer;

import org.springframework.context.annotation.Primary;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Primary
public @interface Overseer {
    Type type();

    enum Type {
        DATASOURCE, EUREKA
    }
}
