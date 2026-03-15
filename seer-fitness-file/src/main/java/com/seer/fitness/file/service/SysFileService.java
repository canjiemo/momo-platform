package com.seer.fitness.file.service;

import com.seer.fitness.file.dto.SysFileDTO;
import com.seer.fitness.file.entity.SysFile;
import com.seer.fitness.file.storage.FileStorageManager;
import com.seer.fitness.file.storage.IFileStorageAdapter;
import com.seer.fitness.file.storage.model.FileUploadResult;
import com.seer.fitness.framework.dto.UserCacheInfo;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import com.seer.fitness.system.constants.ConfigKeys;
import com.seer.fitness.system.utils.ConfigUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class SysFileService extends BaseServiceImpl implements ISysFileService {

    @Autowired
    private FileStorageManager fileStorageManager;

    private void validateFile(MultipartFile file) {
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        long size = file.getSize();
        if (contentType.startsWith("image/")) {
            long max = ConfigUtil.getInt(ConfigKeys.FILE_IMAGE_MAX_MB, 10) * 1024 * 1024L;
            if (size > max) throw new BusinessException("图片文件不能超过 " + ConfigUtil.getInt(ConfigKeys.FILE_IMAGE_MAX_MB, 10) + "MB");
        } else if (contentType.startsWith("video/")) {
            long max = ConfigUtil.getInt(ConfigKeys.FILE_VIDEO_MAX_MB, 500) * 1024 * 1024L;
            if (size > max) throw new BusinessException("视频文件不能超过 " + ConfigUtil.getInt(ConfigKeys.FILE_VIDEO_MAX_MB, 500) + "MB");
        } else {
            long max = ConfigUtil.getInt(ConfigKeys.FILE_OTHER_MAX_MB, 100) * 1024 * 1024L;
            if (size > max) throw new BusinessException("文件不能超过 " + ConfigUtil.getInt(ConfigKeys.FILE_OTHER_MAX_MB, 100) + "MB");
        }
    }

    @Override
    @Transactional
    public SysFileDTO upload(MultipartFile file, String bizType, String bizId) throws Exception {
        validateFile(file);
        IFileStorageAdapter adapter = fileStorageManager.getActive();
        UserCacheInfo user = SecurityContextUtil.getCurrentUser();
        String tenantSegment = (user != null && user.getTenantId() != null)
                ? String.valueOf(user.getTenantId()) : "platform";
        String typeSegment = StringUtils.hasText(bizType) ? bizType : "common";
        String dateSegment = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String directory = tenantSegment + "/" + typeSegment + "/" + dateSegment;
        FileUploadResult result = adapter.upload(file, directory);

        SysFile sysFile = new SysFile();
        sysFile.setFileName(result.getOriginalName());
        sysFile.setFileKey(result.getFileKey());
        sysFile.setFileUrl(result.getAccessUrl());
        sysFile.setFileSize(result.getFileSize());
        sysFile.setContentType(result.getContentType());
        sysFile.setStorageType(adapter.getType());
        sysFile.setBizType(bizType);
        sysFile.setBizId(bizId);
        sysFile.setDeleteFlag(0);
        baseDao.insertPO(sysFile, true);

        SysFileDTO dto = new SysFileDTO();
        BeanUtils.copyProperties(sysFile, dto);
        return dto;
    }

    @Override
    public SysFileDTO getById(Long id) {
        SysFile file = baseDao.queryById(id, SysFile.class);
        if (file == null) throw new BusinessException("文件不存在");
        SysFileDTO dto = new SysFileDTO();
        BeanUtils.copyProperties(file, dto);
        return dto;
    }

    @Override
    @Transactional
    public void delete(Long id) throws Exception {
        SysFile file = baseDao.queryById(id, SysFile.class);
        if (file == null) throw new BusinessException("文件不存在");
        IFileStorageAdapter adapter = fileStorageManager.getActive();
        adapter.delete(file.getFileKey());
        baseDao.delByIds(SysFile.class, String.valueOf(id));
        log.info("文件已删除: id={}, key={}", id, file.getFileKey());
    }

    @Override
    public Pager<SysFileDTO> search(String bizType, String bizId, Pager<SysFileDTO> pager) {
        return lambdaQuery(SysFile.class, SysFileDTO.class)
                .eq(SysFile::getBizType, bizType)
                .eq(SysFile::getBizId, bizId)
                .orderByDesc(SysFile::getCreateTime)
                .page(pager);
    }
}
