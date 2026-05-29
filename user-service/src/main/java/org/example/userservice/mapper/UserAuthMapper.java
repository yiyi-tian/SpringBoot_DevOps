package org.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.userservice.entity.UserAuth;

@Mapper
public interface UserAuthMapper extends BaseMapper<UserAuth> {
}