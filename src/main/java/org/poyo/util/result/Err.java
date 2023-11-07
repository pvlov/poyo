package org.poyo.util.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Err<Void, E extends RuntimeException> implements Result<Void, E> {

	E exception;

	public Err(E e) {
		this.exception = e;
	}

	@Override
	public Void orElseThrow() throws E {
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

	@Override
	public void ifOk(Consumer<Void> consumer) {
	}

	@Override
	public void ifErr(Consumer<E> consumer) {
		consumer.accept(exception);
	}

	@Override
	public void ifPresentOrElse(Consumer<Void> consumer, Runnable runnable) {
		runnable.run();
	}

	@Override
	public Void or(Void orValue) {
		return orValue;
	}

	@Override
	public Void orElse(Supplier<Void> supplier) {
		return supplier.get();
	}

	@Override
	public <U> Optional<U> map(Function<Void, U> function) {
		return Optional.empty();
	}
}
