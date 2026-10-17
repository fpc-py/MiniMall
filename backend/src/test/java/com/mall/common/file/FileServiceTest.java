package com.mall.common.file;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 文件上传服务测试：格式/大小校验、目录白名单、对象名服务端生成、失败错误码。
 */
@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private MinioClient minioClient;

    private MinioProperties properties;
    private FileService fileService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEndpoint("http://localhost:9000");
        properties.setAccessKey("admin");
        properties.setSecretKey("minioadmin");
        properties.setBucket("mall");
        properties.setMaxSizeBytes(1024);
        fileService = new FileService(minioClient, properties);
    }

    private MultipartFile png() {
        return new MockMultipartFile("file", "photo.png", "image/png", "fake-image".getBytes());
    }

    @Test
    @DisplayName("上传成功：桶不存在时自动创建，对象名由服务端生成")
    void uploadCreatesBucketAndReturnsUrl() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String url = fileService.upload(png(), "product");

        assertThat(url).startsWith("http://localhost:9000/mall/product/");
        assertThat(url).endsWith(".png");
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));

        ArgumentCaptor<PutObjectArgs> captor = ArgumentCaptor.forClass(PutObjectArgs.class);
        verify(minioClient).putObject(captor.capture());
        PutObjectArgs args = captor.getValue();
        assertThat(args.bucket()).isEqualTo("mall");
        assertThat(args.object()).startsWith("product/");
        // 不使用用户原始文件名，避免路径穿越
        assertThat(args.object()).doesNotContain("photo.png");
    }

    @Test
    @DisplayName("桶已存在：不重复创建")
    void uploadSkipsBucketCreation() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        fileService.upload(png(), "product");

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("空文件：抛 10001")
    void rejectEmptyFile() {
        MultipartFile empty = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(empty, "product"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Param.MISSING_PARAM.getCode());
    }

    @Test
    @DisplayName("非图片类型：抛 30023")
    void rejectNonImageContentType() {
        MultipartFile txt = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> fileService.upload(txt, "product"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.FILE_TYPE_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("伪装扩展名（image 类型但 .txt）：抛 30023")
    void rejectDisallowedExtension() {
        MultipartFile fake = new MockMultipartFile("file", "evil.txt", "image/png", "x".getBytes());

        assertThatThrownBy(() -> fileService.upload(fake, "product"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.FILE_TYPE_NOT_ALLOWED.getCode());
    }

    @Test
    @DisplayName("超出大小上限：抛 30024")
    void rejectOversize() {
        byte[] big = new byte[2048];
        MultipartFile file = new MockMultipartFile("file", "big.png", "image/png", big);

        assertThatThrownBy(() -> fileService.upload(file, "product"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.FILE_TOO_LARGE.getCode());
    }

    @Test
    @DisplayName("非法目录（路径穿越）：归入白名单目录 other")
    void sanitizeIllegalDir() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String url = fileService.upload(png(), "../../etc");

        assertThat(url).contains("/mall/other/");
    }

    @Test
    @DisplayName("存储异常：抛 30022")
    void wrapStorageFailure() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("connection refused"));

        assertThatThrownBy(() -> fileService.upload(png(), "product"))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.FILE_UPLOAD_FAILED.getCode());
    }
}
