package org.pvlov.util.result;

public class Ok<T> implements Result {
    T value;

    public Ok(T val) {
        this.value = val;
    }
    @Override
    public T unwrap() {
        return value;
    }

    @Override
    public boolean isOk() {
        return true;
    }

    @Override
    public boolean isErr() {
        return false;
    }
}
