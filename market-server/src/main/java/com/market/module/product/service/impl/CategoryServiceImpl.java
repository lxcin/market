package com.market.module.product.service.impl;

import com.market.common.cache.CacheHelper;
import com.market.module.product.entity.Category;
import com.market.module.product.mapper.CategoryMapper;
import com.market.module.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Duration TREE_TTL = Duration.ofMinutes(30);

    private final CategoryMapper categoryMapper;
    private final CacheHelper cacheHelper;

    @Override
    public List<Category> listAll() {
        return cacheHelper.getOrLoad("cache:category:tree", TREE_TTL, this::loadTree);
    }

    private List<Category> loadTree() {
        List<Category> all = categoryMapper.selectList(null);
        Map<Long, List<Category>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() != null ? c.getParentId() : 0L));

        List<Category> roots = childrenMap.getOrDefault(0L, new ArrayList<>());
        for (Category root : roots) {
            buildChildren(root, childrenMap);
        }
        return roots;
    }

    private void buildChildren(Category parent, Map<Long, List<Category>> childrenMap) {
        List<Category> children = childrenMap.get(parent.getId());
        if (children != null) {
            parent.setChildren(children);
            for (Category child : children) {
                buildChildren(child, childrenMap);
            }
        }
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public void evictTree() {
        cacheHelper.evict("cache:category:tree");
    }
}
