package com.tinyshop.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @author TinyShop Team
 * @param <T> 数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class R<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码：200 成功，其他为失败 */
    private Integer code;

    /** 提示信息 */
    private String msg;

    /** 响应数据 */
    private T data;

    /** 时间戳 */
    private Long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("success");
        return r;
    }

    public static <T> R<T> ok(T data) {
        R<T> r = ok();
        r.setData(data);
        return r;
    }

    public static <T> R<T> ok(String msg, T data) {
        R<T> r = ok(data);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> error(String msg) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> error(Integer code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> error(Integer code, String msg, T data) {
        R<T> r = error(code, msg);
        r.setData(data);
        return r;
    }

    /**
     * 业务错误码枚举
     */
    public interface Code {
        int SUCCESS = 200;
        int BAD_REQUEST = 400;
        int UNAUTHORIZED = 401;
        int FORBIDDEN = 403;
        int NOT_FOUND = 404;
        int INTERNAL_ERROR = 500;
        int SERVICE_UNAVAILABLE = 503;

        // 业务自定义码
        int USER_NOT_FOUND = 1001;
        int USER_PASSWORD_ERROR = 1002;
        int TOKEN_INVALID = 1003;
        int TOKEN_EXPIRED = 1004;
        int PRODUCT_NOT_FOUND = 2001;
        int PRODUCT_STOCK_NOT_ENOUGH = 2002;
        int ORDER_NOT_FOUND = 3001;
        int ORDER_CREATE_FAILED = 3002;
        int SECKILL_NOT_START = 4001;
        int SECKILL_ALREADY_END = 4002;
        int SECKILL_STOCK_EMPTY = 4003;
        int SECKILL_RATE_LIMIT = 4004;
        int SECKILL_REPEAT = 4005;
        int PAY_FAILED = 5001;
    }
}
