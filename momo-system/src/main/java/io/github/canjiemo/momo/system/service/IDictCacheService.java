package io.github.canjiemo.momo.system.service;

import io.github.canjiemo.momo.system.dto.DictDataDTO;
import io.github.canjiemo.momo.system.dto.DictTypeDTO;

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
