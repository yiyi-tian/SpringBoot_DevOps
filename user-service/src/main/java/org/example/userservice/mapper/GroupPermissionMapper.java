package org.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.userservice.entity.GroupPermission;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GroupPermissionMapper extends BaseMapper<GroupPermission> {
}
