package org.cloud.sonic.controller.services.impl;

import org.cloud.sonic.controller.mapper.AgentVersionMapper;
import org.cloud.sonic.controller.models.domain.AgentVersion;
import org.cloud.sonic.controller.services.AgentVersionService;
import org.cloud.sonic.controller.services.impl.base.SonicServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgentVersionServiceImpl extends SonicServiceImpl<AgentVersionMapper, AgentVersion> implements AgentVersionService {

    @Autowired
    private AgentVersionMapper agentVersionMapper;

    @Override
    public boolean delete(Long id) {
        return baseMapper.deleteById(id) > 0;
    }

    @Override
    public List<AgentVersion> findAll() {
        return lambdaQuery()
                .orderByDesc(AgentVersion::getId)
                .list();
    }

    @Override
    public AgentVersion findById(Long id) {
        return baseMapper.selectById(id);
    }

    @Override
    public AgentVersion findByVersion(String version) {
        return lambdaQuery()
                .eq(AgentVersion::getVersion, version)
                .one();
    }
}