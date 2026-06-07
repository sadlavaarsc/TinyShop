package com.tinyshop.user.service.impl;

import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.result.R;
import com.tinyshop.common.utils.JwtUtil;
import com.tinyshop.user.entity.User;
import com.tinyshop.user.mapper.UserMapper;
import com.tinyshop.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 用户 Service 实现类
 *
 * @author TinyShop Team
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /** Token 缓存前缀 */
    private static final String TOKEN_PREFIX = "user:token:";

    @Override
    public Long register(String username, String password, String phone) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectByUsername(username);
        if (existUser != null) {
            throw new BusinessException(R.Code.BAD_REQUEST, "用户名已存在");
        }

        // 检查手机号是否已注册
        User existPhone = userMapper.selectByPhone(phone);
        if (existPhone != null) {
            throw new BusinessException(R.Code.BAD_REQUEST, "手机号已注册");
        }

        // 密码加密（MD5 + 盐值）
        String salt = username.substring(0, 2);
        String encryptedPassword = DigestUtils.md5DigestAsHex(
                (password + salt).getBytes(StandardCharsets.UTF_8));

        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptedPassword);
        user.setPhone(phone);
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), username);

        return user.getId();
    }

    @Override
    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(R.Code.USER_NOT_FOUND, "用户不存在");
        }

        if (user.getStatus() == 0) {
            throw new BusinessException(R.Code.FORBIDDEN, "账号已被禁用");
        }

        // 密码校验
        String salt = username.substring(0, 2);
        String encryptedPassword = DigestUtils.md5DigestAsHex(
                (password + salt).getBytes(StandardCharsets.UTF_8));

        if (!encryptedPassword.equals(user.getPassword())) {
            throw new BusinessException(R.Code.USER_PASSWORD_ERROR, "密码错误");
        }

        // 生成 JWT Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername());

        // 缓存 Token，7天过期
        redisTemplate.opsForValue().set(
                TOKEN_PREFIX + user.getId(), token, 7, TimeUnit.DAYS);

        log.info("用户登录成功: userId={}, username={}", user.getId(), username);
        return token;
    }

    @Override
    public User getById(Long userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }
}
