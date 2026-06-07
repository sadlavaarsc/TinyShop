package com.tinyshop.pay.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tinyshop.pay.entity.PayRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 支付记录 Mapper 接口
 *
 * @author TinyShop Team
 */
@Mapper
public interface PayRecordMapper extends BaseMapper<PayRecord> {

    /**
     * 根据订单ID查询支付记录
     */
    @Select("SELECT * FROM t_pay_record WHERE order_id = #{orderId} AND deleted = 0")
    PayRecord selectByOrderId(@Param("orderId") Long orderId);
}
