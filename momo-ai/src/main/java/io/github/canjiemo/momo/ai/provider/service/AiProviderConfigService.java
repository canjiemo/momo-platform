package io.github.canjiemo.momo.ai.provider.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class AiProviderConfigService extends BaseServiceImpl implements IAiProviderConfigService {

    @Lazy
    @Autowired
    private AiProviderManager providerManager;

    public List<AiProviderConfig> list() {
        return lambdaQuery(AiProviderConfig.class)
                .orderByDesc(AiProviderConfig::getCreateTime)
                .list();
    }

    @Transactional
    public void create(AiProviderConfig config) {
        config.setIsActive(0);
        config.setDeleteFlag(0);
        baseDao.insertPO(config, true);
    }

    @Transactional
    public void update(AiProviderConfig request) {
        AiProviderConfig config = baseDao.queryById(request.getId(), AiProviderConfig.class);
        if (config == null) throw new BusinessException("配置不存在");
        if (request.getConfigName() != null) config.setConfigName(request.getConfigName());
        if (request.getChatModel()   != null) config.setChatModel(request.getChatModel());
        if (request.getEmbedModel()  != null) config.setEmbedModel(request.getEmbedModel());
        if (request.getBaseUrl()     != null) config.setBaseUrl(request.getBaseUrl());
        if (request.getApiKey()      != null) config.setApiKey(request.getApiKey());
        if (request.getConfig()      != null) config.setConfig(request.getConfig());
        if (request.getRemark()      != null) config.setRemark(request.getRemark());
        // isActive 不允许通过 update 接口修改，必须通过 activate 接口切换，以确保同时只有一个激活配置
        config.setUpdateTime(null);
        baseDao.updatePO(config);
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            providerManager.invalidate();
        }
    }

    @Transactional
    public void activate(Long id) {
        AiProviderConfig target = baseDao.queryById(id, AiProviderConfig.class);
        if (target == null) throw new BusinessException("配置不存在");
        List<AiProviderConfig> active = lambdaQuery(AiProviderConfig.class)
                .eq(AiProviderConfig::getIsActive, 1).list();
        for (AiProviderConfig c : active) {
            c.setIsActive(0);
            c.setUpdateTime(null);
            baseDao.updatePO(c);
        }
        target.setIsActive(1);
        target.setUpdateTime(null);
        baseDao.updatePO(target);
        providerManager.invalidate();
        log.info("AI Provider 已激活: id={}, provider={}", id, target.getProvider());
    }

    @Transactional
    public void delete(Long id) {
        AiProviderConfig config = baseDao.queryById(id, AiProviderConfig.class);
        if (config == null) throw new BusinessException("配置不存在");
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            throw new BusinessException("当前激活的配置不允许删除");
        }
        baseDao.delByIds(AiProviderConfig.class, String.valueOf(id));
    }
}
