package com.nordic.base.descriptor.strategy;

import java.util.List;

public abstract class StrategyContext<R> {

    private final List<Strategy<R>> strategies;

    public StrategyContext(List<Strategy<R>> strategies) {
        this.strategies = strategies;
    }

    public void trigger(R r) {
        strategies.forEach(o -> o.trigger(r));
    }

    public List<Strategy<R>> strategy() {
        return this.strategies;
    }
}
