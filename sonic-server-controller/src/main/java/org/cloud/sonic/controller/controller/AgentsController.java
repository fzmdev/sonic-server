/*
 *   sonic-server  Sonic Cloud Real Machine Platform.
 *   Copyright (C) 2022 SonicCloudOrg
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.cloud.sonic.controller.controller;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cloud.sonic.common.config.WebAspect;
import org.cloud.sonic.common.http.RespEnum;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.models.base.TypeConverter;
import org.cloud.sonic.controller.models.domain.Agents;
import org.cloud.sonic.controller.models.dto.AgentsDTO;
import org.cloud.sonic.controller.services.AgentsService;
import org.cloud.sonic.controller.transport.TransportWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Agent端管理控制器
 *
 * <p>
 * 该控制器提供与Agent端相关的REST API接口，包括：</p>
 * <ul>
 * <li>Agent端设备控制和管理</li>
 * <li>Hub设备位置控制</li>
 * <li>Agent端信息查询和更新</li>
 * <li>Agent端状态监控</li>
 * </ul>
 *
 * <p>
 * Agent是Sonic平台的核心组件，负责：</p>
 * <ul>
 * <li>连接和管理真实设备</li>
 * <li>执行测试指令</li>
 * <li>设备状态监控和温度管理</li>
 * <li>设备Hub控制（如果配置）</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/28
 */
@Tag(name = "Agent端相关")
@RestController
@RequestMapping("/agents")
public class AgentsController {

    /**
     * Agent服务层注入
     */
    @Autowired
    private AgentsService agentsService;

    /**
     * Hub设备控制接口
     *
     * <p>
     * 用于控制Agent端连接的Hub设备，可以控制设备的位置切换和类型操作。 Hub是一个硬件设备，可以在多个移动设备之间进行切换。</p>
     *
     * <p>
     * 支持的操作类型：</p>
     * <ul>
     * <li>设备位置切换</li>
     * <li>设备连接状态控制</li>
     * <li>设备电源管理</li>
     * </ul>
     *
     * @param id Agent的唯一标识符
     * @param position Hub设备的目标位置（1-N，具体数量取决于Hub型号）
     * @param type 控制类型，支持多种操作模式
     * @return 响应结果，包含操作是否成功的信息
     *
     * @throws IllegalArgumentException 当参数不合法时抛出
     * @since 1.0
     */
    @WebAspect
    @GetMapping("/hubControl")
    public RespModel<?> hubControl(@RequestParam(name = "id") int id, @RequestParam(name = "position") int position,
            @RequestParam(name = "type") String type) {
        JSONObject result = new JSONObject();
        result.put("msg", "hub");
        result.put("position", position);
        result.put("type", type);
        TransportWorker.send(id, result);
        return new RespModel<>(RespEnum.HANDLE_OK);
    }

    /**
     * 查询所有Agent端列表
     *
     * <p>
     * 获取系统中所有已注册的Agent端信息，包括：</p>
     * <ul>
     * <li>Agent基本信息（名称、IP、端口）</li>
     * <li>Agent状态（在线/离线）</li>
     * <li>Agent版本信息</li>
     * <li>系统类型和配置</li>
     * <li>温度监控配置</li>
     * <li>机器人通知配置</li>
     * </ul>
     *
     * <p>
     * 返回的数据会自动转换为DTO格式，便于前端展示和处理。</p>
     *
     * @return Agent端列表的响应结果
     * <ul>
     * <li>成功时返回所有Agent的详细信息列表</li>
     * <li>失败时返回错误信息</li>
     * </ul>
     *
     * @see AgentsDTO Agent数据传输对象
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "查询所有Agent端", description = "获取所有Agent端以及详细信息")
    @GetMapping("/list")
    public RespModel<List<AgentsDTO>> findAgents() {
        return new RespModel<>(
                RespEnum.SEARCH_OK,
                agentsService.findAgents().stream().map(TypeConverter::convertTo).collect(Collectors.toList())
        );
    }

    /**
     * 更新Agent配置信息
     *
     * <p>
     * 更新指定Agent的配置参数，支持修改以下信息：</p>
     * <ul>
     * <li>Agent显示名称</li>
     * <li>高温预警阈值设置</li>
     * <li>高温持续时间阈值</li>
     * <li>机器人通知配置</li>
     * <li>告警机器人ID列表</li>
     * </ul>
     *
     * <p>
     * 温度监控功能说明：</p>
     * <ul>
     * <li>highTemp: 设备温度超过此值时触发预警</li>
     * <li>highTempTime: 高温持续超过此时间（分钟）后发送告警</li>
     * </ul>
     *
     * @param jsonObject Agent配置信息的数据传输对象，包含要更新的字段
     * @return 更新操作的响应结果
     * <ul>
     * <li>成功时返回处理成功信息</li>
     * <li>失败时返回相应错误码和错误信息</li>
     * </ul>
     *
     * @throws IllegalArgumentException 当传入的参数格式不正确时抛出
     * @see AgentsDTO Agent数据传输对象
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "修改agent信息", description = "修改agent信息")
    @PutMapping("/update")
    public RespModel<String> update(@RequestBody AgentsDTO jsonObject) {
        agentsService.update(jsonObject.getId(),
                jsonObject.getName(), jsonObject.getHighTemp(),
                jsonObject.getHighTempTime(), jsonObject.getRobotType(),
                jsonObject.getRobotToken(), jsonObject.getRobotToken(),
                jsonObject.getAlertRobotIds());
        return new RespModel<>(RespEnum.HANDLE_OK);
    }

    /**
     * 根据ID查询单个Agent详细信息
     *
     * <p>
     * 根据提供的Agent ID获取对应的Agent详细信息，包括：</p>
     * <ul>
     * <li>Agent基本配置（名称、主机、端口等）</li>
     * <li>Agent当前状态和版本信息</li>
     * <li>系统类型和密钥配置</li>
     * <li>温度监控相关配置</li>
     * <li>机器人通知配置</li>
     * <li>Hub设备配置状态</li>
     * </ul>
     *
     * <p>
     * 常用于以下场景：</p>
     * <ul>
     * <li>Agent详情页面展示</li>
     * <li>Agent配置修改前的数据加载</li>
     * <li>Agent状态检查和监控</li>
     * </ul>
     *
     * @param id Agent的唯一标识符，必须是有效的正整数
     * @return 查询结果的响应对象
     * <ul>
     * <li>成功时返回Agent的完整信息</li>
     * <li>ID不存在时返回NOT_FOUND错误</li>
     * </ul>
     *
     * @throws IllegalArgumentException 当ID格式不正确时抛出
     * @see Agents Agent领域模型
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "查询Agent端信息", description = "获取对应id的Agent信息")
    @GetMapping
    public RespModel<?> findOne(@RequestParam(name = "id") int id) {
        Agents agents = agentsService.findById(id);
        if (agents != null) {
            return new RespModel<>(RespEnum.SEARCH_OK, agents);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }

    /**
     * 更新Agent的tideviceSocket字段
     *
     * <p>该接口专门用于Agent端自动更新其tideviceSocket地址。当Agent端启动tidevice socket服务后，
     * 会自动调用此接口将socket地址同步到服务端。</p>
     *
     * @param request HTTP请求对象，从SonicToken header获取secretKey
     * @param requestBody 包含tideviceSocket字段的JSON对象
     * @return 更新结果响应
     */
    @WebAspect
    @Operation(summary = "更新Agent的tideviceSocket字段", description = "Agent端自动更新tideviceSocket地址")
    @PutMapping("/updateTideviceSocket")
    public RespModel<String> updateTideviceSocket(HttpServletRequest request, @RequestBody JSONObject requestBody) {
        String secretKey = requestBody.getString("secretKey");
        if (secretKey == null || secretKey.trim().isEmpty()) {
            return new RespModel<>(RespEnum.UNAUTHORIZED);
        }

        Agents agent = agentsService.findBySecretKey(secretKey);
        if (agent == null) {
            return new RespModel<>(RespEnum.UNAUTHORIZED);
        }

        String tideviceSocket = requestBody.getString("tideviceSocket");
        if (tideviceSocket != null) {
            agent.setTideviceSocket(tideviceSocket);
            agentsService.updateById(agent);
            agentsService.updateAgentsByLockVersion(agent);
        }

        return new RespModel<>(RespEnum.HANDLE_OK);
    }
}
