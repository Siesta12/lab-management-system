<template>
  <section class="content-grid split-grid">
    <BasePanel tag="我的预约" title="预约记录列表" :note="`共${reservationState.total} 项`">
      <div class="toolbar">
        <select v-model="statusFilter">
          <option value="">全部状态</option>
          <option value="1">待审核</option>
          <option value="2">已通过</option>
          <option value="3">已驳回</option>
          <option value="4">已取消</option>
          <option value="5">已完成</option>
        </select>
        <button type="button" class="ghost-btn" @click="loadReservations">刷新列表</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="['预约编号', '实验室', '时段详情', '状态', '操作']">
        <tr v-for="item in filteredReservations" :key="item.id" class="clickable-row" @click="selectReservation(item.id)">
          <td>{{ item.reservationNo }}</td>
          <td>{{ labName(item.labId) }}</td>
          <td>{{ slotSummary(item) }}</td>
          <td><span :class="getBadgeClass(statusText(item.status))">{{ statusText(item.status) }}</span></td>
          <td>
            <button v-if="canCancel(item)" type="button" class="ghost-btn small-btn" @click.stop="handleCancel(item.id)">
              取消预约
            </button>
          </td>
        </tr>
      </BaseTable>
    </BasePanel>

    <BasePanel tag="预约详情" title="预约单信息与节次明细" panel-class="detail-panel">
      <div v-if="selectedReservation" class="detail-stack">
        <div class="detail-card">
          <h4>{{ labName(selectedReservation.labId) }}</h4>
          <p>{{ selectedReservation.reservationNo }}</p>
        </div>

        <div class="detail-list">
          <div><strong>状态</strong><span>{{ statusText(selectedReservation.status) }}</span></div>
          <div><strong>类型</strong><span>{{ reservationTypeText(selectedReservation.reservationType) }}</span></div>
          <div><strong>用途</strong><span>{{ selectedReservation.usagePurpose }}</span></div>
          <div><strong>课程/项目</strong><span>{{ selectedReservation.courseOrProjectName || '未填写' }}</span></div>
          <div><strong>参与人数</strong><span>{{ selectedReservation.participantCount }} 人</span></div>
          <div><strong>联系电话</strong><span>{{ selectedReservation.contactPhone || '未填写' }}</span></div>
          <div><strong>驳回原因</strong><span>{{ selectedReservation.rejectReason || '无' }}</span></div>
        </div>

        <div class="detail-section">
          <h5>节次明细</h5>
          <ul class="bullet-list compact-list">
            <li v-for="slot in selectedReservation.slots" :key="slot.id">
              {{ slot.reservationDate }} · {{ slot.periodName }} · {{ slot.slotStatus === 1 ? '占用' : '已取消' }}
            </li>
          </ul>
        </div>

        <div class="detail-section">
          <h5>审核日志</h5>
          <ul class="bullet-list compact-list">
            <li v-for="log in auditLogs" :key="log.id">
              {{ log.createdAt }} · {{ auditActionText(log.auditAction) }} · {{ log.auditComment || '无备注' }}
            </li>
          </ul>
        </div>
      </div>

      <p v-else class="info-text">请选择一条预约记录查看详情。</p>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { fetchReservationAuditLogs } from '../api/reservationAuditLogs';
import { cancelReservation, fetchMyReservations, fetchReservationById } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { fetchLabs } from '../api/labs';
import { useAuthStore } from '../stores/auth';
import type { LabDto, ReservationAuditLogDto, ReservationDto } from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const message = ref('');
const statusFilter = ref('');
const labs = ref<LabDto[]>([]);
const reservationState = ref<{ list: ReservationDto[]; total: number }>({ list: [], total: 0 });
const selectedReservation = ref<ReservationDto | null>(null);
const auditLogs = ref<ReservationAuditLogDto[]>([]);

const filteredReservations = computed(() => {
  if (!statusFilter.value) {
    return reservationState.value.list;
  }
  return reservationState.value.list.filter((item) => String(item.status) === statusFilter.value);
});

function labName(id: number): string {
  return labs.value.find((item) => item.id === id)?.labName ?? `实验室${id}`;
}

function slotSummary(item: ReservationDto): string {
  const parts = item.slots?.map((s) => `${s.reservationDate} ${s.periodName}`) ?? [];
  if (parts.length <= 2) return parts.join('，') || '--';
  return `${parts.slice(0, 2).join('，')}等（共${parts.length}项）`;
}

function statusText(status: number): '待审核' | '已通过' | '已驳回' | '已取消' | '已完成' {
  if (status === 2) return '已通过';
  if (status === 3) return '已驳回';
  if (status === 4) return '已取消';
  if (status === 5) return '已完成';
  return '待审核';
}

function reservationTypeText(type: number): string {
  if (type === 1) return '教学预约';
  if (type === 2) return '管理员预约';
  return '个人预约';
}

function auditActionText(action: number): string {
  if (action === 1) return '提交申请';
  if (action === 2) return '审核通过';
  if (action === 3) return '审核驳回';
  if (action === 4) return '取消预约';
  if (action === 5) return '完成/结束';
  return '状态变更';
}

function canCancel(item: ReservationDto): boolean {
  return item.status === 1 || item.status === 2;
}

async function loadReservations(): Promise<void> {
  try {
    const [reservationData, labData] = await Promise.all([
      fetchMyReservations({ pageNum: 1, pageSize: 30 }, auth.token.value),
      fetchLabs({ pageNum: 1, pageSize: 100 }, auth.token.value),
    ]);
    reservationState.value = reservationData;
    labs.value = labData.list;
    message.value = '已加载当前用户的预约记录。';

    if (reservationData.list.length && !selectedReservation.value) {
      await selectReservation(reservationData.list[0].id);
    }
  } catch (error) {
    message.value = error instanceof Error ? error.message : '预约记录加载失败。';
  }
}

async function selectReservation(id: number): Promise<void> {
  try {
    const [reservation, logs] = await Promise.all([
      fetchReservationById(id, auth.token.value),
      fetchReservationAuditLogs(id, auth.token.value),
    ]);
    selectedReservation.value = reservation;
    auditLogs.value = logs;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '预约详情加载失败。';
  }
}

async function handleCancel(id: number): Promise<void> {
  try {
    await cancelReservation(id, auth.token.value);
    message.value = '预约已取消。';
    selectedReservation.value = null;
    await loadReservations();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '取消预约失败。';
  }
}

onMounted(() => {
  void loadReservations();
});
</script>


