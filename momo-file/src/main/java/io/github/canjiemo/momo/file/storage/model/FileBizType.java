package io.github.canjiemo.momo.file.storage.model;

import java.util.Set;

/**
 * 文件业务类型常量。
 * 新增类型在此处添加常量并加入 ALL 集合即可，接口层会自动校验。
 */
public final class FileBizType {

    public static final String COMMON   = "common";    // 通用
    public static final String AVATAR   = "avatar";    // 头像
    public static final String DOCUMENT = "document";  // 文档

    public static final Set<String> ALL = Set.of(COMMON, AVATAR, DOCUMENT);

    private FileBizType() {}
}
