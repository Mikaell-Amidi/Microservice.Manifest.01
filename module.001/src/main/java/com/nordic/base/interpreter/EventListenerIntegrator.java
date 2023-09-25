package com.nordic.base.interpreter;

import lombok.RequiredArgsConstructor;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;


@RequiredArgsConstructor
public class EventListenerIntegrator implements Integrator {

    @Override
    public void integrate(Metadata data, SessionFactoryImplementor session, SessionFactoryServiceRegistry registry) {
        final EventListenerRegistry eventListenerRegistry = session
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);
    }

    @Override
    public void disintegrate(SessionFactoryImplementor session, SessionFactoryServiceRegistry registry) {

    }
}
