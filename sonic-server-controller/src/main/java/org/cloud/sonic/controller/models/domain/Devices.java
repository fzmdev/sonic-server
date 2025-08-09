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
package org.cloud.sonic.controller.models.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.*;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlCharsetConstant;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlEngineConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.cloud.sonic.controller.models.base.TypeConverter;
import org.cloud.sonic.controller.models.dto.DevicesDTO;

import java.io.Serializable;

/**
 * 设备实体类
 *
 * <p>
 * 该实体类映射devices数据库表，代表Sonic测试平台中的真实移动设备。</p>
 *
 * <p>
 * 设备管理核心功能：</p>
 * <ul>
 * <li>设备基本信息管理（型号、系统版本、制造商等）</li>
 * <li>设备状态监控（在线、调试中、测试中、离线等）</li>
 * <li>设备占用管理（用户占用、释放控制）</li>
 * <li>设备性能监控（电池、温度、电压等）</li>
 * </ul>
 *
 * <p>
 * 支持的设备平台：</p>
 * <ul>
 * <li>Android设备（包括鸿蒙系统）</li>
 * <li>iOS设备</li>
 * <li>其他移动平台设备</li>
 * </ul>
 *
 * <p>
 * 数据库表结构：</p>
 * <ul>
 * <li>表名：devices</li>
 * <li>引擎：InnoDB</li>
 * <li>字符集：默认UTF8</li>
 * <li>主键：id（自增）</li>
 * <li>索引：ud_id（设备序列号）</li>
 * </ul>
 *
 * <p>
 * 关键字段说明：</p>
 * <ul>
 * <li>udId: 设备唯一序列号</li>
 * <li>agentId: 所属Agent端ID</li>
 * <li>platform: 平台类型（1=Android, 2=iOS）</li>
 * <li>status: 设备状态</li>
 * <li>user: 当前占用用户</li>
 * <li>temperature/voltage/level: 设备性能监控数据</li>
 * </ul>
 *
 * @author JayWenStar
 * @version 1.0
 * @since 2021-12-17
 * @see DevicesDTO 设备数据传输对象
 */
@Schema(name = "Devices对象", description = "设备实体，管理测试平台的真实移动设备信息")
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("devices")
@TableComment("设备表")
@TableCharset(MySqlCharsetConstant.DEFAULT)
@TableEngine(MySqlEngineConstant.InnoDB)
public class Devices implements Serializable, TypeConverter<Devices, DevicesDTO> {

    @TableId(value = "id", type = IdType.AUTO)
    @IsAutoIncrement
    private Integer id;

    @TableField
    @Column(value = "agent_id", isNull = false, comment = "所属agent的id")
    private Integer agentId;

    @TableField
    @Column(comment = "cpu架构", defaultValue = "")
    private String cpu;

    @TableField
    @Column(value = "img_url", comment = "手机封面", defaultValue = "")
    private String imgUrl;

    @TableField
    @Column(comment = "制造商", defaultValue = "")
    private String manufacturer;

    @TableField
    @Column(comment = "手机型号", defaultValue = "")
    private String model;

    @TableField
    @Column(comment = "设备名称", defaultValue = "")
    private String name;

    @TableField
    @Column(comment = "设备安装app的密码", defaultValue = "")
    private String password;

    @TableField
    @Column(isNull = false, comment = "系统类型 1：android 2：ios")
    private Integer platform;

    @TableField
    @Column(value = "is_hm", isNull = false, comment = "是否为鸿蒙类型 1：鸿蒙 0：非鸿蒙", defaultValue = "0")
    private Integer isHm;

    @TableField
    @Column(comment = "设备分辨率", defaultValue = "")
    private String size;

    @TableField
    @Column(comment = "设备状态", defaultValue = "")
    private String status;

    @TableField
    @Column(value = "ud_id", comment = "设备序列号", defaultValue = "")
    @Index(value = "IDX_UD_ID", columns = {"ud_id"})
    private String udId;

    @TableField
    @Column(comment = "设备系统版本", defaultValue = "")
    private String version;

    @TableField
    @Column(value = "nick_name", comment = "设备备注", defaultValue = "")
    private String nickName;

    @TableField
    @Column(comment = "设备当前占用者", defaultValue = "")
    private String user;

    @TableField
    @Column(value = "chi_name", comment = "中文设备", defaultValue = "")
    String chiName;

    @TableField
    @Column(defaultValue = "0", comment = "设备温度")
    Integer temperature;

    @TableField
    @Column(defaultValue = "0", comment = "设备电池电压")
    Integer voltage;

    @TableField
    @Column(defaultValue = "0", comment = "设备电量")
    Integer level;

    @TableField
    @Column(defaultValue = "0", comment = "HUB位置")
    Integer position;

    public static Devices newDeletedDevice(int id) {
        String tips = "Device does not exist.";
        return new Devices()
                .setAgentId(0)
                .setStatus("DISCONNECTED")
                .setPlatform(0)
                .setIsHm(0)
                .setId(id)
                .setVersion("unknown")
                .setSize("unknown")
                .setCpu("unknown")
                .setManufacturer("unknown")
                .setName(tips)
                .setModel(tips)
                .setChiName(tips)
                .setNickName(tips)
                .setName(tips)
                .setUser(tips)
                .setUdId(tips)
                .setPosition(0)
                .setTemperature(0)
                .setVoltage(0)
                .setLevel(0);
    }
}
