package com.server.framework.common;

public interface CustomBiFunction<T, U, R>
{
	R apply(T t, U u) throws Exception;
}
