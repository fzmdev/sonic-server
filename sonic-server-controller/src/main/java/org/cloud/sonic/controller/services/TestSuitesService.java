package org.cloud.sonic.controller.services;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.cloud.sonic.common.http.RespModel;
import org.cloud.sonic.controller.models.base.CommentPage;
import org.cloud.sonic.controller.models.domain.TestSuites;
import org.cloud.sonic.controller.models.dto.StepsDTO;
import org.cloud.sonic.controller.models.dto.TestSuitesDTO;

import java.util.List;

/**
 * 测试套件业务逻辑服务接口
 *
 * <p>
 * 该接口定义了测试套件管理的核心业务逻辑，提供测试套件的完整执行和管理功能。</p>
 *
 * <p>
 * 套件管理功能：</p>
 * <ul>
 * <li>测试套件的创建、配置、更新、删除</li>
 * <li>套件用例组织和关联管理</li>
 * <li>套件设备分配和调度</li>
 * <li>套件执行策略配置</li>
 * </ul>
 *
 * <p>
 * 套件执行控制：</p>
 * <ul>
 * <li>套件启动、暂停、停止控制</li>
 * <li>并发执行管理</li>
 * <li>执行进度监控</li>
 * <li>异常处理和恢复</li>
 * </ul>
 *
 * <p>
 * 结果收集分析：</p>
 * <ul>
 * <li>执行结果实时收集</li>
 * <li>测试报告生成</li>
 * <li>统计数据分析</li>
 * <li>趋势分析和预测</li>
 * </ul>
 *
 * @author ZhouYiXun
 * @version 1.0
 * @since 2021/8/20
 * @see TestSuites 测试套件实体类
 */
public interface TestSuitesService extends IService<TestSuites> {

    RespModel<Integer> runSuite(int id, String strike);

    RespModel<String> forceStopSuite(int id, String strike);

    TestSuitesDTO findById(int id);

    JSONObject getStep(StepsDTO steps);

    boolean delete(int id);

    void saveTestSuites(TestSuitesDTO testSuitesDTO);

    CommentPage<TestSuitesDTO> findByProjectId(int projectId, String name, Page<TestSuites> pageable);

    List<TestSuitesDTO> findByProjectId(int projectId);

    boolean deleteByProjectId(int projectId);

    List<TestSuites> listTestSuitesByTestCasesId(int testCasesId);
}
