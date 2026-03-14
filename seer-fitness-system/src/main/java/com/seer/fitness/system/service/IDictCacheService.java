package com.seer.fitness.system.service;

import com.seer.fitness.system.dto.DictDataDTO;
import com.seer.fitness.system.dto.DictTypeDTO;

import java.util.List;

public interface IDictCacheService {

    List<DictTypeDTO> getAllDictTypesFromCache();

    void cacheAllDictTypes(List<DictTypeDTO> dictTypes);

    DictTypeDTO getDictTypeFromCache(String dictType);

    void cacheDictType(DictTypeDTO dictTypeDTO);

    void deleteDictTypeCache(String dictType);

    List<DictDataDTO> getDictDataFromCache(String dictType);

    void cacheDictData(String dictType, List<DictDataDTO> dictDataList);

    void deleteDictDataCache(String dictType);

    String getDictLabelByValue(String dictType, String dictValue);

    void clearAllDictCache();
}
