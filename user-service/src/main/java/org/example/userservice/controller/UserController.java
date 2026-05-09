package org.example.userservice.controller;

import org.example.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/internal/user/register")
    public String register(@RequestParam String phone, @RequestParam String password){

        return userService.register(phone,password);
    }
}
