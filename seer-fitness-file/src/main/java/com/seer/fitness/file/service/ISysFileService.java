package com.seer.fitness.file.service;

import com.seer.fitness.file.dto.SysFileDTO;
import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.mycommon.pager.Pager;
import org.springframework.web.multipart.MultipartFile;

public interface ISysFileService extends IBaseService {
    SysFileDTO upload(MultipartFile file, String bizType, String bizId) throws Exception;
    SysFileDTO getById(Long id);
    void delete(Long id) throws Exception;
    Pager<SysFileDTO> search(String bizType, String bizId, Pager<SysFileDTO> pager);
}
