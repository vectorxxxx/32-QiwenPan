package com.qiwenshare.file.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author VectorX
 * @version V1.0
 * @description
 * @date 2024-08-06 16:06:15
 */
@Getter
@AllArgsConstructor
public enum RoleType
{
    ADMIN(1, "管理员"),
    USER(2, "普通用户");

    private final Integer roleId;
    private final String roleName;
}
