package com.seer.fitness.ai.provider.entity;

import io.github.canjiemo.base.myjdbc.MyTableEntity;
import io.github.canjiemo.base.myjdbc.annotation.AuditFill;
import io.github.canjiemo.base.myjdbc.annotation.MyField;
import io.github.canjiemo.base.myjdbc.annotation.MyTable;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MyTable("ai_provider_config")
public class AiProviderConfig implements MyTableEntity {
    private Long id;
    private String configName;
    private String provider;
    private String chatModel;
    private String embedModel;
    private String baseUrl;
    private String apiKey;
    private Integer isActive;
    private String config;
    private String remark;
    @MyField(fill = AuditFill.CREATE_TIME)
    private LocalDateTime createTime;
    @MyField(fill = AuditFill.UPDATE_TIME)
    private LocalDateTime updateTime;
    private Integer deleteFlag;
}
