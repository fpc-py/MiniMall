package com.mall.product.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mall.common.ErrorCode;
import com.mall.common.MallException;
import com.mall.product.converter.ProductConverter;
import com.mall.product.entity.Category;
import com.mall.product.mapper.CategoryMapper;
import com.mall.product.vo.CategoryTreeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 商品分类服务。
 * 类目树一次性查全量启用记录后在内存构建，避免逐级查询的 N+1 问题。
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    /** 一级分类的 parent_id */
    public static final long ROOT_PARENT_ID = 0L;
    /** 状态：1 显示 */
    public static final int STATUS_ENABLED = 1;
    /** 构树最大深度，防御脏数据导致的深递归 */
    private static final int MAX_DEPTH = 5;

    private final CategoryMapper categoryMapper;
    private final ProductConverter converter;

    /** 启用状态的分类树（按 sort、id 升序） */
    public List<CategoryTreeVO> tree() {
        return buildTree(listEnabled());
    }

    /** 全量启用分类，已按 sort、id 升序 */
    public List<Category> listEnabled() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .eq(Category::getStatus, STATUS_ENABLED)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
    }

    public Category findById(Long categoryId) {
        return categoryId == null ? null : categoryMapper.selectById(categoryId);
    }

    public Category getRequiredById(Long categoryId) {
        Category category = findById(categoryId);
        if (category == null) {
            throw new MallException(ErrorCode.Biz.CATEGORY_NOT_FOUND);
        }
        return category;
    }

    /**
     * 自身 + 全部启用子孙分类ID（按分类筛选商品时把子分类商品一起查出来）。
     */
    public List<Long> listSelfAndDescendantIds(Long categoryId) {
        if (categoryId == null) {
            return Collections.emptyList();
        }
        Map<Long, List<Category>> byParent = groupByParent(listEnabled());
        Set<Long> result = new LinkedHashSet<>();
        result.add(categoryId);
        Deque<Long> pending = new ArrayDeque<>();
        pending.push(categoryId);
        while (!pending.isEmpty()) {
            Long current = pending.pop();
            for (Category child : byParent.getOrDefault(current, Collections.emptyList())) {
                if (child.getId() != null && result.add(child.getId())) {
                    pending.push(child.getId());
                }
            }
        }
        return new ArrayList<>(result);
    }

    /** 内存构树：父节点缺失的脏数据兜底为根节点，visited 集合防御环 */
    List<CategoryTreeVO> buildTree(List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Category> byId = new LinkedHashMap<>();
        for (Category category : categories) {
            if (category.getId() != null) {
                byId.putIfAbsent(category.getId(), category);
            }
        }
        Map<Long, List<Category>> byParent = new HashMap<>();
        List<Category> roots = new ArrayList<>();
        for (Category category : categories) {
            long parentId = category.getParentId() == null ? ROOT_PARENT_ID : category.getParentId();
            if (parentId == ROOT_PARENT_ID || !byId.containsKey(parentId)) {
                roots.add(category);
            } else {
                byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(category);
            }
        }
        Set<Long> visited = new HashSet<>();
        List<CategoryTreeVO> tree = new ArrayList<>(roots.size());
        for (Category root : roots) {
            if (root.getId() != null && !visited.add(root.getId())) {
                continue;
            }
            tree.add(buildNode(root, byParent, visited, 1));
        }
        return tree;
    }

    private CategoryTreeVO buildNode(Category category, Map<Long, List<Category>> byParent, Set<Long> visited, int depth) {
        CategoryTreeVO node = converter.toCategoryNode(category);
        node.setLevel(category.getLevel() == null ? depth : category.getLevel());
        List<CategoryTreeVO> children = new ArrayList<>();
        node.setChildren(children);
        if (depth >= MAX_DEPTH || category.getId() == null) {
            return node;
        }
        for (Category child : byParent.getOrDefault(category.getId(), Collections.emptyList())) {
            if (child.getId() == null || !visited.add(child.getId())) {
                continue;
            }
            children.add(buildNode(child, byParent, visited, depth + 1));
        }
        return node;
    }

    private Map<Long, List<Category>> groupByParent(List<Category> categories) {
        Map<Long, List<Category>> byParent = new HashMap<>();
        for (Category category : categories) {
            if (category.getId() == null || category.getParentId() == null) {
                continue;
            }
            byParent.computeIfAbsent(category.getParentId(), k -> new ArrayList<>()).add(category);
        }
        return byParent;
    }
}
