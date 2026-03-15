package com.seer.fitness.file.service;

import com.seer.fitness.file.dto.SysFileDTO;
import com.seer.fitness.file.entity.SysFile;
import com.seer.fitness.file.storage.FileStorageManager;
import com.seer.fitness.file.storage.IFileStorageAdapter;
import com.seer.fitness.file.storage.model.FileUploadResult;
import com.seer.fitness.framework.utils.SecurityContextUtil;
import io.github.canjiemo.base.myjdbc.service.impl.BaseServiceImpl;
import io.github.canjiemo.mycommon.exception.BusinessException;
import io.github.canjiemo.mycommon.pager.Pager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Slf4j
@Service
public class SysFileService extends BaseServiceImpl implements ISysFileService {

    @Autowired
    private FileStorageManager fileStorageManager;

    @Override
    @Transactional
    public SysFileDTO upload(MultipartFile file, String bizType, String bizId) throws Exception {
        IFileStorageAdapter adapter = fileStorageManager.getActive();
        String directory = bizType != null ? bizType : "common";
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
        sysFile.setCreateBy(SecurityContextUtil.getCurrentUsername());
        sysFile.setCreateTime(LocalDateTime.now());
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
