package com.xindai.xindai.modules.file.service.impl;

import com.xindai.xindai.common.config.StorageProperties;
import com.xindai.xindai.modules.file.dto.FileUploadVO;
import com.xindai.xindai.modules.file.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final StorageProperties storageProperties;

    private Set<String> allowedTypes;
    private long maxFileSizeBytes;
    private Path storageRoot;

    @PostConstruct
    public void init() {
        this.allowedTypes = Arrays.stream(storageProperties.getAllowedTypes().split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
        this.maxFileSizeBytes = parseSize(storageProperties.getMaxSize());
        this.storageRoot = Paths.get(storageProperties.getPath()).normalize().toAbsolutePath();

        try {
            Files.createDirectories(storageRoot);
            log.info("文件存储目录初始化完成: {}", storageRoot);
        } catch (IOException e) {
            log.error("无法创建文件存储目录: {}", storageRoot, e);
            throw new RuntimeException("无法初始化文件存储目录", e);
        }
    }

    @Override
    public FileUploadVO upload(MultipartFile file, String bizType) {
        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("不支持的文件类型: " + contentType);
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new RuntimeException("文件大小超过限制: " + storageProperties.getMaxSize());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String storedFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        String relativePath = bizType + "/" + datePath + "/" + storedFilename;
        Path fullPath = storageRoot.resolve(relativePath).normalize();

        // Path traversal check
        if (!fullPath.startsWith(storageRoot)) {
            throw new RuntimeException("非法的文件路径");
        }

        try {
            Files.createDirectories(fullPath.getParent());
            Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("文件保存失败: {}", fullPath, e);
            throw new RuntimeException("文件保存失败", e);
        }

        FileUploadVO vo = new FileUploadVO();
        vo.setFileUrl("/uploads/" + relativePath);
        vo.setFileName(originalFilename);
        vo.setFileType(contentType);
        vo.setFileSize(file.getSize());

        log.info("文件上传成功: {}", vo.getFileUrl());
        return vo;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            throw new RuntimeException("无效的文件路径: " + fileUrl);
        }

        String relativePath = fileUrl.substring("/uploads/".length());
        Path fullPath = storageRoot.resolve(relativePath).normalize();

        if (!fullPath.startsWith(storageRoot)) {
            throw new RuntimeException("非法的文件路径");
        }

        try {
            Files.deleteIfExists(fullPath);
            log.info("文件删除成功: {}", fileUrl);
        } catch (IOException e) {
            log.error("文件删除失败: {}", fullPath, e);
            throw new RuntimeException("文件删除失败", e);
        }
    }

    @Override
    public Resource load(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) {
            throw new RuntimeException("无效的文件路径: " + fileUrl);
        }

        String relativePath = fileUrl.substring("/uploads/".length());
        Path fullPath = storageRoot.resolve(relativePath).normalize();

        if (!fullPath.startsWith(storageRoot)) {
            throw new RuntimeException("非法的文件路径");
        }

        try {
            Resource resource = new UrlResource(fullPath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new RuntimeException("文件不存在或不可读: " + fileUrl);
        } catch (MalformedURLException e) {
            log.error("文件路径错误: {}", fullPath, e);
            throw new RuntimeException("文件路径错误", e);
        }
    }

    private long parseSize(String size) {
        size = size.toUpperCase().trim();
        if (size.endsWith("KB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.substring(0, size.length() - 2)) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(size);
    }
}
