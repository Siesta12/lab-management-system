<template>
  <section class="content-grid split-grid">
    <BasePanel tag="预约管理" title="预约单列表" :note="`共 ${state.total} 条`">
      <div class="toolbar">
        <select v-model="statusFilter">
          <option value="">全部状态</option>
          <option value="1">待审批</option>
          <option value="2">已通过</option>
          <option value="3">已驳回</option>
          <option value="4">已取消</option>
          <option value="5">已完成</option>
        </select>
        <button type="button" class="ghost-btn" @click="loadPage">刷新</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="['预约编号', '实验室', '申请人', '节次明细', '状态']">
        <tr v-for="item in filteredList" :key="item.id" class="clickable-row" @click="select(item.id)">
          <td>{{ item.reservationNo }}</td>
          <td>{{ labName(item.labId) }}</td>
          <td>#{{ item.applicantUserId }}</td>
          <td>{{ slotSummary(item) }}</td>
          <td><span :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span></td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="审批" title="预约详情与审批操作" panel-class="detail-panel">
      <div v-if="selected" class="detail-stack">
        <div class="detail-card">
          <h4>{{ labName(selected.labId) }}</h4>
          <p>{{ selected.reservationNo }}</p>
        </div>

        <div class="detail-list">
          <div><strong>状态</strong><span>{{ statusText(selected.status) }}</span></div>
          <div><strong>类型</strong><span>{{ typeText(selected.reservationType) }}</span></div>
          <div><strong>优先级</strong><span>{{ priorityText(selected.priorityLevel) }}</span></div>
          <div><strong>用途</strong><span>{{ selected.usagePurpose }}</span></div>
          <div><strong>课程/项目</strong><span>{{ selected.courseOrProjectName || '未填写' }}</span></div>
          <div><strong>参与人数</strong><span>{{ selected.participantCount }} 人</span></div>
          <div><strong>联系电话</strong><span>{{ selected.contactPhone || '未填写' }}</span></div>
          <div><strong>驳回原因</strong><span>{{ selected.rejectReason || '无' }}</span></div>
        </div>

        <div class="detail-section">
          <h5>节次明细</h5>
          <ul class="bullet-list compact-list">
            <li v-for="slot in selected.slots" :key="slot.id">
              {{ slot.reservationDate }} · {{ slot.periodName }} · {{ slot.slotStatus === 1 ? '占用' : '已取消' }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h5>审批操作</h5>
          <div class="stack-form">
            <label class="wide">
              <span>备注（可选）</span>
              <input v-model="auditComment" placeholder="例如：请注意设备使用规范" />
            </label>
            <label class="wide">
              <span>驳回原因（驳回时必填）</span>
              <input v-model="rejectReason" placeholder="例如：节次不开放/人数超限/维护中" />
            </label>
            <div class="button-row">
              <button type="button" class="primary-btn wide" :disabled="selected.status !== 1" @click="handleApprove">通过</button>
              <button type="button" class="ghost-btn wide" :disabled="selected.status !== 1" @click="handleReject">驳回</button>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h5>审核日志</h5>
          <ul class="bullet-list compact-list">
            <li v-for="log in logs" :key="log.id">
              {{ log.createdAt }} · {{ auditActionText(log.auditAction) }} · {{ log.auditComment || '无备注' }}
            </li>
          </ul>
        </div>
      </div>

      <p v-else class="info-text">请选择一条预约单进行审批。</p>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { fetchReservationAuditLogs } from '../api/reservationAuditLogs';
import { approveReservation, fetchReservationById, fetchReservations, rejectReservation } from '../api/reservations';
import { fetchLabs } from '../api/labs';
import { useAuthStore } from '../stores/auth';
import type { LabDto, ReservationAuditLogDto, ReservationDto } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const message = ref('');
const statusFilter = ref('');
const state = ref<{ list: ReservationDto[]; total: number }>({ list: [], total: 0 });
const selected = ref<ReservationDto | null>(null);
const logs = ref<ReservationAuditLogDto[]>([]);
const labs = ref<LabDto[]>([]);
const auditComment = ref('');
const rejectReason = ref('');

const filteredList = computed(() => {
  if (!statusFilter.value) return state.value.list;
  return state.value.list.filter((r) => String(r.status) === statusFilter.value);
});

function labName(id: number): string {
  return labs.value.find((l) => l.id === id)?.labName ?? `实验室#${id}`;
}

function slotSummary(item: ReservationDto): string {
  const parts = item.slots?.map((s) => `${s.reservationDate} ${s.periodName}`) ?? [];
  if (parts.length <= 2) return parts.join('；') || '--';
  return `${parts.slice(0, 2).join('；')}…（共${parts.length}条）`;
}

function statusText(status: number): '待审批' | '已通过' | '已驳回' | '已取消' | '已完成' {
  if (status === 2) return '已通过';
  if (status === 3) return '已驳回';
  if (status === 4) return '已取消';
  if (status === 5) return '已完成';
  return '待审批';
}

function typeText(type: number): string {
  if (type === 1) return '课程预约';
  if (type === 2) return '科研预约';
  return '个人预约';
}

function priorityText(value: number): string {
  if (value === 1) return '高';
  if (value === 2) return '中';
  return '低';
}

function auditActionText(action: number): string {
  if (action === 1) return '提交申请';
  if (action === 2) return '审批通过';
  if (action === 3) return '审批驳回';
  if (action === 4) return '取消预约';
  if (action === 5) return '完成/签退';
  return '状态变更';
}

async function loadPage(): Promise<void> {
  try {
    const [page, labData] = await Promise.all([
      fetchReservations({ pageNum: 1, pageSize: 30 }, auth.token.value),
      fetchLabs({ pageNum: 1, pageSize: 200 }, auth.token.value),
    ]);
    state.value = page;
    labs.value = labData.list;
    message.value = '已加载预约单列表。';
    if (page.list.length && !selected.value) {
      await select(page.list[0].id);
    }
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载失败。';
  }
}

async function select(id: number): Promise<void> {
  try {
    const [detail, auditLogs] = await Promise.all([
      fetchReservationById(id, auth.token.value),
      fetchReservationAuditLogs(id, auth.token.value),
    ]);
    selected.value = detail;
    logs.value = auditLogs;
    auditComment.value = '';
    rejectReason.value = '';
  } catch (error) {
    message.value = error instanceof Error ? error.message : '加载预约详情失败。';
  }
}

async function handleApprove(): Promise<void> {
  if (!selected.value) return;
  try {
    await approveReservation(selected.value.id, auth.token.value, auditComment.value || undefined);
    message.value = '已审批通过。';
    selected.value = null;
    await loadPage();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '审批失败。';
  }
}

async function handleReject(): Promise<void> {
  if (!selected.value) return;
  if (!rejectReason.value.trim()) {
    message.value = '驳回原因必填。';
    return;
  }
  try {
    await rejectReservation(selected.value.id, auth.token.value, rejectReason.value.trim(), auditComment.value || undefined);
    message.value = '已驳回。';
    selected.value = null;
    await loadPage();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '驳回失败。';
  }
}

onMounted(() => {
  void loadPage();
});
</script>

