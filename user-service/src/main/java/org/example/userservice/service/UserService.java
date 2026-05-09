package org.example.userservice.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.example.userservice.entity.User;
import org.example.userservice.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public String register(String phone, String password){

        //查询手机号是否存在
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("phone",phone);

        User existUser = userMapper.selectOne(wrapper);

        if(existUser != null){
            return "手机号已存在";
        }

        //密码加密
        String passwordHash = DigestUtils.md5DigestAsHex(password.getBytes());

        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setCreateTime(LocalDateTime.now());

        userMapper.insert(user);

        return "注册成功";
    }
}
