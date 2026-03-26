package com.xindai.xindai.modules.file.controller;

import com.xindai.xindai.common.result.Result;
import com.xindai.xindai.modules.file.dto.FileUploadVO;
import com.xindai.xindai.modules.file.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/file")
@Tag(name = "文件管理")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    @Operation(summary = "上传文件")
    public Result<FileUploadVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String bizType) {
        return Result.success(fileStorageService.upload(file, bizType));
    }
}
