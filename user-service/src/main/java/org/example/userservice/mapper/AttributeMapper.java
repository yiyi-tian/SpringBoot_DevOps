package org.example.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.userservice.entity.Attribute;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AttributeMapper extends BaseMapper<Attribute> {
}
