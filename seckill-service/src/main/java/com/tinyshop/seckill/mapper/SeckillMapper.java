package com.tinyshop.seckill.mapper;

import com.tinyshop.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SeckillMapper {
    @Select("SELECT * FROM seckill_activity WHERE id = #{id}")
    SeckillActivity selectById(Long id);

    @Update("UPDATE seckill_activity SET stock = stock - 1, version = version + 1 WHERE id = #{id} AND stock > 0 AND version = #{version}")
    int decreaseStock(@Param("id") Long id, @Param("version") int version);
}
