package com.tinyshop.common.utils;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法 ID 生成器
 *
 * @author TinyShop Team
 */
public class SnowflakeIdUtil {

    /** 起始时间戳 (2024-01-01) */
    private static final long START_TIMESTAMP = 1704067200000L;

    /** 机器ID位数 */
    private static final long WORKER_ID_BITS = 5L;

    /** 数据中心ID位数 */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /** 序列号位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器ID */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /** 最大数据中心ID */
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /** 机器ID左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /** 数据中心ID左移位数 */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /** 时间戳左移位数 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;

    /** 序列号掩码 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private final long workerId;
    private final long dataCenterId;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    private static final SnowflakeIdUtil INSTANCE = new SnowflakeIdUtil(1, 1);

    private SnowflakeIdUtil(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出范围");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("dataCenterId 超出范围");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }

    public static synchronized long nextId() {
        return INSTANCE.generateId();
    }

    private synchronized long generateId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
