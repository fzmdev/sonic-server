package org.cloud.sonic.controller.models.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
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
import org.cloud.sonic.controller.models.dto.TestCasesDTO;

import java.io.Serializable;
import java.util.Date;

/**
 * 测试用例实体类
 *
 * <p>
 * 该实体类映射test_cases数据库表，代表Sonic测试平台中的测试用例。</p>
 *
 * <p>
 * 测试用例核心功能：</p>
 * <ul>
 * <li>测试用例基本信息管理</li>
 * <li>用例执行步骤组织</li>
 * <li>用例版本和历史追踪</li>
 * <li>用例分类和标签管理</li>
 * </ul>
 *
 * <p>
 * 用例组织结构：</p>
 * <ul>
 * <li>项目级别分组</li>
 * <li>平台类型分类</li>
 * <li>功能模块归属</li>
 * <li>优先级设定</li>
 * </ul>
 *
 * <p>
 * 关键字段说明：</p>
 * <ul>
 * <li>projectId: 所属项目ID</li>
 * <li>platform: 平台类型</li>
 * <li>moduleId: 功能模块ID</li>
 * <li>name: 用例名称</li>
 * <li>designer: 用例设计者</li>
 * <li>createTime/editTime: 创建和修改时间</li>
 * </ul>
 *
 * @author JayWenStar
 * @version 1.0
 * @since 2021-12-17
 * @see TestCasesDTO 测试用例数据传输对象
 */
@Schema(name = "TestCases对象", description = "测试用例实体，管理测试平台的测试用例信息")
@Data
@Accessors(chain = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("test_cases")
@TableComment("测试用例表")
@TableCharset(MySqlCharsetConstant.DEFAULT)
@TableEngine(MySqlEngineConstant.InnoDB)
public class TestCases implements Serializable, TypeConverter<TestCases, TestCasesDTO> {

    @TableId(value = "id", type = IdType.AUTO)
    @IsAutoIncrement
    private Integer id;

    @TableField
    @Column(isNull = false, comment = "用例描述")
    private String des;

    @TableField
    @Column(isNull = false, comment = "用例设计人")
    private String designer;

    @Schema(description = "最后修改日期", required = true, example = "2021-08-15 11:10:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @Column(value = "edit_time", type = MySqlTypeConstant.DATETIME, isNull = false, comment = "最后修改日期")
    private Date editTime;

    @TableField
    @Column(value = "module_id", isNull = true, comment = "所属模块", defaultValue = "0")
    @Index(value = "IDX_MODULE_ID", columns = {"module_id"})
    private Integer moduleId;

    @TableField
    @Column(isNull = false, comment = "用例名称")
    private String name;

    @TableField
    @Column(isNull = false, comment = "设备系统类型")
    private Integer platform;

    @TableField
    @Column(value = "project_id", isNull = false, comment = "所属项目id")
    @Index(value = "IDX_PROJECT_ID", columns = {"project_id"})
    private Integer projectId;

    @TableField
    @Column(value = "version", isNull = false, comment = "版本号")
    private String version;
}
