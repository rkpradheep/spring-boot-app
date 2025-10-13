package com.server.framework.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Table(name = "`Lock`")
@Entity (name = "Lock")
public class LockEntity
{
	@Id
	@Column(name = "name", length = 255, nullable = false, unique = true)
	private String name;

	public LockEntity()
	{

	}

	public LockEntity(String name)
	{
		this.name = name;
	}
}
