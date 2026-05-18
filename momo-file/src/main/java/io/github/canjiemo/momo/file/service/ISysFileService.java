package io.github.canjiemo.momo.file.service;

import io.github.canjiemo.base.myjdbc.service.IBaseService;
import io.github.canjiemo.momo.file.dto.SysFileDTO;
import io.github.canjiemo.mycommon.pager.Pager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ISysFileService extends IBaseService {
    SysFileDTO upload(MultipartFile file, String bizType) throws Exception;
    SysFileDTO getById(Long id);
    void delete(Long id) throws Exception;
    int batchDelete(List<Long> ids) throws Exception;
    Pager<SysFileDTO> search(String bizType, Pager<SysFileDTO> pager);
}
