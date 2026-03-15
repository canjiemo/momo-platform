package com.seer.fitness.system.service;

import com.seer.fitness.framework.utils.RedisUtil;
import com.seer.fitness.system.constants.ConfigKeys;
import com.seer.fitness.system.dto.SysConfigCreateRequest;
import com.seer.fitness.system.dto.SysConfigDTO;
import com.seer.fitness.system.dto.SysConfigQueryParam;
import com.seer.fitness.system.dto.SysConfigUpdateRequest;
import com.seer.fitness.system.entity.SysConfig;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class SysConfigService extends BaseServiceImpl implements ISysConfigService {

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Pager<SysConfigDTO> search(SysConfigQueryParam param, Pager<SysConfigDTO> pager) {
        return lambdaQuery(SysConfig.class, SysConfigDTO.class)
                .like(SysConfig::getConfigKey, param.getConfigKey())
                .like(SysConfig::getConfigName, param.getConfigName())
                .eq(SysConfig::getConfigType, param.getConfigType())
                .isNull(SysConfig::getTenantId)
                .orderByAsc(SysConfig::getConfigKey)
                .page(pager);
    }

    @Override
    public SysConfigDTO getByKey(String configKey) {
        SysConfigDTO dto = lambdaQuery(SysConfig.class, SysConfigDTO.class)
                .eq(SysConfig::getConfigKey, configKey)
                .isNull(SysConfig::getTenantId)
                .one();
        if (dto == null) throw new BusinessException("配置项不存在: " + configKey);
        return dto;
    }

    @Override
    public String getValue(String configKey) {
        // 1. 读 Redis
        String cacheKey = ConfigKeys.CACHE_PREFIX + configKey;
        String cached = redisUtil.get(cacheKey, String.class);
        if (cached != null) return cached;

        // 2. 查 DB
        SysConfig config = lambdaQuery(SysConfig.class)
                .eq(SysConfig::getConfigKey, configKey)
                .isNull(SysConfig::getTenantId)
                .one();
        if (config == null || config.getConfigValue() == null) return null;

        // 3. 写入 Redis（无 TTL）
        redisUtil.set(cacheKey, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    @Transactional(readOnly = false)
    public void create(SysConfigCreateRequest request) {
        boolean exists = lambdaQuery(SysConfig.class)
                .eq(SysConfig::getConfigKey, request.getConfigKey())
                .isNull(SysConfig::getTenantId)
                .exists();
        if (exists) throw new BusinessException("配置键已存在: " + request.getConfigKey());

        SysConfig config = new SysConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setConfigName(request.getConfigName());
        config.setConfigType(2);
        config.setRemark(request.getRemark());
        config.setDeleteFlag(0);

        baseDao.insertPO(config, true);
        log.info("创建配置项: key={}", request.getConfigKey());
    }

    @Override
    @Transactional(readOnly = false)
    public void update(SysConfigUpdateRequest request) {
        SysConfig config = baseDao.queryById(request.getId(), SysConfig.class);
        if (config == null) throw new BusinessException("配置项不存在");

        if (request.getConfigValue() != null) config.setConfigValue(request.getConfigValue());
        if (request.getConfigName() != null) config.setConfigName(request.getConfigName());
        if (request.getRemark() != null) config.setRemark(request.getRemark());

        baseDao.updatePO(config);
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + config.getConfigKey());
        log.info("更新配置项: key={}, value={}", config.getConfigKey(), config.getConfigValue());
    }

    @Override
    @Transactional(readOnly = false)
    public void delete(Long id) {
        SysConfig config = baseDao.queryById(id, SysConfig.class);
        if (config == null) throw new BusinessException("配置项不存在");
        if (config.getConfigType() == 1) throw new BusinessException("系统内置配置不允许删除");

        baseDao.delByIds(SysConfig.class, String.valueOf(id));
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + config.getConfigKey());
        log.info("删除配置项: key={}", config.getConfigKey());
    }

    @Override
    public void refreshCache() {
        redisUtil.deleteByPattern(ConfigKeys.CACHE_PREFIX + "*");
        List<SysConfig> all = lambdaQuery(SysConfig.class).isNull(SysConfig::getTenantId).list();
        for (SysConfig c : all) {
            if (c.getConfigValue() != null) {
                redisUtil.set(ConfigKeys.CACHE_PREFIX + c.getConfigKey(), c.getConfigValue());
            }
        }
        log.info("刷新配置缓存完成，共 {} 项", all.size());
    }

    @Override
    public void refreshCache(String configKey) {
        redisUtil.delete(ConfigKeys.CACHE_PREFIX + configKey);
        getValue(configKey);
        log.info("刷新配置缓存: key={}", configKey);
    }
}
