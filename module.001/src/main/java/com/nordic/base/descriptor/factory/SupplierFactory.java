package com.nordic.base.descriptor.factory;

import java.util.function.Supplier;

public class SupplierFactory<T> extends Factory<T> {

    @Override
    public T instantiate(Supplier<? extends T> supplier) {
        return supplier.get();
    }
}
