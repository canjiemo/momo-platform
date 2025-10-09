package com.seer.fitness.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录响应结果
 *
 * @author seer-fitness
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT Token
     */
    private String token;

    /**
     * 用户信息
     */
//    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class UserInfo {
//
//        /**
//         * 用户ID
//         */
//        private Long id;
//
//        /**
//         * 用户名
//         */
//        private String username;
//
//        /**
//         * 真实姓名
//         */
//        private String realName;
//
//        /**
//         * 用户角色列表
//         */
//        private List<RoleDTO> roles;
//
//        /**
//         * 用户权限列表
//         */
//        private List<String> permissions;
//    }
}