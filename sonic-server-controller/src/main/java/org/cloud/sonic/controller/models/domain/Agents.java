package org.cloud.sonic.controller.models.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.gitee.sunchenbin.mybatis.actable.annotation.*;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlCharsetConstant;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlEngineConstant;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.cloud.sonic.controller.models.base.TypeConverter;
import org.cloud.sonic.controller.models.dto.AgentsDTO;
import org.cloud.sonic.controller.tools.NullableIntArrayTypeHandler;

import java.io.Serializable;

/**
 * Agent端实体类
 *
 * <p>
 * 该实体类映射agents数据库表，代表Sonic测试平台中的Agent端节点。</p>
 *
 * <p>
 * Agent端是Sonic平台的核心组件，主要功能包括：</p>
 * <ul>
 * <li>管理连接的真实设备</li>
 * <li>执行测试指令和操作</li>
 * <li>设备状态监控和数据收集</li>
 * <li>与服务端进行通信协调</li>
 * </ul>
 *
 * <p>
 * 数据库表结构：</p>
 * <ul>
 * <li>表名：agents</li>
 * <li>引擎：InnoDB</li>
 * <li>字符集：默认UTF8</li>
 * <li>主键：id（自增）</li>
 * </ul>
 *
 * <p>
 * 核心字段说明：</p>
 * <ul>
 * <li>host: Agent端的IP地址</li>
 * <li>port: Agent端的服务端口</li>
 * <li>status: Agent端当前状态（在线/离线）</li>
 * <li>version: Agent端代码版本号</li>
 * <li>systemType: 系统类型（Windows/Linux/macOS）</li>
 * <li>highTemp/highTempTime: 高温监控阈值配置</li>
 * <li>alertRobotIds: 关联的告警机器人ID数组</li>
 * </ul>
 *
 * @author JayWenStar, Eason
 * @version 1.0
 * @since 2021-12-17
 * @see AgentsDTO Agent数据传输对象
 */
@Schema(name = "Agents对象", description = "Agent端实体，管理测试平台的Agent节点信息")
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "agents", autoResultMap = true)
@TableComment("agents表")
@TableCharset(MySqlCharsetConstant.DEFAULT)
@TableEngine(MySqlEngineConstant.InnoDB)
public class Agents implements Serializable, TypeConverter<Agents, AgentsDTO> {

    @TableId(value = "id", type = IdType.AUTO)
    @IsAutoIncrement
    private Integer id;

    @TableField
    @Column(isNull = false, comment = "agent的ip")
    private String host;

    @TableField
    @Column(isNull = false, comment = "agent name")
    private String name;

    @TableField
    @Column(isNull = false, comment = "agent的端口")
    private Integer port;

    @TableField
    @Column(value = "secret_key", comment = "agent的密钥", defaultValue = "")
    private String secretKey;

    @TableField
    @Column(isNull = false, comment = "agent的状态")
    private Integer status;

    @TableField
    @Column(value = "system_type", isNull = false, comment = "agent的系统类型")
    private String systemType;

    @TableField
    @Column(isNull = false, comment = "agent端代码版本", defaultValue = "")
    private String version;

    @TableField
    @Column(value = "lock_version", isNull = false, defaultValue = "0", comment = "乐观锁，优先保证上下线状态落库")
    private Long lockVersion;

    @TableField
    @Column(value = "high_temp", isNull = false, comment = "highTemp", defaultValue = "45")
    private Integer highTemp;

    @TableField
    @Column(value = "high_temp_time", isNull = false, comment = "highTempTime", defaultValue = "15")
    private Integer highTempTime;

    /**
     * @see org.cloud.sonic.controller.config.mybatis.RobotConfigMigrate
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @TableField
    @Column(value = "robot_secret", isNull = false, comment = "机器人秘钥", defaultValue = "")
    private String robotSecret;

    /**
     * @see org.cloud.sonic.controller.config.mybatis.RobotConfigMigrate
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @TableField
    @Column(value = "robot_token", isNull = false, comment = "机器人token", defaultValue = "")
    private String robotToken;

    /**
     * @see org.cloud.sonic.controller.config.mybatis.RobotConfigMigrate
     * @deprecated
     */
    @Deprecated(forRemoval = true)
    @TableField
    @Column(value = "robot_type", isNull = false, comment = "机器人类型", defaultValue = "1")
    private Integer robotType;

    @TableField
    @Column(value = "has_hub", isNull = false, comment = "是否使用了Sonic hub", defaultValue = "0")
    private Integer hasHub;

    @TableField(typeHandler = NullableIntArrayTypeHandler.class, updateStrategy = FieldStrategy.IGNORED)
    @Column(value = "alert_robot_ids", type = MySqlTypeConstant.VARCHAR, length = 1024, comment = "逗号分隔通知机器人id串，为null时自动选取所有可用机器人")
    private int[] alertRobotIds;

    @TableField
    private String tideviceSocket;
}
