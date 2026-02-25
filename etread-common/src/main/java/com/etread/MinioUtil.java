package com.etread;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Component
public class MinioUtil {
    @Value("${minio.endpoint}") private String endpoint;
    @Value("${minio.accessKey}") private String accessKey;
    @Value("${minio.secretKey}") private String secretKey;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    public String uploadFile(MultipartFile file, String bucket) throws Exception {
        String fileName = UUID.randomUUID().toString().replace("-", "") + "-" + file.getOriginalFilename();
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(fileName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        return endpoint + "/" + bucket + "/" + fileName;
    }
}