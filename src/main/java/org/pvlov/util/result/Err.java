package org.pvlov.util.result;

public class Err<E extends Exception> implements Result {

    E exception;
    public Err(E e) {
        this.exception = e;
    }

    @Override
    public Object unwrap() throws E {
        throw exception;
    }

    @Override
    public boolean isOk() {
        return false;
    }

    @Override
    public boolean isErr() {
        return true;
    }

    public String getMessage() {
        return this.exception.getMessage();
    }
}
