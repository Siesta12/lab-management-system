package com.nlt.service.impl;

import com.nlt.common.exception.BusinessException;
import com.nlt.common.security.CurrentUserScopeService;
import com.nlt.domain.dto.statistics.AdminStatisticsQuery;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ChartItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ConsumableStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.CreditStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.DeviceStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.LabUsageStatsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OptionsVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.OverviewVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.RankItemVo;
import com.nlt.domain.vo.statistics.AdminStatisticsVo.ReservationStatsVo;
import com.nlt.domain.vo.statistics.export.ConsumableExportVo;
import com.nlt.domain.vo.statistics.export.CreditViolationExportVo;
import com.nlt.domain.vo.statistics.export.ExperimentReportExportVo;
import com.nlt.domain.vo.statistics.export.ReservationExportVo;
import com.nlt.mapper.AdminStatisticsMapper;
import com.nlt.mapper.ConsumableMapper;
import com.nlt.mapper.ExperimentReportMapper;
import com.nlt.mapper.ReservationMapper;
import com.nlt.mapper.ViolationMapper;
import com.nlt.service.AdminStatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {

    private static final int EXPORT_LIMIT = 5000;
    private static final int LOW_CREDIT_SCORE = 60;
    private static final DateTimeFormatter FILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter CELL_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CurrentUserScopeService currentUserScopeService;
    private final AdminStatisticsMapper adminStatisticsMapper;
    private final ReservationMapper reservationMapper;
    private final ExperimentReportMapper experimentReportMapper;
    private final ConsumableMapper consumableMapper;
    private final ViolationMapper violationMapper;

    @Override
    public OptionsVo options() {
        Long departmentId = requireAdminDepartmentId();
        return new OptionsVo(
            adminStatisticsMapper.selectLabOptions(departmentId),
            List.of(
                new OptionItemVo("待审核", "1"),
                new OptionItemVo("已通过", "2"),
                new OptionItemVo("已驳回", "3"),
                new OptionItemVo("已取消", "4"),
                new OptionItemVo("已完成", "5")
            ),
            List.of(
                new OptionItemVo("课程教学", "1"),
                new OptionItemVo("科研训练", "2"),
                new OptionItemVo("个人预约", "3")
            ),
            List.of(
                new OptionItemVo("预约数据", "reservation"),
                new OptionItemVo("实验报告数据", "experimentReport"),
                new OptionItemVo("耗材统计数据", "consumable"),
                new OptionItemVo("违规/信用数据", "creditViolation")
            )
        );
    }

    @Override
    public OverviewVo overview(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);
        validateLab(query.getLabId(), departmentId);

        long monthReservationCount = adminStatisticsMapper.countOverviewMonthReservations(
            departmentId,
            trimToNull(query.getLabType()),
            range.startDate(),
            range.endDate()
        );
        long todayReservationCount = adminStatisticsMapper.countTodayReservations(departmentId, LocalDate.now());
        long pendingReservationCount = adminStatisticsMapper.countPendingReservations(departmentId);
        long brokenDeviceCount = adminStatisticsMapper.countBrokenDevices(departmentId);
        long repairingDeviceCount = adminStatisticsMapper.countRepairingDevices(departmentId);
        long lowStockConsumableCount = adminStatisticsMapper.countLowStockConsumables(departmentId);
        long monthViolationCount = adminStatisticsMapper.countOverviewMonthViolations(departmentId, range.startDate(), range.endDate());
        BigDecimal averageCreditScore = defaultDecimal(adminStatisticsMapper.selectAverageCreditScore(departmentId));
        long lowCreditUserCount = adminStatisticsMapper.countLowCreditUsers(departmentId, LOW_CREDIT_SCORE);

        return new OverviewVo(
            monthReservationCount,
            todayReservationCount,
            pendingReservationCount,
            labUsageRate(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate()),
            brokenDeviceCount,
            repairingDeviceCount,
            lowStockConsumableCount,
            monthViolationCount,
            averageCreditScore,
            lowCreditUserCount
        );
    }

    @Override
    public ReservationStatsVo reservations(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);

        long totalCount = adminStatisticsMapper.countReservationTotal(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getStatus(), query.getReservationType(), range.startDate(), range.endDate()
        );
        long pendingCount = adminStatisticsMapper.countReservationPending(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getReservationType(), range.startDate(), range.endDate()
        );
        long completedCount = adminStatisticsMapper.countReservationCompleted(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getReservationType(), range.startDate(), range.endDate()
        );

        List<ChartItemVo> trend = chartRows(adminStatisticsMapper.selectReservationTrend(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getStatus(), query.getReservationType(), range.startDate(), range.endDate()
        ));
        List<ChartItemVo> statusDistribution = namedChartRows(
            adminStatisticsMapper.selectReservationStatusDistribution(
                departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getReservationType(), range.startDate(), range.endDate()
            ),
            this::reservationStatusLabel
        );
        List<ChartItemVo> typeDistribution = namedChartRows(
            adminStatisticsMapper.selectReservationTypeDistribution(
                departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getStatus(), range.startDate(), range.endDate()
            ),
            this::reservationTypeLabel
        );
        List<ChartItemVo> applicantRoleDistribution = namedChartRows(
            adminStatisticsMapper.selectReservationApplicantRoleDistribution(
                departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getStatus(), query.getReservationType(), range.startDate(), range.endDate()
            ),
            this::roleLabel
        );
        List<ChartItemVo> labTypeDistribution = namedChartRows(
            adminStatisticsMapper.selectReservationLabTypeDistribution(
                departmentId, query.getLabId(), trimToNull(query.getLabType()), query.getStatus(), query.getReservationType(), range.startDate(), range.endDate()
            ),
            this::labTypeLabel
        );

        return new ReservationStatsVo(
            totalCount,
            pendingCount,
            completedCount,
            totalCount == 0 ? BigDecimal.ZERO : rate(completedCount, totalCount),
            trend,
            statusDistribution,
            typeDistribution,
            applicantRoleDistribution,
            labTypeDistribution
        );
    }

    @Override
    public LabUsageStatsVo labUsage(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);

        BigDecimal usageRate = labUsageRate(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate());
        List<RankItemVo> ranking = rankRows(adminStatisticsMapper.selectLabUsageRanking(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        List<RankItemVo> highUsageLabs = List.copyOf(ranking);
        List<RankItemVo> idleLabs = rankRows(adminStatisticsMapper.selectLabUsageIdleLabs(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        List<RankItemVo> typeUsageRates = labTypeRates(departmentId, query.getLabId(), query.getLabType(), range.startDate(), range.endDate());
        List<ChartItemVo> timeHeat = chartRows(adminStatisticsMapper.selectLabUsageTimeHeat(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        long totalOccupiedSlots = timeHeat.stream().mapToLong(ChartItemVo::getValue).sum();
        long highUsageLabCount = adminStatisticsMapper.countHighUsageLabs(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        );
        long idleLabCount = adminStatisticsMapper.countIdleLabs(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        );

        return new LabUsageStatsVo(usageRate, totalOccupiedSlots, highUsageLabCount, idleLabCount, ranking, typeUsageRates, highUsageLabs, idleLabs, timeHeat);
    }

    @Override
    public DeviceStatsVo devices(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);

        long total = adminStatisticsMapper.countDevicesTotal(departmentId, query.getLabId(), trimToNull(query.getLabType()));
        long normal = adminStatisticsMapper.countDevicesByStatus(departmentId, query.getLabId(), trimToNull(query.getLabType()), 1);
        long repairing = adminStatisticsMapper.countDevicesByStatus(departmentId, query.getLabId(), trimToNull(query.getLabType()), 2);
        long disabled = adminStatisticsMapper.countDevicesByStatus(departmentId, query.getLabId(), trimToNull(query.getLabType()), 3);
        long repairOrders = adminStatisticsMapper.countDeviceRepairOrders(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        );
        List<RankItemVo> labDeviceCounts = rankRows(adminStatisticsMapper.selectLabDeviceCounts(
            departmentId, query.getLabId(), trimToNull(query.getLabType())
        ));
        List<RankItemVo> abnormalDevices = rankRows(adminStatisticsMapper.selectAbnormalDevices(
            departmentId, query.getLabId(), trimToNull(query.getLabType())
        )).stream()
            .map(item -> new RankItemVo(item.getId(), item.getName(), deviceStatusLabel(item.getValue().intValue()) + " / " + item.getSecondary(), item.getValue(), item.getRate()))
            .toList();
        List<ChartItemVo> statusDistribution = withRates(List.of(
            new ChartItemVo("正常", normal, BigDecimal.ZERO),
            new ChartItemVo("维修中", repairing, BigDecimal.ZERO),
            new ChartItemVo("停用", disabled, BigDecimal.ZERO)
        ));
        List<ChartItemVo> categoryDistribution = namedChartRows(
            adminStatisticsMapper.selectDeviceCategoryDistribution(departmentId, query.getLabId(), trimToNull(query.getLabType())),
            this::deviceCategoryLabel
        );
        List<ChartItemVo> repairTrend = chartRows(adminStatisticsMapper.selectDeviceRepairTrend(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));

        return new DeviceStatsVo(
            total,
            normal,
            repairing,
            disabled,
            repairOrders,
            "品牌分布",
            statusDistribution,
            categoryDistribution,
            repairTrend,
            labDeviceCounts,
            abnormalDevices
        );
    }

    @Override
    public ConsumableStatsVo consumables(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);
        validateLab(query.getLabId(), departmentId);

        long totalTypeCount = adminStatisticsMapper.countConsumableTypes(departmentId, query.getLabId(), trimToNull(query.getLabType()));
        long lowStockCount = adminStatisticsMapper.countLowStockConsumablesByQuery(departmentId, query.getLabId(), trimToNull(query.getLabType()));
        long inQuantity = adminStatisticsMapper.sumConsumableInQuantity(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        );
        long outQuantity = adminStatisticsMapper.sumConsumableOutQuantity(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        );
        List<RankItemVo> consumptionRanking = rankRows(adminStatisticsMapper.selectConsumableConsumptionRanking(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        List<RankItemVo> warningList = rankRows(adminStatisticsMapper.selectConsumableWarningList(
            departmentId, query.getLabId(), trimToNull(query.getLabType())
        ));
        List<ChartItemVo> inTrend = chartRows(adminStatisticsMapper.selectConsumableInTrend(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        List<ChartItemVo> outTrend = chartRows(adminStatisticsMapper.selectConsumableOutTrend(
            departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
        ));
        List<RankItemVo> labUsageRanking = namedRankRows(
            adminStatisticsMapper.selectConsumableLabUsageRanking(
                departmentId, query.getLabId(), trimToNull(query.getLabType()), range.startDate(), range.endDate()
            ),
            this::labTypeLabel
        );

        return new ConsumableStatsVo(
            totalTypeCount,
            lowStockCount,
            inQuantity,
            outQuantity,
            totalTypeCount == 0 ? BigDecimal.ZERO : rate(lowStockCount, totalTypeCount),
            inTrend,
            outTrend,
            consumptionRanking,
            labUsageRanking,
            warningList
        );
    }

    @Override
    public CreditStatsVo credit(AdminStatisticsQuery query) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = monthRange(query);

        long lateCount = adminStatisticsMapper.countViolationsByType(departmentId, range.startDate(), range.endDate(), 2);
        long noShowCount = adminStatisticsMapper.countViolationsByType(departmentId, range.startDate(), range.endDate(), 1);
        BigDecimal averageCreditScore = defaultDecimal(adminStatisticsMapper.selectAverageCreditScore(departmentId));
        long lowCreditUserCount = adminStatisticsMapper.countLowCreditUsers(departmentId, LOW_CREDIT_SCORE);
        List<ChartItemVo> distribution = namedChartRows(
            adminStatisticsMapper.selectViolationTypeDistribution(departmentId, range.startDate(), range.endDate()),
            this::violationTypeLabel
        );
        List<RankItemVo> lowCreditUsers = rankRows(adminStatisticsMapper.selectLowCreditUsers(departmentId, LOW_CREDIT_SCORE));
        List<RankItemVo> violationRanking = rankRows(adminStatisticsMapper.selectViolationRanking(departmentId, range.startDate(), range.endDate()));
        List<ChartItemVo> violationTrend = chartRows(adminStatisticsMapper.selectViolationTrend(departmentId, range.startDate(), range.endDate()));
        List<ChartItemVo> lateTrend = chartRows(adminStatisticsMapper.selectLateTrend(departmentId, range.startDate(), range.endDate()));
        List<ChartItemVo> noShowTrend = chartRows(adminStatisticsMapper.selectNoShowTrend(departmentId, range.startDate(), range.endDate()));
        List<ChartItemVo> creditScoreDistribution = namedChartRows(
            adminStatisticsMapper.selectCreditScoreDistribution(departmentId),
            this::creditScoreRangeLabel
        );
        List<ChartItemVo> roleViolationDistribution = namedChartRows(
            adminStatisticsMapper.selectRoleViolationDistribution(departmentId, range.startDate(), range.endDate()),
            this::roleLabel
        );
        List<ChartItemVo> reservationTypeDistribution = namedChartRows(
            adminStatisticsMapper.selectCreditReservationTypeDistribution(departmentId, range.startDate(), range.endDate()),
            this::reservationTypeLabel
        );
        List<ChartItemVo> timeSegmentDistribution = namedChartRows(
            adminStatisticsMapper.selectCreditTimeSegmentDistribution(departmentId, range.startDate(), range.endDate()),
            this::timeSegmentLabel
        );

        return new CreditStatsVo(
            lateCount,
            noShowCount,
            averageCreditScore,
            lowCreditUserCount,
            distribution,
            violationTrend,
            lateTrend,
            noShowTrend,
            creditScoreDistribution,
            roleViolationDistribution,
            reservationTypeDistribution,
            timeSegmentDistribution,
            lowCreditUsers,
            violationRanking
        );
    }

    @Override
    public void export(AdminStatisticsQuery query, HttpServletResponse response) {
        Long departmentId = requireAdminDepartmentId();
        DateRange range = defaultRange(query);
        validateLab(query.getLabId(), departmentId);
        String exportType = query.getExportType();
        if (exportType == null || exportType.isBlank()) {
            throw new BusinessException("导出类型不能为空");
        }

        ExportData exportData = switch (exportType) {
            case "reservation" -> reservationExport(departmentId, range, query);
            case "labUsage" -> labUsageExport(query);
            case "device" -> deviceExport(query);
            case "experimentReport" -> experimentReportExport(departmentId, range, query);
            case "consumable" -> consumableExport(departmentId, range, query);
            case "creditViolation" -> creditViolationExport(departmentId, range, query);
            default -> throw new BusinessException("不支持的导出类型");
        };

        String filename = exportData.title() + "_" + LocalDateTime.now().format(FILE_DATE_FORMATTER) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8));

        try (SXSSFWorkbook workbook = new SXSSFWorkbook()) {
            writeSheet(workbook, exportData);
            workbook.write(response.getOutputStream());
            workbook.dispose();
        } catch (IOException ex) {
            throw new BusinessException("导出失败，请稍后重试");
        }
    }

    private ExportData reservationExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        ensureExportWithinLimit(reservationMapper.countExport(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType()
        ));
        List<ReservationExportVo> rows = reservationMapper.selectExportList(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType(),
            EXPORT_LIMIT
        );
        List<List<Object>> data = rows.stream()
            .map(row -> List.<Object>of(
                cell(row.getReservationNo()),
                cell(row.getLabName()),
                cell(row.getLabType()),
                cell(row.getApplicantName()),
                cell(row.getUserNo()),
                cell(row.getUserRole()),
                reservationTypeLabel(row.getReservationType()),
                reservationStatusLabel(row.getStatus()),
                cell(row.getStartTime()),
                cell(row.getEndTime()),
                yesNo(row.getLateViolation()),
                yesNo(row.getNoShowViolation()),
                cell(row.getCreatedAt()),
                cell(row.getApproverName()),
                cell(row.getAuditedAt())
            ))
            .toList();
        return new ExportData(
            "预约数据",
            List.of(
                "预约编号", "实验室名称", "实验室类型", "预约人姓名", "学号/工号",
                "用户角色", "预约类型", "预约状态", "开始时间", "结束时间",
                "是否迟到", "是否爽约", "创建时间", "审核人", "审核时间"
            ),
            data
        );
    }

    private ExportData experimentReportExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        ensureExportWithinLimit(experimentReportMapper.countExport(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType()
        ));
        List<ExperimentReportExportVo> rows = experimentReportMapper.selectExportList(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType(),
            EXPORT_LIMIT
        );
        List<List<Object>> data = rows.stream()
            .map(row -> List.<Object>of(
                cell(row.getReportNo()),
                cell(row.getReservationNo()),
                cell(row.getLabName()),
                cell(row.getExperimentName()),
                cell(row.getSubmitterName()),
                cell(row.getUserNo()),
                cell(row.getSubmittedAt()),
                reportStatusLabel(row.getStatus()),
                cell(row.getReviewerName()),
                cell(row.getReviewedAt()),
                "",
                cell(row.getTeacherComment()),
                ""
            ))
            .toList();
        return new ExportData(
            "实验报告数据",
            List.of(
                "报告编号", "预约编号", "实验室名称", "实验名称",
                "提交人姓名", "学号/工号", "提交时间", "审核状态",
                "审核人", "审核时间", "得分", "评价", "备注"
            ),
            data
        );
    }

    private ExportData labUsageExport(AdminStatisticsQuery query) {
        LabUsageStatsVo stats = labUsage(query);
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("汇总指标", "实验室使用率", "当前筛选范围内的预约使用率", formatPercent(stats.getUsageRate()), ""));
        data.add(List.of("汇总指标", "总占用节次数", "当前筛选范围内已占用节次数", cell(stats.getTotalOccupiedSlots()), ""));
        data.add(List.of("汇总指标", "高频实验室数量", "当前筛选范围内有预约的实验室数量", cell(stats.getHighUsageLabCount()), ""));
        data.add(List.of("汇总指标", "空闲实验室数量", "当前筛选范围内无预约的实验室数量", cell(stats.getIdleLabCount()), ""));
        appendChartRows(data, "时间段使用热度", stats.getTimeHeat(), "时间段使用次数");
        appendRankRows(data, "实验室类型使用率", stats.getTypeUsageRates(), "实验室类型", true);
        appendRankRows(data, "高频实验室", stats.getHighUsageLabs(), "实验室", true);
        appendRankRows(data, "空闲实验室", stats.getIdleLabs(), "实验室", true);
        return new ExportData(
            "实验室使用统计",
            List.of("分类", "名称", "说明", "数值", "占比/比率"),
            data
        );
    }

    private ExportData consumableExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        ensureExportWithinLimit(consumableMapper.countExport(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId()
        ));
        List<ConsumableExportVo> rows = consumableMapper.selectExportList(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            EXPORT_LIMIT
        );
        List<List<Object>> data = rows.stream()
            .map(row -> List.<Object>of(
                cell(row.getConsumableName()),
                cell(row.getLabName()),
                cell(row.getLabType()),
                cell(row.getStockQuantity()),
                cell(row.getWarningThreshold()),
                cell(row.getUnit()),
                cell(row.getInQuantity()),
                cell(row.getOutQuantity()),
                cell(row.getLastInTime()),
                cell(row.getLastOutTime()),
                yesNo(row.getLowStock())
            ))
            .toList();
        return new ExportData(
            "耗材统计数据",
            List.of(
                "耗材名称", "所属实验室", "实验室类型", "当前库存", "预警阈值",
                "单位", "统计期入库数量", "统计期出库数量", "最近入库时间",
                "最近出库时间", "是否低库存"
            ),
            data
        );
    }

    private ExportData deviceExport(AdminStatisticsQuery query) {
        DeviceStatsVo stats = devices(query);
        List<List<Object>> data = new ArrayList<>();
        data.add(List.of("汇总指标", "设备总数", "当前筛选范围内设备总量", cell(stats.getTotalCount()), ""));
        data.add(List.of("汇总指标", "正常设备数", "状态为正常的设备数量", cell(stats.getNormalCount()), ""));
        data.add(List.of("汇总指标", "维修中设备数", "状态为维修中的设备数量", cell(stats.getRepairingCount()), ""));
        data.add(List.of("汇总指标", "停用设备数", "状态为停用的设备数量", cell(stats.getDisabledCount()), ""));
        data.add(List.of("汇总指标", "报修数量", "当前筛选范围内报修记录数量", cell(stats.getRepairOrderCount()), ""));
        appendChartRows(data, "设备状态分布", stats.getStatusDistribution(), "设备状态");
        appendChartRows(data, stats.getCategoryLabel() + "分布", stats.getCategoryDistribution(), stats.getCategoryLabel());
        appendChartRows(data, "报修趋势", stats.getRepairTrend(), "报修日期");
        appendRankRows(data, "实验室设备数量", stats.getLabDeviceCounts(), "实验室", true);
        return new ExportData(
            "设备统计",
            List.of("分类", "名称", "说明", "数值", "占比/比率"),
            data
        );
    }

    private ExportData creditViolationExport(Long departmentId, DateRange range, AdminStatisticsQuery query) {
        ensureExportWithinLimit(violationMapper.countExport(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType()
        ));
        List<CreditViolationExportVo> rows = violationMapper.selectExportList(
            departmentId,
            range.startDate(),
            range.endDate(),
            trimToNull(query.getLabType()),
            query.getLabId(),
            query.getStatus(),
            query.getReservationType(),
            EXPORT_LIMIT
        );
        List<List<Object>> data = rows.stream()
            .map(row -> List.<Object>of(
                cell(row.getRealName()),
                cell(row.getUserNo()),
                cell(row.getUserRole()),
                cell(row.getCreditScore()),
                violationTypeLabel(row.getViolationType()),
                cell(row.getScoreChange()),
                cell(row.getViolationTime()),
                cell(row.getReservationNo()),
                "",
                cell(row.getTotalViolationCount()),
                cell(row.getRemark())
            ))
            .toList();
        return new ExportData(
            "信用违规数据",
            List.of(
                "用户姓名", "学号/工号", "用户角色", "当前信用分", "违规类型",
                "扣分", "违规时间", "对应预约编号", "处理状态", "累计违规次数", "备注"
            ),
            data
        );
    }

    private void writeSheet(SXSSFWorkbook workbook, ExportData exportData) {
        Sheet sheet = workbook.createSheet(exportData.title());
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        for (int i = 0; i < exportData.headers().size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(exportData.headers().get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 5200);
        }

        for (int rowIndex = 0; rowIndex < exportData.rows().size(); rowIndex++) {
            Row row = sheet.createRow(rowIndex + 1);
            List<Object> values = exportData.rows().get(rowIndex);
            for (int columnIndex = 0; columnIndex < values.size(); columnIndex++) {
                row.createCell(columnIndex).setCellValue(cell(values.get(columnIndex)));
            }
        }
    }

    private Long requireAdminDepartmentId() {
        Long departmentId = currentUserScopeService.resolveAdminDepartmentId();
        if (departmentId == null) {
            throw new BusinessException(403, "仅管理员可访问统计分析");
        }
        return departmentId;
    }

    private void validateLab(Long labId, Long departmentId) {
        if (labId == null) {
            return;
        }
        if (adminStatisticsMapper.countAccessibleLab(departmentId, labId) == 0) {
            throw new BusinessException(403, "实验室不存在或无权访问");
        }
    }

    private DateRange defaultRange(AdminStatisticsQuery query) {
        LocalDate endDate = query.getEndDate() == null ? LocalDate.now() : query.getEndDate();
        LocalDate startDate = query.getStartDate() == null ? endDate.minusDays(29) : query.getStartDate();
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new DateRange(startDate, endDate);
    }

    private DateRange monthRange(AdminStatisticsQuery query) {
        LocalDate now = LocalDate.now();
        LocalDate startDate = query.getStartDate() == null ? now.withDayOfMonth(1) : query.getStartDate();
        LocalDate endDate = query.getEndDate() == null ? now : query.getEndDate();
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        return new DateRange(startDate, endDate);
    }

    private List<ChartItemVo> chartRows(List<Map<String, Object>> rows) {
        return withRates(rows.stream()
            .map(row -> new ChartItemVo(Objects.toString(row.get("name"), ""), number(row.get("value")), BigDecimal.ZERO))
            .toList());
    }

    private List<ChartItemVo> namedChartRows(List<Map<String, Object>> rows, LabelResolver resolver) {
        return withRates(rows.stream()
            .map(row -> new ChartItemVo(resolver.resolve(row.get("name")), number(row.get("value")), BigDecimal.ZERO))
            .toList());
    }

    private List<ChartItemVo> withRates(List<ChartItemVo> items) {
        long total = items.stream().mapToLong(ChartItemVo::getValue).sum();
        if (total == 0) {
            return items;
        }
        return items.stream()
            .map(item -> new ChartItemVo(item.getName(), item.getValue(), rate(item.getValue(), total)))
            .toList();
    }

    private List<RankItemVo> rankRows(List<Map<String, Object>> rows) {
        long total = rows.stream().mapToLong(row -> number(row.get("value"))).sum();
        return rows.stream()
            .map(row -> new RankItemVo(
                number(row.get("id")),
                Objects.toString(row.get("name"), ""),
                Objects.toString(row.get("secondary"), ""),
                number(row.get("value")),
                total == 0 ? BigDecimal.ZERO : rate(number(row.get("value")), total)
            ))
            .toList();
    }

    private List<RankItemVo> namedRankRows(List<Map<String, Object>> rows, LabelResolver resolver) {
        long total = rows.stream().mapToLong(row -> number(row.get("value"))).sum();
        return rows.stream()
            .map(row -> new RankItemVo(
                number(row.get("id")),
                Objects.toString(row.get("name"), ""),
                resolver.resolve(row.get("secondary")),
                number(row.get("value")),
                total == 0 ? BigDecimal.ZERO : rate(number(row.get("value")), total)
            ))
            .toList();
    }

    private BigDecimal labUsageRate(Long departmentId, Long labId, String labType, LocalDate startDate, LocalDate endDate) {
        long occupied = adminStatisticsMapper.countLabUsageOccupiedSlots(departmentId, labId, trimToNull(labType), startDate, endDate);
        long totalOpenSlots = totalOpenSlots(departmentId, labId, startDate, endDate, trimToNull(labType));
        return totalOpenSlots == 0 ? BigDecimal.ZERO : rate(occupied, totalOpenSlots);
    }

    private List<RankItemVo> labTypeRates(Long departmentId, Long labId, String selectedLabType, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> labTypes = adminStatisticsMapper.selectUsageLabTypes(departmentId, labId, trimToNull(selectedLabType));
        return labTypes.stream()
            .map(row -> {
                String labType = Objects.toString(row.get("labType"), "UNCLASSIFIED");
                long occupied = adminStatisticsMapper.countLabTypeOccupied(departmentId, labId, labType, startDate, endDate);
                long total = totalOpenSlots(departmentId, labId, startDate, endDate, labType);
                return new RankItemVo(null, labTypeLabel(labType), "预约使用率", occupied, total == 0 ? BigDecimal.ZERO : rate(occupied, total));
            })
            .sorted((left, right) -> right.getRate().compareTo(left.getRate()))
            .toList();
    }

    private long totalOpenSlots(Long departmentId, Long labId, LocalDate startDate, LocalDate endDate, String labType) {
        Map<Integer, Long> openSlotsByWeekday = new LinkedHashMap<>();
        for (Map<String, Object> row : adminStatisticsMapper.selectOpenSlotWeekdayCounts(departmentId, labId, labType)) {
            openSlotsByWeekday.put((int) number(row.get("weekday")), number(row.get("value")));
        }

        long total = 0L;
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            total += openSlotsByWeekday.getOrDefault(date.getDayOfWeek().getValue(), 0L);
        }
        if (total > 0) {
            return total;
        }

        long labCount = adminStatisticsMapper.countOpenSlotLabCount(departmentId, labId, labType);
        long periodCount = adminStatisticsMapper.countActiveClassPeriods();
        long days = endDate.toEpochDay() - startDate.toEpochDay() + 1;
        return labCount * periodCount * Math.max(days, 0L);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal rate(long value, long total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private long number(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private String cell(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.format(CELL_TIME_FORMATTER);
        }
        if (value instanceof LocalDate date) {
            return date.toString();
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        return Objects.toString(value, "");
    }

    private String yesNo(Number value) {
        return value != null && value.intValue() > 0 ? "是" : "否";
    }

    private String preferText(String primary, String fallback) {
        String normalizedPrimary = trimToNull(primary);
        return normalizedPrimary != null ? normalizedPrimary : Objects.toString(fallback, "");
    }

    private void appendChartRows(List<List<Object>> target, String section, List<ChartItemVo> items, String notePrefix) {
        for (ChartItemVo item : items) {
            target.add(List.of(section, item.getName(), notePrefix, cell(item.getValue()), formatPercent(item.getRate())));
        }
    }

    private void appendRankRows(List<List<Object>> target, String section, List<RankItemVo> items, String notePrefix, boolean includeSecondary) {
        for (RankItemVo item : items) {
            String note = includeSecondary && trimToNull(item.getSecondary()) != null
                ? notePrefix + " / " + item.getSecondary()
                : notePrefix;
            target.add(List.of(section, item.getName(), note, cell(item.getValue()), formatPercent(item.getRate())));
        }
    }
    private void ensureExportWithinLimit(long count) {
        if (count > EXPORT_LIMIT) {
            throw new BusinessException("单次导出最多支持 " + EXPORT_LIMIT + " 条，请缩小筛选范围");
        }
    }

    private String formatPercent(BigDecimal value) {
        return value == null ? "" : value.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }
    private String reservationStatusLabel(Object value) {
        int status = (int) number(value);
        return switch (status) {
            case 1 -> "待审核";
            case 2 -> "已通过";
            case 3 -> "已驳回";
            case 4 -> "已取消";
            case 5 -> "已完成";
            default -> "未知状态";
        };
    }

    private String reservationTypeLabel(Object value) {
        int type = (int) number(value);
        return switch (type) {
            case 1 -> "课程教学";
            case 2 -> "科研训练";
            case 3 -> "个人预约";
            default -> "未分类";
        };
    }

    private String roleLabel(Object value) {
        String role = Objects.toString(value, "");
        return switch (role) {
            case "TEACHER" -> "教师";
            case "STUDENT" -> "学生";
            default -> role;
        };
    }

    private String labTypeLabel(Object value) {
        String labType = Objects.toString(value, "");
        return "UNCLASSIFIED".equals(labType) ? "未分类" : labType;
    }

    private String deviceStatusLabel(int status) {
        return switch (status) {
            case 1 -> "正常";
            case 2 -> "维修中";
            case 3 -> "停用";
            default -> "异常";
        };
    }

    private String deviceCategoryLabel(Object value) {
        String label = Objects.toString(value, "");
        return "OTHER_BRAND".equals(label) ? "其他品牌" : label;
    }

    private String reportStatusLabel(Object value) {
        int status = (int) number(value);
        return switch (status) {
            case 1 -> "草稿";
            case 2 -> "待审核";
            case 3 -> "已通过";
            case 4 -> "已退回";
            default -> "未知状态";
        };
    }

    private String violationTypeLabel(Object value) {
        int type = (int) number(value);
        return switch (type) {
            case 1 -> "爽约";
            case 2 -> "迟到";
            case 3 -> "其他";
            case 4 -> "其他";
            case 5 -> "临近取消";
            default -> "未分类";
        };
    }

    private String creditScoreRangeLabel(Object value) {
        String label = Objects.toString(value, "");
        return switch (label) {
            case "LT60" -> "60分以下";
            case "60_69" -> "60-69";
            case "70_79" -> "70-79";
            case "80_89" -> "80-89";
            case "GTE90" -> "90分及以上";
            default -> label;
        };
    }

    private String timeSegmentLabel(Object value) {
        String label = Objects.toString(value, "");
        return switch (label) {
            case "MORNING" -> "上午";
            case "AFTERNOON" -> "下午";
            case "EVENING" -> "晚间";
            default -> label;
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(LocalDate startDate, LocalDate endDate) {
    }

    private record ExportData(String title, List<String> headers, List<List<Object>> rows) {
    }

    @FunctionalInterface
    private interface LabelResolver {
        String resolve(Object value);
    }
}
