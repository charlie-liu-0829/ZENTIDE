package com.zentide.utils;

public class SnowFlakeUtils {

    // 起始时间戳（2025-05-01）
    private final static long START_STMP = 1746028800000L;

    // 各部分位数
    private final static long SEQUENCE_BIT = 12; // 序列号位数
    private final static long MACHINE_BIT = 5;   // 机器标识位数
    private final static long DATACENTER_BIT = 5; // 数据中心位数

    // 最大值
    private final static long MAX_SEQUENCE = ~(-1L << SEQUENCE_BIT);

    // 移位偏移
    private final static long MACHINE_LEFT = SEQUENCE_BIT;
    private final static long DATACENTER_LEFT = SEQUENCE_BIT + MACHINE_BIT;
    private final static long TIMESTMP_LEFT = DATACENTER_LEFT + DATACENTER_BIT;

    private static long datacenterId = 1;  // 数据中心ID
    private static long machineId = 1;    // 机器ID
    private static long sequence = 0L; // 序列号
    private static long lastStmp = -1L; // 上次时间戳

    /**
     * 生成下一个ID
     */
    public static synchronized long nextId() {
        long currStmp = getNewstmp();

        if (currStmp < lastStmp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        if (currStmp == lastStmp) {
            // 相同毫秒内，序列号自增
            sequence = (sequence + 1) & MAX_SEQUENCE;
            // 同一毫秒内序列数已达最大
            if (sequence == 0L) {
                currStmp = getNextMill();
            }
        } else {
            // 不同毫秒内，序列号置为0
            sequence = 0L;
        }

        lastStmp = currStmp;

        return (currStmp - START_STMP) << TIMESTMP_LEFT // 时间戳部分
                | datacenterId << DATACENTER_LEFT      // 数据中心部分
                | machineId << MACHINE_LEFT            // 机器标识部分
                | sequence;                             // 序列号部分
    }

    // 获取下一毫秒
    private static long getNextMill() {
        long mill = getNewstmp();
        while (mill <= lastStmp) {
            mill = getNewstmp();
        }
        return mill;
    }

    // 获取当前时间戳
    private static long getNewstmp() {
        return System.currentTimeMillis();
    }
}