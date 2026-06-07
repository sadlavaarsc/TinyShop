package com.tinyshop.user.service;

import com.tinyshop.user.entity.User;

/**
 * 用户 Service 接口
 *
 * @author TinyShop Team
 */
public interface UserService {

    /**
     * 用户注册
     *
     * @param username 用户名
     * @param password 密码
     * @param phone    手机号
     * @return 用户ID
     */
    Long register(String username, String password, String phone);

    /**
     * 用户登录
     *
     * @param username 用户名
     * @param password 密码
     * @return JWT Token
     */
    String login(String username, String password);

    /**
     * 根据ID查询用户
     */
    User getById(Long userId);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);
}
