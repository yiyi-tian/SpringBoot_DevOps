package org.example.userservice.controller;

import org.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/internal/user/register")
    public Map<String, Object> register(@RequestBody Map<String, Object> request) {
        return userService.register(request);
    }

    @PostMapping("/internal/user/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> request) {
        return userService.login(request);
    }
}