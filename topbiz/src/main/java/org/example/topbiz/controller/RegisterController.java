package org.example.topbiz.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class RegisterController {

    @Autowired
    private RestTemplate restTemplate;

    @PostMapping("api/v1/register")
    public String register(@RequestParam String phone, @RequestParam String password) {

        // 参数校验
        if (phone == null || phone.length() != 11) {
            return "手机号非法";
        }

        if (password == null || password.length() < 6) {
            return "密码太短";
        }

        //调用UserService
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("phone", phone);
        params.add("password", password);

        String userResult = restTemplate.postForObject(
                "http://localhost:8081/internal/user/register",
                params,
                String.class);

        //注册失败
        if(!"注册成功".equals(userResult)){
            return userResult;
        }

        // 调用 MessageService
        MultiValueMap<String, String> msgParams = new LinkedMultiValueMap<>();
        msgParams.add("userId", "1");
        msgParams.add("content", "欢迎注册");

        restTemplate.postForObject(
                "http://localhost:8082/internal/message/send",
                msgParams,
                String.class
        );

        // 调用 LogService
        MultiValueMap<String, String> logParams = new LinkedMultiValueMap<>();
        logParams.add("userId", "1");
        logParams.add("operation", "用户注册");

        restTemplate.postForObject(
                "http://localhost:8083/internal/log/record",
                logParams,
                String.class
        );

        return "TopBiz：注册成功";
    }

}
