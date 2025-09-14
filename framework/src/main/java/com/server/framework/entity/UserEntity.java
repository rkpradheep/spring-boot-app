package com.server.framework.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.GenericGenerator;

import com.server.framework.id.CustomIdGenerator;

@Entity(name = "User")
@Table(name = "\"User\"") //User is a reserved keyword in some databases like h2
public class UserEntity
{
    
    @Id
    @Column(name = "Id")
    @GeneratedValue(generator = "custom-id")
    @GenericGenerator(name = "custom-id", type = CustomIdGenerator.class)
    private Long id;
    
    @Column(name = "Name", nullable = false, length = 255)
    private String name;
    
    @Column(name = "Password", nullable = false, length = 255)
    private String password;
    
    @Column(name = "RoleType", nullable = false)
    private Integer roleType;

    public UserEntity() {}
    
    public UserEntity(Long id, String name, String password, Integer roleType) {
        this.id = id;
        this.name = name;
        this.password = password;
        this.roleType = roleType;
    }

    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public Integer getRoleType() {
        return roleType;
    }
    
    public void setRoleType(Integer roleType) {
        this.roleType = roleType;
    }
    
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", roleType=" + roleType +
                '}';
    }
}
