package com.seer.fitness.system.constants;

/**
 * 字典常量
 *
 * @author seer-fitness
 */
public class DictConstants {

    /**
     * Redis缓存Key前缀
     */
    public static final String CACHE_KEY_PREFIX = "dict:";

    /**
     * 字典类型缓存Key前缀
     */
    public static final String DICT_TYPE_CACHE_KEY = CACHE_KEY_PREFIX + "type:";

    /**
     * 字典数据缓存Key前缀
     */
    public static final String DICT_DATA_CACHE_KEY = CACHE_KEY_PREFIX + "data:";

    /**
     * 所有字典类型缓存Key
     */
    public static final String ALL_DICT_TYPES_CACHE_KEY = DICT_TYPE_CACHE_KEY + "all";

    /**
     * 默认排序
     */
    public static final int DEFAULT_SORT_ORDER = 0;
}