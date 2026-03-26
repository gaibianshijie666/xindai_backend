package com.xindai.xindai.modules.file.dto;

import lombok.Data;

@Data
public class FileUploadVO {
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
}
