package com.mall.common.file;

import com.mall.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "通用 - 文件上传")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传图片", description = "需登录；仅支持图片格式，返回可访问 URL")
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file,
                            @RequestParam(value = "dir", required = false) String dir) {
        return R.ok(fileService.upload(file, dir));
    }
}
