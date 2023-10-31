package org.pvlov.util.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Result<T, E> {

    T orElseThrow() throws RuntimeException;
    boolean isOk();
    boolean isErr();

    void ifOk(final Consumer<T> consumer);
    void ifErr(final Consumer<E> consumer);
    void ifPresentOrElse(final Consumer<T> consumer, final Runnable runnable);
    T or(T orValue);
    T orElse(Supplier<T> supplier);
    void mapOk(final Consumer<T> action);
    <U> Optional<U> map(final Function<T, U> function);
}
