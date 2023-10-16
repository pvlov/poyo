package org.pvlov.util.result;

public interface Result<T, E> {

    public abstract T unwrap() throws RuntimeException;
    public abstract boolean isOk();
    public abstract boolean isErr();
}
