package com.ikkkk.teamrag.bean;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class DocumentForm {
    private MultipartFile file;
    private String docType;
    private String description;
}
