package org.example.messageservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.messageservice.entity.MsgTemplate;

@Mapper
public interface MsgTemplateMapper extends BaseMapper<MsgTemplate> {
}
