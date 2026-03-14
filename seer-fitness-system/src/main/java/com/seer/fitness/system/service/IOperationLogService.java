package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.OperationLogDTO;
import com.seer.fitness.system.dto.OperationLogQueryParam;
import com.seer.fitness.system.entity.SysOperationLog;
import io.github.canjiemo.mycommon.pager.Pager;

import java.util.List;
import java.util.Map;

/**
 * 操作日志服务接口
 * 提供操作日志的查询、统计、清理等功能
 *
 * @author seer-fitness
 */
public interface IOperationLogService {

    /**
     * 保存操作日志
     * 用于AOP切面记录操作日志
     *
     * @param operationLog 操作日志实体
     */
    void save(SysOperationLog operationLog);

    /**
     * 分页查询操作日志
     * 支持复杂查询条件、分页、排序
     *
     * @param param 查询参数
     * @param pager 分页参数
     * @return 分页结果
     */
    Pager<OperationLogDTO> search(OperationLogQueryParam param, Pager<OperationLogDTO> pager);

    /**
     * 根据ID获取操作日志详情
     *
     * @param id 日志ID
     * @return 操作日志详情
     */
    OperationLogDTO getById(Long id);

    /**
     * 批量删除操作日志
     * 物理删除，谨慎使用
     *
     * @param ids 日志ID数组
     */
    void delete(Long[] ids);

    /**
     * 清理历史日志
     * 删除指定天数之前的日志记录
     *
     * @param days 保留天数，超过该天数的日志将被删除
     * @return 删除的记录数
     */
    int cleanHistoryLogs(int days);

    /**
     * 统计操作类型分布
     * 用于展示各种操作类型的使用频率
     *
     * @param days 统计最近几天的数据，0表示统计全部
     * @return 操作类型统计结果 Map<操作类型, 次数>
     */
    Map<String, Long> getOperationTypeStats(int days);

    /**
     * 统计模块操作分布
     * 用于展示各个模块的操作频率
     *
     * @param days 统计最近几天的数据，0表示统计全部
     * @return 模块操作统计结果 Map<模块名, 次数>
     */
    Map<String, Long> getModuleStats(int days);

    /**
     * 统计用户操作活跃度
     * 用于展示用户的操作活跃程度
     *
     * @param days 统计最近几天的数据
     * @param limit 返回结果数量限制
     * @return 用户操作统计结果 List<Map<用户信息, 操作次数>>
     */
    List<Map<String, Object>> getUserActivityStats(int days, int limit);

    /**
     * 统计操作失败情况
     * 用于监控系统异常情况
     *
     * @param days 统计最近几天的数据
     * @return 失败操作统计结果
     */
    Map<String, Object> getFailureStats(int days);

    /**
     * 获取操作趋势数据
     * 用于展示操作量的时间趋势
     *
     * @param days 统计最近几天的数据
     * @return 操作趋势数据 List<Map<日期, 操作次数>>
     */
    List<Map<String, Object>> getOperationTrend(int days);

    /**
     * 导出操作日志
     * 根据查询条件导出日志数据
     *
     * @param param 查询参数
     * @return 导出的日志列表
     */
    List<OperationLogDTO> exportLogs(OperationLogQueryParam param);
}