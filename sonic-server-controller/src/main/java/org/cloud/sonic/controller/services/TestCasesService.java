package org.cloud.sonic.controller.services;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.cloud.sonic.controller.models.base.CommentPage;
import org.cloud.sonic.controller.models.domain.TestCases;
import org.cloud.sonic.controller.models.dto.TestCasesDTO;

import java.util.List;

/**
 * 测试用例业务逻辑服务接口
 *
 * <p>
 * 该接口定义了测试用例管理的核心业务逻辑，提供测试用例的完整生命周期管理。</p>
 *
 * <p>
 * 用例管理功能：</p>
 * <ul>
 * <li>测试用例的创建、更新、删除</li>
 * <li>用例版本控制和历史追踪</li>
 * <li>用例模板管理和复用</li>
 * <li>用例分类和标签管理</li>
 * </ul>
 *
 * <p>
 * 用例查询功能：</p>
 * <ul>
 * <li>多条件组合查询（项目、平台、模块、作者）</li>
 * <li>分页查询和排序</li>
 * <li>用例依赖关系分析</li>
 * <li>用例使用统计</li>
 * </ul>
 *
 * <p>
 * 用例组织功能：</p>
 * <ul>
 * <li>用例模块化管理</li>
 * <li>用例优先级设定</li>
 * <li>用例执行策略配置</li>
 * <li>用例关联步骤管理</li>
 * </ul>
 *
 * <p>
 * 用例操作功能：</p>
 * <ul>
 * <li>用例批量操作</li>
 * <li>用例复制和导入导出</li>
 * <li>用例执行结果关联</li>
 * <li>用例质量分析</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/20
 * @see TestCases 测试用例实体类
 * @see TestCasesDTO 测试用例数据传输对象
 */
public interface TestCasesService extends IService<TestCases> {

    CommentPage<TestCasesDTO> findAll(int projectId, int platform, String name, List<Integer> moduleIds,
            List<String> caseAuthorNames, Page<TestCases> pageable,
            String idSort, String editTimeSort);

    List<TestCases> findAll(int projectId, int platform);

    boolean delete(int id);

    TestCasesDTO findById(int id);

    JSONObject findSteps(int id);

    List<TestCases> findByIdIn(List<Integer> ids);

    boolean deleteByProjectId(int projectId);

    List<TestCases> listByPublicStepsId(int publicStepsId);

    /**
     * 复制测试用例
     *
     * @param id 测试用例id （test_cases，步骤表 steps case_id字段）
     * @return
     */
    boolean copyTestById(int id);

    Boolean updateTestCaseModuleByModuleId(Integer module);

    /**
     * 查询指定项目，指定平台下，所有的用例作者列表集合
     *
     * @param projectId 项目id
     * @param platform 平台
     * @return 用例作者列表集合
     */
    List<String> findAllCaseAuthor(int projectId, int platform);
}
