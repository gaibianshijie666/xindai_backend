package com.xindai.xindai.security;

import lombok.Getter;

@Getter
public enum Role {
    USER("ROLE_USER"),
    ADMIN("ROLE_ADMIN"),
    ENTERPRISE("ROLE_ENTERPRISE"),
    ENTERPRISE_ADMIN("ROLE_ENTERPRISE_ADMIN"),
    ENTERPRISE_OPERATOR("ROLE_ENTERPRISE_OPERATOR");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }
}
