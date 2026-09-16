package com.market.module.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.market.module.product.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    IPage<Book> selectPageWithCategory(Page<Book> page, @Param("categoryId") Long categoryId,
                                        @Param("keyword") String keyword);

    List<Book> searchByKeyword(@Param("keyword") String keyword, @Param("field") String field,
                               @Param("limit") Integer limit);

    List<Book> selectHotBooks(@Param("limit") Integer limit);

    List<Book> selectTopRated(@Param("limit") Integer limit);
}
