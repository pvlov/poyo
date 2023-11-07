package org.poyo.util.result;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Ok<T, Void> implements Result<T, Void> {
	T okValue;

	public Ok(T val) {
		this.okValue = val;
	}

	@Override
	public T orElseThrow() {
		return okValue;
	}

	@Override
	public boolean isOk() {
		return true;
	}

	@Override
	public boolean isErr() {
		return false;
	}

	@Override
	public void ifOk(Consumer<T> consumer) {
		consumer.accept(okValue);
	}

	@Override
	public void ifErr(Consumer<Void> consumer) {
	}

	@Override
	public void ifPresentOrElse(Consumer<T> consumer, Runnable runnable) {
		consumer.accept(okValue);
	}

	@Override
	public T or(T orValue) {
		return okValue;
	}

	@Override
	public T orElse(Supplier<T> supplier) {
		return okValue;
	}

	@Override
	public <U> Optional<U> map(final Function<T, U> function) {
		return Optional.of(function.apply(okValue));
	}
}
