package com.server.framework.user;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

public enum RoleEnum
{
	NORMAL("user", 0),
	ADMIN("admin", -1);

	private final String name;
	private final int type;

	RoleEnum(String name, int type)
	{
		this.name = name;
		this.type = type;
	}

	public int getType()
	{
		return type;
	}

	public static int getType(String name)
	{
		return Arrays.stream(values()).filter(roleEnum -> StringUtils.equals(roleEnum.name, name)).findFirst().get().type;
	}
}
