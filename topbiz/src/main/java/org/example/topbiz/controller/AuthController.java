package org.example.topbiz.controller;

import org.example.common.Result;
import org.example.topbiz.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class AuthController {

    @Autowired
    private AuthService authService;

    // ==================== 注册 ====================

    @PostMapping("/api/v1/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        String credential = authService.extractCredential(request);
        String password = (String) request.get("password");
        String code = (String) request.get("code");

        if (credential == null || credential.isEmpty()) {
            return Result.error(400, "请输入手机号/邮箱/用户名");
        }
        if (password == null || password.length() < 6) {
            return Result.error(400, "密码长度不能少于6位");
        }

        Map<String, Object> result = authService.register(credential, password, code);
        return Result.ok(result);
    }

    @PostMapping("/api/v1/register/email_code")
    public Result<Void> sendRegisterEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error(400, "请输入邮箱");
        }
        authService.sendRegisterEmailCode(email);
        return Result.ok();
    }

    @PostMapping("/api/v1/register/phone_code")
    public Result<Void> sendRegisterPhoneCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.isEmpty()) {
            return Result.error(400, "请输入手机号");
        }
        authService.sendRegisterPhoneCode(phone);
        return Result.ok();
    }

    // ==================== 登录 ====================

    @PostMapping("/api/v1/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, Object> request) {
        String credential = authService.extractCredential(request);
        String password = (String) request.get("password");
        String code = (String) request.get("code");

        if (credential == null || credential.isEmpty()) {
            return Result.error(400, "请输入手机号/邮箱/用户名");
        }
        if (password == null || password.isEmpty()) {
            return Result.error(400, "请输入密码");
        }

        Map<String, Object> result = authService.login(credential, password, code);
        return Result.ok(result);
    }

    @PostMapping("/api/v1/login/email_code")
    public Result<Void> sendLoginEmailCode(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return Result.error(400, "请输入邮箱");
        }
        authService.sendLoginEmailCode(email);
        return Result.ok();
    }

    @PostMapping("/api/v1/login/phone_code")
    public Result<Void> sendLoginPhoneCode(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || phone.isEmpty()) {
            return Result.error(400, "请输入手机号");
        }
        authService.sendLoginPhoneCode(phone);
        return Result.ok();
    }
}