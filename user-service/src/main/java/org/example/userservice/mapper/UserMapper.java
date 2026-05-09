package org.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.userservice.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User>{

}
