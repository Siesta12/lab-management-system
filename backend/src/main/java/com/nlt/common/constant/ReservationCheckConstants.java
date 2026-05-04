package com.nlt.common.constant;

public final class ReservationCheckConstants {

    private ReservationCheckConstants() {
    }

    public static final double MAX_CHECK_IN_DISTANCE_METERS = 100D;

    public static final int CHECK_IN_EARLY_MINUTES = 15;

    public static final int CHECK_IN_LATE_MINUTES = 20;

    public static final int LATE_SCORE_DEDUCTION = -5;

    public static final int NO_SHOW_SCORE_DEDUCTION = -15;

    public static final int NEAR_CANCEL_SCORE_DEDUCTION = -3;

    public static final int CANCEL_EARLY_MINUTES = 30;

    public static final int VIOLATION_TYPE_NO_SHOW = 1;

    public static final int VIOLATION_TYPE_LATE = 2;

    public static final int VIOLATION_TYPE_NEAR_CANCEL = 5;

    public static final int AUDIT_ACTION_CHECK_IN = 5;

    public static final int AUDIT_ACTION_CHECK_OUT = 6;
}
