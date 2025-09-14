package com.server.framework.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity(name = "AuthToken")
@Table(name = "AuthToken")
public class AuthTokenEntity
{

	@Id
	@GeneratedValue(generator = "custom-id")
	@GenericGenerator(name = "custom-id", type = com.server.framework.id.CustomIdGenerator.class)
	@Column(name = "Id")
	private Long id;

	@Column(name = "Token", nullable = false, columnDefinition = "LONGTEXT")
	private String token;

	@OnDelete(action = OnDeleteAction.CASCADE)
	@ManyToOne
	@JoinColumn(name = "UserId", nullable = false)
	private UserEntity user;

	public AuthTokenEntity()
	{
	}

	public AuthTokenEntity(Long id, String token, UserEntity user)
	{
		this.id = id;
		this.token = token;
		this.user = user;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getToken()
	{
		return token;
	}

	public void setToken(String token)
	{
		this.token = token;
	}

	public UserEntity getUser()
	{
		return user;
	}

	public void setUser(UserEntity userEntity)
	{
		this.user = userEntity;
	}

	@Override
	public String toString()
	{
		return "AuthToken{" +
			"id=" + id +
			", token='" + token + '\'' +
			", user=" + user +
			'}';
	}
}
