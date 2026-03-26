package com.xindai.xindai.modules.file.service;

import com.xindai.xindai.modules.file.dto.FileUploadVO;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    FileUploadVO upload(MultipartFile file, String bizType);
    void delete(String fileUrl);
    Resource load(String fileUrl);
}
