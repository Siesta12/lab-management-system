package com.nlt.domain.vo.statistics;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class AdminStatisticsVo {

    private AdminStatisticsVo() {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItemVo {
        private String label;
        private String value;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionsVo {
        private List<OptionItemVo> labs = new ArrayList<>();
        private List<OptionItemVo> reservationStatuses = new ArrayList<>();
        private List<OptionItemVo> reservationTypes = new ArrayList<>();
        private List<OptionItemVo> exportTypes = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChartItemVo {
        private String name;
        private Long value;
        private BigDecimal rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankItemVo {
        private Long id;
        private String name;
        private String secondary;
        private Long value;
        private BigDecimal rate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverviewVo {
        private Long monthReservationCount;
        private Long todayReservationCount;
        private Long pendingReservationCount;
        private BigDecimal labUsageRate;
        private Long brokenDeviceCount;
        private Long repairingDeviceCount;
        private Long lowStockConsumableCount;
        private Long monthViolationCount;
        private BigDecimal averageCreditScore;
        private Long lowCreditUserCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationStatsVo {
        private Long totalCount;
        private Long pendingCount;
        private Long completedCount;
        private BigDecimal completionRate;
        private List<ChartItemVo> trend = new ArrayList<>();
        private List<ChartItemVo> statusDistribution = new ArrayList<>();
        private List<ChartItemVo> typeDistribution = new ArrayList<>();
        private List<ChartItemVo> applicantRoleDistribution = new ArrayList<>();
        private List<ChartItemVo> labTypeDistribution = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LabUsageStatsVo {
        private BigDecimal usageRate;
        private Long totalOccupiedSlots;
        private Long highUsageLabCount;
        private Long idleLabCount;
        private List<RankItemVo> reservationRanking = new ArrayList<>();
        private List<RankItemVo> typeUsageRates = new ArrayList<>();
        private List<RankItemVo> highUsageLabs = new ArrayList<>();
        private List<RankItemVo> idleLabs = new ArrayList<>();
        private List<ChartItemVo> timeHeat = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceStatsVo {
        private Long totalCount;
        private Long normalCount;
        private Long repairingCount;
        private Long disabledCount;
        private Long repairOrderCount;
        private String categoryLabel;
        private List<ChartItemVo> statusDistribution = new ArrayList<>();
        private List<ChartItemVo> categoryDistribution = new ArrayList<>();
        private List<ChartItemVo> repairTrend = new ArrayList<>();
        private List<RankItemVo> labDeviceCounts = new ArrayList<>();
        private List<RankItemVo> abnormalDevices = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsumableStatsVo {
        private Long totalTypeCount;
        private Long lowStockCount;
        private Long monthInQuantity;
        private Long monthOutQuantity;
        private BigDecimal lowStockRate;
        private List<ChartItemVo> inTrend = new ArrayList<>();
        private List<ChartItemVo> outTrend = new ArrayList<>();
        private List<RankItemVo> consumptionRanking = new ArrayList<>();
        private List<RankItemVo> labUsageRanking = new ArrayList<>();
        private List<RankItemVo> warningList = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreditStatsVo {
        private Long monthLateCount;
        private Long monthNoShowCount;
        private BigDecimal averageCreditScore;
        private Long lowCreditUserCount;
        private List<ChartItemVo> violationTypeDistribution = new ArrayList<>();
        private List<ChartItemVo> violationTrend = new ArrayList<>();
        private List<ChartItemVo> lateTrend = new ArrayList<>();
        private List<ChartItemVo> noShowTrend = new ArrayList<>();
        private List<ChartItemVo> creditScoreDistribution = new ArrayList<>();
        private List<ChartItemVo> roleViolationDistribution = new ArrayList<>();
        private List<ChartItemVo> reservationTypeDistribution = new ArrayList<>();
        private List<ChartItemVo> timeSegmentDistribution = new ArrayList<>();
        private List<RankItemVo> lowCreditUsers = new ArrayList<>();
        private List<RankItemVo> violationRanking = new ArrayList<>();
    }
}
