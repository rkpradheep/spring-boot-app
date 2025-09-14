package com.server.framework.common;

import java.util.Objects;

@FunctionalInterface
public interface CustomConsumer<T>
{
	void accept(T t) throws Exception;

	default CustomConsumer<T> andThen(CustomConsumer<? super T> after)
	{
		Objects.requireNonNull(after);
		return (T t) -> {
			accept(t);
			after.accept(t);
		};
	}
}