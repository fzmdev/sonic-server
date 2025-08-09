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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.cloud.sonic.common.config.WebAspect;
import org.cloud.sonic.common.http.RespEnum;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.models.base.CommentPage;
import org.cloud.sonic.controller.models.domain.Devices;
import org.cloud.sonic.controller.models.http.DeviceDetailChange;
import org.cloud.sonic.controller.models.http.OccupyParams;
import org.cloud.sonic.controller.models.http.UpdateDeviceImg;
import org.cloud.sonic.controller.services.DevicesService;
import org.cloud.sonic.controller.transport.TransportWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 设备管理控制器
 *
 * <p>
 * 该控制器提供设备管理相关的REST API接口，是Sonic测试平台的核心功能模块。</p>
 *
 * <p>
 * 主要功能包括：</p>
 * <ul>
 * <li>设备占用与释放管理</li>
 * <li>设备信息查询与筛选</li>
 * <li>设备配置更新（密码、图片、位置等）</li>
 * <li>设备状态监控（电池、温度）</li>
 * <li>设备远程控制（重启、调试停止）</li>
 * <li>设备批量操作</li>
 * </ul>
 *
 * <p>
 * 支持的设备平台：</p>
 * <ul>
 * <li>Android设备</li>
 * <li>iOS设备</li>
 * <li>HarmonyOS设备</li>
 * </ul>
 *
 * <p>
 * 设备状态管理：</p>
 * <ul>
 * <li>ONLINE - 设备在线可用</li>
 * <li>DEBUGGING - 设备被占用调试中</li>
 * <li>TESTING - 设备执行测试中</li>
 * <li>OFFLINE - 设备离线</li>
 * <li>ERROR - 设备异常</li>
 * </ul>
 *
 * @author Sonic Team
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "设备管理相关")
@RestController
@RequestMapping("/devices")
public class DevicesController {

    /**
     * 设备服务层注入
     */
    @Autowired
    private DevicesService devicesService;

    /**
     * 设备占用接口
     *
     * <p>
     * 通过REST API远程占用设备，用于远程调试和测试。设备被占用后会：</p>
     * <ul>
     * <li>设置设备状态为DEBUGGING</li>
     * <li>开启设备相关调试端口（ADB、iOS调试端口等）</li>
     * <li>记录占用者信息</li>
     * <li>启动设备监控</li>
     * </ul>
     *
     * <p>
     * 安全机制：</p>
     * <ul>
     * <li>需要有效的SonicToken进行身份验证</li>
     * <li>同一设备同时只能被一个用户占用</li>
     * <li>占用时间有限制，超时自动释放</li>
     * </ul>
     *
     * <p>
     * 支持的占用参数：</p>
     * <ul>
     * <li>设备序列号（udId）</li>
     * <li>占用时长限制</li>
     * <li>特殊配置参数</li>
     * </ul>
     *
     * @param occupyParams 设备占用参数对象，包含设备信息和占用配置
     * @param request HTTP请求对象，用于获取认证token
     * @return 占用操作的响应结果
     * <ul>
     * <li>成功时返回设备调试信息（端口、连接方式等）</li>
     * <li>未授权时返回UNAUTHORIZED错误</li>
     * <li>设备不可用时返回相应错误信息</li>
     * </ul>
     *
     * @throws IllegalArgumentException 当参数不合法时抛出
     * @see OccupyParams 设备占用参数
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "通过REST API占用设备", description = "远程占用设备并开启相关端口")
    @PostMapping("/occupy")
    public RespModel occupy(@Validated @RequestBody OccupyParams occupyParams, HttpServletRequest request) {
        String token = request.getHeader("SonicToken");
        if (token == null) {
            return new RespModel(RespEnum.UNAUTHORIZED);
        }
        return devicesService.occupy(occupyParams, token);
    }

    /**
     * 设备释放接口
     *
     * <p>
     * 释放之前占用的设备，将设备状态恢复为可用状态。释放操作包括：</p>
     * <ul>
     * <li>关闭设备调试端口</li>
     * <li>清除占用者信息</li>
     * <li>恢复设备状态为ONLINE</li>
     * <li>停止设备监控进程</li>
     * </ul>
     *
     * <p>
     * 安全限制：</p>
     * <ul>
     * <li>只有设备的当前占用者才能释放设备</li>
     * <li>需要有效的SonicToken进行身份验证</li>
     * <li>管理员可以强制释放任何设备</li>
     * </ul>
     *
     * <p>
     * 释放后的设备将：</p>
     * <ul>
     * <li>重新变为可用状态，其他用户可以占用</li>
     * <li>清除所有临时配置和调试信息</li>
     * <li>重置网络端口分配</li>
     * </ul>
     *
     * @param udId 设备序列号，用于唯一标识要释放的设备
     * @param request HTTP请求对象，用于获取认证token和用户信息
     * @return 释放操作的响应结果
     * <ul>
     * <li>成功时返回释放成功信息</li>
     * <li>未授权时返回UNAUTHORIZED错误</li>
     * <li>设备不存在或不是当前用户占用时返回相应错误</li>
     * </ul>
     *
     * @throws IllegalArgumentException 当设备序列号格式不正确时抛出
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "通过REST API释放设备", description = "远程释放设备并释放相关端口，只能由占用者释放")
    @Parameter(name = "udId", description = "设备序列号")
    @GetMapping("/release")
    public RespModel release(@RequestParam(name = "udId") String udId, HttpServletRequest request) {
        String token = request.getHeader("SonicToken");
        if (token == null) {
            return new RespModel(RespEnum.UNAUTHORIZED);
        }
        return devicesService.release(udId, token);
    }

    @WebAspect
    @Operation(summary = "强制解除设备占用", description = "强制解除设备占用")
    @Parameter(name = "udId", description = "设备序列号")
    @GetMapping("/stopDebug")
    public RespModel<List<Devices>> stopDebug(@RequestParam(name = "udId") String udId) {
        Devices devices = devicesService.findByUdId(udId);
        if (devices != null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("msg", "stopDebug");
            jsonObject.put("udId", udId);
            TransportWorker.send(devices.getAgentId(), jsonObject);
            return new RespModel<>(RespEnum.HANDLE_OK);
        } else {
            return new RespModel<>(RespEnum.DEVICE_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "查询Agent所有设备", description = "不分页的设备列表")
    @Parameter(name = "agentId", description = "平台")
    @GetMapping("/listByAgentId")
    public RespModel<List<Devices>> listByAgentId(@RequestParam(name = "agentId") int agentId) {
        return new RespModel<>(RespEnum.SEARCH_OK,
                devicesService.listByAgentId(agentId));
    }

    @WebAspect
    @Operation(summary = "修改设备安装密码", description = "修改对应设备id的安装密码")
    @PutMapping("/saveDetail")
    public RespModel<String> saveDetail(@Validated @RequestBody DeviceDetailChange deviceDetailChange) {
        if (devicesService.saveDetail(deviceDetailChange)) {
            return new RespModel<>(RespEnum.UPDATE_OK);
        } else {
            return new RespModel<>(3000, "fail.save");
        }
    }

    @WebAspect
    @Operation(summary = "更新设备Pos", description = "更新设备Pos")
    @Parameters(value = {
        @Parameter(name = "id", description = "id"),
        @Parameter(name = "position", description = "position")
    })
    @GetMapping("/updatePosition")
    public RespModel updatePosition(@RequestParam(name = "id") int id, @RequestParam(name = "position") int position) {
        devicesService.updatePosition(id, position);
        return new RespModel<>(RespEnum.HANDLE_OK);
    }

    @WebAspect
    @Operation(summary = "修改设备图片", description = "修改对应设备id的图片")
    @PutMapping("/updateImg")
    public RespModel<String> updateImg(@Validated @RequestBody UpdateDeviceImg updateDeviceImg) {
        devicesService.updateImg(updateDeviceImg);
        return new RespModel<>(RespEnum.UPDATE_OK);
    }

    /**
     * 分页查询设备列表（支持多条件筛选）
     *
     * <p>
     * 提供强大的设备筛选和分页查询功能，支持多种筛选条件的组合使用：</p>
     *
     * <p>
     * 平台版本筛选：</p>
     * <ul>
     * <li>Android版本筛选（支持多选）</li>
     * <li>iOS版本筛选（支持多选）</li>
     * <li>HarmonyOS版本筛选（支持多选）</li>
     * </ul>
     *
     * <p>
     * 硬件规格筛选：</p>
     * <ul>
     * <li>设备制造商（如Samsung、Apple、Huawei等）</li>
     * <li>CPU架构类型（如arm64、x86等）</li>
     * <li>屏幕尺寸规格</li>
     * </ul>
     *
     * <p>
     * 管理维度筛选：</p>
     * <ul>
     * <li>所属Agent筛选（支持多选）</li>
     * <li>设备状态筛选（支持多选）</li>
     * <li>设备型号或序列号模糊搜索</li>
     * </ul>
     *
     * <p>
     * 注意事项：</p>
     * <ul>
     * <li>带[]的参数支持传递多个值进行多选筛选</li>
     * <li>所有筛选条件为AND关系</li>
     * <li>返回结果按设备ID排序</li>
     * </ul>
     *
     * @param androidVersion Android版本列表，可选
     * @param iOSVersion iOS版本列表，可选
     * @param hmVersion HarmonyOS版本列表，可选
     * @param manufacturer 设备制造商列表，可选
     * @param cpu CPU架构类型列表，可选
     * @param size 屏幕尺寸列表，可选
     * @param agentId 所属Agent ID列表，可选
     * @param status 设备状态列表，可选
     * @param deviceInfo 设备型号或序列号关键字，可选
     * @param page 页码，从1开始
     * @param pageSize 每页数据量
     * @return 分页设备列表响应结果
     *
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "查询所有设备", description = "查找筛选条件下的所有设备，带[]的参数可以重复传")
    @Parameters(value = {
        @Parameter(name = "androidVersion[]", description = "安卓版本"),
        @Parameter(name = "iOSVersion[]", description = "iOS版本"),
        @Parameter(name = "hmVersion[]", description = "鸿蒙版本"),
        @Parameter(name = "manufacturer[]", description = "制造商"),
        @Parameter(name = "cpu[]", description = "cpu类型"),
        @Parameter(name = "size[]", description = "屏幕尺寸"),
        @Parameter(name = "agentId[]", description = "所在Agent"),
        @Parameter(name = "status[]", description = "当前状态"),
        @Parameter(name = "deviceInfo", description = "设备型号或udId"),
        @Parameter(name = "page", description = "页码"),
        @Parameter(name = "pageSize", description = "页数据大小")
    })
    @GetMapping("/list")
    public RespModel<CommentPage<Devices>> findAll(@RequestParam(name = "androidVersion[]", required = false) List<String> androidVersion,
            @RequestParam(name = "iOSVersion[]", required = false) List<String> iOSVersion,
            @RequestParam(name = "hmVersion[]", required = false) List<String> hmVersion,
            @RequestParam(name = "manufacturer[]", required = false) List<String> manufacturer,
            @RequestParam(name = "cpu[]", required = false) List<String> cpu,
            @RequestParam(name = "size[]", required = false) List<String> size,
            @RequestParam(name = "agentId[]", required = false) List<Integer> agentId,
            @RequestParam(name = "status[]", required = false) List<String> status,
            @RequestParam(name = "deviceInfo", required = false) String deviceInfo,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize) {
        Page<Devices> pageable = new Page<>(page, pageSize);
        return new RespModel<>(
                RespEnum.SEARCH_OK,
                CommentPage.convertFrom(
                        devicesService.findAll(iOSVersion, androidVersion, hmVersion, manufacturer, cpu, size,
                                agentId, status, deviceInfo, pageable)
                )
        );
    }

    @WebAspect
    @Operation(summary = "查询所有设备", description = "不分页的设备列表")
    @Parameter(name = "platform", description = "平台")
    @GetMapping("/listAll")
    public RespModel<List<Devices>> listAll(@RequestParam(name = "platform") int platform) {
        return new RespModel<>(RespEnum.SEARCH_OK,
                devicesService.findAll(platform));
    }

    @WebAspect
    @Operation(summary = "批量查询设备", description = "查找id列表的设备信息，可以传多个ids[]")
    @Parameter(name = "ids[]", description = "id列表")
    @GetMapping("/findByIdIn")
    public RespModel<List<Devices>> findByIdIn(@RequestParam(name = "ids[]") List<Integer> ids) {
        return new RespModel<>(RespEnum.SEARCH_OK,
                devicesService.findByIdIn(ids));
    }

    @WebAspect
    @Operation(summary = "获取查询条件", description = "获取现有筛选条件（所有设备有的条件）")
    @GetMapping("/getFilterOption")
    public RespModel<JSONObject> getFilterOption() {
        return new RespModel<>(RespEnum.SEARCH_OK, devicesService.getFilterOption());
    }

//    @WebAspect
//    @Operation(summary = "查询单个设备信息", description = "获取单个设备的详细信息")
//    @Parameter(name = "udId", value = "设备序列号")
//    @GetMapping
//    public RespModel<Devices> findByUdId(@RequestParam(name = "udId") String udId) {
//        Devices devices = devicesService.findByUdId(udId);
//        if (devices != null) {
//            return new RespModel(RespEnum.SEARCH_OK, devices);
//        } else {
//            return new RespModel(3000, "设备不存在！");
//        }
//    }
    @WebAspect
    @Operation(summary = "设备信息", description = "获取指定设备信息")
    @GetMapping
    public RespModel<Devices> findById(@RequestParam(name = "id") int id) {
        Devices devices = devicesService.findById(id);
        if (devices != null) {
            return new RespModel<>(RespEnum.SEARCH_OK, devices);
        } else {
            return new RespModel<>(RespEnum.DEVICE_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "获取电池概况", description = "获取现有电池概况")
    @GetMapping("/findTemper")
    public RespModel<Integer> findTemper() {
        return new RespModel<>(RespEnum.SEARCH_OK, devicesService.findTemper());
    }

    @WebAspect
    @Operation(summary = "删除设备", description = "设备必须离线才能删除，会删除设备与套件绑定关系")
    @DeleteMapping()
    public RespModel<String> delete(@RequestParam(name = "id") int id) {
        return devicesService.delete(id);
    }
}
