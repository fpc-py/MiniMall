package com.mall.common.file;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MinIO 对象存储配置（mall.minio）。
 * 生产环境用环境变量 / application-local.yml 覆盖，仓库内只放本地开发默认值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mall.minio")
public class MinioProperties {

    /** 服务端点，如 http://localhost:9000 */
    private String endpoint = "http://localhost:9000";

    private String accessKey;

    private String secretKey;

    /** 存储桶名 */
    private String bucket = "mall";

    /** 对外访问前缀（CDN/反向代理）；为空时使用 endpoint */
    private String publicUrl;

    /** 单文件大小上限（字节），默认 5MB */
    private long maxSizeBytes = 5L * 1024 * 1024;

    public String resolvedPublicUrl() {
        String url = (publicUrl == null || publicUrl.isBlank()) ? endpoint : publicUrl;
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
