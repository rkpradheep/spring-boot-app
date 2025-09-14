package com.server.framework.persistence;

public class Function extends Column
{
	String name;
	Object[] args;

	private Function(String name, Object... args)
	{
		super();
		this.name = name;
		this.args = args;
	}

	public static Function createFunction(String functionName, Object... args)
	{
		return new Function(functionName, args);
	}

	public static final class Constants
	{
		public static final String UPPER = "UPPER";
		public static final String LOWER = "LOWER";
		public static final String POW = "POW";
		public static final String SUM = "SUM";
		public static final String MAX = "MAX";
		public static final String MIN = "MIN";
	}
}
