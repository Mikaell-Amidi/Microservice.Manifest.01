package com.nordic.base.decorator.overseer;


import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Aspect
@Component
@RequiredArgsConstructor
public class OverseerAspect {

    private final StateMachine<MachineState, MachineEvent> stateMachine;

    @AfterReturning(value = "@annotation(Overseer)", returning = "result")
    public void overseerAspect(Object result) {
        if (result instanceof DataSource) {
            try {
                ((DataSource) result).getConnection().getSchema();
                stateMachine.sendEvent(
                                Mono.just(MessageBuilder
                                        .withPayload(MachineEvent.DATASOURCE_PREPARED_EVENT).build()))
                        .subscribe();
            } catch (Exception e) {
                stateMachine.sendEvent(
                        Mono.just(MessageBuilder
                                .withPayload(MachineEvent.IOC_PURGE_EVENT).build())).subscribe();
                stateMachine.sendEvent(
                        Mono.just(MessageBuilder
                                .withPayload(MachineEvent.IOC_DESTROY_EVENT).build())).subscribe();
            }
        }
    }
}
