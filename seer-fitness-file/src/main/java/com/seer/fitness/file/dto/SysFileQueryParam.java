package com.seer.fitness.file.dto;

import io.github.canjiemo.mycommon.pager.PagerParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysFileQueryParam extends PagerParam {
    private String bizType;
}
