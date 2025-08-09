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
package org.cloud.sonic.controller.services;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import org.cloud.sonic.controller.models.domain.Agents;

import java.util.List;

/**
 * Agent端业务逻辑服务接口
 *
 * <p>
 * 该接口定义了Agent端管理的核心业务逻辑，提供Agent端的完整生命周期管理功能。</p>
 *
 * <p>
 * 核心业务功能：</p>
 * <ul>
 * <li>Agent端注册与认证管理</li>
 * <li>Agent端状态监控与更新</li>
 * <li>Agent端配置管理（温度阈值、通知配置等）</li>
 * <li>Agent端版本管理和升级</li>
 * <li>Agent端设备管理协调</li>
 * </ul>
 *
 * <p>
 * 状态管理：</p>
 * <ul>
 * <li>在线状态检测和维护</li>
 * <li>心跳机制实现</li>
 * <li>异常状态处理和恢复</li>
 * <li>负载均衡和资源分配</li>
 * </ul>
 *
 * <p>
 * 监控告警功能：</p>
 * <ul>
 * <li>Agent端设备温度监控</li>
 * <li>机器人通知配置管理</li>
 * <li>异常情况自动告警</li>
 * <li>性能指标收集</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/19
 * @see Agents Agent实体类
 */
public interface AgentsService extends IService<Agents> {

    List<Agents> findAgents();

    void update(int id, String name, int highTemp, int highTempTime, int robotType, String robotToken, String robotSecret, int[] alertRobotIds);

    boolean offLine(int id);

    Agents auth(String key);

    Agents findById(int id);

    void saveAgents(JSONObject jsonObject);

    void saveAgents(Agents agents);

    /**
     * 会根据 {@link Agents#getLockVersion()} 更新Agent状态
     *
     * @param agents agent对象
     * @return 是否更新成功
     */
    boolean updateAgentsByLockVersion(Agents agents);

    Agents findBySecretKey(String secretKey);

    void errCall(int id, String udId, int tem, int type);
}
