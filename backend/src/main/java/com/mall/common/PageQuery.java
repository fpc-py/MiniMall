package com.mall.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 分页请求基类：所有列表接口统一 pageNum/pageSize。
 */
@Data
public class PageQuery {

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    @Schema(description = "页码，从 1 开始", example = "1")
    @Min(value = 1, message = "pageNum 不能小于 1")
    private Integer pageNum = 1;

    @Schema(description = "每页条数，最大 100", example = "10")
    @Min(value = 1, message = "pageSize 不能小于 1")
    @Max(value = MAX_PAGE_SIZE, message = "pageSize 不能大于 100")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /** 兜底取值：即使调用方绕过校验，也不会产生非法分页参数。 */
    public int safePageNum() {
        return (pageNum == null || pageNum < 1) ? 1 : pageNum;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}
