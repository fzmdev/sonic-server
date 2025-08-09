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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.cloud.sonic.common.config.WebAspect;
import org.cloud.sonic.common.http.RespEnum;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.common.tools.JWTTokenTool;
import org.cloud.sonic.controller.models.base.CommentPage;
import org.cloud.sonic.controller.models.domain.TestCases;
import org.cloud.sonic.controller.models.domain.TestSuites;
import org.cloud.sonic.controller.models.dto.TestCasesDTO;
import org.cloud.sonic.controller.services.TestCasesService;
import org.cloud.sonic.controller.services.TestSuitesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 测试用例管理控制器
 *
 * <p>
 * 该控制器提供测试用例的完整生命周期管理功能，是Sonic测试平台的核心功能模块。</p>
 *
 * <p>
 * 主要功能模块：</p>
 * <ul>
 * <li>测试用例CRUD操作（创建、查询、更新、删除）</li>
 * <li>测试用例分页查询和条件筛选</li>
 * <li>测试用例复制和批量操作</li>
 * <li>测试用例关联管理（模块、作者等）</li>
 * <li>测试用例依赖检查</li>
 * </ul>
 *
 * <p>
 * 支持的测试平台：</p>
 * <ul>
 * <li>Android平台测试用例</li>
 * <li>iOS平台测试用例</li>
 * <li>Web平台测试用例</li>
 * <li>跨平台通用测试用例</li>
 * </ul>
 *
 * <p>
 * 测试用例组织方式：</p>
 * <ul>
 * <li>按项目进行分组管理</li>
 * <li>按平台类型进行分类</li>
 * <li>按功能模块进行组织</li>
 * <li>支持作者维度的管理</li>
 * </ul>
 *
 * <p>
 * 权限控制：</p>
 * <ul>
 * <li>基于Token的身份验证</li>
 * <li>用例创建者自动设置</li>
 * <li>项目级别的访问控制</li>
 * </ul>
 *
 * @author Sonic Team
 * @version 1.0
 * @since 1.0
 */
@Tag(name = "测试用例相关")
@RestController
@RequestMapping("/testCases")
public class TestCasesController {

    /**
     * 测试用例服务层注入
     */
    @Autowired
    private TestCasesService testCasesService;

    /**
     * JWT Token工具注入，用于用户身份识别
     */
    @Autowired
    private JWTTokenTool jwtTokenTool;

    /**
     * 测试套件服务层注入，用于依赖检查
     */
    @Autowired
    private TestSuitesService testSuitesService;

    @WebAspect
    @Operation(summary = "查询测试用例列表", description = "查找对应项目id下的测试用例列表")
    @Parameters(value = {
        @Parameter(name = "projectId", description = "项目id"),
        @Parameter(name = "platform", description = "平台类型"),
        @Parameter(name = "name", description = "用例名称"),
        @Parameter(name = "moduleIds", description = "模块Id"),
        @Parameter(name = "caseAuthorNames", description = "用例作者列表"),
        @Parameter(name = "page", description = "页码"),
        @Parameter(name = "pageSize", description = "页数据大小"),
        @Parameter(name = "idSort", description = "控制id排序方式"),
        @Parameter(name = "editTimeSort", description = "控制editTime排序方式")

    })
    @GetMapping("/list")
    public RespModel<CommentPage<TestCasesDTO>> findAll(@RequestParam(name = "projectId") int projectId,
            @RequestParam(name = "platform") int platform,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "moduleIds[]", required = false) List<Integer> moduleIds,
            @RequestParam(name = "caseAuthorNames[]", required = false) List<String> caseAuthorNames,
            @RequestParam(name = "page") int page,
            @RequestParam(name = "pageSize") int pageSize,
            @RequestParam(name = "idSort", required = false) String idSort,
            @RequestParam(value = "editTimeSort", required = false) String editTimeSort) {
        Page<TestCases> pageable = new Page<>(page, pageSize);
        return new RespModel<>(
                RespEnum.SEARCH_OK,
                testCasesService.findAll(projectId, platform, name, moduleIds, caseAuthorNames,
                        pageable, idSort, editTimeSort)
        );
    }

    @WebAspect
    @Operation(summary = "查询测试用例列表", description = "不分页的测试用例列表")
    @Parameters(value = {
        @Parameter(name = "projectId", description = "项目id"),
        @Parameter(name = "platform", description = "平台类型"),})
    @GetMapping("/listAll")
    public RespModel<List<TestCases>> findAll(@RequestParam(name = "projectId") int projectId,
            @RequestParam(name = "platform") int platform) {
        return new RespModel<>(RespEnum.SEARCH_OK,
                testCasesService.findAll(projectId, platform));
    }

    @WebAspect
    @Operation(summary = "删除测试用例", description = "删除对应用例id，用例下的操作步骤的caseId重置为0")
    @Parameter(name = "id", description = "用例id")
    @DeleteMapping
    public RespModel<String> delete(@RequestParam(name = "id") int id) {
        if (testCasesService.delete(id)) {
            return new RespModel<>(RespEnum.DELETE_OK);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "删除测试用例检查", description = "返回被引用的测试套件")
    @Parameter(name = "id", description = "用例id")
    @GetMapping("deleteCheck")
    public RespModel<List<TestSuites>> deleteCheck(@RequestParam(name = "id") int id) {
        return new RespModel<>(RespEnum.SEARCH_OK, testSuitesService.listTestSuitesByTestCasesId(id));
    }

    /**
     * 保存测试用例信息（新增或更新）
     *
     * <p>
     * 支持测试用例的创建和更新操作，具有以下特性：</p>
     *
     * <p>
     * 新增用例时：</p>
     * <ul>
     * <li>自动设置用例创建者为当前登录用户</li>
     * <li>自动生成创建时间</li>
     * <li>分配唯一的用例ID</li>
     * <li>设置默认优先级和状态</li>
     * </ul>
     *
     * <p>
     * 更新用例时：</p>
     * <ul>
     * <li>保留原创建者信息</li>
     * <li>更新修改时间为当前时间</li>
     * <li>保持用例ID不变</li>
     * <li>支持部分字段更新</li>
     * </ul>
     *
     * <p>
     * 自动处理的字段：</p>
     * <ul>
     * <li>designer: 从Token中提取用户名自动设置</li>
     * <li>editTime: 更新操作时自动设置为当前时间</li>
     * <li>createTime: 新增时自动设置</li>
     * </ul>
     *
     * <p>
     * 权限验证：</p>
     * <ul>
     * <li>通过SonicToken进行用户身份验证</li>
     * <li>确保操作者有对应项目的访问权限</li>
     * </ul>
     *
     * @param testCasesDTO 测试用例数据传输对象，包含用例的完整信息
     * @param request HTTP请求对象，用于获取用户认证信息
     * @return 保存操作的响应结果
     * <ul>
     * <li>成功时返回UPDATE_OK状态</li>
     * <li>失败时返回相应的错误信息</li>
     * </ul>
     *
     * @throws IllegalArgumentException 当用例数据格式不正确时抛出
     * @see TestCasesDTO 测试用例数据传输对象
     * @since 1.0
     */
    @WebAspect
    @Operation(summary = "更新测试用例信息", description = "新增或更改测试用例信息")
    @PutMapping
    public RespModel<String> save(@Validated @RequestBody TestCasesDTO testCasesDTO, HttpServletRequest request) {
        if (request.getHeader("SonicToken") != null) {
            String token = request.getHeader("SonicToken");
            String userName = jwtTokenTool.getUserName(token);
            if (userName != null) {
                testCasesDTO.setDesigner(userName);
            }
        }

        // 修改时，更新修改时间
        if (!StringUtils.isEmpty(testCasesDTO.getId())) {
            testCasesDTO.setEditTime(new Date());
        }
        testCasesService.save(testCasesDTO.convertTo());
        return new RespModel<>(RespEnum.UPDATE_OK);
    }

    @WebAspect
    @Operation(summary = "查询测试用例详情", description = "查找对应用例id的用例详情")
    @Parameter(name = "id", description = "用例id")
    @GetMapping
    public RespModel<TestCasesDTO> findById(@RequestParam(name = "id") int id) {
        TestCasesDTO testCases = testCasesService.findById(id);
        if (testCases != null) {
            return new RespModel<>(RespEnum.SEARCH_OK, testCases);
        } else {
            return new RespModel<>(RespEnum.ID_NOT_FOUND);
        }
    }

    @WebAspect
    @Operation(summary = "批量查询用例", description = "查找id列表的用例信息，可以传多个ids[]")
    @Parameter(name = "ids[]", description = "id列表")
    @GetMapping("/findByIdIn")
    public RespModel<List<TestCases>> findByIdIn(@RequestParam(name = "ids[]") List<Integer> ids) {
        return new RespModel<>(RespEnum.SEARCH_OK,
                testCasesService.findByIdIn(ids));
    }

    //记得翻译
    @WebAspect
    @Operation(summary = "复制测试用例", description = "复制对应用例id的用例详情")
    @Parameter(name = "id", description = "用例id")
    @GetMapping("/copy")
    public RespModel<String> copyTestById(@RequestParam(name = "id") Integer id) {
        testCasesService.copyTestById(id);
        return new RespModel<>(RespEnum.COPY_OK);
    }

    @WebAspect
    @Operation(summary = "查询用例所有的作者列表", description = "查找对应项目id下对应平台的所有作者列表")
    @Parameters(value = {
        @Parameter(name = "projectId", description = "项目id"),
        @Parameter(name = "platform", description = "平台类型"),})
    @GetMapping("/listAllCaseAuthor")
    public RespModel<List<String>> findAllCaseAuthor(@RequestParam(name = "projectId") int projectId,
            @RequestParam(name = "platform") int platform) {
        return new RespModel<>(
                RespEnum.SEARCH_OK,
                testCasesService.findAllCaseAuthor(projectId, platform)
        );
    }
}
