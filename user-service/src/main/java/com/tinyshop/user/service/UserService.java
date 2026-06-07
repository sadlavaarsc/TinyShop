package com.tinyshop.user.service;

import com.tinyshop.common.utils.JwtUtil;
import com.tinyshop.user.entity.User;
import com.tinyshop.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    public boolean register(User user) {
        return userMapper.insert(user) > 0;
    }

    public String login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return JwtUtil.generateToken(user.getId());
        }
        return null;
    }
}
