package com.nlt.common.constant;

/**
 * 实验室签到相关常量。
 */
public final class ReservationCheckConstants {

    private ReservationCheckConstants() {
    }

    /**
     * 签到允许距离，单位：米。
     */
    public static final double MAX_CHECK_IN_DISTANCE_METERS = 100D;

    /**
     * 最早可签到时间：节次开始前 15 分钟。
     */
    public static final int CHECK_IN_EARLY_MINUTES = 15;

    /**
     * 最晚可签到时间：节次开始后 20 分钟。
     */
    public static final int CHECK_IN_LATE_MINUTES = 20;

    /**
     * 迟到扣减信誉分。
     */
    public static final int LATE_SCORE_DEDUCTION = -5;

    /**
     * 爽约扣减信誉分。
     */
    public static final int NO_SHOW_SCORE_DEDUCTION = -15;

    /**
     * 临近取消扣减信誉分。
     */
    public static final int NEAR_CANCEL_SCORE_DEDUCTION = -3;

    /**
     * 提前取消的免责时间，单位：分钟。
     */
    public static final int CANCEL_EARLY_MINUTES = 30;

    /**
     * 违规类型：爽约。
     */
    public static final int VIOLATION_TYPE_NO_SHOW = 1;

    /**
     * 违规类型：迟到。
     */
    public static final int VIOLATION_TYPE_LATE = 2;

    /**
     * 违规类型：临近取消。
     */
    public static final int VIOLATION_TYPE_NEAR_CANCEL = 5;

    /**
     * 审计动作：签到。
     */
    public static final int AUDIT_ACTION_CHECK_IN = 5;

    /**
     * 审计动作：签退。
     */
    public static final int AUDIT_ACTION_CHECK_OUT = 6;
}
