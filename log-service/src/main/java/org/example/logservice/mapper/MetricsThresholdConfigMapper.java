package org.example.logservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.logservice.entity.MetricsThresholdConfig;

@Mapper
public interface MetricsThresholdConfigMapper extends BaseMapper<MetricsThresholdConfig> {
}
