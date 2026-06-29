package org.example.messageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.messageservice.entity.TemplateVariable;

@Mapper
public interface TemplateVariableMapper extends BaseMapper<TemplateVariable> {
}