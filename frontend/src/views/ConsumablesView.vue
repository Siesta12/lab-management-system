<template>
  <section class="consumables-page">
    <BasePanel tag="耗材台账" title="库存与预警状态">
      <div class="toolbar">
        <div class="toolbar-filters">
          <select v-model="query.labType" class="toolbar-select" @change="handleLabTypeChange">
            <option value="">全部类型</option>
            <option v-for="type in labTypeOptions" :key="type" :value="type">{{ type }}</option>
          </select>
          <select v-model.number="query.labId" class="toolbar-select" @change="loadConsumables(1)">
            <option :value="0">全部实验室</option>
            <option v-for="lab in filteredLabOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
          </select>
          <input v-model.trim="query.keyword" class="toolbar-input" placeholder="搜索名称或编号" @keyup.enter="loadConsumables(1)" />
          <select v-model.number="query.status" class="toolbar-select" @change="loadConsumables(1)">
            <option :value="-1">全部状态</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="toolbar-actions">
          <button type="button" class="ghost-btn" @click="loadConsumables(1)">查询</button>
          <button type="button" class="primary-btn" @click="openCreate">新增耗材</button>
        </div>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>
      <BaseTable :headers="['名称', '编号', '规格', '实验室', '库存', '预警线', '单位', '状态', '操作']">
        <tr v-if="loading">
          <td colspan="9" class="empty-cell">加载中...</td>
        </tr>
        <tr v-else-if="consumableState.list.length === 0">
          <td colspan="9" class="empty-cell">暂无耗材</td>
        </tr>
        <tr v-for="item in consumableState.list" :key="item.id" class="row-clickable" @click="openDetail(item)">
          <td>{{ item.consumableName }}</td>
          <td>{{ item.consumableCode }}</td>
          <td>{{ item.specification || '--' }}</td>
          <td>{{ labName(item.labId) }}</td>
          <td>{{ item.stockQuantity }}</td>
          <td>{{ item.warningThreshold }}</td>
          <td>{{ item.unit }}</td>
          <td><span :class="getBadgeClass(item.status === 0 ? '停用' : item.stockQuantity <= item.warningThreshold ? '预警' : '正常')">{{ item.status === 0 ? '停用' : item.stockQuantity <= item.warningThreshold ? '预警' : '正常' }}</span></td>
          <td>
            <div class="row-actions">
              <button type="button" class="ghost-btn small-btn" @click.stop="openDetail(item)">查看</button>
            </div>
          </td>
        </tr>
      </BaseTable>
      <div class="pagination-wrap">
        <span>共 {{ consumableState.total }} 条</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="consumableState.pageNum <= 1 || loading"
          @click="loadConsumables(consumableState.pageNum - 1)"
        >
          上一页
        </button>
        <span>第 {{ consumableState.pageNum }} / {{ consumableTotalPages }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="consumableState.pageNum >= consumableTotalPages || loading"
          @click="loadConsumables(consumableState.pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>

    <div v-if="detailVisible && selectedConsumable" class="dialog-mask" @click.self="closeDetail">
      <div class="dialog-card small-dialog">
        <div class="dialog-head">
          <h3>{{ selectedConsumable.consumableName }}</h3>
          <button type="button" class="ghost-btn small-btn" @click="closeDetail">关闭</button>
        </div>
        <div class="detail-grid">
          <div><span>编号</span><strong>{{ selectedConsumable.consumableCode }}</strong></div>
          <div><span>实验室</span><strong>{{ labName(selectedConsumable.labId) }}</strong></div>
          <div><span>规格</span><strong>{{ selectedConsumable.specification || '--' }}</strong></div>
          <div><span>单位</span><strong>{{ selectedConsumable.unit }}</strong></div>
          <div><span>当前库存</span><strong>{{ selectedConsumable.stockQuantity }}</strong></div>
          <div><span>预警线</span><strong>{{ selectedConsumable.warningThreshold }}</strong></div>
          <div><span>状态</span><strong>{{ selectedConsumable.status === 0 ? '停用' : selectedConsumable.stockQuantity <= selectedConsumable.warningThreshold ? '预警' : '正常' }}</strong></div>
          <div><span>备注</span><strong>{{ selectedConsumable.remark || '--' }}</strong></div>
        </div>
        <div class="dialog-actions">
          <button type="button" class="ghost-btn" @click="openEdit(selectedConsumable)">编辑</button>
          <button type="button" class="ghost-btn" @click="openStock(selectedConsumable)">调库存</button>
          <button type="button" class="ghost-btn" @click="handleDelete(selectedConsumable)">删除</button>
        </div>
      </div>
    </div>

    <div v-if="editorVisible" class="dialog-mask" @click.self="closeEditor">
      <div class="dialog-card">
        <div class="dialog-head">
          <h3>{{ editingId ? '编辑耗材' : '新增耗材' }}</h3>
          <button type="button" class="ghost-btn small-btn" @click="closeEditor">关闭</button>
        </div>
        <div class="form-grid">
          <label><span>实验室</span><select v-model.number="form.labId"><option v-for="lab in labOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option></select></label>
          <label><span>名称</span><input v-model.trim="form.consumableName" /></label>
          <label><span>编号</span><input v-model.trim="form.consumableCode" /></label>
          <label><span>规格</span><input v-model.trim="form.specification" /></label>
          <label><span>单位</span><input v-model.trim="form.unit" /></label>
          <label><span>库存</span><input v-model.number="form.stockQuantity" type="number" min="0" /></label>
          <label><span>预警线</span><input v-model.number="form.warningThreshold" type="number" min="0" /></label>
          <label><span>状态</span><select v-model.number="form.status"><option :value="1">启用</option><option :value="0">停用</option></select></label>
          <label class="full-width"><span>备注</span><textarea v-model.trim="form.remark" rows="3" /></label>
        </div>
        <div class="dialog-actions">
          <button type="button" class="ghost-btn" @click="closeEditor">取消</button>
          <button type="button" class="primary-btn" :disabled="saving" @click="handleSave">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </div>
    </div>

    <div v-if="stockVisible" class="dialog-mask" @click.self="closeStock">
      <div class="dialog-card small-dialog">
        <div class="dialog-head">
          <h3>库存调整</h3>
          <button type="button" class="ghost-btn small-btn" @click="closeStock">关闭</button>
        </div>
        <div class="form-grid single">
          <label><span>变更类型</span><select v-model="stockForm.changeType"><option value="IN">入库</option><option value="ADJUST">调整</option></select></label>
          <label><span>变更后库存</span><input v-model.number="stockForm.stockQuantity" type="number" min="0" /></label>
          <label><span>备注</span><textarea v-model.trim="stockForm.remark" rows="3" /></label>
        </div>
        <div class="dialog-actions">
          <button type="button" class="primary-btn" :disabled="saving" @click="handleStockSave">保存库存调整</button>
        </div>
      </div>
    </div>

  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import {
  createConsumable,
  deleteConsumable,
  fetchConsumables,
  updateConsumable,
  updateConsumableStock,
} from '../api/consumables';
import { fetchLabs } from '../api/labs';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useGlobalToast } from '../composables/useGlobalToast';
import { useAuthStore } from '../stores/auth';
import { getBadgeClass } from '../utils/format';
import type { ConsumableDto, ConsumableSavePayload, LabDto, OptionItem, PageData } from '../types';

const auth = useAuthStore();
const { showToast } = useGlobalToast();
const labOptions = ref<OptionItem[]>([]);
const labCatalog = ref<LabDto[]>([]);
const consumableState = ref<PageData<ConsumableDto>>({ list: [], total: 0, pageNum: 1, pageSize: 10 });
const loading = ref(false);
const saving = ref(false);
const message = ref('');
const detailVisible = ref(false);
const editorVisible = ref(false);
const stockVisible = ref(false);
const editingId = ref<number | null>(null);
const selectedConsumable = ref<ConsumableDto | null>(null);
const stockTarget = ref<ConsumableDto | null>(null);

const query = reactive({ labType: '', labId: 0, keyword: '', status: -1 });
const form = reactive<ConsumableSavePayload>({
  labId: 0,
  consumableName: '',
  consumableCode: '',
  specification: '',
  unit: '',
  stockQuantity: 0,
  warningThreshold: 0,
  status: 1,
  remark: '',
});
const stockForm = reactive({ stockQuantity: 0, changeType: 'ADJUST', remark: '' });
const consumableTotalPages = computed(() => Math.max(1, Math.ceil(consumableState.value.total / consumableState.value.pageSize)));
const labTypeOptions = computed(() => Array.from(new Set(labCatalog.value.map((lab) => lab.labType).filter(Boolean) as string[])));
const filteredLabOptions = computed(() => labCatalog.value
  .filter((lab) => !query.labType || lab.labType === query.labType)
  .map((lab) => ({ label: lab.labName, value: lab.id })));

onMounted(async () => {
  await loadLabOptions();
  await loadConsumables(1);
});

async function loadLabOptions(): Promise<void> {
  const data = await fetchLabs({ pageNum: 1, pageSize: 1000, departmentId: auth.currentUser.value?.departmentId ?? undefined }, auth.token.value);
  labCatalog.value = data.list;
  labOptions.value = data.list.map((lab) => ({ label: lab.labName, value: lab.id }));
}

function labName(labId?: number): string {
  return labOptions.value.find((lab) => lab.value === labId)?.label ?? '--';
}

async function loadConsumables(pageNum = 1): Promise<void> {
  loading.value = true;
  message.value = '';
  try {
    consumableState.value = await fetchConsumables(
      {
        pageNum,
        pageSize: consumableState.value.pageSize,
        labId: query.labId || undefined,
        labType: query.labType || undefined,
        consumableName: query.keyword || undefined,
        status: query.status >= 0 ? query.status : undefined,
      },
      auth.token.value,
    );
  } catch (error) {
    message.value = error instanceof Error ? error.message : '耗材加载失败';
  } finally {
    loading.value = false;
  }
}

function handleLabTypeChange(): void {
  query.labId = 0;
  void loadConsumables(1);
}

function openCreate(): void {
  closeDetail();
  editingId.value = null;
  Object.assign(form, {
    labId: labOptions.value[0]?.value ?? 0,
    consumableName: '',
    consumableCode: '',
    specification: '',
    unit: '',
    stockQuantity: 0,
    warningThreshold: 0,
    status: 1,
    remark: '',
  });
  editorVisible.value = true;
}

function openDetail(item: ConsumableDto): void {
  selectedConsumable.value = { ...item };
  detailVisible.value = true;
}

function closeDetail(): void {
  detailVisible.value = false;
}

function openEdit(item: ConsumableDto): void {
  closeDetail();
  editingId.value = item.id;
  Object.assign(form, { ...item });
  editorVisible.value = true;
}

function closeEditor(): void {
  editorVisible.value = false;
}

async function handleSave(): Promise<void> {
  if (!form.labId || !form.consumableName || !form.consumableCode || !form.unit) {
    showToast('error', '请填写实验室、名称、编号和单位');
    return;
  }
  saving.value = true;
  try {
    if (editingId.value) {
      await updateConsumable(editingId.value, form, auth.token.value);
    } else {
      await createConsumable(form, auth.token.value);
    }
    showToast('success', '耗材已保存');
    editorVisible.value = false;
    await loadConsumables(consumableState.value.pageNum);
    if (selectedConsumable.value && editingId.value) {
      selectedConsumable.value = { ...selectedConsumable.value, ...form, id: editingId.value };
    }
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '保存失败');
  } finally {
    saving.value = false;
  }
}

async function handleDelete(item: ConsumableDto): Promise<void> {
  if (!window.confirm(`确认删除 ${item.consumableName}？`)) return;
  await deleteConsumable(item.id, auth.token.value);
  showToast('success', '耗材已删除');
  if (selectedConsumable.value?.id === item.id) {
    closeDetail();
    selectedConsumable.value = null;
  }
  await loadConsumables(consumableState.value.pageNum);
}

function openStock(item: ConsumableDto): void {
  closeDetail();
  stockTarget.value = item;
  stockForm.stockQuantity = item.stockQuantity;
  stockForm.changeType = 'ADJUST';
  stockForm.remark = '';
  stockVisible.value = true;
}

function closeStock(): void {
  stockVisible.value = false;
}

async function handleStockSave(): Promise<void> {
  if (!stockTarget.value || stockForm.stockQuantity < 0) return;
  saving.value = true;
  try {
    await updateConsumableStock(stockTarget.value.id, stockForm, auth.token.value);
    showToast('success', '库存已调整');
    stockVisible.value = false;
    if (selectedConsumable.value?.id === stockTarget.value.id) {
      selectedConsumable.value = {
        ...selectedConsumable.value,
        stockQuantity: stockForm.stockQuantity,
      };
    }
    await loadConsumables(consumableState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '库存调整失败');
  } finally {
    saving.value = false;
  }
}

</script>

<style scoped>
.consumables-page {
  display: grid;
  gap: 18px;
}

.toolbar,
.row-actions,
.dialog-actions,
.pagination-wrap {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar {
  margin-bottom: 16px;
}

.toolbar-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.pagination-wrap {
  justify-content: flex-end;
  margin-top: 16px;
}

.toolbar-input,
.toolbar-select,
.form-grid input,
.form-grid select,
.form-grid textarea,
.reject-field textarea {
  border: 1px solid #dbe4ee;
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff;
  color: #0f172a;
  font: inherit;
}

.toolbar-input {
  width: 220px;
}

.toolbar-select {
  width: 160px;
}

.ghost-btn,
.primary-btn {
  border-radius: 999px;
  padding: 10px 16px;
  font-weight: 700;
}

.ghost-btn {
  border: 1px solid #dbe4ee;
  background: #fff;
  color: #334155;
}

.primary-btn {
  border: none;
  background: linear-gradient(135deg, #2563eb, #0891b2);
  color: #fff;
}

.small-btn {
  padding: 7px 11px;
  font-size: 13px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 30;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.35);
}

.dialog-card {
  width: min(760px, 94vw);
  max-height: 90vh;
  overflow: auto;
  border-radius: 18px;
  background: #fff;
  padding: 24px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.22);
}

.small-dialog {
  width: min(520px, 94vw);
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-head h3 {
  margin: 0;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 16px;
  border-radius: 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.detail-grid div,
.form-grid {
  display: grid;
  gap: 8px;
}

.detail-grid span {
  color: #64748b;
  font-size: 14px;
}

.row-clickable {
  cursor: pointer;
}

.row-clickable:hover {
  background: #f8fbff;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-grid.single {
  grid-template-columns: 1fr;
}

.form-grid label,
.reject-field {
  display: grid;
  gap: 8px;
}

.full-width {
  grid-column: 1 / -1;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 18px;
}

.info-text {
  color: #64748b;
}

td small {
  display: block;
  color: #64748b;
}

@media (max-width: 760px) {
  .toolbar-input,
  .toolbar-select,
  .detail-grid,
  .form-grid {
    width: 100%;
    grid-template-columns: 1fr;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
