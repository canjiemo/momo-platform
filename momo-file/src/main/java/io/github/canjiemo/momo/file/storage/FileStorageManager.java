package io.github.canjiemo.momo.file.storage;

import io.github.canjiemo.momo.file.entity.SysFileConfig;
import io.github.canjiemo.momo.file.service.ISysFileConfigService;
import io.github.canjiemo.mycommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileStorageManager {

    /** key = storageType（"local"/"minio"），Spring 自动注入所有 IFileStorageAdapter 实现 */
    private final Map<String, IFileStorageAdapter> adapterMap;

    @Autowired
    private ISysFileConfigService fileConfigService;

    /** volatile 保证多线程可见；null 表示待初始化 */
    private volatile IFileStorageAdapter activeAdapter;

    public FileStorageManager(List<IFileStorageAdapter> adapters) {
        this.adapterMap = adapters.stream()
                .collect(Collectors.toMap(IFileStorageAdapter::getType, Function.identity()));
        log.info("已注册文件存储适配器: {}", adapterMap.keySet());
    }

    /**
     * 获取当前激活的适配器（懒加载 + 双重检查锁）
     */
    public IFileStorageAdapter getActive() {
        if (activeAdapter == null) {
            synchronized (this) {
                if (activeAdapter == null) {
                    refresh();
                }
            }
        }
        return activeAdapter;
    }

    /**
     * 从数据库重新加载激活配置并初始化适配器。
     * 管理员切换激活配置后调用此方法生效。
     */
    public void refresh() {
        SysFileConfig config = fileConfigService.getActiveConfig();
        if (config == null) {
            throw new BusinessException("未配置文件存储，请先在平台配置中激活一个存储方案");
        }
        IFileStorageAdapter adapter = adapterMap.get(config.getStorageType());
        if (adapter == null) {
            throw new BusinessException("不支持的存储类型: " + config.getStorageType()
                    + "，已注册类型: " + adapterMap.keySet());
        }
        adapter.init(config.getConfig());
        this.activeAdapter = adapter;
        log.info("文件存储已切换为: type={}, configName={}", config.getStorageType(), config.getConfigName());
    }

    /**
     * 使当前缓存的适配器失效，下次 getActive() 时重新从 DB 加载。
     */
    public void invalidate() {
        this.activeAdapter = null;
        log.info("文件存储适配器缓存已失效，下次调用将重新初始化");
    }
}
