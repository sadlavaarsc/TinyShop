package com.tinyshop.user.controller;

import com.tinyshop.common.result.R;
import com.tinyshop.user.entity.User;
import com.tinyshop.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public R<Boolean> register(@RequestBody User user) {
        return R.ok(userService.register(user));
    }

    @PostMapping("/login")
    public R<String> login(@RequestParam String username, @RequestParam String password) {
        String token = userService.login(username, password);
        return token != null ? R.ok(token) : R.error("登录失败");
    }
}
