package com.tinyshop.pay.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 *
 * @author TinyShop Team
 */
@Data
@TableName("t_pay_record")
public class PayRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单ID */
    private Long orderId;

    /** 用户ID */
    private Long userId;

    /** 支付金额 */
    private BigDecimal amount;

    /** 支付状态：0-待支付，1-支付成功，2-支付失败 */
    private Integer status;

    /** 第三方支付流水号 */
    private String tradeNo;

    /** 支付渠道：alipay/wechat */
    private String channel;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
