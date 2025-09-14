package com.server.framework.dto;

import org.apache.commons.codec.digest.DigestUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.server.framework.user.RoleEnum;

public class UserDto {
    private String name;
    private String password;

    @JsonProperty("role")
    private String roleName;
    private Integer roleType;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = DigestUtils.sha256Hex(password.trim()); }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) {
        this.roleName = roleName;
        try {
            this.roleType = RoleEnum.getType(roleName);
        } catch (Exception e) {
            this.roleType = null;
        }
    }

    public Integer getRoleType() { return roleType; }
    public void setRoleType(Integer roleType) { this.roleType = roleType; }
}
