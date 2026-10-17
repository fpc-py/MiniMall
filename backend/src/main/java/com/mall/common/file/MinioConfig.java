package com.mall.common.file;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端装配。仅创建客户端，不在启动时连接/建桶，
 * 避免本地未启动 MinIO 时把应用启动搞崩（桶按需在首次上传时创建）。
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
