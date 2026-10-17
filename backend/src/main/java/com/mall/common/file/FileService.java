package com.mall.common.file;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传服务（MinIO）。
 * <p>
 * 安全约束：仅允许图片扩展名与 image/* 类型；对象名由服务端生成（不使用用户文件名），
 * 目录白名单化，杜绝路径穿越；大小按配置限制。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final Set<String> ALLOWED_DIRS =
            Set.of("product", "shop", "avatar", "banner", "apply", "other");
    private static final String DEFAULT_DIR = "other";
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /**
     * 上传图片并返回可访问 URL。
     *
     * @param file 图片文件
     * @param dir  业务目录（白名单：product/shop/avatar/banner/apply），非法值归入 other
     */
    public String upload(MultipartFile file, String dir) {
        validate(file);
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new MallException(ErrorCode.Biz.FILE_TYPE_NOT_ALLOWED);
        }

        String objectName = buildObjectName(sanitizeDir(dir), ext);
        try {
            ensureBucket();
            try (InputStream in = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.getBucket())
                        .object(objectName)
                        .stream(in, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
        } catch (Exception e) {
            log.error("上传文件到 MinIO 失败: object={}", objectName, e);
            throw new MallException(ErrorCode.Biz.FILE_UPLOAD_FAILED);
        }

        String url = properties.resolvedPublicUrl() + "/" + properties.getBucket() + "/" + objectName;
        log.info("图片上传成功: {}", url);
        return url;
    }

    // ==================== 私有方法 ====================

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new MallException(ErrorCode.Param.MISSING_PARAM, "上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxSizeBytes()) {
            throw new MallException(ErrorCode.Biz.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new MallException(ErrorCode.Biz.FILE_TYPE_NOT_ALLOWED);
        }
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    private String sanitizeDir(String dir) {
        if (dir == null || dir.isBlank()) {
            return DEFAULT_DIR;
        }
        String normalized = dir.trim().toLowerCase();
        return ALLOWED_DIRS.contains(normalized) ? normalized : DEFAULT_DIR;
    }

    private String buildObjectName(String dir, String ext) {
        String name = UUID.randomUUID().toString().replace("-", "");
        return dir + "/" + LocalDate.now().format(DATE_DIR) + "/" + name + "." + ext;
    }

    private void ensureBucket() throws Exception {
        String bucket = properties.getBucket();
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            try {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            } catch (Exception e) {
                // 多实例并发时可能已被其他实例创建，忽略即可
                log.warn("创建存储桶失败（可能已存在）: bucket={}", bucket, e);
            }
        }
    }
}
