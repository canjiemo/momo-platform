package com.seer.fitness.file.controller;

import com.seer.fitness.file.dto.SysFileDTO;
import com.seer.fitness.file.service.ISysFileService;
import com.seer.fitness.file.storage.FileStorageManager;
import com.seer.fitness.file.storage.adapter.LocalStorageAdapter;
import com.seer.fitness.framework.annotation.RequireAuth;
import io.github.canjiemo.base.mymvc.controller.MyBaseController;
import io.github.canjiemo.base.mymvc.data.MyResponseResult;
import io.github.canjiemo.mycommon.pager.Pager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RestController
@RequestMapping("/system/file")
public class FileController extends MyBaseController {

    @Autowired
    private ISysFileService fileService;

    @Autowired
    private FileStorageManager fileStorageManager;

    @PostMapping("/upload")
    @RequireAuth(login = true)
    public MyResponseResult<SysFileDTO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", defaultValue = "common") String bizType,
            @RequestParam(value = "bizId", required = false) String bizId) throws Exception {
        return doJsonOut(fileService.upload(file, bizType, bizId));
    }

    @GetMapping("/{id}")
    @RequireAuth(login = true)
    public MyResponseResult<SysFileDTO> getById(@PathVariable Long id) {
        return doJsonOut(fileService.getById(id));
    }

    @PostMapping("/delete/{id}")
    @RequireAuth(permissions = {"file:delete"})
    public MyResponseResult delete(@PathVariable Long id) throws Exception {
        fileService.delete(id);
        return doJsonDefaultMsg();
    }

    @PostMapping("/search")
    @RequireAuth(login = true)
    public MyResponseResult<Pager<SysFileDTO>> search(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) String bizId,
            Pager<SysFileDTO> pager) {
        return doJsonOut(fileService.search(bizType, bizId, pager));
    }

    /**
     * 本地存储文件服务（仅本地适配器激活时有意义）
     * 路径: /system/file/local/{directory}/{filename}
     */
    @GetMapping("/local/**")
    @RequireAuth(login = false)
    public void serveLocalFile(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String fileKey = new AntPathMatcher().extractPathWithinPattern(pattern, request.getServletPath());

        if (!(fileStorageManager.getActive() instanceof LocalStorageAdapter localAdapter)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "当前非本地存储模式");
            return;
        }

        Path filePath = localAdapter.resolvePath(fileKey);
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String contentType = Files.probeContentType(filePath);
        response.setContentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(Files.size(filePath));

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }
}
