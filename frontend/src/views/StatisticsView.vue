<template>
  <section class="hero-panel statistics-hero">
    <div>
      <span class="eyebrow">STATISTICS</span>
      <h2>统计分析</h2>
      <p>基于当前管理员所属学院，查看预约、实验室、设备、耗材和信用违规的聚合统计。</p>
    </div>
  </section>

  <section class="panel statistics-filter-panel">
    <div class="quick-range-row">
      <button
        v-for="item in quickRanges"
        :key="item.value"
        class="range-btn"
        :class="{ active: activeRange === item.value }"
        type="button"
        @click="applyQuickRange(item.value, true)"
      >
        {{ item.label }}
      </button>
    </div>

    <div class="statistics-filters">
      <label>
        <span>开始日期</span>
        <input v-model="filters.startDate" type="date" @change="handleFilterChange" />
      </label>
      <label>
        <span>结束日期</span>
        <input v-model="filters.endDate" type="date" @change="handleFilterChange" />
      </label>
      <label>
        <span>实验室类型</span>
        <select v-model="filters.labType" @change="handleLabTypeChange">
          <option value="">全部类型</option>
          <option v-for="type in labTypeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
      </label>
      <label>
        <span>具体实验室</span>
        <select v-model="filters.labId" @change="loadStatistics">
          <option value="">全部实验室</option>
          <option v-for="lab in filteredLabOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
        </select>
      </label>
      <label>
        <span>预约状态</span>
        <select v-model="filters.status" @change="loadStatistics">
          <option value="">全部状态</option>
          <option v-for="item in options.reservationStatuses" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <label>
        <span>预约类型</span>
        <select v-model="filters.reservationType" @change="loadStatistics">
          <option value="">全部类型</option>
          <option v-for="item in options.reservationTypes" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>
      <div class="filter-actions">
        <button class="ghost-btn" type="button" :disabled="loading" @click="resetFilters">重置</button>
      </div>
    </div>
  </section>

  <p v-if="errorText" class="error-text">{{ errorText }}</p>

  <section class="statistics-entry-grid">
    <button
      v-for="card in entryCards"
      :key="card.type"
      class="statistics-entry-card"
      type="button"
      @click="openDialog(card.type)"
    >
      <div class="entry-card-head">
        <div>
          <span>{{ card.tag }}</span>
          <h3>{{ card.title }}</h3>
        </div>
        <b>{{ card.actionText }}</b>
      </div>
      <p>{{ card.description }}</p>
      <div class="entry-metric-row">
        <div v-for="metric in card.metrics" :key="metric.label">
          <span>{{ metric.label }}</span>
          <strong>{{ metric.value }}</strong>
        </div>
      </div>
    </button>
  </section>

  <div v-if="dialogModule" class="statistics-dialog-mask" @click.self="closeDialog">
    <section
      class="statistics-dialog"
      role="dialog"
      aria-modal="true"
      tabindex="-1"
      @keydown.esc="closeDialog"
    >
      <header class="statistics-dialog-head">
        <div class="dialog-head-main">
          <span class="dialog-tag">{{ activeDialogMeta?.tag }}</span>
          <h3>{{ dialogTitle }}</h3>
          <p>{{ filterSummary }}</p>
        </div>
        <div class="dialog-head-actions">
          <button
            v-if="dialogHeaderActionLabel"
            class="ghost-btn"
            type="button"
            :disabled="exporting"
            @click="handleDialogHeaderAction"
          >
            {{ dialogHeaderActionLabel }}
          </button>
          <button class="dialog-close" type="button" @click="closeDialog">关闭</button>
        </div>
      </header>

      <div class="statistics-dialog-body">
        <template v-if="dialogModule === 'export'">
          <div class="export-choice-grid">
            <button
              v-for="item in exportChoices"
              :key="item.type"
              class="export-choice-card"
              type="button"
              :disabled="exporting"
              @click="exportByType(item.type)"
            >
              <span>{{ item.label }}</span>
              <strong>{{ exporting ? '正在导出...' : item.action }}</strong>
              <small>{{ item.description }}</small>
              <em>{{ item.fields }}</em>
            </button>
          </div>
          <p class="export-note">导出内容始终基于当前筛选条件和当前管理员所属学院范围。</p>
        </template>

        <template v-else-if="dialogView === 'overview'">
          <div class="dialog-summary-grid">
            <button
              v-for="metric in currentOverviewMetrics"
              :key="metric.label"
              class="dialog-summary-card"
              type="button"
            >
              <span>{{ metric.label }}</span>
              <strong>{{ metric.value }}</strong>
              <small>{{ metric.note }}</small>
            </button>
          </div>

          <div class="dialog-grid">
            <template v-if="dialogModule === 'reservation'">
              <BasePanel title="预约趋势">
                <BarChart
                  :items="reservationStats?.trend ?? []"
                  empty-text="暂无预约趋势数据"
                />
              </BasePanel>
              <BasePanel title="预约状态分布">
                <DonutList
                  :items="reservationStats?.statusDistribution ?? []"
                  empty-text="暂无预约状态数据"
                />
              </BasePanel>
              <BasePanel title="预约类型分布">
                <DonutList
                  :items="reservationStats?.typeDistribution ?? []"
                  empty-text="暂无预约类型数据"
                />
              </BasePanel>
              <BasePanel title="学生 / 教师预约对比">
                <DonutList
                  :items="reservationStats?.applicantRoleDistribution ?? []"
                  empty-text="暂无预约角色数据"
                />
              </BasePanel>
            </template>

            <template v-else-if="dialogModule === 'labUsage'">
              <BasePanel title="实验室类型使用率">
                <RateList
                  :items="topRanks(labUsageStats?.typeUsageRates)"
                  empty-text="暂无实验室类型使用率数据"
                />
              </BasePanel>
              <BasePanel title="时间段使用热度">
                <BarChart
                  :items="labUsageStats?.timeHeat ?? []"
                  empty-text="暂无时间段热度数据"
                />
              </BasePanel>
              <BasePanel title="高频实验室 Top 5">
                <RankList
                  :items="topRanks(labUsageStats?.highUsageLabs)"
                  unit="次"
                  empty-text="暂无高频实验室数据"
                />
              </BasePanel>
              <BasePanel title="空闲实验室 Top 5">
                <RankList
                  :items="topRanks(labUsageStats?.idleLabs)"
                  unit="次"
                  empty-text="暂无空闲实验室数据"
                />
              </BasePanel>
            </template>

            <template v-else-if="dialogModule === 'device'">
              <BasePanel title="设备状态分布">
                <DonutList
                  :items="deviceStats?.statusDistribution ?? []"
                  empty-text="暂无设备状态数据"
                />
              </BasePanel>
              <BasePanel :title="`${deviceStats?.categoryLabel || '品牌'}分布`">
                <DonutList
                  :items="deviceStats?.categoryDistribution ?? []"
                  empty-text="暂无设备品牌分布数据"
                />
              </BasePanel>
              <BasePanel title="各实验室设备数量 Top 5">
                <RankList
                  :items="topRanks(deviceStats?.labDeviceCounts)"
                  unit="台"
                  empty-text="暂无设备分布数据"
                />
              </BasePanel>
              <BasePanel title="报修数量趋势">
                <BarChart
                  :items="deviceStats?.repairTrend ?? []"
                  empty-text="暂无报修趋势数据"
                />
              </BasePanel>
            </template>

            <template v-else-if="dialogModule === 'consumable'">
              <BasePanel title="入库趋势">
                <BarChart
                  :items="consumableStats?.inTrend ?? []"
                  empty-text="暂无入库趋势数据"
                />
              </BasePanel>
              <BasePanel title="出库趋势">
                <BarChart
                  :items="consumableStats?.outTrend ?? []"
                  empty-text="暂无出库趋势数据"
                />
              </BasePanel>
              <BasePanel title="耗材消耗 Top 5">
                <RankList
                  :items="topRanks(consumableStats?.consumptionRanking)"
                  unit=""
                  empty-text="暂无耗材消耗数据"
                />
              </BasePanel>
              <BasePanel title="实验室耗材使用 Top 5">
                <RankList
                  :items="topRanks(consumableStats?.labUsageRanking)"
                  unit=""
                  empty-text="暂无实验室耗材使用数据"
                />
              </BasePanel>
            </template>

            <template v-else-if="dialogModule === 'credit'">
              <BasePanel title="违规类型分布">
                <DonutList
                  :items="creditStats?.violationTypeDistribution ?? []"
                  empty-text="暂无违规类型数据"
                />
              </BasePanel>
              <BasePanel title="违规趋势">
                <BarChart
                  :items="creditStats?.violationTrend ?? []"
                  empty-text="暂无违规趋势数据"
                />
              </BasePanel>
              <BasePanel title="信用分区间分布">
                <DonutList
                  :items="creditStats?.creditScoreDistribution ?? []"
                  empty-text="暂无信用分区间数据"
                />
              </BasePanel>
              <BasePanel title="信誉分最低 Top">
                <RankList
                  :items="topRanks(creditStats?.lowCreditUsers)"
                  unit="分"
                  empty-text="暂无低信誉分排行数据"
                />
              </BasePanel>
            </template>
          </div>
        </template>

        <template v-else>
          <div class="detail-toolbar">
            <div>
              <strong>{{ detailState?.breadcrumb }}</strong>
              <p>{{ detailState?.description }}</p>
            </div>
            <button class="ghost-btn" type="button" @click="backToOverview">返回概览</button>
          </div>

          <div v-if="detailLoading" class="empty-state">正在加载详情数据...</div>
          <template v-else-if="detailState">
            <div class="dialog-summary-grid">
              <article v-for="metric in detailState.metrics" :key="metric.label" class="dialog-summary-card static">
                <span>{{ metric.label }}</span>
                <strong>{{ metric.value }}</strong>
                <small>{{ metric.note }}</small>
              </article>
            </div>

            <div class="dialog-grid">
              <BasePanel v-for="panel in detailState.panels" :key="panel.title" :title="panel.title">
                <BarChart v-if="panel.kind === 'bar'" :items="panel.items" :empty-text="panel.emptyText" />
                <DonutList v-else-if="panel.kind === 'donut'" :items="panel.items" :empty-text="panel.emptyText" />
                <RankList
                  v-else-if="panel.kind === 'rank'"
                  :items="panel.items"
                  :unit="panel.unit || ''"
                  :empty-text="panel.emptyText"
                />
                <RateList v-else-if="panel.kind === 'rate'" :items="panel.items" :empty-text="panel.emptyText" />
                <div v-else class="empty-state">{{ panel.content }}</div>
              </BasePanel>
            </div>
          </template>
        </template>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, defineComponent, h, nextTick, onMounted, onUnmounted, reactive, ref, type PropType } from 'vue';
import {
  downloadStatisticsExport,
  fetchConsumableStatistics,
  fetchCreditStatistics,
  fetchDeviceStatistics,
  fetchLabUsageStatistics,
  fetchReservationStatistics,
  fetchStatisticsOptions,
  fetchStatisticsOverview,
  type ChartItem,
  type ConsumableStats,
  type CreditStats,
  type DeviceStats,
  type LabUsageStats,
  type OverviewStats,
  type RankItem,
  type ReservationStats,
  type StatisticsOptions,
  type StatisticsQuery,
} from '../api/statistics';
import { fetchLabs } from '../api/labs';
import BasePanel from '../components/BasePanel.vue';
import { useGlobalToast } from '../composables/useGlobalToast';
import { useAuthStore } from '../stores/auth';
import type { LabDto } from '../types';

type RangeKey = 'today' | 'month' | '7days' | '30days' | 'custom';
type DialogModule = 'reservation' | 'labUsage' | 'device' | 'consumable' | 'credit' | 'export';
type DialogView = 'overview' | 'detail';

interface EntryMetric {
  label: string;
  value: string;
}

interface EntryCard {
  type: DialogModule;
  tag: string;
  title: string;
  description: string;
  actionText: string;
  metrics: EntryMetric[];
}

interface DetailContext {
  kind: string;
  label: string;
  id?: string | number;
  value?: number;
  rate?: number;
  meta?: Record<string, string | number | undefined>;
}

interface DetailMetric {
  label: string;
  value: string;
  note: string;
}

type DetailPanel =
  | { kind: 'bar'; title: string; items: ChartItem[]; emptyText: string }
  | { kind: 'donut'; title: string; items: ChartItem[]; emptyText: string }
  | { kind: 'rank'; title: string; items: RankItem[]; emptyText: string; unit?: string }
  | { kind: 'rate'; title: string; items: RankItem[]; emptyText: string }
  | { kind: 'note'; title: string; content: string };

interface DetailState {
  breadcrumb: string;
  description: string;
  metrics: DetailMetric[];
  panels: DetailPanel[];
}

interface DialogMetricCard {
  label: string;
  value: string;
  note: string;
  context?: DetailContext;
}

const auth = useAuthStore();
const { showToast } = useGlobalToast();
const loading = ref(false);
const exporting = ref(false);
const detailLoading = ref(false);
const errorText = ref('');
const activeRange = ref<RangeKey>('month');
const dialogModule = ref<DialogModule | null>(null);
const dialogView = ref<DialogView>('overview');
const detailContext = ref<DetailContext | null>(null);
const detailState = ref<DetailState | null>(null);

const filters = reactive({
  startDate: '',
  endDate: '',
  labType: '',
  labId: '',
  status: '',
  reservationType: '',
});

const labCatalog = ref<LabDto[]>([]);

const options = reactive<StatisticsOptions>({
  labs: [],
  reservationStatuses: [],
  reservationTypes: [],
  exportTypes: [
    { label: '预约数据', value: 'reservation' },
    { label: '实验报告数据', value: 'experimentReport' },
    { label: '耗材统计数据', value: 'consumable' },
    { label: '违规/信用数据', value: 'creditViolation' },
  ],
});

const overviewStats = ref<OverviewStats | null>(null);
const reservationStats = ref<ReservationStats | null>(null);
const labUsageStats = ref<LabUsageStats | null>(null);
const deviceStats = ref<DeviceStats | null>(null);
const consumableStats = ref<ConsumableStats | null>(null);
const creditStats = ref<CreditStats | null>(null);

const quickRanges: Array<{ label: string; value: RangeKey }> = [
  { label: '今日', value: 'today' },
  { label: '本月', value: 'month' },
  { label: '近 7 天', value: '7days' },
  { label: '近 30 天', value: '30days' },
];

const exportChoices = [
  { type: 'reservation', label: '导出预约数据', action: '立即导出', description: '查看预约编号、实验室、申请人、状态和时间段。', fields: '字段摘要：预约编号、实验室、申请人、角色、类型、状态、时间' },
  { type: 'experimentReport', label: '导出实验报告数据', action: '立即导出', description: '查看报告编号、实验室、提交人、审核状态和时间。', fields: '字段摘要：报告编号、预约编号、实验室、提交人、状态、审核时间' },
  { type: 'consumable', label: '导出耗材统计数据', action: '立即导出', description: '查看库存、预警阈值、入库和出库统计。', fields: '字段摘要：耗材名称、实验室、库存、阈值、本月入库、本月出库' },
  { type: 'creditViolation', label: '导出信用 / 违规数据', action: '立即导出', description: '查看违规类型、扣分、发生时间和当前信用分。', fields: '字段摘要：姓名、学工号、角色、违规类型、扣分、时间、信用分' },
];

const entryCards = computed<EntryCard[]>(() => {
  const primaryStatus = topItem(reservationStats.value?.statusDistribution);
  const hotLab = labUsageStats.value?.highUsageLabs?.[0]?.name ?? '--';
  const deviceIssue = `${overviewStats.value?.brokenDeviceCount ?? 0} / ${deviceStats.value?.repairingCount ?? 0}`;
  const creditPair = `${creditStats.value?.monthLateCount ?? 0} / ${creditStats.value?.monthNoShowCount ?? 0}`;

  return [
    {
      type: 'reservation',
      tag: 'RESERVATION',
      title: '预约统计',
      description: '查看预约趋势、状态分布、类型分布和师生预约对比。',
      actionText: '查看详情',
      metrics: [
        { label: '总预约数', value: String(reservationStats.value?.totalCount ?? 0) },
        { label: '主要状态', value: primaryStatus },
      ],
    },
    {
      type: 'labUsage',
      tag: 'LAB USAGE',
      title: '实验室使用统计',
      description: '查看使用率、时间段热度和实验室使用 Top 5。',
      actionText: '查看详情',
      metrics: [
        { label: '使用率', value: `${formatNumber(labUsageStats.value?.usageRate ?? overviewStats.value?.labUsageRate ?? 0)}%` },
        { label: '高频实验室', value: hotLab },
      ],
    },
    {
      type: 'device',
      tag: 'DEVICE',
      title: '设备统计',
      description: '查看设备状态、品牌分布、报修趋势和实验室设备分布。',
      actionText: '查看详情',
      metrics: [
        { label: '设备总数', value: String(deviceStats.value?.totalCount ?? 0) },
        { label: '异常 / 维修', value: deviceIssue },
      ],
    },
    {
      type: 'consumable',
      tag: 'CONSUMABLE',
      title: '耗材统计',
      description: '查看低库存、出入库趋势、消耗 Top 5 和实验室使用情况。',
      actionText: '查看详情',
      metrics: [
        { label: '低库存耗材', value: String(consumableStats.value?.lowStockCount ?? 0) },
        { label: '本月出库', value: String(consumableStats.value?.monthOutQuantity ?? 0) },
      ],
    },
    {
      type: 'credit',
      tag: 'CREDIT',
      title: '信用 / 违规统计',
      description: '查看迟到、爽约、信用分区间和违规趋势分布。',
      actionText: '查看详情',
      metrics: [
        { label: '迟到 / 爽约', value: creditPair },
        { label: '低信用用户', value: String(creditStats.value?.lowCreditUserCount ?? 0) },
      ],
    },
    {
      type: 'export',
      tag: 'EXPORT',
      title: 'Excel 导出',
      description: '基于当前筛选条件导出预约、实验报告、耗材统计和信用违规数据。',
      actionText: '选择导出',
      metrics: [
        { label: '支持类型', value: '4 类' },
        { label: '当前范围', value: selectedLabName.value },
      ],
    },
  ];
});

const activeDialogMeta = computed(() => entryCards.value.find((item) => item.type === dialogModule.value));
const labTypeOptions = computed(() => Array.from(new Set(labCatalog.value.map((lab) => lab.labType).filter(Boolean) as string[])));
const filteredLabOptions = computed(() => labCatalog.value
  .filter((lab) => !filters.labType || lab.labType === filters.labType)
  .map((lab) => ({ label: lab.labName, value: String(lab.id) })));

const selectedLabName = computed(() => {
  if (!filters.labId) {
    return '全部实验室';
  }
  return filteredLabOptions.value.find((item) => item.value === String(filters.labId))?.label ?? '当前实验室';
});

const filterSummary = computed(() => {
  const range = filters.startDate && filters.endDate ? `${filters.startDate} 至 ${filters.endDate}` : '默认时间范围';
  const status = options.reservationStatuses.find((item) => item.value === String(filters.status))?.label ?? '全部状态';
  const type = options.reservationTypes.find((item) => item.value === String(filters.reservationType))?.label ?? '全部类型';
  const labType = filters.labType || '全部类型';
  return `${range} / ${labType} / ${selectedLabName.value} / ${status} / ${type}`;
});

const dialogTitle = computed(() => {
  if (dialogView.value === 'detail' && detailState.value) {
    return detailState.value.breadcrumb;
  }
  return activeDialogMeta.value?.title ?? '统计详情';
});

const dialogHeaderActionLabel = computed(() => {
  if (!dialogModule.value || dialogModule.value === 'export') {
    return '';
  }
  const exportMap: Partial<Record<DialogModule, string>> = {
    reservation: '导出预约数据',
    consumable: '导出耗材统计',
    credit: '导出信用 / 违规数据',
  };
  return exportMap[dialogModule.value] ?? 'Excel 导出';
});

const currentOverviewMetrics = computed<DialogMetricCard[]>(() => {
  if (dialogModule.value === 'reservation') {
    const stats = reservationStats.value;
    return [
      { label: '总预约数', value: String(stats?.totalCount ?? 0), note: '当前筛选结果下的预约总量' },
      { label: '待审核数', value: String(stats?.pendingCount ?? 0), note: '当前筛选范围内等待处理的预约' },
      { label: '已完成数', value: String(stats?.completedCount ?? 0), note: '当前筛选范围内已完成的预约' },
      { label: '完成率', value: `${formatNumber(stats?.completionRate ?? 0)}%`, note: '已完成预约占总预约比例' },
    ];
  }
  if (dialogModule.value === 'labUsage') {
    const stats = labUsageStats.value;
    return [
      { label: '实验室使用率', value: `${formatNumber(stats?.usageRate ?? 0)}%`, note: '当前筛选下预约使用率' },
      { label: '总占用节次数', value: String(stats?.totalOccupiedSlots ?? 0), note: '当前筛选下占用开放节次' },
      { label: '高频实验室数', value: String(stats?.highUsageLabCount ?? 0), note: '有预约记录的实验室数量' },
      { label: '空闲实验室数', value: String(stats?.idleLabCount ?? 0), note: '当前筛选下无预约记录的实验室数量' },
    ];
  }
  if (dialogModule.value === 'device') {
    const stats = deviceStats.value;
    return [
      { label: '设备总数', value: String(stats?.totalCount ?? 0), note: '当前学院设备总量' },
      { label: '正常设备', value: String(stats?.normalCount ?? 0), note: '可用状态设备' },
      { label: '维修中', value: String(stats?.repairingCount ?? 0), note: '当前处于维修中的设备数量' },
      { label: '停用设备', value: String(stats?.disabledCount ?? 0), note: '当前处于停用状态的设备数量' },
    ];
  }
  if (dialogModule.value === 'consumable') {
    const stats = consumableStats.value;
    return [
      { label: '耗材总类数', value: String(stats?.totalTypeCount ?? 0), note: '当前筛选下耗材种类数量' },
      { label: '低库存耗材数', value: String(stats?.lowStockCount ?? 0), note: '达到库存预警线的耗材数量' },
      { label: '本月入库数量', value: String(stats?.monthInQuantity ?? 0), note: '当前统计期入库总量' },
      { label: '本月出库数量', value: String(stats?.monthOutQuantity ?? 0), note: '当前统计期出库总量' },
    ];
  }
  if (dialogModule.value === 'credit') {
    const stats = creditStats.value;
    return [
      { label: '本月迟到次数', value: String(stats?.monthLateCount ?? 0), note: '当前统计期迟到违规数量' },
      { label: '本月爽约次数', value: String(stats?.monthNoShowCount ?? 0), note: '当前统计期爽约违规数量' },
      { label: '平均信用分', value: formatNumber(stats?.averageCreditScore ?? 0), note: '当前学院用户平均信用分' },
      { label: '低信用用户数', value: String(stats?.lowCreditUserCount ?? 0), note: '信用分低于阈值的用户数量' },
    ];
  }
  return [];
});

const BarChart = defineComponent({
  props: {
    items: { type: Array as PropType<ChartItem[]>, required: true },
    emptyText: { type: String, required: true },
    onSelect: Function as PropType<(item: ChartItem) => void>,
  },
  setup(props) {
    return () => {
      const max = Math.max(1, ...props.items.map((item) => item.value));
      if (!props.items.length) {
        return h('div', { class: 'empty-state' }, props.emptyText);
      }
      return h('div', { class: 'vertical-bar-chart' }, props.items.map((item) => {
        const clickable = typeof props.onSelect === 'function';
        const height = Math.max(8, Math.round((item.value / max) * 100));
        return h(
          clickable ? 'button' : 'div',
          {
            class: ['vertical-bar-item', clickable && 'clickable-row'],
            key: item.name,
            type: clickable ? 'button' : undefined,
            onClick: clickable ? () => props.onSelect?.(item) : undefined,
          },
          [
            h('strong', item.value),
            h('div', { class: 'vertical-bar-track' }, [
              h('span', {
                class: 'vertical-bar-fill',
                style: {
                  height: `${height}%`,
                  background: 'linear-gradient(180deg, #2563eb, #14b8a6)',
                },
              }),
            ]),
            h('span', { class: 'vertical-bar-name', title: item.name }, compactChartName(item.name)),
          ],
        );
      }));
    };
  },
});

const DonutList = defineComponent({
  props: {
    items: { type: Array as PropType<ChartItem[]>, required: true },
    emptyText: { type: String, required: true },
    onSelect: Function as PropType<(item: ChartItem) => void>,
  },
  setup(props) {
    return () => {
      if (!props.items.length) {
        return h('div', { class: 'empty-state' }, props.emptyText);
      }
      const palette = ['#2563eb', '#14b8a6', '#f59e0b', '#ef4444', '#8b5cf6', '#64748b'];
      const total = Math.max(1, props.items.reduce((sum, item) => sum + item.value, 0));
      const radius = 46;
      const circumference = 2 * Math.PI * radius;
      let offset = 0;
      const top = [...props.items].sort((left, right) => right.value - left.value)[0];
      return h('div', { class: 'donut-chart-wrap' }, [
        h('div', { class: 'donut-visual-shell' }, [
          h('svg', { class: 'donut-svg', viewBox: '0 0 120 120', role: 'img' }, [
            h('circle', {
              cx: '60',
              cy: '60',
              r: String(radius),
              fill: 'none',
              stroke: 'rgba(148, 163, 184, 0.16)',
              'stroke-width': '18',
            }),
            ...props.items.map((item, index) => {
              const length = (item.value / total) * circumference;
              const segment = h('circle', {
                key: item.name,
                cx: '60',
                cy: '60',
                r: String(radius),
                fill: 'none',
                stroke: palette[index % palette.length],
                'stroke-width': '18',
                'stroke-linecap': 'round',
                'stroke-dasharray': `${Math.max(0, length - 2)} ${circumference}`,
                'stroke-dashoffset': String(-offset),
              });
              offset += length;
              return segment;
            }),
          ]),
          h('div', { class: 'donut-center' }, [
            h('strong', String(total)),
            h('span', '总量'),
          ]),
        ]),
        h('div', { class: 'donut-legend' }, props.items.map((item, index) =>
          h('div', { class: 'donut-legend-row', key: item.name }, [
            h('i', { style: { background: palette[index % palette.length] } }),
            h('span', item.name),
            h('strong', `${item.value} / ${formatNumber(item.rate || (item.value / total) * 100)}%`),
          ]),
        )),
        h('p', { class: 'chart-note' }, `占比最高：${top.name} ${formatNumber(top.rate || (top.value / total) * 100)}%`),
      ]);
    };
  },
});

const RankList = defineComponent({
  props: {
    items: { type: Array as PropType<RankItem[]>, required: true },
    unit: { type: String, default: '' },
    emptyText: { type: String, required: true },
    onSelect: Function as PropType<(item: RankItem) => void>,
  },
  setup(props) {
    return () => {
      if (!props.items.length) {
        return h('div', { class: 'empty-state' }, props.emptyText);
      }
      const max = Math.max(1, ...props.items.map((item) => item.value));
      return h('div', { class: 'rank-list' }, props.items.map((item, index) => {
        const clickable = typeof props.onSelect === 'function';
        return h(
          clickable ? 'button' : 'div',
          {
            class: ['rank-row', clickable && 'clickable-row'],
            key: `${item.name}-${index}`,
            type: clickable ? 'button' : undefined,
            onClick: clickable ? () => props.onSelect?.(item) : undefined,
          },
          [
            h('span', { class: 'rank-index' }, String(index + 1).padStart(2, '0')),
            h('div', { class: 'rank-main' }, [
              h('div', [h('strong', item.name), h('small', item.secondary || '--')]),
              h('div', { class: 'rank-track' }, [
                h('i', { style: { width: `${Math.max(5, Math.round((item.value / max) * 100))}%` } }),
              ]),
            ]),
            h('b', `${item.value}${props.unit}`),
          ],
        );
      }));
    };
  },
});

const RateList = defineComponent({
  props: {
    items: { type: Array as PropType<RankItem[]>, required: true },
    emptyText: { type: String, required: true },
    onSelect: Function as PropType<(item: RankItem) => void>,
  },
  setup(props) {
    return () => {
      if (!props.items.length) {
        return h('div', { class: 'empty-state' }, props.emptyText);
      }
      return h('div', { class: 'rate-list' }, props.items.map((item) => {
        const clickable = typeof props.onSelect === 'function';
        return h(
          clickable ? 'button' : 'div',
          {
            class: ['rate-row', clickable && 'clickable-row'],
            key: item.name,
            type: clickable ? 'button' : undefined,
            onClick: clickable ? () => props.onSelect?.(item) : undefined,
          },
          [
            h('div', [h('strong', item.name), h('small', `${item.value} 个占用节次`)]),
            h('div', { class: 'rate-track' }, [
              h('i', { style: { width: `${Math.max(4, Number(item.rate || 0))}%` } }),
            ]),
            h('b', `${formatNumber(item.rate || 0)}%`),
          ],
        );
      }));
    };
  },
});

function buildQuery(): StatisticsQuery {
  return {
    startDate: filters.startDate,
    endDate: filters.endDate,
    labType: filters.labType,
    labId: filters.labId,
    status: filters.status,
    reservationType: filters.reservationType,
  };
}

function formatDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function formatNumber(value: number): string {
  return Number(value || 0).toFixed(2).replace(/\.00$/, '');
}

function compactChartName(value: string): string {
  if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
    return value.slice(5);
  }
  return value.length > 8 ? `${value.slice(0, 8)}...` : value;
}

function sumChart(items?: ChartItem[]): number {
  return items?.reduce((sum, item) => sum + item.value, 0) ?? 0;
}

function topItem(items?: ChartItem[]): string {
  if (!items?.length) {
    return '--';
  }
  const top = [...items].sort((left, right) => right.value - left.value)[0];
  return `${top.name} ${top.value}`;
}

function topRanks(items?: RankItem[], size = 5): RankItem[] {
  return (items ?? []).slice(0, size);
}

function findItem(items: ChartItem[] | undefined, name: string): ChartItem | undefined {
  return items?.find((item) => item.name === name);
}

function selectedLabMeta(labId?: string | number): LabDto | undefined {
  return labCatalog.value.find((lab) => String(lab.id) === String(labId));
}

function applyQuickRange(range: RangeKey, autoLoad = false): void {
  activeRange.value = range;
  const today = new Date();
  const start = new Date(today);
  if (range === 'today') {
    filters.startDate = formatDate(today);
    filters.endDate = formatDate(today);
  } else if (range === 'month') {
    start.setDate(1);
    filters.startDate = formatDate(start);
    filters.endDate = formatDate(today);
  } else if (range === '7days') {
    start.setDate(today.getDate() - 6);
    filters.startDate = formatDate(start);
    filters.endDate = formatDate(today);
  } else if (range === '30days') {
    start.setDate(today.getDate() - 29);
    filters.startDate = formatDate(start);
    filters.endDate = formatDate(today);
  }
  if (autoLoad) {
    void loadStatistics();
  }
}

function resetFilters(): void {
  filters.labType = '';
  filters.labId = '';
  filters.status = '';
  filters.reservationType = '';
  applyQuickRange('month');
  void loadStatistics();
}

function handleFilterChange(): void {
  activeRange.value = 'custom';
  void loadStatistics();
}

function handleLabTypeChange(): void {
  filters.labId = '';
  void loadStatistics();
}

async function loadOptions(): Promise<void> {
  const data = await fetchStatisticsOptions(auth.token.value || '');
  options.labs = data.labs ?? [];
  options.reservationStatuses = data.reservationStatuses ?? [];
  options.reservationTypes = data.reservationTypes ?? [];
  options.exportTypes = data.exportTypes?.length ? data.exportTypes : options.exportTypes;
  try {
    const labs = await fetchLabs(
      { pageNum: 1, pageSize: 1000, departmentId: auth.currentUser.value?.departmentId ?? undefined },
      auth.token.value || undefined,
    );
    labCatalog.value = labs.list;
  } catch {
    labCatalog.value = options.labs.map((lab) => ({
      id: Number(lab.value),
      labName: lab.label,
      labCode: '',
      capacity: 0,
      openStatus: 1,
      labStatus: 1,
    }));
  }
}

async function loadStatistics(): Promise<void> {
  loading.value = true;
  errorText.value = '';
  const query = buildQuery();
  try {
    const token = auth.token.value || '';
    const [overview, reservations, labs, devices, consumables, credit] = await Promise.all([
      fetchStatisticsOverview(query, token),
      fetchReservationStatistics(query, token),
      fetchLabUsageStatistics(query, token),
      fetchDeviceStatistics(query, token),
      fetchConsumableStatistics(query, token),
      fetchCreditStatistics(query, token),
    ]);
    overviewStats.value = overview;
    reservationStats.value = reservations;
    labUsageStats.value = labs;
    deviceStats.value = devices;
    consumableStats.value = consumables;
    creditStats.value = credit;
    if (dialogView.value === 'detail' && detailContext.value) {
      await buildDetail(detailContext.value);
    }
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '统计数据加载失败';
  } finally {
    loading.value = false;
  }
}

function openDialog(type: DialogModule): void {
  dialogModule.value = type;
  dialogView.value = 'overview';
  detailContext.value = null;
  detailState.value = null;
  void nextTick(() => {
    document.querySelector<HTMLElement>('.statistics-dialog')?.focus();
  });
}

function closeDialog(): void {
  dialogModule.value = null;
  dialogView.value = 'overview';
  detailContext.value = null;
  detailState.value = null;
}

function backToOverview(): void {
  dialogView.value = 'overview';
  detailContext.value = null;
  detailState.value = null;
}

function handleGlobalKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape' && dialogModule.value) {
    closeDialog();
  }
}

function handleDialogHeaderAction(): void {
  if (!dialogModule.value) {
    return;
  }
  const exportMap: Partial<Record<DialogModule, string>> = {
    reservation: 'reservation',
    consumable: 'consumable',
    credit: 'creditViolation',
  };
  const exportType = exportMap[dialogModule.value];
  if (exportType) {
    void exportByType(exportType);
    return;
  }
  openDialog('export');
}

async function exportByType(type: string): Promise<void> {
  exporting.value = true;
  try {
    await downloadStatisticsExport({ ...buildQuery(), exportType: type }, auth.token.value || '');
    showToast('success', '导出已开始下载');
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '导出失败');
  } finally {
    exporting.value = false;
  }
}

async function buildDetail(context: DetailContext): Promise<void> {
  detailLoading.value = true;
  try {
    if (context.kind.startsWith('reservation-')) {
      detailState.value = await buildReservationDetail(context);
    } else if (context.kind.startsWith('lab-')) {
      detailState.value = await buildLabUsageDetail(context);
    } else if (context.kind.startsWith('device-')) {
      detailState.value = await buildDeviceDetail(context);
    } else if (context.kind.startsWith('consumable-')) {
      detailState.value = await buildConsumableDetail(context);
    } else if (context.kind.startsWith('credit-')) {
      detailState.value = await buildCreditDetail(context);
    } else {
      detailState.value = {
        breadcrumb: `${activeDialogMeta.value?.title ?? '统计详情'} > ${context.label}`,
        description: '当前项暂无可展示的二级聚合详情。',
        metrics: [],
        panels: [{ kind: 'note', title: '提示', content: '当前项暂无更多聚合维度。' }],
      };
    }
  } finally {
    detailLoading.value = false;
  }
}

async function buildReservationDetail(context: DetailContext): Promise<DetailState> {
  const baseQuery = buildQuery();
  let stats = reservationStats.value;
  if (context.kind === 'reservation-status') {
    stats = await fetchReservationStatistics({ ...baseQuery, status: Number(context.id) }, auth.token.value || '');
  } else if (context.kind === 'reservation-type') {
    stats = await fetchReservationStatistics({ ...baseQuery, reservationType: Number(context.id) }, auth.token.value || '');
  }
  const currentValue = context.value ?? context.meta?.value as number ?? stats?.totalCount ?? 0;
  const currentRate = context.rate ?? (stats?.totalCount && reservationStats.value?.totalCount ? (stats.totalCount / reservationStats.value.totalCount) * 100 : 0);
  return {
    breadcrumb: `预约统计 > ${context.label}`,
    description: '仅展示当前项的趋势、分布和占比，不展示具体预约记录。',
    metrics: [
      { label: '当前项数量', value: String(currentValue), note: '当前筛选结果下的聚合数量' },
      { label: '当前项占比', value: `${formatNumber(currentRate ?? 0)}%`, note: '占当前筛选范围总预约量' },
      { label: '趋势总量', value: String(sumChart(stats?.trend)), note: '趋势图中累计预约数量' },
      { label: '完成率', value: `${formatNumber(stats?.completionRate ?? 0)}%`, note: '当前明细范围完成率' },
    ],
    panels: [
      { kind: 'bar', title: '预约趋势', items: stats?.trend ?? [], emptyText: '暂无预约趋势数据' },
      { kind: 'donut', title: '实验室类型分布', items: stats?.labTypeDistribution ?? [], emptyText: '暂无实验室类型分布' },
      {
        kind: 'donut',
        title: context.kind === 'reservation-status' ? '预约类型分布' : '预约状态分布',
        items: context.kind === 'reservation-status' ? (stats?.typeDistribution ?? []) : (stats?.statusDistribution ?? []),
        emptyText: '暂无分布数据',
      },
      { kind: 'donut', title: '学生 / 教师占比', items: stats?.applicantRoleDistribution ?? [], emptyText: '暂无角色占比数据' },
    ],
  };
}

async function buildLabUsageDetail(context: DetailContext): Promise<DetailState> {
  const baseQuery = buildQuery();
  if (context.kind === 'lab-hot' || context.kind === 'lab-idle') {
    const labId = Number(context.id);
    const [overview, usage, reservation] = await Promise.all([
      fetchStatisticsOverview({ ...baseQuery, labId }, auth.token.value || ''),
      fetchLabUsageStatistics({ ...baseQuery, labId }, auth.token.value || ''),
      fetchReservationStatistics({ ...baseQuery, labId }, auth.token.value || ''),
    ]);
    const lab = selectedLabMeta(labId);
    const peakTime = topItem(usage.timeHeat);
    const idleTime = [...(usage.timeHeat ?? [])].sort((left, right) => left.value - right.value)[0]?.name ?? '--';
    return {
      breadcrumb: `实验室使用统计 > ${context.label}`,
      description: `${lab?.labType || '未设置类型'} / 容纳 ${lab?.capacity ?? 0} 人 / 学院 ${auth.currentUser.value?.departmentId ?? '--'}`,
      metrics: [
        { label: '预约次数', value: String(reservation.totalCount ?? 0), note: '当前实验室预约总数' },
        { label: '使用率', value: `${formatNumber(overview.labUsageRate ?? usage.usageRate ?? 0)}%`, note: '当前筛选条件下预约使用率' },
        { label: '占用节次数', value: String(usage.totalOccupiedSlots ?? 0), note: '当前实验室被占用节次数' },
        { label: '高峰 / 空闲时段', value: `${peakTime} / ${idleTime}`, note: '基于当前筛选结果统计' },
      ],
      panels: [
        { kind: 'bar', title: '近 7 / 30 天使用趋势', items: reservation.trend ?? [], emptyText: '暂无实验室使用趋势' },
        { kind: 'bar', title: '时间段占用分布', items: usage.timeHeat ?? [], emptyText: '暂无时间段占用分布' },
        { kind: 'donut', title: '预约类型分布', items: reservation.typeDistribution ?? [], emptyText: '暂无预约类型分布' },
        { kind: 'donut', title: '学生 / 教师使用占比', items: reservation.applicantRoleDistribution ?? [], emptyText: '暂无角色占比数据' },
      ],
    };
  }
  if (context.kind === 'lab-type') {
    const labType = String(context.id);
    const [overview, usage, reservation] = await Promise.all([
      fetchStatisticsOverview({ ...baseQuery, labType }, auth.token.value || ''),
      fetchLabUsageStatistics({ ...baseQuery, labType }, auth.token.value || ''),
      fetchReservationStatistics({ ...baseQuery, labType }, auth.token.value || ''),
    ]);
    const labCount = labCatalog.value.filter((lab) => (lab.labType || '未分类') === labType).length;
    return {
      breadcrumb: `实验室使用统计 > ${context.label}`,
      description: '仅展示该实验室类型的聚合趋势与占比，不展示具体预约记录。',
      metrics: [
        { label: '实验室数量', value: String(labCount), note: '该类型实验室数量' },
        { label: '总预约次数', value: String(reservation.totalCount ?? 0), note: '当前筛选下预约总量' },
        { label: '平均使用率', value: `${formatNumber(overview.labUsageRate ?? usage.usageRate ?? 0)}%`, note: '该类型实验室平均预约使用率' },
        { label: '总占用节次数', value: String(usage.totalOccupiedSlots ?? 0), note: '该类型占用开放节次数' },
      ],
      panels: [
        { kind: 'bar', title: '该类型使用趋势', items: reservation.trend ?? [], emptyText: '暂无使用趋势' },
        { kind: 'rank', title: '该类型实验室使用 Top 5', items: topRanks(usage.highUsageLabs), emptyText: '暂无实验室排行', unit: '次' },
        { kind: 'bar', title: '时间段使用热度', items: usage.timeHeat ?? [], emptyText: '暂无时间段热度' },
        { kind: 'donut', title: '预约类型分布', items: reservation.typeDistribution ?? [], emptyText: '暂无预约类型分布' },
      ],
    };
  }
  return {
    breadcrumb: `实验室使用统计 > ${context.label}`,
    description: '当前时段的详情以聚合分布展示，不展示具体预约人和预约记录。',
    metrics: [
      { label: '当前时段数量', value: String(context.value ?? 0), note: '该时间段在当前筛选下的占用次数' },
      { label: '当前项占比', value: `${formatNumber(context.rate ?? 0)}%`, note: '占全部时间段占用比例' },
      { label: '整体使用率', value: `${formatNumber(labUsageStats.value?.usageRate ?? 0)}%`, note: '当前筛选下实验室预约使用率' },
      { label: '高频实验室数', value: String(labUsageStats.value?.highUsageLabCount ?? 0), note: '有预约记录的实验室数量' },
    ],
    panels: [
      { kind: 'rate', title: '实验室类型使用率', items: topRanks(labUsageStats.value?.typeUsageRates, 5), emptyText: '暂无类型使用率数据' },
      { kind: 'rank', title: '高频实验室 Top 5', items: topRanks(labUsageStats.value?.highUsageLabs), emptyText: '暂无实验室排行', unit: '次' },
      { kind: 'donut', title: '预约类型分布', items: reservationStats.value?.typeDistribution ?? [], emptyText: '暂无预约类型分布' },
      { kind: 'donut', title: '学生 / 教师占比', items: reservationStats.value?.applicantRoleDistribution ?? [], emptyText: '暂无角色占比数据' },
    ],
  };
}

async function buildDeviceDetail(context: DetailContext): Promise<DetailState> {
  if (context.kind === 'device-lab') {
    const stats = await fetchDeviceStatistics({ ...buildQuery(), labId: Number(context.id) }, auth.token.value || '');
    const lab = selectedLabMeta(context.id);
    return {
      breadcrumb: `设备统计 > ${context.label}`,
      description: `${lab?.labType || '未设置类型'} / 容纳 ${lab?.capacity ?? 0} 人 / 学院 ${auth.currentUser.value?.departmentId ?? '--'}`,
      metrics: [
        { label: '设备总数', value: String(stats.totalCount ?? 0), note: '该实验室设备总量' },
        { label: '正常设备', value: String(stats.normalCount ?? 0), note: '当前可用设备数量' },
        { label: '维修中设备', value: String(stats.repairingCount ?? 0), note: '当前处于维修状态' },
        { label: '报修数量', value: String(stats.repairOrderCount ?? 0), note: '统计周期内报修次数' },
      ],
      panels: [
        { kind: 'donut', title: '设备状态分布', items: stats.statusDistribution ?? [], emptyText: '暂无设备状态分布' },
        { kind: 'donut', title: `${stats.categoryLabel || '品牌'}分布`, items: stats.categoryDistribution ?? [], emptyText: '暂无品牌分布数据' },
        { kind: 'bar', title: '报修数量趋势', items: stats.repairTrend ?? [], emptyText: '暂无报修趋势数据' },
        { kind: 'rank', title: '实验室设备数量 Top 5', items: topRanks(stats.labDeviceCounts), emptyText: '暂无设备排行', unit: '台' },
      ],
    };
  }
  return {
    breadcrumb: `设备统计 > ${context.label}`,
    description: '当前项仅展示聚合趋势、品牌分布和实验室分布，不展示具体设备列表。',
    metrics: [
      { label: '当前项数量', value: String(context.value ?? 0), note: '当前筛选结果下的设备数量' },
      { label: '当前项占比', value: `${formatNumber(context.rate ?? 0)}%`, note: '占全部设备的比例' },
      { label: '涉及实验室数量', value: String(countActiveRanks(deviceStats.value?.labDeviceCounts)), note: '当前筛选中涉及设备的实验室数量' },
      { label: '本月报修数量', value: String(deviceStats.value?.repairOrderCount ?? 0), note: '统计周期内累计报修数' },
    ],
    panels: [
      { kind: 'bar', title: '报修趋势', items: deviceStats.value?.repairTrend ?? [], emptyText: '暂无报修趋势数据' },
      { kind: 'donut', title: `${deviceStats.value?.categoryLabel || '品牌'}分布`, items: deviceStats.value?.categoryDistribution ?? [], emptyText: '暂无分布数据' },
      { kind: 'rank', title: '实验室分布 Top 5', items: topRanks(deviceStats.value?.labDeviceCounts), emptyText: '暂无实验室分布', unit: '台' },
      { kind: 'donut', title: '设备状态分布', items: deviceStats.value?.statusDistribution ?? [], emptyText: '暂无状态分布数据' },
    ],
  };
}

async function buildConsumableDetail(context: DetailContext): Promise<DetailState> {
  if (context.kind === 'consumable-lab') {
    const stats = await fetchConsumableStatistics({ ...buildQuery(), labId: Number(context.id) }, auth.token.value || '');
    const lab = selectedLabMeta(context.id);
    return {
      breadcrumb: `耗材统计 > ${context.label}`,
      description: `${lab?.labType || '未设置类型'} / 容纳 ${lab?.capacity ?? 0} 人 / 学院 ${auth.currentUser.value?.departmentId ?? '--'}`,
      metrics: [
        { label: '耗材总类数', value: String(stats.totalTypeCount ?? 0), note: '该实验室耗材种类数' },
        { label: '低库存数量', value: String(stats.lowStockCount ?? 0), note: '达到库存预警线的耗材数量' },
        { label: '本月入库', value: String(stats.monthInQuantity ?? 0), note: '统计周期内入库总量' },
        { label: '本月出库', value: String(stats.monthOutQuantity ?? 0), note: '统计周期内出库总量' },
      ],
      panels: [
        { kind: 'bar', title: '库存入库趋势', items: stats.inTrend ?? [], emptyText: '暂无入库趋势数据' },
        { kind: 'bar', title: '库存出库趋势', items: stats.outTrend ?? [], emptyText: '暂无出库趋势数据' },
        { kind: 'rank', title: '耗材消耗 Top 5', items: topRanks(stats.consumptionRanking), emptyText: '暂无耗材消耗排行' },
        { kind: 'rank', title: '实验室耗材使用 Top 5', items: topRanks(stats.labUsageRanking), emptyText: '暂无实验室耗材使用排行' },
      ],
    };
  }
  return {
    breadcrumb: `耗材统计 > ${context.label}`,
    description: '当前项仅展示库存变化趋势、入出库趋势和实验室分布，不展示完整耗材明细。',
    metrics: [
      { label: '当前项数量', value: String(context.value ?? 0), note: '当前筛选范围内的聚合数量' },
      { label: '当前项占比', value: `${formatNumber(context.rate ?? 0)}%`, note: '占全部耗材种类比例' },
      { label: '涉及实验室数量', value: String(countActiveRanks(consumableStats.value?.labUsageRanking)), note: '当前筛选下涉及耗材使用的实验室数量' },
      { label: '本月出库', value: String(consumableStats.value?.monthOutQuantity ?? 0), note: '统计期内出库总量' },
    ],
    panels: [
      { kind: 'bar', title: '库存入库趋势', items: consumableStats.value?.inTrend ?? [], emptyText: '暂无入库趋势数据' },
      { kind: 'bar', title: '库存出库趋势', items: consumableStats.value?.outTrend ?? [], emptyText: '暂无出库趋势数据' },
      { kind: 'rank', title: '实验室分布 Top 5', items: topRanks(consumableStats.value?.labUsageRanking), emptyText: '暂无实验室分布' },
      { kind: 'rank', title: '耗材消耗 Top 5', items: topRanks(consumableStats.value?.consumptionRanking), emptyText: '暂无耗材消耗排行' },
    ],
  };
}

async function buildCreditDetail(context: DetailContext): Promise<DetailState> {
  const stats = creditStats.value;
  const trend = context.kind === 'credit-late'
    ? (stats?.lateTrend ?? [])
    : context.kind === 'credit-no-show'
      ? (stats?.noShowTrend ?? [])
      : (stats?.violationTrend ?? []);
  const selectedRange = context.kind === 'credit-score-range' ? findItem(stats?.creditScoreDistribution, context.label) : undefined;
  return {
    breadcrumb: `信用 / 违规统计 > ${context.label}`,
    description: '当前项仅展示趋势、分布和占比，不展示姓名、学号和用户列表。',
    metrics: [
      { label: '当前项数量', value: String(context.value ?? selectedRange?.value ?? 0), note: '当前筛选下聚合数量' },
      { label: '当前项占比', value: `${formatNumber(context.rate ?? selectedRange?.rate ?? 0)}%`, note: '占当前筛选范围比例' },
      { label: '平均信用分', value: formatNumber(stats?.averageCreditScore ?? 0), note: '当前学院用户平均信用分' },
      { label: '较上期变化', value: `${sumChart(trend)} 次`, note: '当前趋势累计值，供横向比较' },
    ],
    panels: [
      { kind: 'bar', title: '趋势图', items: trend, emptyText: '暂无趋势数据' },
      { kind: 'donut', title: '时间段分布', items: stats?.timeSegmentDistribution ?? [], emptyText: '暂无时间段分布数据' },
      { kind: 'donut', title: '预约类型分布', items: stats?.reservationTypeDistribution ?? [], emptyText: '暂无预约类型分布' },
      { kind: 'rank', title: '信誉分最低 Top', items: topRanks(stats?.lowCreditUsers), unit: '分', emptyText: '暂无低信誉分排行数据' },
    ],
  };
}

function countActiveRanks(items?: RankItem[]): number {
  return (items ?? []).filter((item) => item.value > 0).length;
}

onMounted(async () => {
  applyQuickRange('month');
  window.addEventListener('keydown', handleGlobalKeydown);
  try {
    await loadOptions();
  } catch {
    showToast('error', '筛选项加载失败');
  }
  await loadStatistics();
});

onUnmounted(() => {
  window.removeEventListener('keydown', handleGlobalKeydown);
});
</script>

<style scoped>
.statistics-hero {
  align-items: center;
}

.statistics-filter-panel {
  padding: 18px;
  border-radius: 20px;
}

.quick-range-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.range-btn {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid var(--line);
  color: var(--muted);
}

.range-btn.active {
  background: rgba(37, 99, 235, 0.12);
  border-color: rgba(37, 99, 235, 0.28);
  color: var(--brand-deep);
  font-weight: 700;
}

.statistics-filters {
  display: grid;
  grid-template-columns: repeat(6, minmax(140px, 1fr)) auto;
  gap: 12px;
  align-items: end;
}

.statistics-filters label {
  display: grid;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
}

.statistics-filters input,
.statistics-filters select {
  min-height: 44px;
  padding: 10px 12px;
  border: 1px solid rgba(148, 163, 184, 0.34);
  border-radius: 14px;
  background-color: #f8fbff;
  color: var(--text);
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.statistics-entry-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.statistics-entry-card {
  min-height: 210px;
  display: grid;
  align-content: space-between;
  gap: 16px;
  padding: 22px;
  border-radius: var(--radius-lg);
  border: 1px solid var(--line);
  background: var(--panel);
  box-shadow: var(--shadow-soft);
  color: var(--text);
  text-align: left;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.statistics-entry-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 18px 34px rgba(15, 23, 42, 0.11);
}

.entry-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
}

.entry-card-head span,
.dialog-tag {
  display: inline-flex;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

.entry-card-head h3 {
  margin: 10px 0 0;
  font-size: 22px;
  line-height: 1.2;
}

.entry-card-head b {
  flex: 0 0 auto;
  padding: 7px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid var(--line);
  color: var(--brand-deep);
  font-size: 13px;
}

.statistics-entry-card p {
  margin: 0;
  color: var(--muted);
  line-height: 1.6;
}

.entry-metric-row,
.dialog-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.entry-metric-row div,
.dialog-summary-card {
  padding: 12px;
  border-radius: 14px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.entry-metric-row span,
.dialog-summary-card span {
  display: block;
  color: var(--muted);
  font-size: 12px;
}

.entry-metric-row strong,
.dialog-summary-card strong {
  display: block;
  margin-top: 6px;
  color: var(--text);
  font-size: 21px;
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-summary-card {
  text-align: left;
}

.dialog-summary-card small {
  display: block;
  margin-top: 6px;
  color: var(--muted);
  line-height: 1.5;
}

.dialog-summary-card.clickable {
  cursor: pointer;
}

.dialog-summary-card.clickable:hover {
  border-color: rgba(37, 99, 235, 0.3);
  box-shadow: 0 12px 22px rgba(15, 23, 42, 0.08);
}

.dialog-summary-card.static {
  cursor: default;
}

.statistics-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.42);
  backdrop-filter: blur(10px);
}

.statistics-dialog {
  width: min(1120px, calc(100vw - 48px));
  max-height: calc(100vh - 48px);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 30px 70px rgba(15, 23, 42, 0.24);
  overflow: hidden;
  outline: none;
}

.statistics-dialog-head {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 24px 18px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.98);
}

.dialog-head-main h3 {
  margin: 10px 0 6px;
  color: var(--text);
  font-size: 26px;
}

.dialog-head-main p {
  margin: 0;
  color: var(--muted);
}

.dialog-head-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.dialog-close {
  flex: 0 0 auto;
  padding: 9px 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: #fff;
  color: var(--text);
}

.statistics-dialog-body {
  overflow: auto;
  padding: 20px 24px 24px;
}

.dialog-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.detail-toolbar {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.detail-toolbar p {
  margin: 6px 0 0;
  color: var(--muted);
}

.vertical-bar-chart,
.donut-chart-wrap,
.rank-list,
.rate-list {
  display: grid;
  gap: 12px;
}

.rate-row,
.rank-row {
  border: 0;
  background: rgba(248, 250, 252, 0.84);
}

.rate-row {
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
}

.vertical-bar-chart {
  height: 272px;
  display: flex;
  align-items: end;
  gap: 12px;
  padding: 14px 4px 16px;
  overflow-x: auto;
  overflow-y: hidden;
}

.vertical-bar-item {
  flex: 1 0 48px;
  max-width: 72px;
  height: 242px;
  display: grid;
  grid-template-rows: 24px 158px 44px;
  gap: 8px;
  align-items: end;
  justify-items: center;
  border: 0;
  background: transparent;
  color: var(--text);
}

.vertical-bar-item strong {
  color: var(--text);
  font-size: 13px;
  line-height: 1;
}

.vertical-bar-track {
  position: relative;
  width: min(100%, 44px);
  height: 158px;
  border-radius: 14px 14px 8px 8px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.08), rgba(148, 163, 184, 0.16));
  overflow: hidden;
}

.vertical-bar-fill {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  width: 100%;
  min-height: 8px;
  display: block;
  border-radius: 14px 14px 4px 4px;
  background: linear-gradient(180deg, var(--brand), var(--accent));
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.2);
}

.vertical-bar-name {
  width: 100%;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.25;
  text-align: center;
  word-break: keep-all;
  padding-bottom: 2px;
}

.rank-track,
.rate-track {
  height: 12px;
  border-radius: 999px;
  overflow: hidden;
  background: rgba(148, 163, 184, 0.15);
}

.rank-track i,
.rate-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--brand), var(--accent));
}

.rate-row {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) 90px;
  gap: 12px;
  align-items: center;
}

.rate-row small {
  color: var(--muted);
  font-size: 13px;
}

.donut-chart-wrap {
  grid-template-columns: 174px minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  min-height: 210px;
}

.donut-visual-shell {
  position: relative;
  width: 156px;
  height: 156px;
  display: grid;
  place-items: center;
}

.donut-svg {
  width: 156px;
  height: 156px;
  display: block;
  transform: rotate(-90deg);
}

.donut-center {
  position: absolute;
  width: 88px;
  height: 88px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
}

.donut-center strong {
  color: var(--text);
  font-size: 22px;
  line-height: 1;
}

.donut-center span,
.chart-note {
  color: var(--muted);
  font-size: 12px;
}

.donut-legend {
  display: grid;
  gap: 10px;
}

.donut-legend-row {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.84);
}

.donut-legend-row i {
  width: 10px;
  aspect-ratio: 1;
  border-radius: 50%;
}

.donut-legend-row span {
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.donut-legend-row strong {
  color: var(--text);
  font-size: 13px;
}

.chart-note {
  grid-column: 1 / -1;
  margin: 0;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.06);
}

.rank-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) 74px;
  gap: 12px;
  align-items: center;
  width: 100%;
  padding: 12px 14px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.clickable-row {
  cursor: pointer;
}

.clickable-row:hover {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 10px 18px rgba(15, 23, 42, 0.08);
}

.rank-index {
  color: var(--brand-deep);
  font-weight: 800;
}

.rank-row strong,
.rate-row strong {
  display: block;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-main {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.rank-row small {
  display: block;
  margin-top: 4px;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-row b,
.rate-row b {
  color: var(--text);
  text-align: right;
}

.rate-row {
  grid-template-columns: 140px minmax(0, 1fr) 62px;
}

.empty-state {
  padding: 16px 18px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.82);
  border: 1px dashed rgba(148, 163, 184, 0.4);
  color: var(--muted);
  font-size: 13px;
}

.export-choice-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.export-choice-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid var(--line);
  background: rgba(248, 250, 252, 0.86);
  text-align: left;
  color: var(--text);
}

.export-choice-card span {
  color: var(--muted);
  font-size: 13px;
}

.export-choice-card strong {
  color: var(--brand-deep);
  font-size: 18px;
}

.export-choice-card small,
.export-choice-card em,
.export-note {
  color: var(--muted);
  line-height: 1.6;
}

.export-choice-card em {
  font-style: normal;
  font-size: 12px;
}

.export-note {
  margin: 16px 0 0;
}

:deep(.vertical-bar-chart) {
  height: 272px;
  display: flex;
  align-items: end;
  gap: 12px;
  padding: 14px 4px 16px;
  overflow-x: auto;
  overflow-y: hidden;
}

:deep(.vertical-bar-item) {
  flex: 1 0 48px;
  max-width: 72px;
  height: 242px;
  display: grid;
  grid-template-rows: 24px 158px 44px;
  gap: 8px;
  align-items: end;
  justify-items: center;
  border: 0;
  background: transparent;
  color: var(--text);
}

:deep(.vertical-bar-item strong) {
  color: var(--text);
  font-size: 13px;
  line-height: 1;
}

:deep(.vertical-bar-track) {
  position: relative;
  width: min(100%, 44px);
  height: 158px;
  border-radius: 14px 14px 8px 8px;
  background: linear-gradient(180deg, rgba(148, 163, 184, 0.08), rgba(148, 163, 184, 0.16));
  overflow: hidden;
}

:deep(.vertical-bar-fill) {
  position: absolute;
  inset-inline: 0;
  bottom: 0;
  width: 100%;
  min-height: 8px;
  display: block;
  border-radius: 14px 14px 4px 4px;
  box-shadow: 0 10px 18px rgba(37, 99, 235, 0.2);
}

:deep(.vertical-bar-name) {
  width: 100%;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.25;
  text-align: center;
  word-break: keep-all;
  padding-bottom: 2px;
}

:deep(.donut-chart-wrap) {
  display: grid;
  grid-template-columns: 174px minmax(0, 1fr);
  align-items: center;
  gap: 18px;
  min-height: 210px;
}

:deep(.donut-visual-shell) {
  position: relative;
  width: 156px;
  height: 156px;
  display: grid;
  place-items: center;
}

:deep(.donut-svg) {
  width: 156px;
  height: 156px;
  display: block;
  transform: rotate(-90deg);
}

:deep(.donut-center) {
  position: absolute;
  width: 88px;
  height: 88px;
  display: grid;
  place-items: center;
  align-content: center;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.1);
}

:deep(.donut-center strong) {
  color: var(--text);
  font-size: 22px;
  line-height: 1;
}

:deep(.donut-center span),
:deep(.chart-note) {
  color: var(--muted);
  font-size: 12px;
}

:deep(.donut-legend) {
  display: grid;
  gap: 10px;
}

:deep(.donut-legend-row) {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) auto;
  gap: 10px;
  align-items: center;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(248, 250, 252, 0.84);
}

:deep(.donut-legend-row i) {
  width: 10px;
  aspect-ratio: 1;
  display: block;
  border-radius: 50%;
}

:deep(.donut-legend-row span) {
  overflow: hidden;
  color: var(--muted);
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.donut-legend-row strong) {
  color: var(--text);
  font-size: 13px;
}

:deep(.chart-note) {
  grid-column: 1 / -1;
  margin: 0;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.06);
}

:deep(.rank-list),
:deep(.rate-list) {
  display: grid;
  gap: 14px;
}

:deep(.rank-row),
:deep(.rate-row) {
  width: 100%;
  min-height: 62px;
  display: grid;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(248, 250, 252, 0.86);
  color: var(--text);
}

:deep(.rank-row) {
  grid-template-columns: 34px minmax(0, 1fr) 68px;
}

:deep(.rate-row) {
  grid-template-columns: 150px minmax(0, 1fr) 66px;
}

:deep(.rank-index) {
  width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 10px;
  background: rgba(37, 99, 235, 0.1);
  color: var(--brand-deep);
  font-size: 12px;
  font-weight: 900;
}

:deep(.rank-main) {
  display: grid;
  gap: 9px;
  min-width: 0;
}

:deep(.rank-row strong),
:deep(.rate-row strong) {
  display: block;
  overflow: hidden;
  color: var(--text);
  font-size: 14px;
  font-weight: 800;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.rank-row small),
:deep(.rate-row small) {
  display: block;
  margin-top: 3px;
  overflow: hidden;
  color: var(--muted);
  font-size: 12px;
  line-height: 1.3;
  text-overflow: ellipsis;
  white-space: nowrap;
}

:deep(.rank-track),
:deep(.rate-track) {
  height: 14px;
  border-radius: 999px;
  overflow: hidden;
  background: linear-gradient(90deg, rgba(226, 232, 240, 0.9), rgba(241, 245, 249, 0.88));
  box-shadow: inset 0 1px 2px rgba(15, 23, 42, 0.08);
}

:deep(.rank-track i),
:deep(.rate-track i) {
  display: block;
  height: 100%;
  min-width: 8px;
  border-radius: inherit;
  background: linear-gradient(90deg, var(--accent), var(--brand));
  box-shadow: 0 6px 14px rgba(37, 99, 235, 0.2);
}

:deep(.rank-row b),
:deep(.rate-row b) {
  color: var(--text);
  font-size: 14px;
  font-weight: 900;
  line-height: 1.2;
  text-align: right;
}

@media (max-width: 1360px) {
  .statistics-filters {
    grid-template-columns: repeat(3, minmax(150px, 1fr));
  }

  .filter-actions {
    grid-column: span 3;
  }

  .statistics-entry-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 980px) {
  .dialog-grid,
  .export-choice-grid,
  .dialog-summary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .statistics-filters,
  .statistics-entry-grid {
    grid-template-columns: 1fr;
  }

  .filter-actions {
    grid-column: auto;
  }

  .statistics-dialog-mask {
    padding: 12px;
  }

  .statistics-dialog {
    width: calc(100vw - 24px);
    max-height: calc(100vh - 24px);
  }

  .statistics-dialog-head,
  .detail-toolbar {
    display: grid;
  }

  .rate-row {
    grid-template-columns: 1fr;
  }

  .donut-chart-wrap {
    grid-template-columns: 1fr;
    justify-items: center;
  }

  .donut-legend {
    width: 100%;
  }

  .vertical-bar-chart {
    height: 252px;
    padding-bottom: 16px;
  }

  :deep(.vertical-bar-chart) {
    height: 252px;
    padding-bottom: 16px;
  }
}
</style>

