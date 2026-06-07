package com.tinyshop.seckill.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyshop.seckill.entity.SeckillActivity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 秒杀活动 Mapper 接口
 *
 * @author TinyShop Team
 */
@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    /**
     * 查询进行中的秒杀活动
     */
    @Select("SELECT * FROM t_seckill_activity WHERE status = 1 AND deleted = 0 " +
            "AND start_time <= #{now} AND end_time >= #{now}")
    List<SeckillActivity> selectActiveList(@Param("now") LocalDateTime now);

    /**
     * 乐观锁扣减秒杀库存
     */
    @Update("UPDATE t_seckill_activity SET stock = stock - #{quantity}, version = version + 1 " +
            "WHERE id = #{id} AND stock >= #{quantity} AND version = #{version}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity, @Param("version") Integer version);
}
