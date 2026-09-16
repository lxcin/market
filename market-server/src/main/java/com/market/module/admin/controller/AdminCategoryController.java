package com.market.module.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.market.common.response.Result;
import com.market.module.product.entity.Category;
import com.market.module.product.mapper.CategoryMapper;
import com.market.module.product.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/category")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;

    @GetMapping("/tree")
    public Result<List<Category>> tree() {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Category>> childrenMap = all.stream()
                .filter(c -> c.getParentId() != 0L)
                .collect(Collectors.groupingBy(Category::getParentId));
        for (Category category : all) {
            category.setChildren(childrenMap.getOrDefault(category.getId(), new ArrayList<>()));
        }
        List<Category> roots = all.stream()
                .filter(c -> c.getParentId() == 0L)
                .collect(Collectors.toList());
        return Result.success(roots);
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        category.setName(request.getName());
        category.setSort(request.getSort() != null ? request.getSort() : 0);
        categoryMapper.insert(category);
        categoryService.evictTree();
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        Category category = new Category();
        category.setId(id);
        category.setParentId(request.getParentId());
        category.setName(request.getName());
        category.setSort(request.getSort());
        categoryMapper.updateById(category);
        categoryService.evictTree();
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount > 0) {
            return Result.error("该分类下存在子分类，无法删除");
        }
        categoryMapper.deleteById(id);
        categoryService.evictTree();
        return Result.success();
    }

    @Data
    public static class CategoryRequest {

        private Long parentId;

        @NotBlank(message = "分类名称不能为空")
        private String name;

        @NotNull(message = "排序不能为空")
        private Integer sort;
    }
}
