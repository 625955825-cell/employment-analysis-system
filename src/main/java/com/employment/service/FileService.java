package com.employment.service;

import com.employment.model.entity.EmploymentAttachment;
import com.employment.repository.EmploymentAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {

    private final EmploymentAttachmentRepository attachmentRepository;

    @Transactional
    public EmploymentAttachment uploadAttachment(Long employmentId, String attachmentType, MultipartFile file) throws IOException {
        String uploadDir = System.getProperty("user.dir") + "/uploads/employment/";
        Path dirPath = Paths.get(uploadDir);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = UUID.randomUUID().toString() + extension;
        Path filePath = dirPath.resolve(savedFilename);
        Files.write(filePath, file.getBytes());

        EmploymentAttachment attachment = new EmploymentAttachment();
        attachment.setEmploymentId(employmentId);
        attachment.setAttachmentType(attachmentType);
        attachment.setAttachmentName(originalFilename);
        attachment.setFilePath("/uploads/employment/" + savedFilename);
        attachment.setFileSize(file.getSize());
        attachment.setUploadStatus("uploaded");
        return attachmentRepository.save(attachment);
    }

    public List<EmploymentAttachment> getAttachments(Long employmentId) {
        return attachmentRepository.findByEmploymentId(employmentId);
    }
}
