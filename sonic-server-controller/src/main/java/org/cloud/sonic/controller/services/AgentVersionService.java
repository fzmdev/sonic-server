package org.cloud.sonic.controller.services;

import com.baomidou.mybatisplus.extension.service.IService;
import org.cloud.sonic.controller.models.domain.AgentVersion;

import java.util.List;

public interface AgentVersionService extends IService<AgentVersion> {

    boolean delete(Long id);

    List<AgentVersion> findAll();

    AgentVersion findById(Long id);

    AgentVersion findByVersion(String version);
}