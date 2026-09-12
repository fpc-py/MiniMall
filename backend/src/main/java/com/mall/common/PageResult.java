package com.mall.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 统一分页返回体。
 */
@Data
@Schema(description = "分页结果")
public class PageResult<T> {

    @Schema(description = "当前页码")
    private int pageNum;

    @Schema(description = "每页条数")
    private int pageSize;

    @Schema(description = "总记录数")
    private long total;

    @Schema(description = "总页数")
    private int pages;

    @Schema(description = "数据列表")
    private List<T> list;

    public static <T> PageResult<T> of(long pageNum, long pageSize, long total, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.pageNum = (int) pageNum;
        result.pageSize = (int) pageSize;
        result.total = total;
        result.pages = pageSize <= 0 ? 0 : (int) ((total + pageSize - 1) / pageSize);
        result.list = list == null ? Collections.emptyList() : list;
        return result;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        return of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    /** 分页实体 -> 分页 VO */
    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        List<T> list = page.getRecords() == null
                ? Collections.emptyList()
                : page.getRecords().stream().map(converter).toList();
        return of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    public static <T> PageResult<T> empty(long pageNum, long pageSize) {
        return of(pageNum, pageSize, 0L, Collections.emptyList());
    }
}
