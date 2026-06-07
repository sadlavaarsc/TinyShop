package com.tinyshop.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyshop.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 商品 Mapper 接口
 *
 * @author TinyShop Team
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM t_product WHERE id = #{id} AND deleted = 0")
    Product selectById(@Param("id") Long id);

    @Select("SELECT * FROM t_product WHERE deleted = 0 LIMIT #{offset}, #{limit}")
    List<Product> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 乐观锁扣减库存
     */
    @Update("UPDATE t_product SET stock = stock - #{count}, version = version + 1 " +
            "WHERE id = #{id} AND stock >= #{count} AND version = #{version} AND deleted = 0")
    int decreaseStock(@Param("id") Long id, @Param("count") int count, @Param("version") int version);
}
