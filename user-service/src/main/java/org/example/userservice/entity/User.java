package org.example.userservice.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;

    private String phone;

    private String passwordHash;

    private LocalDateTime createTime;
}
