<template>
  <section class="device-page">
    <BasePanel :title="panelTitle" panel-class="device-panel">
      <div class="toolbar device-toolbar">
        <div class="toolbar-top">
          <div v-if="showRepairTab" class="tab-strip">
            <button type="button" class="tab-btn" :class="{ active: activeTab === 'inventory' }" @click="activeTab = 'inventory'">
              设备台账
            </button>
            <button
              v-if="showRepairTab"
              type="button"
              class="tab-btn"
              :class="{ active: activeTab === 'repair' }"
              @click="activeTab = 'repair'"
            >
              设备报修
            </button>
          </div>
          <button v-if="isAdmin && activeTab === 'inventory'" type="button" class="primary-btn add-device-btn" @click="openDeviceDialog()">
            新增设备
          </button>
        </div>

        <div class="toolbar-filters">
          <template v-if="activeTab === 'inventory'">
            <input v-model.trim="deviceKeyword" class="toolbar-input" placeholder="搜索设备名称或编号" @keyup.enter="loadDevices(1)" />
            <select v-model="deviceFilters.labType" class="toolbar-select" @change="handleDeviceLabTypeChange">
              <option value="">全部类型</option>
              <option v-for="type in labTypeOptions" :key="type" :value="type">{{ type }}</option>
            </select>
            <select v-model="deviceFilters.labId" class="toolbar-select" @change="loadDevices(1)">
              <option :value="null">全部实验室</option>
              <option v-for="lab in filteredDeviceLabOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
            </select>
            <select v-model="deviceFilters.status" class="toolbar-select" @change="loadDevices(1)">
              <option :value="null">全部状态</option>
              <option :value="1">正常</option>
              <option :value="2">维修中</option>
              <option :value="3">禁用</option>
            </select>
            <button type="button" class="ghost-btn" @click="loadDevices(1)">查询</button>
            <button type="button" class="ghost-btn" @click="resetDeviceFilters">重置</button>
          </template>

          <template v-else>
            <input v-model.trim="repairKeyword" class="toolbar-input" placeholder="搜索报修说明" @keyup.enter="loadRepairs(1)" />
            <select v-model="repairFilters.labType" class="toolbar-select" @change="handleRepairLabTypeChange">
              <option value="">全部类型</option>
              <option v-for="type in labTypeOptions" :key="type" :value="type">{{ type }}</option>
            </select>
            <select v-model="repairFilters.labId" class="toolbar-select" @change="loadRepairs(1)">
              <option :value="null">全部实验室</option>
              <option v-for="lab in filteredRepairLabOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
            </select>
            <select v-model="repairFilters.status" class="toolbar-select" @change="loadRepairs(1)">
              <option :value="null">全部状态</option>
              <option :value="1">待处理</option>
              <option :value="2">处理中</option>
              <option :value="3">已完成</option>
              <option :value="4">已驳回</option>
            </select>
            <button type="button" class="ghost-btn" @click="loadRepairs(1)">查询</button>
            <button type="button" class="ghost-btn" @click="resetRepairFilters">重置</button>
          </template>
        </div>
      </div>
      <p v-if="message" class="info-text">{{ message }}</p>

      <div v-if="activeTab === 'inventory'" class="table-stack">
        <BaseTable :headers="deviceHeaders">
          <tr v-for="device in deviceState.list" :key="device.id" class="row-clickable" @click="openDeviceDetail(device)">
            <td>{{ device.deviceName }}</td>
            <td>{{ device.deviceCode }}</td>
            <td>{{ device.labName || labLabel(device.labId) }}</td>
            <td>{{ device.brand || '--' }}</td>
            <td>{{ device.quantity }}</td>
            <td>{{ device.availableQuantity }}</td>
            <td><span :class="getBadgeClass(deviceStatusText(device.status))">{{ deviceStatusText(device.status) }}</span></td>
            <td>
              <button type="button" class="ghost-btn small-btn" @click.stop="openDeviceDetail(device)">详情</button>
            </td>
          </tr>
          <tr v-if="!deviceLoading && deviceLoaded && deviceState.list.length === 0">
            <td :colspan="deviceHeaders.length" class="empty-cell">暂无设备数据</td>
          </tr>
        </BaseTable>

        <div class="pagination-wrap">
          <span class="pagination-total">共 {{ deviceState.total }} 条</span>
          <button type="button" class="ghost-btn small-btn" :disabled="deviceState.pageNum <= 1" @click="loadDevices(deviceState.pageNum - 1)">
            上一页
          </button>
          <span class="pagination-text">第 {{ deviceState.pageNum }} / {{ deviceTotalPages }} 页</span>
          <button
            type="button"
            class="ghost-btn small-btn"
            :disabled="deviceState.pageNum >= deviceTotalPages"
            @click="loadDevices(deviceState.pageNum + 1)"
          >
            下一页
          </button>
        </div>
      </div>

      <div v-else class="table-stack">
        <BaseTable :headers="repairHeaders">
          <tr
            v-for="repair in repairState.list"
            :key="repair.id"
            class="row-clickable"
            @click="openRepairDetail(repair)"
          >
            <td>{{ repair.deviceName || '--' }}</td>
            <td>{{ repair.labName || '--' }}</td>
            <td>{{ repair.applicantName || '--' }}</td>
            <td>{{ urgencyText(repair.urgencyLevel) }}</td>
            <td><span :class="getBadgeClass(repairStatusText(repair.status))">{{ repairStatusText(repair.status) }}</span></td>
            <td class="repair-note">{{ repair.issueDescription }}</td>
            <td v-if="isAdmin">
              <button type="button" class="ghost-btn small-btn" @click.stop="openRepairDetail(repair)">处理</button>
            </td>
          </tr>
          <tr v-if="!repairLoading && repairLoaded && repairState.list.length === 0">
            <td :colspan="repairHeaders.length" class="empty-cell">暂无报修记录</td>
          </tr>
        </BaseTable>

        <div class="pagination-wrap">
          <span class="pagination-total">共 {{ repairState.total }} 条</span>
          <button type="button" class="ghost-btn small-btn" :disabled="repairState.pageNum <= 1" @click="loadRepairs(repairState.pageNum - 1)">
            上一页
          </button>
          <span class="pagination-text">第 {{ repairState.pageNum }} / {{ repairTotalPages }} 页</span>
          <button
            type="button"
            class="ghost-btn small-btn"
            :disabled="repairState.pageNum >= repairTotalPages"
            @click="loadRepairs(repairState.pageNum + 1)"
          >
            下一页
          </button>
        </div>
      </div>
    </BasePanel>

    <div v-if="deviceDetailVisible" class="dialog-mask" @click.self="closeDeviceDetail">
      <div class="dialog-card detail-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-kicker">设备详情</p>
            <h3>{{ selectedDevice?.deviceName || '--' }}</h3>
          </div>
          <button type="button" class="close-btn" @click="closeDeviceDetail">关闭</button>
        </div>

        <div v-if="selectedDevice" class="detail-grid">
          <div>
            <span>设备编号</span>
            <strong>{{ selectedDevice.deviceCode }}</strong>
          </div>
          <div>
            <span>实验室</span>
            <strong>{{ selectedDevice.labName || labLabel(selectedDevice.labId) }}</strong>
          </div>
          <div>
            <span>品牌 / 型号</span>
            <strong>{{ (selectedDevice.brand || '--') + ' / ' + (selectedDevice.modelNo || '--') }}</strong>
          </div>
          <div>
            <span>库存</span>
            <strong>{{ selectedDevice.quantity }} / {{ selectedDevice.availableQuantity }}</strong>
          </div>
          <div>
            <span>当前状态</span>
            <strong>{{ deviceStatusText(selectedDevice.status) }}</strong>
          </div>
          <div>
            <span>采购日期</span>
            <strong>{{ selectedDevice.purchaseDate || '--' }}</strong>
          </div>
          <div class="detail-note">
            <span>备注</span>
            <strong>{{ selectedDevice.remark || '暂无备注' }}</strong>
          </div>
        </div>

        <label v-if="isAdmin" class="status-select-field">
          <span>修改状态</span>
          <div class="status-select-wrap">
            <select v-model.number="deviceDetailStatus" class="status-select">
              <option :value="1">正常</option>
              <option :value="2">维修中</option>
              <option :value="3">禁用</option>
            </select>
            <span class="status-select-arrow" aria-hidden="true"></span>
          </div>
        </label>

        <div class="dialog-actions">
          <button v-if="isAdmin" type="button" class="ghost-btn" @click="openDeviceDialog(selectedDevice, 'edit')">编辑信息</button>
          <button v-if="isAdmin" type="button" class="ghost-btn" @click="saveDeviceStatus">保存状态</button>
          <button v-if="canCreateRepair" type="button" class="primary-btn" @click="openRepairCreate">报修</button>
          <button v-if="isAdmin" type="button" class="danger-btn" @click="handleDeleteDevice">删除</button>
        </div>
      </div>
    </div>

    <div v-if="deviceDialogVisible" class="dialog-mask" @click.self="closeDeviceDialog">
      <div class="dialog-card form-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-kicker">{{ deviceDialogMode === 'create' ? '新增设备' : '编辑设备' }}</p>
            <h3>设备信息</h3>
          </div>
          <button type="button" class="close-btn" @click="closeDeviceDialog">关闭</button>
        </div>

        <div class="form-grid">
          <label>
            <span>实验室</span>
            <select v-model.number="deviceForm.labId">
              <option v-for="lab in labOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
            </select>
          </label>
          <label>
            <span>设备名称</span>
            <input v-model.trim="deviceForm.deviceName" type="text" />
          </label>
          <label>
            <span>设备编号</span>
            <input v-model.trim="deviceForm.deviceCode" type="text" />
          </label>
          <label>
            <span>品牌</span>
            <input v-model.trim="deviceForm.brand" type="text" />
          </label>
          <label>
            <span>型号</span>
            <input v-model.trim="deviceForm.modelNo" type="text" />
          </label>
          <label>
            <span>总数</span>
            <input v-model.number="deviceForm.quantity" type="number" min="0" />
          </label>
          <label>
            <span>可用数量</span>
            <input v-model.number="deviceForm.availableQuantity" type="number" min="0" />
          </label>
          <label>
            <span>状态</span>
            <select v-model.number="deviceForm.status">
              <option :value="1">正常</option>
              <option :value="2">维修中</option>
              <option :value="3">禁用</option>
            </select>
          </label>
          <label>
            <span>采购日期</span>
            <input v-model="deviceForm.purchaseDate" type="date" />
          </label>
          <label class="full-width">
            <span>备注</span>
            <textarea v-model.trim="deviceForm.remark" rows="4" />
          </label>
        </div>

        <div class="dialog-actions">
          <button type="button" class="ghost-btn" @click="closeDeviceDialog">取消</button>
          <button type="button" class="primary-btn" :disabled="deviceSaving" @click="handleSaveDevice">
            {{ deviceSaving ? '保存中...' : '保存设备' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="repairCreateVisible" class="dialog-mask" @click.self="closeRepairCreate">
      <div class="dialog-card form-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-kicker">设备报修</p>
            <h3>{{ repairCreateContext.deviceName || '--' }}</h3>
          </div>
          <button type="button" class="close-btn" @click="closeRepairCreate">关闭</button>
        </div>

        <div class="detail-grid">
          <div>
            <span>设备编号</span>
            <strong>{{ repairCreateContext.deviceCode || '--' }}</strong>
          </div>
          <div>
            <span>实验室</span>
            <strong>{{ repairCreateContext.labName || '--' }}</strong>
          </div>
        </div>

        <div class="form-grid">
          <label class="full-width">
            <span>问题描述</span>
            <textarea v-model.trim="repairForm.issueDescription" rows="5" placeholder="请描述设备故障、异常现象或报修原因" />
          </label>
          <label>
            <span>紧急程度</span>
            <select v-model.number="repairForm.urgencyLevel">
              <option :value="1">普通</option>
              <option :value="2">紧急</option>
              <option :value="3">非常紧急</option>
            </select>
          </label>
        </div>

        <div class="dialog-actions">
          <button type="button" class="ghost-btn" @click="closeRepairCreate">取消</button>
          <button type="button" class="primary-btn" :disabled="repairSaving" @click="handleCreateRepair">
            {{ repairSaving ? '提交中...' : '提交报修' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="repairDetailVisible" class="dialog-mask" @click.self="closeRepairDetail">
      <div class="dialog-card detail-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-kicker">报修处理</p>
            <h3>{{ selectedRepair?.deviceName || '--' }}</h3>
          </div>
          <button type="button" class="close-btn" @click="closeRepairDetail">关闭</button>
        </div>

        <div v-if="selectedRepair" class="detail-grid">
          <div>
            <span>报修状态</span>
            <strong>{{ repairStatusText(selectedRepair.status) }}</strong>
          </div>
          <div>
            <span>报修人</span>
            <strong>{{ selectedRepair.applicantName || '--' }}</strong>
          </div>
          <div>
            <span>紧急程度</span>
            <strong>{{ urgencyText(selectedRepair.urgencyLevel) }}</strong>
          </div>
          <div>
            <span>处理人</span>
            <strong>{{ selectedRepair.handlerName || '--' }}</strong>
          </div>
          <div class="detail-note">
            <span>问题描述</span>
            <strong>{{ selectedRepair.issueDescription }}</strong>
          </div>
        </div>

        <template v-if="isAdmin">
          <label class="status-select-field">
            <span>处理状态</span>
            <div class="status-select-wrap">
              <select v-model.number="repairProcess.status" class="status-select">
                <option :value="1">待处理</option>
                <option :value="2">处理中</option>
                <option :value="3">已完成</option>
                <option :value="4">已驳回</option>
              </select>
              <span class="status-select-arrow" aria-hidden="true"></span>
            </div>
          </label>
          <div class="form-grid repair-process-grid">
            <label>
              <span>设备状态</span>
              <select v-model.number="repairProcess.deviceStatus">
                <option :value="null">不调整</option>
                <option :value="1">恢复正常</option>
                <option :value="2">调整为维修中</option>
                <option :value="3">调整为禁用</option>
              </select>
            </label>
            <label class="full-width">
              <span>处理结果</span>
              <textarea v-model.trim="repairProcess.handlingResult" rows="4" placeholder="填写处理结果、维修说明或驳回原因" />
            </label>
          </div>
          <div class="dialog-actions">
            <button type="button" class="ghost-btn" @click="closeRepairDetail">取消</button>
            <button type="button" class="primary-btn" :disabled="repairSaving" @click="handleUpdateRepairStatus">
              {{ repairSaving ? '保存中...' : '保存处理结果' }}
            </button>
          </div>
        </template>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useAuthStore } from '../stores/auth';
import { useGlobalToast } from '../composables/useGlobalToast';
import { getBadgeClass } from '../utils/format';
import { getPrimaryRole } from '../access';
import {
  createDevice,
  createDeviceRepair,
  deleteDevice,
  fetchDeviceById,
  fetchDeviceRepairs,
  fetchDevices,
  updateDevice,
  updateDeviceRepairStatus,
  updateDeviceStatus,
  type DeviceQuery,
  type DeviceRepairQuery,
} from '../api/devices';
import { fetchLabs } from '../api/labs';
import type { DeviceDto, DeviceRepairDto, DeviceRepairCreatePayload, DeviceSavePayload, LabDto, OptionItem, PageData } from '../types';

type TabKey = 'inventory' | 'repair';
type DeviceDialogMode = 'create' | 'edit';

const auth = useAuthStore();
const { showToast } = useGlobalToast();

const currentRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const isAdmin = computed(() => currentRole.value === 'ADMIN');
const isTeacher = computed(() => currentRole.value === 'TEACHER');
const canCreateRepair = computed(() => isAdmin.value || isTeacher.value);
const showRepairTab = computed(() => canCreateRepair.value);
const panelTitle = computed(() => (isAdmin.value ? '设备管理' : '设备查询'));

const activeTab = ref<TabKey>('inventory');
const message = ref('');

const deviceHeaders = computed(() => ['设备名称', '编号', '实验室', '品牌', '总数', '可用', '状态', '操作']);
const repairHeaders = computed(() => (isAdmin.value ? ['设备名称', '实验室', '报修人', '紧急', '状态', '问题描述', '操作'] : ['设备名称', '实验室', '报修人', '紧急', '状态', '问题描述']));

const deviceState = ref<PageData<DeviceDto>>({ list: [], total: 0, pageNum: 1, pageSize: 10 });
const repairState = ref<PageData<DeviceRepairDto>>({ list: [], total: 0, pageNum: 1, pageSize: 10 });
const deviceLoaded = ref(false);
const repairLoaded = ref(false);
const deviceLoading = ref(false);
const repairLoading = ref(false);
const deviceSaving = ref(false);
const repairSaving = ref(false);

const labOptions = ref<OptionItem[]>([]);
const labCatalog = ref<LabDto[]>([]);

const deviceKeyword = ref('');
const repairKeyword = ref('');
const deviceFilters = reactive<{ labType: string; labId: number | null; status: number | null }>({ labType: '', labId: null, status: null });
const repairFilters = reactive<{ labType: string; labId: number | null; status: number | null }>({ labType: '', labId: null, status: null });

const selectedDevice = ref<DeviceDto | null>(null);
const selectedRepair = ref<DeviceRepairDto | null>(null);
const deviceDetailVisible = ref(false);
const repairDetailVisible = ref(false);
const repairCreateVisible = ref(false);

const deviceDetailStatus = ref<number>(1);
const deviceDialogVisible = ref(false);
const deviceDialogMode = ref<DeviceDialogMode>('create');

const deviceForm = reactive<DeviceSavePayload>({
  labId: 0,
  deviceName: '',
  deviceCode: '',
  brand: '',
  modelNo: '',
  quantity: 1,
  availableQuantity: 1,
  status: 1,
  purchaseDate: '',
  remark: '',
});

const repairForm = reactive<DeviceRepairCreatePayload>({
  deviceId: 0,
  issueDescription: '',
  urgencyLevel: 1,
});

const repairProcess = reactive<{
  status: number;
  handlingResult: string;
  deviceStatus: number | null;
}>({
  status: 1,
  handlingResult: '',
  deviceStatus: null,
});

const repairCreateContext = reactive({
  deviceName: '',
  deviceCode: '',
  labName: '',
});

const deviceTotalPages = computed(() => Math.max(1, Math.ceil(deviceState.value.total / deviceState.value.pageSize)));
const repairTotalPages = computed(() => Math.max(1, Math.ceil(repairState.value.total / repairState.value.pageSize)));
const labTypeOptions = computed(() => Array.from(new Set(labCatalog.value.map((lab) => lab.labType).filter(Boolean) as string[])));
const filteredDeviceLabOptions = computed(() => filteredLabsByType(deviceFilters.labType));
const filteredRepairLabOptions = computed(() => filteredLabsByType(repairFilters.labType));

onMounted(async () => {
  await loadLabOptions();
  await Promise.all([loadDevices(1), showRepairTab.value ? loadRepairs(1) : Promise.resolve()]);
});

function labLabel(labId: number): string {
  return labOptions.value.find((item) => item.value === labId)?.label || '--';
}

function deviceStatusText(status: number): string {
  if (status === 2) return '维修中';
  if (status === 3) return '禁用';
  return '正常';
}

function repairStatusText(status: number): string {
  if (status === 2) return '处理中';
  if (status === 3) return '已完成';
  if (status === 4) return '已驳回';
  return '待处理';
}

function urgencyText(level: number): string {
  if (level === 2) return '紧急';
  if (level === 3) return '非常紧急';
  return '普通';
}

async function loadLabOptions(): Promise<void> {
  try {
    const departmentId = auth.currentUser.value?.departmentId ?? undefined;
    const data = await fetchLabs({ pageNum: 1, pageSize: 1000, departmentId }, auth.token.value || undefined);
    labCatalog.value = data.list;
    labOptions.value = data.list.map((lab) => ({ label: lab.labName, value: lab.id }));
  } catch {
    labCatalog.value = [];
    labOptions.value = [];
  }
}

function filteredLabsByType(labType: string): OptionItem[] {
  return labCatalog.value
    .filter((lab) => !labType || lab.labType === labType)
    .map((lab) => ({ label: lab.labName, value: lab.id }));
}

function handleDeviceLabTypeChange(): void {
  deviceFilters.labId = null;
  void loadDevices(1);
}

function handleRepairLabTypeChange(): void {
  repairFilters.labId = null;
  void loadRepairs(1);
}

async function loadDevices(pageNum = 1): Promise<void> {
  if (deviceLoading.value) return;
  deviceLoading.value = true;
  message.value = '';
  try {
    const query: DeviceQuery = {
      pageNum,
      pageSize: deviceState.value.pageSize,
      labId: deviceFilters.labId ?? undefined,
      labType: deviceFilters.labType || undefined,
      status: deviceFilters.status ?? undefined,
      deviceName: deviceKeyword.value || undefined,
      deviceCode: deviceKeyword.value || undefined,
    };
    deviceState.value = await fetchDevices(query, auth.token.value);
    deviceLoaded.value = true;
    if (showRepairTab.value && activeTab.value === 'repair') {
      await loadRepairs(1);
    }
  } catch (error) {
    message.value = error instanceof Error ? error.message : '设备列表加载失败';
  } finally {
    deviceLoading.value = false;
  }
}

async function loadRepairs(pageNum = 1): Promise<void> {
  if (!showRepairTab.value || repairLoading.value) return;
  repairLoading.value = true;
  message.value = '';
  try {
    const query: DeviceRepairQuery = {
      pageNum,
      pageSize: repairState.value.pageSize,
      labId: repairFilters.labId ?? undefined,
      labType: repairFilters.labType || undefined,
      status: repairFilters.status ?? undefined,
    };
    const data = await fetchDeviceRepairs(query, auth.token.value);
    const keyword = repairKeyword.value.trim();
    repairState.value = keyword
      ? {
          ...data,
          list: data.list.filter(
            (item) =>
              item.issueDescription.includes(keyword) ||
              (item.deviceName ?? '').includes(keyword) ||
              (item.deviceCode ?? '').includes(keyword),
          ),
        }
      : data;
    repairLoaded.value = true;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '报修列表加载失败';
  } finally {
    repairLoading.value = false;
  }
}

function resetDeviceFilters(): void {
  deviceKeyword.value = '';
  deviceFilters.labType = '';
  deviceFilters.labId = null;
  deviceFilters.status = null;
  void loadDevices(1);
}

function resetRepairFilters(): void {
  repairKeyword.value = '';
  repairFilters.labType = '';
  repairFilters.labId = null;
  repairFilters.status = null;
  void loadRepairs(1);
}

async function openDeviceDetail(device: DeviceDto): Promise<void> {
  try {
    const fresh = await fetchDeviceById(device.id, auth.token.value);
    selectedDevice.value = fresh;
    deviceDetailStatus.value = fresh.status ?? 1;
    deviceDetailVisible.value = true;
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '设备详情加载失败');
  }
}

function closeDeviceDetail(): void {
  deviceDetailVisible.value = false;
}

function openDeviceDialog(device?: DeviceDto | null, mode: DeviceDialogMode = 'create'): void {
  deviceDialogMode.value = mode;
  if (device) {
    deviceForm.labId = device.labId;
    deviceForm.deviceName = device.deviceName;
    deviceForm.deviceCode = device.deviceCode;
    deviceForm.brand = device.brand || '';
    deviceForm.modelNo = device.modelNo || '';
    deviceForm.quantity = device.quantity;
    deviceForm.availableQuantity = device.availableQuantity;
    deviceForm.status = device.status;
    deviceForm.purchaseDate = device.purchaseDate || '';
    deviceForm.remark = device.remark || '';
  } else {
    deviceForm.labId = labOptions.value[0]?.value || 0;
    deviceForm.deviceName = '';
    deviceForm.deviceCode = '';
    deviceForm.brand = '';
    deviceForm.modelNo = '';
    deviceForm.quantity = 1;
    deviceForm.availableQuantity = 1;
    deviceForm.status = 1;
    deviceForm.purchaseDate = '';
    deviceForm.remark = '';
  }
  deviceDialogVisible.value = true;
}

function closeDeviceDialog(): void {
  deviceDialogVisible.value = false;
}

async function handleSaveDevice(): Promise<void> {
  if (!auth.token.value || !deviceForm.labId) return;
  deviceSaving.value = true;
  try {
    if (deviceDialogMode.value === 'create') {
      await createDevice(deviceForm, auth.token.value);
      showToast('success', '设备已创建');
    } else if (selectedDevice.value) {
      await updateDevice(selectedDevice.value.id, deviceForm, auth.token.value);
      showToast('success', '设备已更新');
    }
    deviceDialogVisible.value = false;
    await loadDevices(deviceState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '保存设备失败');
  } finally {
    deviceSaving.value = false;
  }
}

async function saveDeviceStatus(): Promise<void> {
  if (!selectedDevice.value || !auth.token.value) return;
  deviceSaving.value = true;
  try {
    await updateDeviceStatus(selectedDevice.value.id, deviceDetailStatus.value, auth.token.value);
    showToast('success', `已更新为${deviceStatusText(deviceDetailStatus.value)}`);
    await loadDevices(deviceState.value.pageNum);
    selectedDevice.value = await fetchDeviceById(selectedDevice.value.id, auth.token.value);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '保存设备状态失败');
  } finally {
    deviceSaving.value = false;
  }
}

async function handleDeleteDevice(): Promise<void> {
  if (!selectedDevice.value || !auth.token.value) return;
  if (!window.confirm(`确认删除设备“${selectedDevice.value.deviceName}”吗？`)) return;
  deviceSaving.value = true;
  try {
    await deleteDevice(selectedDevice.value.id, auth.token.value);
    showToast('success', '设备已删除');
    closeDeviceDetail();
    await loadDevices(1);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '删除设备失败');
  } finally {
    deviceSaving.value = false;
  }
}

function openRepairCreate(): void {
  if (!selectedDevice.value) return;
  repairCreateContext.deviceName = selectedDevice.value.deviceName;
  repairCreateContext.deviceCode = selectedDevice.value.deviceCode;
  repairCreateContext.labName = selectedDevice.value.labName || labLabel(selectedDevice.value.labId);
  repairForm.deviceId = selectedDevice.value.id;
  repairForm.issueDescription = '';
  repairForm.urgencyLevel = 1;
  repairCreateVisible.value = true;
}

function closeRepairCreate(): void {
  repairCreateVisible.value = false;
}

async function handleCreateRepair(): Promise<void> {
  if (!auth.token.value || !repairForm.deviceId || !repairForm.issueDescription.trim()) {
    showToast('error', '请先填写报修说明');
    return;
  }
  repairSaving.value = true;
  try {
    await createDeviceRepair(repairForm, auth.token.value);
    showToast('success', '报修已提交');
    closeRepairCreate();
    await loadRepairs(1);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '提交报修失败');
  } finally {
    repairSaving.value = false;
  }
}

function openRepairDetail(repair: DeviceRepairDto): void {
  selectedRepair.value = repair;
  repairProcess.status = repair.status ?? 1;
  repairProcess.handlingResult = repair.handlingResult || '';
  repairProcess.deviceStatus = repair.status === 3 ? 1 : null;
  repairDetailVisible.value = true;
}

function closeRepairDetail(): void {
  repairDetailVisible.value = false;
}

async function handleUpdateRepairStatus(): Promise<void> {
  if (!selectedRepair.value || !auth.token.value) return;
  repairSaving.value = true;
  try {
    await updateDeviceRepairStatus(
      selectedRepair.value.id,
      {
        status: repairProcess.status,
        handlingResult: repairProcess.handlingResult.trim() || undefined,
        deviceStatus: repairProcess.deviceStatus,
      },
      auth.token.value,
    );
    showToast('success', '报修已更新');
    closeRepairDetail();
    await loadRepairs(repairState.value.pageNum);
    if (selectedDevice.value) {
      selectedDevice.value = await fetchDeviceById(selectedDevice.value.id, auth.token.value);
    }
    await loadDevices(deviceState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '保存处理结果失败');
  } finally {
    repairSaving.value = false;
  }
}
</script>

<style scoped>
.device-page {
  display: grid;
  gap: 20px;
}

.card-grid {
  display: grid;
  gap: 16px;
}

.metrics-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  border-radius: 24px;
  padding: 18px 20px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96));
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.metric-card span {
  display: block;
  font-size: 14px;
  color: #64748b;
}

.metric-card strong {
  display: block;
  margin-top: 10px;
  font-size: 34px;
  line-height: 1;
  color: #0f172a;
}

.metric-card small {
  display: block;
  margin-top: 10px;
  color: #94a3b8;
}

.toolbar {
  display: grid;
  gap: 18px;
  margin-bottom: 18px;
}

.toolbar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-filters {
  display: grid;
  grid-template-columns: minmax(220px, 1.2fr) repeat(3, minmax(160px, 0.8fr)) auto auto;
  gap: 14px;
  align-items: center;
}

.toolbar-filters > .ghost-btn {
  min-width: 86px;
}

.tab-strip {
  display: inline-flex;
  gap: 8px;
  padding: 6px;
  border-radius: 999px;
  background: #eef2ff;
}

.tab-btn {
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  background: transparent;
  color: #475569;
  font-weight: 600;
}

.tab-btn.active {
  background: linear-gradient(135deg, #1d4ed8, #2563eb);
  color: #fff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.28);
}

.toolbar-input,
.toolbar-select,
.form-grid input,
.form-grid select,
.form-grid textarea {
  border: 1px solid #dbe4ee;
  border-radius: 16px;
  background: #fff;
  color: #0f172a;
}

.toolbar-input,
.toolbar-select {
  height: 42px;
  padding: 0 14px;
}

.toolbar-input {
  width: 100%;
  min-width: 0;
}

.toolbar-select {
  width: 100%;
  min-width: 0;
}

.ghost-btn,
.primary-btn,
.danger-btn,
.close-btn,
.small-btn {
  flex: 0 0 auto;
  border: 0;
  border-radius: 999px;
  padding: 11px 18px;
  font-weight: 600;
  white-space: nowrap;
}

.ghost-btn {
  background: #fff;
  color: #1e293b;
  border: 1px solid #dbe4ee;
}

.primary-btn {
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.danger-btn {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
}

.small-btn {
  padding: 8px 14px;
  font-weight: 500;
}

.add-device-btn {
  height: 42px;
  padding: 0 20px;
  white-space: nowrap;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.18);
}

.table-stack {
  display: grid;
  gap: 14px;
}

.row-clickable {
  cursor: pointer;
}

.empty-cell {
  text-align: center;
  color: #64748b;
  padding: 24px 12px;
}

.pagination-wrap {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  color: #475569;
}

.pagination-total,
.pagination-text {
  font-size: 14px;
}

.repair-note {
  max-width: 360px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(2, 6, 23, 0.46);
}

.dialog-card {
  width: min(980px, 96vw);
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 28px;
  background: #fff;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
  padding: 24px;
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.dialog-kicker {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 800;
}

.dialog-head h3 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.close-btn {
  background: #fff;
  border: 1px solid #dbe4ee;
}

.detail-grid,
.form-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detail-grid {
  padding: 18px;
  border: 1px solid #e2e8f0;
  border-radius: 24px;
  background: linear-gradient(180deg, #fbfcff, #f8fafc);
}

.detail-grid > div,
.form-grid label {
  display: grid;
  gap: 8px;
}

.detail-grid span,
.form-grid span {
  color: #64748b;
  font-size: 14px;
}

.detail-grid strong {
  color: #0f172a;
  font-size: 18px;
}

.detail-note {
  grid-column: 1 / -1;
}

.full-width {
  grid-column: 1 / -1;
}

.form-grid input,
.form-grid select,
.form-grid textarea {
  width: 100%;
  padding: 12px 14px;
  font: inherit;
}

.form-grid textarea {
  resize: vertical;
  min-height: 120px;
}

.status-select-field {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.status-select-wrap {
  position: relative;
}

.status-select {
  width: 100%;
  appearance: none;
  padding: 12px 44px 12px 14px;
  border-radius: 16px;
  border: 1px solid #dbe4ee;
  background: linear-gradient(180deg, #ffffff, #f8fbff);
  color: #0f172a;
  font: inherit;
}

.status-select:focus {
  outline: none;
  border-color: #2563eb;
  box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
}

.status-select-arrow {
  position: absolute;
  right: 16px;
  top: 50%;
  width: 9px;
  height: 9px;
  border-right: 2px solid #64748b;
  border-bottom: 2px solid #64748b;
  transform: translateY(-70%) rotate(45deg);
  pointer-events: none;
}

.dialog-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.info-text {
  margin: 0 0 12px;
  color: #b45309;
}

@media (max-width: 960px) {
  .metrics-grid,
  .detail-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .toolbar-top {
    align-items: stretch;
    flex-direction: column;
  }

  .tab-strip {
    width: 100%;
  }

  .tab-btn {
    flex: 1 1 0;
  }

  .add-device-btn {
    width: 100%;
  }

  .toolbar-filters {
    grid-template-columns: 1fr;
  }

  .dialog-card {
    padding: 18px;
  }
}
</style>
