package org.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.userservice.entity.LoginHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginHistoryMapper extends BaseMapper<LoginHistory> {
}
