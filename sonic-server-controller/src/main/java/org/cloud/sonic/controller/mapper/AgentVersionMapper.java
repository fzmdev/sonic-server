package org.cloud.sonic.controller.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.cloud.sonic.controller.models.domain.AgentVersion;

@Mapper
public interface AgentVersionMapper extends BaseMapper<AgentVersion> {

}