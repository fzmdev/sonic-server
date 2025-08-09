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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.models.domain.Devices;
import org.cloud.sonic.controller.models.http.DeviceDetailChange;
import org.cloud.sonic.controller.models.http.OccupyParams;
import org.cloud.sonic.controller.models.http.UpdateDeviceImg;

import java.io.IOException;
import java.util.List;

/**
 * 设备管理业务逻辑服务接口
 *
 * <p>
 * 该接口定义了设备管理的核心业务逻辑，是Sonic测试平台的关键业务服务。</p>
 *
 * <p>
 * 设备生命周期管理：</p>
 * <ul>
 * <li>设备注册与发现</li>
 * <li>设备状态实时监控</li>
 * <li>设备配置动态更新</li>
 * <li>设备资源分配与回收</li>
 * </ul>
 *
 * <p>
 * 设备占用管理：</p>
 * <ul>
 * <li>设备占用与释放控制</li>
 * <li>占用权限验证</li>
 * <li>占用超时自动回收</li>
 * <li>并发占用冲突处理</li>
 * </ul>
 *
 * <p>
 * 设备查询与筛选：</p>
 * <ul>
 * <li>多维度设备筛选（平台、版本、规格等）</li>
 * <li>设备列表分页查询</li>
 * <li>设备使用情况统计</li>
 * <li>设备性能监控数据</li>
 * </ul>
 *
 * <p>
 * 设备维护功能：</p>
 * <ul>
 * <li>设备远程重启控制</li>
 * <li>设备调试状态管理</li>
 * <li>设备异常自动处理</li>
 * <li>设备清理与维护</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/16
 * @see Devices 设备实体类
 */
public interface DevicesService extends IService<Devices> {

    RespModel occupy(OccupyParams occupyParams, String token);

    RespModel release(String udId, String token);

    boolean saveDetail(DeviceDetailChange deviceDetailChange);

    void updatePosition(int id, int position);

    void updateDevicesUser(JSONObject jsonObject);

    void updateImg(UpdateDeviceImg updateDeviceImg);

    Page<Devices> findAll(List<String> iOSVersion, List<String> androidVersion, List<String> hmVersion, List<String> manufacturer,
            List<String> cpu, List<String> size, List<Integer> agentId, List<String> status,
            String deviceInfo, Page<Devices> pageable);

    List<Devices> findAll(int platform);

    List<Devices> findByIdIn(List<Integer> ids);

    Devices findByAgentIdAndUdId(int agentId, String udId);

    Devices findByUdId(String udId);

    JSONObject getFilterOption();

    void deviceStatus(JSONObject jsonObject);

    Devices findById(int id);

    List<Devices> listByAgentId(int agentId);

    String getName(String model) throws IOException;

    void refreshDevicesBattery(JSONObject jsonObject);

    Integer findTemper();

    RespModel<String> delete(int id);

}
