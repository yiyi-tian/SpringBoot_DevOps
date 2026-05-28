package org.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.userservice.entity.User;
import org.example.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> register(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credentialType = (String) request.get("credentialType");
        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        // 参数校验
        if (credentialType == null || credential == null || password == null) {
            result.put("code", 400);
            result.put("message", "参数不完整");
            return result;
        }

        // 校验唯一性（按 credentialType + credential 查询）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("credential_type", credentialType);
        wrapper.eq("credential", credential);

        User existUser = userMapper.selectOne(wrapper);
        if (existUser != null) {
            result.put("code", 409);
            result.put("message", "该凭证已被注册");
            return result;
        }

        // BCrypt 加密
        String passwordHash = passwordEncoder.encode(password);

        // 创建用户
        User user = new User();
        user.setCredentialType(credentialType);
        user.setCredential(credential);
        user.setPasswordHash(passwordHash);
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 返回成功
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        result.put("data", data);
        return result;
    }

    public Map<String, Object> login(Map<String, Object> request) {
        Map<String, Object> result = new HashMap<>();

        String credential = (String) request.get("credential");
        String password = (String) request.get("password");

        // 查询用户（按 credential 字段）
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("credential", credential);

        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            result.put("code", 401);
            result.put("message", "凭证不存在");
            return result;
        }

        // BCrypt 校验
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            result.put("code", 401);
            result.put("message", "密码错误");
            return result;
        }

        // 登录成功
        result.put("code", 0);
        result.put("message", "ok");
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        result.put("data", data);
        return result;
    }
}