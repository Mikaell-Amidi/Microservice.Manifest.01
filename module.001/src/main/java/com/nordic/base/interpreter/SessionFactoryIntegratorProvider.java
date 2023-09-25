package com.nordic.base.interpreter;

import lombok.RequiredArgsConstructor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.IntegratorProvider;

import java.util.List;


@RequiredArgsConstructor
public class SessionFactoryIntegratorProvider implements IntegratorProvider {

    @Override
    public List<Integrator> getIntegrators() {
        return List.of(new EventListenerIntegrator());
    }
}
