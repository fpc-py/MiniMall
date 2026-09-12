package com.mall.product.service;

import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.product.converter.ProductConverterImpl;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.vo.CategoryTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 类目树构建测试：层级、排序、脏数据（孤儿节点/环）兜底。
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryMapper categoryMapper;

    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryMapper, new ProductConverterImpl());
    }

    private Category category(long id, String name, long parentId, int level, int sort) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSort(sort);
        category.setStatus(1);
        return category;
    }

    @Test
    @DisplayName("三级类目树：父子关系与顺序正确")
    void treeBuildsThreeLevels() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "手机数码", 0L, 1, 1),
                category(2L, "服装", 0L, 1, 2),
                category(10L, "手机", 1L, 2, 1),
                category(11L, "平板电脑", 1L, 2, 2),
                category(100L, "智能手机", 10L, 3, 1),
                category(101L, "老人机", 10L, 3, 2)));

        List<CategoryTreeVO> tree = categoryService.tree();

        assertThat(tree).hasSize(2);
        assertThat(tree.get(0).getName()).isEqualTo("手机数码");
        assertThat(tree.get(0).getLevel()).isEqualTo(1);
        assertThat(tree.get(0).getChildren())
                .extracting(CategoryTreeVO::getName)
                .containsExactly("手机", "平板电脑");
        assertThat(tree.get(0).getChildren().get(0).getChildren())
                .extracting(CategoryTreeVO::getName)
                .containsExactly("智能手机", "老人机");
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getChildren()).isEmpty();
        assertThat(tree.get(1).getName()).isEqualTo("服装");
        assertThat(tree.get(1).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("父节点缺失的脏数据兜底为根节点，不丢数据")
    void orphanNodeBecomesRoot() {
        List<CategoryTreeVO> tree = categoryService.buildTree(List.of(
                category(1L, "手机数码", 0L, 1, 1),
                category(5L, "孤儿分类", 999L, 2, 1)));

        assertThat(tree).hasSize(2);
        assertThat(tree).extracting(CategoryTreeVO::getId).containsExactlyInAnyOrder(1L, 5L);
    }

    @Test
    @DisplayName("环状脏数据不会导致无限递归")
    void cycleDoesNotHang() {
        List<CategoryTreeVO> tree = categoryService.buildTree(List.of(
                category(1L, "A", 2L, 1, 1),
                category(2L, "B", 1L, 1, 2)));

        assertThat(tree).isEmpty();
    }

    @Test
    @DisplayName("空数据返回空列表")
    void emptyDataReturnsEmptyList() {
        assertThat(categoryService.buildTree(List.of())).isEmpty();
        assertThat(categoryService.buildTree(null)).isEmpty();
    }

    @Test
    @DisplayName("重复ID只出现一次")
    void duplicatedIdIsRenderedOnce() {
        List<CategoryTreeVO> tree = categoryService.buildTree(List.of(
                category(1L, "手机数码", 0L, 1, 1),
                category(1L, "手机数码", 0L, 1, 1)));

        assertThat(tree).hasSize(1);
    }

    @Test
    @DisplayName("按分类筛选商品时，返回自身 + 全部子孙分类ID")
    void listSelfAndDescendantIds() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "手机数码", 0L, 1, 1),
                category(10L, "手机", 1L, 2, 1),
                category(11L, "平板电脑", 1L, 2, 2),
                category(100L, "智能手机", 10L, 3, 1)));

        assertThat(categoryService.listSelfAndDescendantIds(1L))
                .containsExactlyInAnyOrder(1L, 10L, 11L, 100L);
        assertThat(categoryService.listSelfAndDescendantIds(10L))
                .containsExactlyInAnyOrder(10L, 100L);
    }

    @Test
    @DisplayName("未知分类ID只返回自身，不会产生 IN () 空集合")
    void listSelfAndDescendantIdsForUnknownCategory() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(category(1L, "手机数码", 0L, 1, 1)));

        assertThat(categoryService.listSelfAndDescendantIds(404L)).containsExactly(404L);
        assertThat(categoryService.listSelfAndDescendantIds(null)).isEmpty();
    }

    @Test
    @DisplayName("分类不存在：抛 30013")
    void getRequiredByIdThrowsWhenMissing() {
        when(categoryMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> categoryService.getRequiredById(404L))
                .isInstanceOf(MallException.class)
                .extracting(e -> ((MallException) e).getCode())
                .isEqualTo(ErrorCode.Biz.CATEGORY_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("level 缺失时用实际深度兜底")
    void levelFallsBackToDepth() {
        Category child = category(11L, "手机", 1L, 2, 1);
        child.setLevel(null);

        List<CategoryTreeVO> tree = categoryService.buildTree(List.of(
                category(1L, "手机数码", 0L, 1, 1), child));

        assertThat(tree.get(0).getChildren().get(0).getLevel()).isEqualTo(2);
    }
}
