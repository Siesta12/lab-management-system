<template>
  <section class="content-grid">
    <BasePanel>
      <div class="toolbar">
        <div class="toolbar-title">{{ pagePanelTitle }}</div>
        <input v-model="keyword" :placeholder="isAdmin ? '输入实验室名称或编号' : '输入实验室名称或编号等'" @keyup.enter="handleSearch" />
        <select v-if="isAdmin" v-model="filters.departmentId" :disabled="isDepartmentScopedAdmin">
          <option v-if="!isDepartmentScopedAdmin" :value="undefined">全部学院</option>
          <option v-for="dept in visibleDepartments" :key="dept.value" :value="dept.value">{{ dept.label }}</option>
        </select>
        <select v-if="isAdmin" v-model="filters.labType">
          <option value="">全部类型</option>
          <option v-for="type in filteredLabTypeOptions" :key="type" :value="type">{{ type }}</option>
        </select>
        <select v-if="isAdmin" v-model="filters.labId" :disabled="!filteredAdminLabOptions.length">
          <option :value="undefined">全部实验室</option>
          <option v-for="lab in filteredAdminLabOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
        </select>
        <button type="button" class="ghost-btn" @click="handleSearch">查询</button>
        <button type="button" class="ghost-btn" @click="handleReset">重置</button>
      </div>

      <BaseTable :headers="isAdmin ? ['名称', '编号', '类型', '位置', '开放状态', '运行状态', '容量', '操作'] : ['名称', '编号', '位置', '开放状态', '运行状态', '容量']">
        <tr
          v-for="lab in displayLabs"
          :key="lab.id"
          :class="rowClass(lab)"
          :title="rowTitle(lab)"
          @click="handleLabRowClick(lab.id)"
        >
          <td>{{ lab.labName }}</td>
          <td>{{ lab.labCode }}</td>
          <td v-if="isAdmin">{{ lab.labType || '--' }}</td>
          <td>{{ buildLocation(lab) }}</td>
          <td><span :class="getBadgeClass(openStatusText(lab.openStatus))">{{ openStatusText(lab.openStatus) }}</span></td>
          <td><span :class="getBadgeClass(labStatusText(lab.labStatus))">{{ labStatusText(lab.labStatus) }}</span></td>
          <td>{{ lab.capacity }}</td>
          <td v-if="isAdmin">
            <button type="button" class="ghost-btn small-btn" @click.stop="openLabDetail(lab.id)">管理</button>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共{{ labsState.total }} 条</span>
        <button type="button" class="ghost-btn small-btn" :disabled="currentPage <= 1" @click="changePage(currentPage - 1)">
          上一页
        </button>
        <span class="pagination-text">第{{ currentPage }} / {{ totalPages }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="currentPage >= totalPages"
          @click="changePage(currentPage + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>
  </section>

  <teleport to="body">
    <div v-if="detailVisible" class="detail-modal-mask" @click.self="closeDetailModal">
      <div class="detail-modal">
        <div class="detail-modal-head">
          <div class="detail-modal-title">
            <h3>{{ isAdmin ? '实验室管理' : '选择节次预约' }}</h3>
            <p class="detail-modal-subtitle">
              {{ isAdmin ? '更新基础信息、维护状态与课表安排' : '未来三周课表 · 点击空闲格子选择节次' }}
            </p>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeDetailModal">关闭</button>
        </div>
        <p v-if="loadingDetail" class="info-text">正在加载实验室详情...</p>
        <div v-else-if="selectedLab" class="detail-stack">
          <div class="lab-hero">
            <div class="lab-hero-main">
              <h4 class="lab-title">{{ selectedLab.labName }}</h4>
              <p class="lab-subtitle">{{ selectedDepartmentName }} · {{ selectedLab.labType || '类型暂未设置' }}</p>
            </div>
            <div class="lab-hero-meta">
              <span class="chip">{{ buildLocation(selectedLab) }}</span>
              <span class="chip">{{ selectedLab.capacity }} 人</span>
              <span class="chip" :class="getBadgeClass(openStatusText(selectedLab.openStatus))">{{ openStatusText(selectedLab.openStatus) }}</span>
              <span class="chip" :class="getBadgeClass(labStatusText(selectedLab.labStatus))">{{ labStatusText(selectedLab.labStatus) }}</span>
              <span class="chip subtle">设备 {{ selectedDevices.length }}</span>
              <span class="chip subtle">耗材 {{ selectedConsumables.length }}</span>
            </div>
          </div>

          <div class="detail-tabs">
            <button type="button" class="tab-btn" :class="{ active: detailTab === 'schedule' }" @click="detailTab = 'schedule'">
              未来三周课表
            </button>
            <button type="button" class="tab-btn" :class="{ active: detailTab === 'detail' }" @click="detailTab = 'detail'">
              详情
            </button>
            <button v-if="isAdmin" type="button" class="tab-btn" :class="{ active: detailTab === 'manage' }" @click="detailTab = 'manage'">
              管理
            </button>
          </div>

          <div v-show="detailTab === 'schedule'" class="detail-pane">
            <div class="schedule-head compact-head">
              <div class="schedule-head-left">
                <h5 class="section-title">课表（按节次）</h5>
                <p class="section-hint">未来 21 天</p>
              </div>
              <div v-if="isAuthenticated" class="schedule-actions">
                <button type="button" class="ghost-btn" @click="reloadSchedule">刷新</button>
                <span v-if="isAdmin" class="section-hint">管理员可点击空闲/维护格设置维护</span>
              </div>
            </div>

            <p v-if="scheduleMessage" class="info-text">{{ scheduleMessage }}</p>

            <div v-if="schedule" class="schedule-wrap">
              <div class="legend">
                <template v-if="isAdmin">
                  <span class="legend-item free">空闲</span>
                  <span class="legend-item reserved">已预约</span>
                  <span class="legend-item pending">待审核</span>
                  <span class="legend-item maintenance">维护中</span>
                  <span class="legend-item closed">不开放</span>
                </template>
                <template v-else>
                  <span class="legend-item free">空闲</span>
                  <span class="legend-item reserved">已预约</span>
                  <span class="legend-item pending">待审核（本人）</span>
                  <span class="legend-item pendingOther">可申请（他人待审核）</span>
                  <span class="legend-item maintenance">维护中</span>
                  <span class="legend-item closed">不开放</span>
                </template>
                <span v-if="selectedKeys.length" class="legend-selected">已选 {{ selectedKeys.length }} 个</span>
              </div>

              <div v-if="selectedKeys.length || recommendationRequestKeys.length" class="selection-strip">
                <div class="selection-left">
                  <span class="selection-label">{{ selectedKeys.length ? '已选' : '推荐依据' }}</span>
                  <div class="selection-items">
                    <span
                      v-for="k in (selectedKeys.length ? selectedKeys : recommendationRequestKeys)"
                      :key="`${k.date}-${k.periodId}`"
                      class="selection-pill"
                      :class="{ blocked: !selectedKeys.length }"
                    >
                      {{ k.date.slice(5) }} · {{ periodLabel(k.periodId) }}
                    </span>
                  </div>
                </div>
                <button type="button" class="ghost-btn small-btn" @click="clearSelection">清空</button>
              </div>

              <div class="schedule-table-scroll">
                <table class="schedule-table">
                  <thead>
                    <tr>
                      <th class="sticky-col">节次</th>
                      <th v-for="day in schedule.days" :key="day.date">
                        <div class="day-head">
                          <div class="day-date">{{ day.date.slice(5) }}</div>
                          <div class="day-week">{{ weekdayText(day.weekday) }}</div>
                        </div>
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="period in schedule.periods" :key="period.id">
                      <td class="sticky-col period-col">
                        <div class="period-name">{{ period.periodName }}</div>
                        <div class="period-time">{{ period.startTime }} - {{ period.endTime }}</div>
                      </td>
                      <td
                        v-for="day in schedule.days"
                        :key="`${day.date}-${period.id}`"
                        class="schedule-cell"
                        :class="cellClass(day, period.id)"
                        @click="handleCellClick(day, period.id)"
                      >
                        <div class="cell-main">
                          <span class="cell-status">{{ cellStatusText(day, period.id) }}</span>
                          <span v-if="cell(day, period.id)?.reservationNo" class="cell-sub">#{{ cell(day, period.id)?.reservationNo }}</span>
                          <span v-if="cell(day, period.id)?.maintenanceReason" class="cell-sub">{{ cell(day, period.id)?.maintenanceReason }}</span>
                          <span v-else-if="cell(day, period.id)?.note" class="cell-sub">{{ cell(day, period.id)?.note }}</span>
                        </div>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <div v-if="isAuthenticated" class="schedule-forms">
                <div class="form-card reservation-form-card">
                  <div class="form-card-header" v-if="!isAdmin">
                    <h6>预约信息</h6>
                    <p>请填写预约信息，选择可用时间段后提交预约申请。</p>
                  </div>

                  <div v-if="isAdmin" class="form-card-header">
                    <h6>维护设置</h6>
                    <p>选择未来三周内的空闲或维护节次，可批量设置维护或取消维护。</p>
                  </div>

                  <p v-if="!selectedKeys.length && !isAdmin" class="info-text compact-tip">
                    先在课表中选择空闲节次提交预约，或点击不可预约格子获取推荐。
                  </p>

                  <p v-if="!blockedKeys.length && isAdmin" class="info-text compact-tip">
                    先在课表中点击需要维护的节次，维护中的格子也可再次点击查看并取消。
                  </p>

                  <div v-else-if="!isAdmin" class="beauty-form">
                    <label class="field-card">
                      <span class="field-label">预约类型</span>
                      <select v-if="!isStudent" v-model.number="reservationForm.reservationType">
                        <option v-for="item in teacherReservationTypeOptions" :key="item.value" :value="item.value">
                          {{ item.label }}
                        </option>
                      </select>
                      <input v-else :value="'个人预约'" disabled />
                    </label>

                    <label v-if="reservationForm.reservationType === 1" class="field-card">
                      <span class="field-label">课程名称</span>
                      <input v-model="reservationForm.courseName" placeholder="请输入课程名称" />
                    </label>

                    <label v-if="reservationForm.reservationType === 1" class="field-card">
                      <span class="field-label">班级名称</span>
                      <input v-model="reservationForm.className" placeholder="请输入班级名称" />
                    </label>

                    <label v-if="reservationForm.reservationType === 2" class="field-card">
                      <span class="field-label">科研项目名称</span>
                      <input v-model="reservationForm.projectName" placeholder="请输入科研项目名称" />
                    </label>

                    <label class="field-card">
                      <span class="field-label">参与人数</span>
                      <input v-model.number="reservationForm.participantCount" type="number" min="1" />
                    </label>

                    <label v-if="reservationForm.reservationType === 3" class="field-card field-full">
                      <span class="field-label">用途说明</span>
                      <input
                        v-model="reservationForm.usagePurpose"
                        placeholder="例如：课程实验 / 科研训练 / 项目开发 / 设备调试等"
                      />
                    </label>

                    <label class="field-card">
                      <span class="field-label">联系电话</span>
                      <input
                        v-model="reservationForm.contactPhone"
                        placeholder="请输入联系电话"
                      />
                    </label>
                  </div>

                  <div v-else class="beauty-form">
                    <label class="field-card field-full">
                      <span class="field-label">维护原因</span>
                      <input
                        v-model="maintenanceForm.reason"
                        placeholder="例如：设备检修 / 网络维护 / 深度清洁"
                      />
                    </label>
                  </div>

                  <div v-if="!isAdmin" class="recommend-box">
                    <div class="recommend-title">推荐可选节次</div>
                     <button
                         type="button"
                         class="ghost-btn recommend-btn"
                         :disabled="!recommendationRequestKeys.length || submittingReservation"
                         @click="handleRecommend"
                     >
                       {{ selectedKeys.length ? '为当前选择推荐' : '查看推荐时段' }}
                     </button>
                  </div>

                  <button
                    v-if="isAdmin"
                    type="button"
                    class="primary-btn submit-reservation-btn"
                    :disabled="!canSubmitMaintenance || savingMaintenance"
                    @click="handleCreateMaintenance"
                  >
                    {{ savingMaintenance ? '正在保存维护...' : '设置为维护' }}
                  </button>

                   <button
                       v-if="!isAdmin"
                       type="button"
                       class="primary-btn submit-reservation-btn"
                       :disabled="!canSubmitReservation || submittingReservation"
                       @click="handleCreateReservation"
                   >
                     {{ submittingReservation ? '正在提交...' : '提交预约申请' }}
                   </button>
                 </div>

                <div v-if="conflictPanel.visible" class="form-card conflict-card" :class="conflictPanel.type">
                  <div class="conflict-card-head">
                    <h6>{{ conflictPanel.title }}</h6>
                    <span class="conflict-chip">{{ conflictPanel.type === 'warning' ? '冲突判定' : '提交提示' }}</span>
                  </div>
                  <p>{{ conflictPanel.detail }}</p>
                </div>

                <div v-if="recommendations.length" class="form-card">
                  <h6>推荐结果</h6>
                  <div class="recommendation-list">
                    <div v-for="(item, idx) in recommendations" :key="idx" class="recommendation-card">
                      <div class="recommendation-main">
                        <strong>{{ item.labName }}</strong>
                        <span>{{ item.reservationDate }} · {{ item.periodName || `节次#${item.periodId}` }}</span>
                        <small>{{ item.recommendationReason }}</small>
                      </div>
                      <button type="button" class="ghost-btn small-btn" @click="applyRecommendation(item)">
                        选用
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div v-show="detailTab === 'detail'" class="detail-pane">
            <div class="detail-kv">
              <div class="kv">
                <span class="kv-label">实验室编号</span>
                <span class="kv-value">{{ selectedLab.labCode }}</span>
              </div>
              <div class="kv">
                <span class="kv-label">使用规则</span>
                <span class="kv-value">{{ selectedLab.usageRule || '按固定节次预约，进入实验室须遵守安全规范。' }}</span>
              </div>
            </div>

            <details class="details-card">
              <summary>设备（{{ selectedDevices.length }}）</summary>
              <ul v-if="selectedDevices.length" class="bullet-list compact-list">
                <li v-for="device in selectedDevices" :key="device.id">
                  {{ device.deviceName }} · {{ device.availableQuantity }}/{{ device.quantity }} 可用
                </li>
              </ul>
              <p v-else class="info-text">暂无设备数据。</p>
            </details>

            <details class="details-card">
              <summary>耗材（{{ selectedConsumables.length }}）</summary>
              <ul v-if="selectedConsumables.length" class="bullet-list compact-list">
                <li v-for="consumable in selectedConsumables" :key="consumable.id">
                  {{ consumable.consumableName }} · 库存 {{ consumable.stockQuantity }} {{ consumable.unit }}
                </li>
              </ul>
              <p v-else class="info-text">暂无耗材数据。</p>
            </details>

            <details class="details-card">
              <summary>简介</summary>
              <p class="info-text">{{ selectedLab.description || '暂无简介说明。' }}</p>
            </details>
          </div>

          <div v-if="isAdmin" v-show="detailTab === 'manage'" class="detail-pane">
            <div class="form-card reservation-form-card">
              <div class="form-card-header">
                <h6>基础信息维护</h6>
                <p>管理员可以在这里调整实验室名称、类型、位置、状态与说明。</p>
              </div>

              <div class="beauty-form">
                <label class="field-card">
                  <span class="field-label">实验室名称</span>
                  <input v-model="labEditForm.labName" />
                </label>
                <label class="field-card">
                  <span class="field-label">实验室编号</span>
                  <input v-model="labEditForm.labCode" />
                </label>
                <label class="field-card">
                  <span class="field-label">实验室类型</span>
                  <input v-model="labEditForm.labType" />
                </label>
                <label class="field-card">
                  <span class="field-label">容量</span>
                  <input v-model.number="labEditForm.capacity" type="number" min="0" />
                </label>
                <label class="field-card">
                  <span class="field-label">楼宇</span>
                  <input v-model="labEditForm.buildingName" />
                </label>
                <label class="field-card">
                  <span class="field-label">房间</span>
                  <input v-model="labEditForm.roomNo" />
                </label>
                <label class="field-card">
                  <span class="field-label">开放状态</span>
                  <select v-model.number="labEditForm.openStatus" class="arrow-select">
                    <option :value="1">开放</option>
                    <option :value="0">关闭</option>
                  </select>
                </label>
                <label class="field-card">
                  <span class="field-label">运行状态</span>
                  <select v-model.number="labEditForm.labStatus" class="arrow-select">
                    <option :value="1">正常</option>
                    <option :value="2">维护</option>
                  </select>
                </label>
                <label class="field-card field-full">
                  <span class="field-label">实验室简介</span>
                  <input v-model="labEditForm.description" />
                </label>
                <label class="field-card field-full">
                  <span class="field-label">使用规则</span>
                  <input v-model="labEditForm.usageRule" />
                </label>
              </div>

              <button
                type="button"
                class="primary-btn submit-reservation-btn"
                :disabled="savingLab"
                @click="handleSaveLab"
              >
                {{ savingLab ? '正在保存...' : '保存实验室信息' }}
              </button>
            </div>
          </div>
        </div>

        <p v-else class="info-text">请选择实验室查看详情与课表。</p>
      </div>
    </div>

    <div v-if="feedback.visible && feedback.mode === 'toast'" class="toast" :class="feedback.type" role="status" aria-live="polite">
      {{ feedback.text }}
    </div>

    <div v-if="successDialogVisible" class="success-dialog-mask">
      <div class="success-dialog" role="dialog" aria-modal="true" aria-labelledby="success-dialog-title">
        <div class="success-dialog-badge">预约创建成功</div>
        <h4 id="success-dialog-title">提交成功</h4>
        <p>{{ successDialogText }}</p>
        <button type="button" class="success-dialog-btn" @click="handleSuccessConfirm">确定</button>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { fetchConsumables } from '../api/consumables';
import { fetchDepartmentOptions } from '../api/departments';
import { fetchDevices } from '../api/devices';
import {
  createLabMaintenance,
  fetchLabById,
  fetchLabMaintenance,
  fetchLabs,
  fetchLabSchedule,
  updateLab,
  updateLabOpenStatus,
  updateLabStatus,
} from '../api/labs';
import { applyReservation, recommendSlots } from '../api/reservations';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useAuthStore } from '../stores/auth';
import type {
  ConsumableDto,
  DeviceDto,
  LabDto,
  LabMaintenanceDto,
  LabScheduleDto,
  OptionItem,
  PageData,
  ReservationApplyResponse,
  ScheduleDayDto,
  ScheduleCellDto,
  SlotRecommendationItem,
} from '../types';
import { getBadgeClass } from '../utils/format';

const auth = useAuthStore();
const isAuthenticated = computed(() => Boolean(auth.token.value));
const roleCodes = computed(() => auth.currentUser.value?.roleCodes ?? []);
const isAdmin = computed(() => roleCodes.value.some((item) => item === 'ADMIN' || item === 'ROLE_ADMIN'));
const isStudent = computed(() => roleCodes.value.some((item) => item === 'STUDENT' || item === 'ROLE_STUDENT'));
const isTeacher = computed(() => roleCodes.value.some((item) => item === 'TEACHER' || item === 'ROLE_TEACHER'));
const adminDepartmentId = computed(() => auth.currentUser.value?.departmentId ?? undefined);
const isDepartmentScopedAdmin = computed(() => isAdmin.value && !!adminDepartmentId.value);

const keyword = ref('');
const message = ref('');
const labsState = ref<PageData<LabDto>>({ list: [], total: 0, pageNum: 1, pageSize: 6 });
const departments = ref<OptionItem[]>([]);
const adminLabSource = ref<LabDto[]>([]);
const selectedLab = ref<LabDto | null>(null);
const selectedDevices = ref<DeviceDto[]>([]);
const selectedConsumables = ref<ConsumableDto[]>([]);
const maintenances = ref<LabMaintenanceDto[]>([]);
const detailVisible = ref(false);
const loadingDetail = ref(false);
const detailTab = ref<'schedule' | 'detail' | 'manage'>('schedule');
const currentPage = ref(1);
const pageSize = ref(10);

const schedule = ref<LabScheduleDto | null>(null);
const scheduleMessage = ref('');

const selectedKeys = ref<Array<{ date: string; periodId: number }>>([]);
const blockedKeys = ref<Array<{ date: string; periodId: number }>>([]);
const recommendations = ref<SlotRecommendationItem[]>([]);
const conflictPanel = reactive<{
  visible: boolean;
  type: 'warning' | 'info';
  title: string;
  detail: string;
}>({
  visible: false,
  type: 'info',
  title: '',
  detail: '',
});

const submittingReservation = ref(false);
const feedback = reactive<{ visible: boolean; mode: 'toast' | 'dialog'; type: 'success' | 'error'; text: string; timer?: number }>({
  visible: false,
  mode: 'toast',
  type: 'success',
  text: '',
});
const successDialogVisible = ref(false);
const successDialogText = ref('');
const savingLab = ref(false);
const savingMaintenance = ref(false);

const filters = reactive({
  departmentId: undefined as number | undefined,
  labId: undefined as number | undefined,
  labType: '',
});

const reservationForm = reactive({
  reservationType: 3,
  priorityLevel: 3,
  usagePurpose: '',
  courseName: '',
  className: '',
  projectName: '',
  participantCount: 1,
  contactPhone: '',
});

watch(
  () => isStudent.value,
  (value) => {
    if (value) {
      reservationForm.reservationType = 3;
      reservationForm.priorityLevel = 3;
    }
  },
  { immediate: true },
);

watch(
  () => reservationForm.reservationType,
  (value) => {
    if (value === 1) {
      reservationForm.projectName = '';
      reservationForm.usagePurpose = '';
    } else if (value === 2) {
      reservationForm.courseName = '';
      reservationForm.className = '';
      reservationForm.usagePurpose = '';
    } else {
      reservationForm.courseName = '';
      reservationForm.className = '';
      reservationForm.projectName = '';
    }
  },
);

const labEditForm = reactive({
  labName: '',
  labCode: '',
  labType: '',
  buildingName: '',
  roomNo: '',
  capacity: 0,
  description: '',
  usageRule: '',
  openStatus: 1,
  labStatus: 1,
});

const maintenanceForm = reactive({
  reason: '',
});

const displayLabs = computed(() => labsState.value.list);
const visibleDepartments = computed(() => {
  if (!isDepartmentScopedAdmin.value) {
    return departments.value;
  }
  return departments.value.filter((item) => item.value === adminDepartmentId.value);
});
const departmentScopedLabs = computed(() => {
  if (!isAdmin.value) {
    return [] as LabDto[];
  }
  return adminLabSource.value.filter((item) => !filters.departmentId || item.departmentId === filters.departmentId);
});
const filteredLabTypeOptions = computed(() => {
  return Array.from(new Set(departmentScopedLabs.value.map((item) => item.labType).filter(Boolean) as string[]));
});
const filteredAdminLabOptions = computed(() => {
  return departmentScopedLabs.value
    .filter((item) => !filters.labType || item.labType === filters.labType)
    .map((item) => ({ label: item.labName, value: item.id }));
});
const totalPages = computed(() => {
  const pages = Math.ceil((labsState.value.total || 0) / pageSize.value);
  return Math.max(pages, 1);
});
const selectedDepartmentName = computed(() => {
  if (!selectedLab.value?.departmentId) {
    return '所属学院待补充';
  }
  return departments.value.find((item) => item.value === selectedLab.value?.departmentId)?.label ?? '所属学院待补充';
});

const canSubmitReservation = computed(() => {
  return (
    isAuthenticated.value &&
    selectedKeys.value.length > 0 &&
    composeUsagePurpose().length > 0 &&
    reservationForm.contactPhone.trim().length > 0 &&
    reservationDetailsComplete.value
  );
});

const recommendationRequestKeys = computed(() => {
  return selectedKeys.value.length ? selectedKeys.value : blockedKeys.value;
});
const pagePanelTitle = computed(() => {
  if (isAdmin.value) {
    return '实验室管理';
  }
  if (isTeacher.value) {
    return '实验室预约';
  }
  return '实验室查询';
});
const canSubmitMaintenance = computed(() => isAdmin.value && blockedKeys.value.length > 0 && maintenanceForm.reason.trim().length > 0);
const teacherReservationTypeOptions = computed(() => {
  if (isStudent.value) {
    return [{ value: 3, label: '个人预约' }];
  }
  return [
    { value: 1, label: '课程实验预约' },
    { value: 2, label: '科研训练预约' },
    { value: 3, label: '个人预约' },
  ];
});
const reservationDetailsComplete = computed(() => {
  if (reservationForm.reservationType === 1) {
    return reservationForm.courseName.trim().length > 0 && reservationForm.className.trim().length > 0;
  }
  if (reservationForm.reservationType === 2) {
    return reservationForm.projectName.trim().length > 0;
  }
  return true;
});



function buildLocation(lab: LabDto): string {
  return [lab.buildingName, lab.roomNo].filter(Boolean).join(' / ') || '位置未知';
}

function openStatusText(status: number): '开放' | '关闭' {
  return status === 1 ? '开放' : '关闭';
}

function labStatusText(status: number): '正常' | '维护' {
  return status === 2 ? '维护' : '正常';
}

function weekdayText(weekday: number): string {
  const map = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
  return map[weekday - 1] ?? `星期${weekday}`;
}

function periodLabel(periodId: number): string {
  const p = schedule.value?.periods?.find((item) => item.id === periodId);
  return p?.periodName ?? `节次#${periodId}`;
}

function composeCourseOrProjectName(): string | undefined {
  if (reservationForm.reservationType === 1) {
    const parts = [reservationForm.courseName.trim(), reservationForm.className.trim()].filter(Boolean);
    return parts.length ? parts.join(' / ') : undefined;
  }
  if (reservationForm.reservationType === 2) {
    return reservationForm.projectName.trim() || undefined;
  }
  return '个人预约';
}

function composeUsagePurpose(): string {
  if (reservationForm.reservationType === 1) {
    const parts = [reservationForm.courseName.trim(), reservationForm.className.trim()].filter(Boolean);
    return parts.length ? `课程实验：${parts.join(' / ')}` : '';
  }
  if (reservationForm.reservationType === 2) {
    const projectName = reservationForm.projectName.trim();
    return projectName ? `科研训练：${projectName}` : '';
  }
  return reservationForm.usagePurpose.trim();
}

async function loadLabs(pageNum = currentPage.value): Promise<void> {
  currentPage.value = Math.max(pageNum, 1);
  try {
    const scopedDepartmentId = isAdmin.value ? filters.departmentId : (auth.currentUser.value?.departmentId ?? undefined);
    const data = await fetchLabs(
      {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        labId: isAdmin.value ? filters.labId : undefined,
        labName: keyword.value.trim() || undefined,
        departmentId: scopedDepartmentId,
        labType: isAdmin.value ? filters.labType || undefined : undefined,
      },
      auth.token.value || undefined,
    );
    labsState.value = data;
    currentPage.value = data.pageNum || currentPage.value;
    pageSize.value = data.pageSize || pageSize.value;
    message.value = '已加载实验室列表。';
  } catch (error) {
    message.value = error instanceof Error ? `实验室数据加载失败：${error.message}` : '实验室数据加载失败。';
  }
}

function handleSearch(): void {
  void loadLabs(1);
}

function handleReset(): void {
  keyword.value = '';
  filters.departmentId = adminDepartmentId.value;
  filters.labId = undefined;
  filters.labType = '';
  void loadLabs(1);
}

function changePage(pageNum: number): void {
  void loadLabs(pageNum);
}

function handleLabRowClick(id: number): void {
  if (!isAdmin.value) {
    const lab = displayLabs.value.find((item) => item.id === id);
    if (lab && (lab.openStatus !== 1 || lab.labStatus !== 1)) {
      showToast('error', '当前不可预约此实验室');
      return;
    }
  }
  void openLabDetail(id);
}

function rowClass(lab: LabDto): Record<string, boolean> {
  const unavailable = !isAdmin.value && (lab.openStatus !== 1 || lab.labStatus !== 1);
  return {
    'clickable-row': !unavailable,
    'inactive-row': unavailable,
  };
}

function rowTitle(lab: LabDto): string | undefined {
  if (!isAdmin.value && (lab.openStatus !== 1 || lab.labStatus !== 1)) {
    return '当前不可预约';
  }
  return undefined;
}

function closeDetailModal(): void {
  detailVisible.value = false;
  // Reset state so next open starts clean.
  selectedLab.value = null;
  selectedDevices.value = [];
  selectedConsumables.value = [];
  maintenances.value = [];
  schedule.value = null;
  scheduleMessage.value = '';
  recommendations.value = [];
  clearSelection();
}

function showToast(type: 'success' | 'error', text: string, durationMs = 1800): void {
  if (feedback.timer) {
    window.clearTimeout(feedback.timer);
  }
  feedback.visible = true;
  feedback.mode = 'toast';
  feedback.type = type;
  feedback.text = text;
  feedback.timer = window.setTimeout(() => {
    feedback.visible = false;
    feedback.text = '';
    feedback.timer = undefined;
  }, durationMs);
}

function showSuccessDialog(text: string): void {
  closeDetailModal();
  window.setTimeout(() => {
    showToast('success', text, 2400);
  }, 60);
}

function handleSuccessConfirm(): void {
  successDialogVisible.value = false;
  successDialogText.value = '';
  closeDetailModal();
}

async function openLabDetail(id: number): Promise<void> {
  detailVisible.value = true;
  loadingDetail.value = true;
  try {
    const requests: Promise<unknown>[] = [
      fetchLabById(id, auth.token.value || undefined),
      fetchDevices({ labId: id, pageNum: 1, pageSize: 30 }, auth.token.value || undefined),
      fetchConsumables({ labId: id, pageNum: 1, pageSize: 30 }, auth.token.value || undefined),
    ];
    if (auth.token.value && isAdmin.value) {
      requests.push(fetchLabMaintenance(id, auth.token.value));
    }
    const [lab, devices, consumables, maintenanceList] = await Promise.all(requests) as [LabDto, PageData<DeviceDto>, PageData<ConsumableDto>, LabMaintenanceDto[] | undefined];
    selectedLab.value = lab;
    selectedDevices.value = devices.list;
    selectedConsumables.value = consumables.list;
    maintenances.value = maintenanceList ?? [];
    fillLabEditForm(lab);
      detailTab.value = 'schedule';
    clearSelection();
    recommendations.value = [];
    await reloadSchedule();
  } catch (error) {
    message.value = error instanceof Error ? `实验室详情加载失败：${error.message}` : '实验室详情加载失败。';
  } finally {
    loadingDetail.value = false;
  }
}

function cell(day: ScheduleDayDto, periodId: number): ScheduleCellDto | undefined {
  return day.cells.find((c) => c.periodId === periodId);
}

function cellStatusText(day: ScheduleDayDto, periodId: number): string {
  const c = cell(day, periodId);
  if (!c) return '--';
  if (c.status === 'FREE') return '空闲';
  if (isAdmin.value && (c.status === 'PENDING_SELF' || c.status === 'PENDING_OTHERS' || c.status === 'PENDING')) {
    return '待审核';
  }
  if (c.status === 'PENDING_SELF') return '待审核（本人）';
  if (c.status === 'PENDING_OTHERS') return '可申请';
  if (c.status === 'RESERVED') return '已预约';
  if (c.status === 'PENDING') return '待审核';
  if (c.status === 'MAINTENANCE') return '维护';
  return '不开放';
}

function isSelected(date: string, periodId: number): boolean {
  return selectedKeys.value.some((k) => k.date === date && k.periodId === periodId);
}

function cellClass(day: ScheduleDayDto, periodId: number): Record<string, boolean> {
  const c = cell(day, periodId);
  const status = c?.status ?? 'CLOSED';
  const pendingStatus = isAdmin.value ? (status === 'PENDING' || status === 'PENDING_SELF' || status === 'PENDING_OTHERS') : (status === 'PENDING' || status === 'PENDING_SELF');
  return {
    free: status === 'FREE',
    reserved: status === 'RESERVED',
    pending: pendingStatus,
    pendingOther: !isAdmin.value && status === 'PENDING_OTHERS',
    maintenance: status === 'MAINTENANCE',
    closed: status === 'CLOSED',
    selected: isSelected(day.date, periodId) || (isAdmin.value && isBlocked(day.date, periodId)),
    clickable: status === 'FREE' || (!isAdmin.value && status === 'PENDING_OTHERS'),
  };
}

function isBlocked(date: string, periodId: number): boolean {
  return blockedKeys.value.some((k) => k.date === date && k.periodId === periodId);
}

function toggleSelection(date: string, periodId: number): void {
  const idx = selectedKeys.value.findIndex((k) => k.date === date && k.periodId === periodId);
  if (idx >= 0) {
    // 如果点击的是已选中的时间段，则取消选择
    selectedKeys.value = [];
  } else {
    // 如果点击的是新的时间段，则先清空已选择的，再添加新的
    selectedKeys.value = [{ date, periodId }];
  }
}

async function reloadSchedule(): Promise<void> {
  schedule.value = null;
  scheduleMessage.value = '';
  if (!selectedLab.value || !auth.token.value) {
    scheduleMessage.value = '登录后可查看未来几周预约表并进行预约与管理操作。';
    return;
  }
  try {
    schedule.value = await fetchLabSchedule(selectedLab.value.id, auth.token.value);
    scheduleMessage.value = isAdmin.value ? '课表已加载，可点击格子设置维护。' : '预约已加载，请点击空白格选择时段。';
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? `预约加载失败：${error.message}` : '预约加载失败。';
  }
}

function clearSelection(): void {
  selectedKeys.value = [];
  blockedKeys.value = [];
  recommendations.value = [];
  conflictPanel.visible = false;
  conflictPanel.title = '';
  conflictPanel.detail = '';
  scheduleMessage.value = isAdmin.value ? '课表已加载，可点击格子设置维护。' : '预约已加载，请点击空白格选择时段。';
}

async function handleCellClick(day: ScheduleDayDto, periodId: number): Promise<void> {
  const c = cell(day, periodId);
  if (!c) return;

  if (isAdmin.value) {
    if (c.status === 'FREE' || c.status === 'MAINTENANCE') {
      blockedKeys.value = [{ date: day.date, periodId }];
      selectedKeys.value = [];
      recommendations.value = [];
      conflictPanel.visible = false;
      scheduleMessage.value = c.status === 'MAINTENANCE'
        ? '已选中维护中的节次，可在下方取消维护。'
        : '已选中节次，可填写维护原因后保存。';
    }
    return;
  }

  if (c.status === 'FREE') {
    blockedKeys.value = [];
    recommendations.value = [];
    conflictPanel.visible = false;
    toggleSelection(day.date, periodId);

    if (selectedKeys.value.length) {
      scheduleMessage.value = '已选择可预约时段，请填写预约信息并提交。';
    } else {
      scheduleMessage.value = '预约已加载，请点击空白格选择时段。';
    }
    return;
  }

  if (c.status === 'PENDING_OTHERS') {
    blockedKeys.value = [];
    recommendations.value = [];
    conflictPanel.visible = true;
    conflictPanel.type = 'warning';
    conflictPanel.title = '该时段已有他人待审核';
    conflictPanel.detail = c.note || '你仍然可以提交申请，系统会在提交时进入冲突判定，并为你推荐其他可选方案。';
    toggleSelection(day.date, periodId);
    scheduleMessage.value = '该时段已有他人待审核，你可以继续填写表单并提交申请。';
    return;
  }

  if (c.status === 'PENDING_SELF') {
    selectedKeys.value = [];
    blockedKeys.value = [{ date: day.date, periodId }];
    conflictPanel.visible = true;
    conflictPanel.type = 'warning';
    conflictPanel.title = '你已申请该时段';
    conflictPanel.detail = c.note || '该节次已由你提交待审核申请，不能重复申请。';
    scheduleMessage.value = '你已申请该时段，不能重复提交。';
    showToast('error', '你已申请该时段，当前状态为待审核。');
    return;
  }

  selectedKeys.value = [];
  blockedKeys.value = [{ date: day.date, periodId }];
  recommendations.value = [];
  conflictPanel.visible = true;
  conflictPanel.type = 'info';
  conflictPanel.title = '该时段当前不可直接预约';
  conflictPanel.detail = c.note || '该时段不可预约，已为你锁定推荐依据，点击“查看推荐时段”即可。';
  scheduleMessage.value = '该时段不可预约，已为你锁定推荐依据，点击“查看推荐时段”即可。';
}

async function handleCreateReservation(): Promise<void> {
  if (!selectedLab.value || !auth.token.value) return;
  if (!canSubmitReservation.value) {
    scheduleMessage.value = reservationForm.reservationType === 3
      ? '请先选择空闲时段，并填写用途说明与联系电话。'
      : '请先选择空闲时段，并完善课程/项目名称与联系电话。';
    return;
  }
  if (submittingReservation.value) return;
  submittingReservation.value = true;
  try {
    const response = await applyReservation(
      {
        labId: selectedLab.value.id,
        reservationType: reservationForm.reservationType,
        priorityLevel: reservationForm.priorityLevel,
        usagePurpose: composeUsagePurpose(),
        courseOrProjectName: composeCourseOrProjectName(),
        participantCount: reservationForm.participantCount,
        contactPhone: reservationForm.contactPhone,
        slots: selectedKeys.value.map((k) => ({ reservationDate: k.date, periodId: k.periodId })),
      },
      auth.token.value,
    );
    await applyReservationResult(response);
  } catch (error) {
    const text = error instanceof Error ? error.message : '\u9884\u7ea6\u63d0\u4ea4\u5931\u8d25\u3002';
    scheduleMessage.value = text;
    showToast('error', text);
  } finally {
    submittingReservation.value = false;
  }
}



async function handleRecommend(): Promise<void> {
  if (!selectedLab.value || !auth.token.value) return;
  if (!recommendationRequestKeys.value.length) {
    scheduleMessage.value = '请先选择您要预约的时段（或选择一个不可预约的时段，再查看推荐）。';
    return;
  }
  try {
    recommendations.value = await recommendSlots(
      {
        labId: selectedLab.value.id,
        participantCount: reservationForm.participantCount,
        slots: recommendationRequestKeys.value.map((k) => ({ reservationDate: k.date, periodId: k.periodId })),
      },
      auth.token.value,
    );
    scheduleMessage.value = recommendations.value.length ? '已生成推荐结果。' : '暂无可推荐的时段/实验室。';
  } catch (error) {
    scheduleMessage.value = error instanceof Error ? error.message : '推荐查询失败。';
  }
}

async function applyRecommendation(item: SlotRecommendationItem): Promise<void> {
  if (!auth.token.value) return;
  if (selectedLab.value?.id !== item.labId) {
    await openLabDetail(item.labId);
  }
  selectedKeys.value = [{ date: item.reservationDate, periodId: item.periodId }];
  blockedKeys.value = [];
  recommendations.value = [];
  conflictPanel.visible = false;
  detailTab.value = 'schedule';
  scheduleMessage.value = `已选中推荐节次：${item.reservationDate} ${item.periodName || `节次#${item.periodId}`}，可直接提交预约。`;
}

function fillLabEditForm(lab: LabDto): void {
  labEditForm.labName = lab.labName || '';
  labEditForm.labCode = lab.labCode || '';
  labEditForm.labType = lab.labType || '';
  labEditForm.buildingName = lab.buildingName || '';
  labEditForm.roomNo = lab.roomNo || '';
  labEditForm.capacity = lab.capacity || 0;
  labEditForm.description = lab.description || '';
  labEditForm.usageRule = lab.usageRule || '';
  labEditForm.openStatus = lab.openStatus ?? 1;
  labEditForm.labStatus = lab.labStatus ?? 1;
}

async function handleSaveLab(): Promise<void> {
  if (!selectedLab.value || !auth.token.value || !isAdmin.value) return;
  savingLab.value = true;
  try {
    const updated = await updateLab(
      selectedLab.value.id,
      {
        ...selectedLab.value,
        labName: labEditForm.labName,
        labCode: labEditForm.labCode,
        labType: labEditForm.labType,
        buildingName: labEditForm.buildingName,
        roomNo: labEditForm.roomNo,
        capacity: labEditForm.capacity,
        description: labEditForm.description,
        usageRule: labEditForm.usageRule,
        openStatus: labEditForm.openStatus,
        labStatus: labEditForm.labStatus,
      },
      auth.token.value,
    );
    if (selectedLab.value.openStatus !== labEditForm.openStatus) {
      await updateLabOpenStatus(selectedLab.value.id, labEditForm.openStatus, auth.token.value);
    }
    if (selectedLab.value.labStatus !== labEditForm.labStatus) {
      await updateLabStatus(selectedLab.value.id, labEditForm.labStatus, auth.token.value);
    }
    selectedLab.value = updated;
    fillLabEditForm(updated);
    await loadLabs(currentPage.value);
    showToast('success', '实验室信息已更新');
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '实验室信息保存失败');
  } finally {
    savingLab.value = false;
  }
}

async function handleCreateMaintenance(): Promise<void> {
  if (!selectedLab.value || !auth.token.value || !blockedKeys.value.length || !maintenanceForm.reason.trim()) return;
  savingMaintenance.value = true;
  try {
    const groupedByDate = blockedKeys.value.reduce<Record<string, number[]>>((acc, item) => {
      acc[item.date] = acc[item.date] || [];
      acc[item.date].push(item.periodId);
      return acc;
    }, {});
    for (const [maintenanceDate, periodIds] of Object.entries(groupedByDate)) {
      await createLabMaintenance(
        selectedLab.value.id,
        { maintenanceDate, periodIds, reason: maintenanceForm.reason.trim() },
        auth.token.value,
      );
    }
    maintenances.value = await fetchLabMaintenance(selectedLab.value.id, auth.token.value);
    maintenanceForm.reason = '';
    blockedKeys.value = [];
    await reloadSchedule();
    showToast('success', '维护设置成功');
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '维护设置失败');
  } finally {
    savingMaintenance.value = false;
  }
}

watch(
  () => filters.departmentId,
  async () => {
    filters.labType = '';
    filters.labId = undefined;
    if (isAdmin.value) {
      await loadLabs(1);
    }
  },
);

watch(
  () => filters.labType,
  async () => {
    filters.labId = undefined;
    if (isAdmin.value) {
      await loadLabs(1);
    }
  },
);

watch(
  () => filters.labId,
  async (value, oldValue) => {
    if (!isAdmin.value || value === oldValue) {
      return;
    }
    await loadLabs(1);
  },
);

async function applyReservationResult(response: ReservationApplyResponse): Promise<void> {
  recommendations.value = response.recommendations || [];

  if (response.submitted) {
    await reloadSchedule();
    clearSelection();
    conflictPanel.visible = Boolean(response.conflict);
    conflictPanel.type = response.conflict ? 'warning' : 'info';
    conflictPanel.title = response.conflict ? '申请已提交，系统已判定存在竞争' : '申请已提交';
    conflictPanel.detail = response.conflictNote || '预约申请已经提交成功。';

    if (response.currentStatus === 'PENDING_PRIORITY') {
      showSuccessDialog('预约申请提交成功，当前时段已有低优先级申请，你的申请已进入优先审核队列。');
    } else {
      showSuccessDialog('预约申请已经提交成功。');
    }
    return;
  }

  conflictPanel.visible = true;
  conflictPanel.type = 'warning';
  conflictPanel.title = response.currentStatus === 'PENDING_SELF' ? '你已申请该时段' : '该时段已被占用';
  conflictPanel.detail = response.conflictNote || '当前所选时段无法提交，请改选推荐方案。';
  scheduleMessage.value = response.conflictNote || '当前所选时段无法提交。';
  if (response.currentStatus === 'PENDING_SELF') {
    showToast('error', '你已申请该时段，当前状态为待审核。');
  } else {
    showToast('error', '当前所选时段无法提交，已为你推荐其他方案。');
  }
}

onMounted(async () => {
  if (isDepartmentScopedAdmin.value) {
    filters.departmentId = adminDepartmentId.value;
  }
  try {
    departments.value = await fetchDepartmentOptions(auth.token.value || undefined);
  } catch {
    departments.value = [];
  }
  try {
    const allLabs = await fetchLabs({ pageNum: 1, pageSize: 500 }, auth.token.value || undefined);
    adminLabSource.value = allLabs.list;
  } catch {
    adminLabSource.value = [];
  }
  await loadLabs();
});
</script>

<style scoped>
/* 表格样式优化 */
.table-wrap table {
  font-size: 13px;
}

.table-wrap th,
.table-wrap td {
  padding: 10px 8px;
}

.schedule-wrap {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.66);
  overflow: hidden;
}

.pagination-wrap {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.pagination-total {
  margin-right: auto;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.pagination-text {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.8);
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.toolbar-title {
  margin-right: 8px;
  color: #0f172a;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
}

.toolbar input {
  padding: 8px 12px;
  font-size: 13px;
}

.toolbar button {
  padding: 8px 12px;
  font-size: 13px;
}

.detail-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(2, 6, 23, 0.45);
  display: grid;
  place-items: center;
  z-index: 1200;
  padding: 20px;
}

.detail-modal {
  width: min(1200px, 96vw);
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  background: #ffffff;
  padding: 16px;
  box-shadow: 0 28px 48px rgba(15, 23, 42, 0.3);
}

.success-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1350;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(2, 6, 23, 0.34);
  backdrop-filter: blur(4px);
}

.success-dialog {
  width: min(420px, 92vw);
  border-radius: 24px;
  padding: 28px 24px 24px;
  background: linear-gradient(180deg, #16a34a 0%, #15803d 100%);
  color: #ffffff;
  box-shadow: 0 28px 64px rgba(21, 128, 61, 0.36);
  text-align: center;
}

.success-dialog-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.18);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.4px;
}

.success-dialog h4 {
  margin: 18px 0 8px;
  font-size: 28px;
  font-weight: 800;
}

.success-dialog p {
  margin: 0;
  font-size: 15px;
  line-height: 1.7;
  color: rgba(255, 255, 255, 0.94);
}

.success-dialog-btn {
  margin-top: 22px;
  width: 100%;
  height: 48px;
  border: 0;
  border-radius: 999px;
  background: #ffffff;
  color: #166534;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 12px 24px rgba(255, 255, 255, 0.16);
}

.toast {
  position: fixed;
  top: 18px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1300;
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 22px 42px rgba(15, 23, 42, 0.22);
  font-size: 14px;
  font-weight: 650;
  color: rgba(15, 23, 42, 0.94);
  max-width: min(640px, 92vw);
  text-align: center;
  backdrop-filter: blur(10px);
  animation: toast-pop 160ms ease-out;
}

.toast.success {
  padding: 14px 28px;
  border-radius: 18px;
  background: linear-gradient(180deg, #2fd18a 0%, #18b977 100%);
  border-color: rgba(16, 185, 129, 0.28);
  color: #ffffff;
  box-shadow: 0 18px 34px rgba(16, 185, 129, 0.28);
  font-size: 16px;
  letter-spacing: 0.02em;
}

.toast.error {
  background: rgba(239, 68, 68, 0.92);
  border-color: rgba(239, 68, 68, 0.55);
  color: #ffffff;
}

@keyframes toast-pop {
  from {
    opacity: 0;
    transform: translateX(-50%) translateY(-8px) scale(0.98);
  }
  to {
    opacity: 1;
    transform: translateX(-50%) translateY(0) scale(1);
  }
}

.detail-modal-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 12px;
}

.detail-modal-head h3 {
  margin: 0;
  font-size: 20px;
}

.detail-modal-title {
  display: grid;
  gap: 2px;
}

.detail-modal-subtitle {
  margin: 0;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.detail-stack {
  display: grid;
  gap: 12px;
}

.lab-hero {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.02);
  padding: 12px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.lab-title {
  margin: 0;
  font-size: 18px;
  letter-spacing: 0.2px;
}

.lab-subtitle {
  margin: 3px 0 0;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.lab-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: flex-end;
}

.chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.75);
  line-height: 1.2;
}

.chip.subtle {
  background: rgba(15, 23, 42, 0.03);
  color: rgba(15, 23, 42, 0.72);
}

.detail-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  width: fit-content;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(15, 23, 42, 0.02);
}

.tab-btn {
  border: 0;
  background: transparent;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: rgba(15, 23, 42, 0.82);
  cursor: pointer;
}

.tab-btn.active {
  background: rgba(15, 23, 42, 0.08);
  color: rgba(15, 23, 42, 1);
  font-weight: 650;
}

.detail-pane {
  display: grid;
  gap: 10px;
}

.compact-head {
  margin-top: 4px;
}

.schedule-head-left {
  display: grid;
  gap: 2px;
}

.section-title {
  margin: 0;
  font-size: 14px;
}

.section-hint {
  margin: 0;
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
}

.selection-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(15, 23, 42, 0.02);
}

.selection-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.selection-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
  white-space: nowrap;
}

.selection-items {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  min-width: 0;
}

.selection-pill {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.75);
  white-space: nowrap;
}

.selection-pill.blocked {
  border-color: rgba(59, 130, 246, 0.25);
  background: rgba(59, 130, 246, 0.08);
  color: #1d4ed8;
}

.compact-tip {
  margin-top: 6px;
  margin-bottom: 0;
}

.detail-kv {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.75);
  display: grid;
  gap: 10px;
}

.kv {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.kv-label {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.72);
  white-space: nowrap;
}

.kv-value {
  font-size: 13px;
  text-align: right;
  color: rgba(15, 23, 42, 0.92);
}

.details-card {
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 14px;
  padding: 10px 12px;
  background: rgba(255, 255, 255, 0.75);
}

.details-card > summary {
  cursor: pointer;
  font-weight: 650;
  list-style: none;
}

.details-card > summary::-webkit-details-marker {
  display: none;
}

.details-card[open] > summary {
  margin-bottom: 8px;
}

.schedule-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.schedule-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}



.legend {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
}

.legend-item {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  border: 1px solid rgba(15, 23, 42, 0.12);
}

.legend-selected {
  margin-left: auto;
  font-size: 12px;
  opacity: 0.8;
}

.schedule-table-scroll {
  overflow-x: auto;
}

.schedule-table {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
}

.schedule-table th,
.schedule-table td {
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  padding: 8px;
  text-align: center;
  vertical-align: middle;
  min-width: 92px;
}

.schedule-table th.sticky-col,
.schedule-table td.sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  background: rgba(255, 255, 255, 0.95);
  min-width: 160px;
  text-align: left;
}

.schedule-table tr.clickable-row {
  transition:
    background-color 0.18s ease,
    transform 0.18s ease,
    box-shadow 0.18s ease;
}

.schedule-table tr.clickable-row td {
  cursor: pointer;
}

.schedule-table tr.clickable-row:hover td {
  background: rgba(59, 130, 246, 0.06);
}

.schedule-table tr.inactive-row {
  opacity: 0.68;
}

.schedule-table tr.inactive-row td {
  color: #64748b;
  cursor: not-allowed;
  background: rgba(148, 163, 184, 0.08);
}

.schedule-table tr.inactive-row:hover td {
  background: rgba(148, 163, 184, 0.12);
}

.period-col {
  padding: 10px 12px;
}

.period-name {
  font-weight: 650;
}

.period-time {
  font-size: 12px;
  opacity: 0.75;
  margin-top: 2px;
}

.day-head {
  display: grid;
  gap: 2px;
}

.day-date {
  font-weight: 650;
}

.day-week {
  font-size: 12px;
  opacity: 0.75;
}

.schedule-cell {
  cursor: default;
  user-select: none;
}

.schedule-cell.clickable {
  cursor: pointer;
}

.cell-main {
  display: grid;
  gap: 2px;
  justify-items: center;
}

.cell-status {
  font-size: 12px;
  font-weight: 650;
}

.cell-sub {
  font-size: 11px;
  opacity: 0.85;
  max-width: 90px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.schedule-cell.free {
  background: rgba(16, 185, 129, 0.08);
}
.schedule-cell.reserved {
  background: rgba(239, 68, 68, 0.10);
}
.schedule-cell.pending {
  background: rgba(245, 158, 11, 0.12);
}
.schedule-cell.pendingOther {
  background: rgba(59, 130, 246, 0.1);
}
.schedule-cell.maintenance {
  background: rgba(14, 165, 233, 0.12);
}
.schedule-cell.closed {
  background: rgba(148, 163, 184, 0.16);
}

.schedule-cell.selected {
  background: rgba(219, 234, 254, 0.92);
  box-shadow: inset 0 0 0 3px rgba(37, 99, 235, 0.95);
}

.schedule-cell.selected .cell-status {
  color: #1d4ed8;
}

.schedule-forms {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.form-card {
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  padding: 18px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.reservation-form-card {
  display: grid;
  gap: 18px;
}

.form-card-header {
  display: grid;
  gap: 6px;
}

.form-card-header h6 {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
  color: #1f2a44;
}

.form-card-header p {
  margin: 0;
  padding: 14px 16px;
  border-radius: 14px;
  background: #eef4ff;
  border: 1px solid #d6e4ff;
  color: #42526e;
  font-size: 14px;
}

.beauty-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px 28px;
}

.field-card {
  display: grid;
  gap: 8px;
}

.field-full {
  grid-column: 1 / -1;
}

.field-label {
  font-size: 15px;
  font-weight: 600;
  color: #23314d;
}

.beauty-form input,
.beauty-form select {
  width: 100%;
  height: 46px;
  border: 1px solid #d8dee9;
  border-radius: 12px;
  padding: 0 14px;
  font-size: 15px;
  color: #1f2937;
  background-color: #fff;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.beauty-form input:focus,
.beauty-form select:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.12);
  background-color: #fff;
}

.arrow-select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image:
    linear-gradient(45deg, transparent 50%, #64748b 50%),
    linear-gradient(135deg, #64748b 50%, transparent 50%),
    linear-gradient(180deg, #ffffff, #ffffff);
  background-position:
    calc(100% - 18px) calc(50% - 3px),
    calc(100% - 12px) calc(50% - 3px),
    0 0;
  background-size:
    6px 6px,
    6px 6px,
    100% 100%;
  background-repeat: no-repeat;
  padding-right: 42px;
}

.recommend-box {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  background: #fafbfc;
}

.recommend-title {
  font-size: 18px;
  font-weight: 600;
  color: #334155;
}

.conflict-card {
  display: grid;
  gap: 12px;
}

.conflict-card.warning {
  border-color: rgba(245, 158, 11, 0.25);
  background: rgba(255, 251, 235, 0.9);
}

.conflict-card.info {
  border-color: rgba(59, 130, 246, 0.18);
  background: rgba(239, 246, 255, 0.9);
}

.conflict-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.conflict-card-head h6 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.conflict-card p {
  margin: 0;
  line-height: 1.7;
  color: #475569;
}

.conflict-chip {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(15, 23, 42, 0.08);
  color: #334155;
}

.recommend-btn {
  min-width: 120px;
}

.submit-reservation-btn {
  width: 100%;
  height: 54px;
  border: none;
  border-radius: 999px;
  font-size: 20px;
  font-weight: 600;
}

@media (max-width: 980px) {
  .beauty-form {
    grid-template-columns: 1fr;
  }

  .field-full {
    grid-column: auto;
  }

  .recommend-box {
    flex-direction: column;
    align-items: stretch;
  }
}

.legend-item.free {
  background: rgba(16, 185, 129, 0.08);
}
.legend-item.reserved {
  background: rgba(239, 68, 68, 0.10);
}
.legend-item.pending {
  background: rgba(245, 158, 11, 0.12);
}
.legend-item.pendingOther {
  background: rgba(59, 130, 246, 0.1);
}
.legend-item.maintenance {
  background: rgba(14, 165, 233, 0.12);
}
.legend-item.closed {
  background: rgba(148, 163, 184, 0.16);
}

.recommendation-list {
  display: grid;
  gap: 12px;
}

.recommendation-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(248, 250, 252, 0.88);
}

.recommendation-main {
  display: grid;
  gap: 4px;
}

.recommendation-main strong {
  color: #1e293b;
}

.recommendation-main span {
  color: #334155;
  font-size: 14px;
}

.recommendation-main small {
  color: #64748b;
}

@media (max-width: 980px) {
  .lab-hero-meta {
    justify-content: flex-start;
  }
  .detail-tabs {
    width: 100%;
    justify-content: space-between;
  }
  .tab-btn {
    flex: 1;
    text-align: center;
  }
  .grid-form {
    grid-template-columns: 1fr;
  }
  .schedule-table th.sticky-col,
  .schedule-table td.sticky-col {
    min-width: 140px;
  }
}
</style>
