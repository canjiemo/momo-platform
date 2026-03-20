package io.github.canjiemo.momo.system.utils;

import io.github.canjiemo.momo.system.service.DictDataService;
import io.github.canjiemo.tools.dict.IMyDict;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 字典工具类
 * 提供字典值到标签的转换功能
 *
 * @author canjiemo@gmail.com
 */
@Component
public class DictUtil implements IMyDict {

    private static DictDataService dictDataService;

    @Autowired
    public void setDictDataService(DictDataService dictDataService) {
        DictUtil.dictDataService = dictDataService;
    }

    /**
     * 根据字典类型和值获取字典标签
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签，如果未找到返回原值
     */
    public static String getLabel(String dictType, Object dictValue) {
        if (!StringUtils.hasText(dictType) || dictValue == null) {
            return dictValue == null ? "" : String.valueOf(dictValue);
        }

        try {
            String label = dictDataService.getDesc(dictType, dictValue);
            return StringUtils.hasText(label) ? label : String.valueOf(dictValue);
        } catch (Exception e) {
            // 如果获取字典标签失败，返回原值
            return String.valueOf(dictValue);
        }
    }

    @Override
    public String getDesc(String s, String o) {
        return getLabel(s,o);
    }
}