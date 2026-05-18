package io.github.canjiemo.momo.system.dto;

import io.github.canjiemo.tools.dict.MyDict;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户响应DTO
 * 返回给前端的租户数据
 *
 * @author canjiemo@gmail.com
 */
@Data
public class TenantDTO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称（学校名称）
     */
    private String tenantName;

    /**
     * 管理员真实姓名
     */
    private String realName;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 联系邮箱
     */
    private String contactEmail;

    /**
     * 学校地址
     */
    private String address;

    /**
     * 租户描述
     */
    private String description;

    /**
     * 状态码：0-待激活 1-正常 2-已禁用 3-已过期
     */
    @MyDict(type = "tenant_status")
    private Integer status;

    /**
     * 激活时间
     */
    private LocalDateTime activatedAt;

    /**
     * 过期时间
     */
    private LocalDateTime expiredAt;

    /**
     * 最大用户数限制
     */
    private Integer maxUsers;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 创建人ID
     */
    private Long createBy;

    /**
     * 更新人ID
     */
    private Long updateBy;
}
