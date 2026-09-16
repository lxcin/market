package com.market.module.product.service;

import com.market.module.product.entity.Category;

import java.util.List;

public interface CategoryService {

    List<Category> listAll();

    Category getById(Long id);

    void evictTree();
}
