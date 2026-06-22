package io.github.canjiemo.momo.ai.provider.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.ai.config.AiSecretCipher;
import io.github.canjiemo.momo.ai.provider.AiProviderManager;
import io.github.canjiemo.momo.ai.provider.entity.AiProviderConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Autowired
    private AiSecretCipher secretCipher;

    /** 激活 Provider 前是否做连通性校验（默认开启），失败则取消切换 */
    @Value("${momo.ai.activate.validate:true}")
    private boolean validateOnActivate;

    public List<AiProviderConfig> list() {
        List<AiProviderConfig> configs = lambdaQuery(AiProviderConfig.class)
                .orderByDesc(AiProviderConfig::getCreateTime)
                .list();
        // 响应脱敏：API Key 仅回显末 4 位，避免明文外泄
        configs.forEach(c -> c.setApiKey(secretCipher.mask(c.getApiKey())));
        return configs;
    }

    @Transactional
    public void create(AiProviderConfig config) {
        config.setIsActive(0);
        config.setDeleteFlag(0);
        config.setApiKey(secretCipher.encrypt(config.getApiKey()));
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
        // 仅当传入了新的、非脱敏占位的 Key 时才更新（前端回显的是脱敏值，原样提交则保留旧 Key）
        String reqKey = request.getApiKey();
        if (reqKey != null && !reqKey.isBlank() && !secretCipher.isMasked(reqKey)) {
            config.setApiKey(secretCipher.encrypt(reqKey));
        }
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

        if (validateOnActivate) {
            try {
                // 触发按新配置重新加载，并做一次最小连通性探测（读取的是本事务内未提交的 is_active=1）
                providerManager.getActiveEmbed().embed("connectivity check");
            } catch (Exception e) {
                // 校验失败：清除半切换的缓存，抛异常触发事务回滚，保持旧配置
                providerManager.invalidate();
                throw new BusinessException("目标 Provider 连通性校验失败，已取消切换：" + e.getMessage());
            }
        }
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
