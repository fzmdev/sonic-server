package org.cloud.sonic.controller.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cloud.sonic.common.config.WebAspect;
import org.cloud.sonic.common.http.RespEnum;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.models.domain.AgentVersion;
import org.cloud.sonic.controller.services.AgentVersionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Agent版本管理相关")
@RestController
@RequestMapping("/agent-versions")
public class AgentVersionController {

    @Autowired
    private AgentVersionService agentVersionService;

    @WebAspect
    @Operation(summary = "新增Agent版本", description = "新增Agent版本信息")
    @PostMapping
    public RespModel<String> save(@Validated @RequestBody AgentVersion agentVersion) {
        agentVersionService.save(agentVersion);
        return new RespModel<>(RespEnum.UPDATE_OK);
    }

    @WebAspect
    @Operation(summary = "更新Agent版本", description = "更新Agent版本信息")
    @PutMapping
    public RespModel<String> update(@Validated @RequestBody AgentVersion agentVersion) {
        agentVersionService.updateById(agentVersion);
        return new RespModel<>(RespEnum.UPDATE_OK);
    }

    @WebAspect
    @Operation(summary = "查询所有Agent版本", description = "查询所有Agent版本列表")
    @GetMapping("/list")
    public RespModel<List<AgentVersion>> findAll() {
        return new RespModel<>(RespEnum.SEARCH_OK, agentVersionService.findAll());
    }

    @WebAspect
    @Operation(summary = "根据ID查询Agent版本", description = "查询指定ID的Agent版本详细信息")
    @Parameter(name = "id", description = "版本ID")
    @GetMapping
    public RespModel<AgentVersion> findById(@RequestParam(name = "id") Long id) {
        AgentVersion agentVersion = agentVersionService.findById(id);
        if (agentVersion != null) {
            return new RespModel<>(RespEnum.SEARCH_OK, agentVersion);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "根据版本号查询Agent版本", description = "查询指定版本号的Agent版本信息")
    @Parameter(name = "version", description = "版本号")
    @GetMapping("/by-version")
    public RespModel<AgentVersion> findByVersion(@RequestParam(name = "version") String version) {
        AgentVersion agentVersion = agentVersionService.findByVersion(version);
        if (agentVersion != null) {
            return new RespModel<>(RespEnum.SEARCH_OK, agentVersion);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "删除Agent版本", description = "删除指定ID的Agent版本")
    @Parameter(name = "id", description = "版本ID")
    @DeleteMapping
    public RespModel<String> delete(@RequestParam(name = "id") Long id) {
        if (agentVersionService.delete(id)) {
            return new RespModel<>(RespEnum.DELETE_OK);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }
}