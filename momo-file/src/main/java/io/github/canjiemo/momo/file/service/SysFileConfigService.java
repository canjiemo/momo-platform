package io.github.canjiemo.momo.file.service;

import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.momo.file.dto.SysFileConfigCreateRequest;
import io.github.canjiemo.momo.file.dto.SysFileConfigDTO;
import io.github.canjiemo.momo.file.dto.SysFileConfigUpdateRequest;
import io.github.canjiemo.momo.file.entity.SysFileConfig;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class SysFileConfigService extends BaseServiceImpl implements ISysFileConfigService {

    @Override
    public List<SysFileConfigDTO> list() {
        return lambdaQuery(SysFileConfig.class, SysFileConfigDTO.class)
                .orderByDesc(SysFileConfig::getCreateTime)
                .list();
    }

    @Override
    public SysFileConfig getActiveConfig() {
        return lambdaQuery(SysFileConfig.class)
                .eq(SysFileConfig::getIsActive, 1)
                .one();
    }

    @Override
    @Transactional
    public void create(SysFileConfigCreateRequest request) {
        SysFileConfig config = new SysFileConfig();
        config.setConfigName(request.getConfigName());
        config.setStorageType(request.getStorageType());
        config.setIsActive(0);
        config.setConfig(request.getConfig());
        config.setRemark(request.getRemark());
        config.setDeleteFlag(0);
        baseDao.insertPO(config, true);
    }

    @Override
    @Transactional
    public boolean update(SysFileConfigUpdateRequest request) {
        SysFileConfig config = baseDao.queryById(request.getId(), SysFileConfig.class);
        if (config == null) throw new BusinessException("配置不存在");
        if (request.getConfigName() != null) config.setConfigName(request.getConfigName());
        if (request.getConfig()     != null) config.setConfig(request.getConfig());
        if (request.getRemark()     != null) config.setRemark(request.getRemark());
        baseDao.updatePO(config);
        // 返回是否影响激活配置，由调用方决定是否刷新缓存
        return config.getIsActive() != null && config.getIsActive() == 1;
    }

    @Override
    @Transactional
    public void activate(Long id) {
        SysFileConfig target = baseDao.queryById(id, SysFileConfig.class);
        if (target == null) throw new BusinessException("配置不存在");

        // 先将全部激活的置为未激活
        List<SysFileConfig> all = lambdaQuery(SysFileConfig.class).eq(SysFileConfig::getIsActive, 1).list();
        for (SysFileConfig c : all) {
            c.setIsActive(0);
            baseDao.updatePO(c);
        }

        // 激活目标
        target.setIsActive(1);
        baseDao.updatePO(target);
        log.info("文件存储配置已激活: id={}, type={}", id, target.getStorageType());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SysFileConfig config = baseDao.queryById(id, SysFileConfig.class);
        if (config == null) throw new BusinessException("配置不存在");
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            throw new BusinessException("当前激活的配置不允许删除，请先激活其他配置");
        }
        baseDao.delByIds(SysFileConfig.class, String.valueOf(id));
    }
}
