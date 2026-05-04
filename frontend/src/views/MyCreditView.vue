<template>
  <section class="card-grid metrics-grid">
    <article class="metric-card brand">
      <span>当前信用分</span>
      <strong>{{ profile?.creditScore ?? '--' }}</strong>
      <small>基于预约表现情况自动变更</small>
    </article>
    <article class="metric-card warning">
      <span>违纪次数</span>
      <strong>{{ profile?.violationCount ?? '--' }}</strong>
      <small>多次违纪将会降低预约优先级</small>
    </article>
    <article class="metric-card success">
      <span>最新得分</span>
      <strong>{{ latestScoreChange }}</strong>
      <small>点击记录可查看具体加分或扣分原因</small>
    </article>
    <article class="metric-card accent">
      <span>记录总数</span>
      <strong>{{ violationState.total }}</strong>
      <small>当前用户可查看自己的违纪详情</small>
    </article>
  </section>

  <section class="content-grid credit-layout">
    <BasePanel tag="违纪记录">
      <div class="credit-toolbar-wrap">
        <div class="credit-toolbar-top">
          <h3>我的违纪记录列表</h3>
        </div>
        <div class="credit-toolbar-row">
          <div class="credit-toolbar">
            <select v-model="typeFilter" class="credit-filter-select">
              <option value="">全部类型</option>
              <option value="1">爽约</option>
              <option value="2">迟到</option>
              <option value="5">临近取消</option>
            </select>

            <select v-model="scoreDirectionFilter" class="credit-filter-select">
              <option value="">全部分值</option>
              <option value="-1">扣分记录</option>
              <option value="1">加分记录</option>
            </select>
          </div>
        </div>
      </div>

      <BaseTable :headers="['时间', '记录类型', '分值变化', '摘要']">
        <tr v-if="loading">
          <td colspan="4" class="table-empty">记录加载中...</td>
        </tr>
        <tr v-else-if="!violationState.list.length">
          <td colspan="4" class="table-empty">暂无符合条件的信用记录。</td>
        </tr>
        <tr
          v-for="item in violationState.list"
          v-else
          :key="item.id"
          class="credit-record-row"
          @click="openViolationDetail(item)"
        >
          <td>{{ formatDateTime(item.createdAt) }}</td>
          <td>{{ violationTypeText(item.violationType) }}</td>
          <td :class="scoreChangeClass(item.scoreChange)">{{ scoreChangeText(item.scoreChange) }}</td>
          <td>{{ remarkPreview(item.remark) }}</td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共 {{ violationState.total }} 条</span>
        <button type="button" class="ghost-btn small-btn" :disabled="currentPage <= 1 || loading" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <span class="pagination-text">第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="currentPage >= totalPages || loading"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>
  </section>

  <div v-if="detailVisible && selectedViolation" class="detail-dialog-mask" @click.self="closeViolationDetail">
    <div class="detail-dialog credit-detail-dialog" role="dialog" aria-modal="true" aria-labelledby="credit-detail-title">
      <div class="detail-dialog-head">
        <div>
          <span class="dialog-tag">信用记录</span>
          <h3 id="credit-detail-title">{{ scoreTitle(selectedViolation.scoreChange) }}</h3>
        </div>
        <button type="button" class="dialog-close" @click="closeViolationDetail">关闭</button>
      </div>

      <div class="detail-stack">
        <div class="detail-card">
          <h4>{{ violationTypeText(selectedViolation.violationType) }}</h4>
          <p>{{ formatDateTime(selectedViolation.createdAt) }}</p>
        </div>

        <div class="detail-list">
          <div><strong>分值变化</strong><span :class="scoreChangeClass(selectedViolation.scoreChange)">{{ scoreChangeText(selectedViolation.scoreChange) }}</span></div>
          <div><strong>记录类型</strong><span>{{ violationTypeText(selectedViolation.violationType) }}</span></div>
          <div><strong>记录时间</strong><span>{{ formatDateTime(selectedViolation.createdAt) }}</span></div>
        </div>

        <div class="detail-section">
          <h5>{{ scoreReasonTitle(selectedViolation.scoreChange) }}</h5>
          <p class="reason-text">{{ selectedViolation.remark || '暂无详细说明。' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { fetchMyProfile } from '../api/users';
import { fetchMyViolations } from '../api/violations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useAuthStore } from '../stores/auth';
import type { PageData, UserVO, ViolationRecordDto } from '../types';

const auth = useAuthStore();
const profile = ref<UserVO | null>(null);
const violationState = ref<PageData<ViolationRecordDto>>({
  list: [],
  total: 0,
  pageNum: 1,
  pageSize: 10,
});
const loading = ref(false);
const typeFilter = ref('');
const scoreDirectionFilter = ref('');
const currentPage = ref(1);
const selectedViolation = ref<ViolationRecordDto | null>(null);
const detailVisible = ref(false);

const totalPages = computed(() => Math.max(Math.ceil((violationState.value.total || 0) / violationState.value.pageSize), 1));

const latestScoreChange = computed(() => {
  const first = violationState.value.list[0];
  if (!first) {
    return '0';
  }
  return scoreChangeText(first.scoreChange);
});

function violationTypeText(type: number): string {
  if (type === 1) return '爽约';
  if (type === 2) return '迟到';
  if (type === 5) return '临近取消';
  return '其他';
}

function scoreChangeText(value: number): string {
  if (value > 0) return `+${value}`;
  if (value < 0) return `${value}`;
  return '0';
}

function scoreChangeClass(value: number): string {
  if (value > 0) return 'score-positive';
  if (value < 0) return 'score-negative';
  return 'score-neutral';
}

function scoreTitle(value: number): string {
  return value >= 0 ? '加分记录详情' : '扣分记录详情';
}

function scoreReasonTitle(value: number): string {
  return value >= 0 ? '本次加分原因' : '本次扣分原因';
}

function remarkPreview(remark: string): string {
  if (!remark) {
    return '暂无说明';
  }
  return remark.length > 20 ? `${remark.slice(0, 20)}...` : remark;
}

function formatDateTime(value?: string): string {
  if (!value) {
    return '--';
  }
  return value.replace('T', ' ');
}

async function loadProfile(): Promise<void> {
  profile.value = await fetchMyProfile(auth.token.value);
}

async function loadViolations(pageNum = currentPage.value): Promise<void> {
  loading.value = true;
  try {
    currentPage.value = Math.max(pageNum, 1);
    const data = await fetchMyViolations(auth.token.value, {
      pageNum: currentPage.value,
      pageSize: violationState.value.pageSize,
      violationType: typeFilter.value ? Number(typeFilter.value) : undefined,
      scoreDirection: scoreDirectionFilter.value ? Number(scoreDirectionFilter.value) : undefined,
    });
    violationState.value = data;
    currentPage.value = data.pageNum || currentPage.value;
  } finally {
    loading.value = false;
  }
}

function changePage(pageNum: number): void {
  if (pageNum < 1 || pageNum > totalPages.value || pageNum === currentPage.value) {
    return;
  }
  void loadViolations(pageNum);
}

function openViolationDetail(item: ViolationRecordDto): void {
  selectedViolation.value = item;
  detailVisible.value = true;
}

function closeViolationDetail(): void {
  detailVisible.value = false;
}

watch([typeFilter, scoreDirectionFilter], () => {
  void loadViolations(1);
});

onMounted(async () => {
  await Promise.all([loadProfile(), loadViolations(1)]);
});
</script>

<style scoped>
.credit-layout {
  align-items: start;
}

.credit-toolbar-wrap {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.credit-toolbar-top {
  display: flex;
  justify-content: flex-start;
}

.credit-toolbar-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12px;
}

.credit-toolbar-top h3 {
  margin: 0;
  font-size: 28px;
  line-height: 1.15;
  color: #0f172a;
}

.credit-toolbar {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.credit-filter-select {
  min-width: 164px;
  height: 42px;
  padding: 0 42px 0 14px;
  border: 1px solid #dbe4ee;
  border-radius: 16px;
  background: #fff;
  color: #0f172a;
  box-shadow: none;
  font-size: 14px;
}

.credit-filter-select:focus {
  outline: none;
  border-color: rgba(37, 99, 235, 0.44);
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.08);
}

.credit-record-row {
  cursor: pointer;
  transition: background-color 0.18s ease, transform 0.18s ease;
}

.credit-record-row:hover {
  background: rgba(248, 250, 252, 0.9);
}

.credit-record-row td {
  vertical-align: middle;
}

.table-empty {
  padding: 28px 14px;
  text-align: center;
  color: #64748b;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.pagination-total {
  margin-right: auto;
  color: rgba(15, 23, 42, 0.72);
}

.pagination-text {
  color: rgba(15, 23, 42, 0.8);
}

.score-positive {
  color: #059669;
  font-weight: 700;
}

.score-negative {
  color: #dc2626;
  font-weight: 700;
}

.score-neutral {
  color: #475569;
  font-weight: 700;
}

.detail-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(2, 6, 23, 0.45);
}

.detail-dialog {
  width: min(760px, 96vw);
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.42);
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 30px 80px rgba(15, 23, 42, 0.28);
}

.detail-dialog-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-tag {
  display: inline-flex;
  padding: 7px 14px;
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.14), rgba(147, 197, 253, 0.28));
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 700;
}

.detail-dialog-head h3 {
  margin: 12px 0 0;
  font-size: 34px;
  line-height: 1.08;
  color: #0f172a;
}

.dialog-close {
  border: 1px solid rgba(148, 163, 184, 0.24);
  background: rgba(255, 255, 255, 0.92);
  color: #334155;
  border-radius: 999px;
  padding: 10px 18px;
  cursor: pointer;
}

.detail-stack {
  display: grid;
  gap: 14px;
}

.detail-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 16px 18px;
  background: rgba(248, 250, 252, 0.9);
}

.detail-card h4 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.detail-card p {
  margin: 8px 0 0;
  color: #64748b;
}

.detail-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.detail-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 14px;
  background: rgba(248, 250, 252, 0.9);
}

.detail-list strong {
  color: #475569;
}

.detail-list span {
  text-align: right;
  color: #0f172a;
}

.detail-section {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  padding: 16px 18px;
  background: #ffffff;
}

.detail-section h5 {
  margin: 0 0 12px;
  font-size: 16px;
  color: #0f172a;
}

.reason-text {
  margin: 0;
  font-size: 16px;
  line-height: 1.7;
  color: #334155;
}

@media (max-width: 900px) {
  .credit-toolbar-row {
    justify-content: flex-start;
  }

  .credit-toolbar {
    justify-content: flex-start;
  }

  .detail-list {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .credit-filter-select {
    width: 100%;
  }

  .credit-toolbar {
    width: 100%;
  }

  .detail-dialog {
    padding: 18px;
    border-radius: 20px;
  }

  .detail-dialog-head h3 {
    font-size: 24px;
  }
}
</style>
