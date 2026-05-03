<template>
  <section class="report-page">
    <BasePanel :title="pageTitle" panel-class="report-panel">
      <div class="toolbar">
        <input
          v-model.trim="keyword"
          class="toolbar-input"
          placeholder="搜索实验名称"
          @keyup.enter="loadReports(1)"
        />
        <select v-model.number="statusFilter" class="toolbar-select">
          <option :value="0">全部状态</option>
          <option :value="1">草稿</option>
          <option :value="2">待审核</option>
          <option :value="3">已通过</option>
          <option :value="4">已退回</option>
        </select>
        <button type="button" class="ghost-btn" @click="loadReports(1)">查询</button>
        <button type="button" class="ghost-btn" @click="resetFilters">重置</button>
        <button v-if="isStudent" type="button" class="primary-btn" @click="openEditor()">新建报告</button>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable :headers="headers">
        <tr v-if="loading">
          <td :colspan="headers.length" class="empty-cell">报告加载中...</td>
        </tr>
        <tr v-else-if="reportState.list.length === 0">
          <td :colspan="headers.length" class="empty-cell">暂无实验报告</td>
        </tr>
        <tr v-for="report in reportState.list" :key="report.id" class="row-clickable" @click="openDetail(report)">
          <td>{{ report.experimentName }}</td>
          <td v-if="!isStudent">{{ report.studentName || '--' }}</td>
          <td>{{ report.teacherName || '--' }}</td>
          <td>{{ report.labName || '--' }}</td>
          <td>{{ report.experimentDate }}</td>
          <td><span :class="getBadgeClass(statusText(report.status))">{{ statusText(report.status) }}</span></td>
          <td>
            <div class="row-actions">
              <button type="button" class="ghost-btn small-btn" @click.stop="openDetail(report)">详情</button>
            </div>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span>共 {{ reportState.total }} 条</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="reportState.pageNum <= 1"
          @click="loadReports(reportState.pageNum - 1)"
        >
          上一页
        </button>
        <span>第 {{ reportState.pageNum }} / {{ totalPages }} 页</span>
        <button
          type="button"
          class="ghost-btn small-btn"
          :disabled="reportState.pageNum >= totalPages"
          @click="loadReports(reportState.pageNum + 1)"
        >
          下一页
        </button>
      </div>
    </BasePanel>

    <div v-if="detailVisible" class="dialog-mask" @click.self="closeDetail">
      <div class="dialog-card detail-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-tag">报告详情</p>
            <h3>{{ selectedReport?.experimentName || '--' }}</h3>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeDetail">关闭</button>
        </div>
        <div v-if="selectedReport" class="detail-grid">
          <div><span>报告编号</span><strong>{{ selectedReport.reportNo }}</strong></div>
          <div><span>状态</span><strong>{{ statusText(selectedReport.status) }}</strong></div>
          <div><span>学生</span><strong>{{ selectedReport.studentName || '--' }}</strong></div>
          <div><span>指导教师</span><strong>{{ selectedReport.teacherName || '--' }}</strong></div>
          <div><span>实验室</span><strong>{{ selectedReport.labName || '--' }}</strong></div>
          <div><span>实验日期</span><strong>{{ selectedReport.experimentDate }}</strong></div>
        </div>
        <div v-if="selectedReport" class="report-sections">
          <article v-for="section in detailSections" :key="section.label">
            <h4>{{ section.label }}</h4>
            <p>{{ section.value || '无' }}</p>
          </article>
          <article class="full-span">
            <h4>耗材使用</h4>
            <p v-if="!selectedReport.consumables?.length">无</p>
            <table v-else class="mini-table">
              <thead>
                <tr>
                  <th>名称</th>
                  <th>规格</th>
                  <th>数量</th>
                  <th>单位</th>
                  <th>状态</th>
                  <th>备注</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in selectedReport.consumables" :key="index">
                  <td>{{ item.consumableName }}</td>
                  <td>{{ item.specification || '--' }}</td>
                  <td>{{ item.quantity ?? 0 }}</td>
                  <td>{{ item.unit || '--' }}</td>
                  <td>{{ consumableUsageStatusText(item.status) }}</td>
                  <td>{{ item.remark || '--' }}</td>
                </tr>
              </tbody>
            </table>
          </article>
          <article class="full-span">
            <h4>教师评语</h4>
            <p>{{ selectedReport.teacherComment || '暂无教师评语' }}</p>
          </article>
        </div>
        <div class="dialog-actions detail-actions">
          <button
            v-if="isStudent && selectedReport && canEdit(selectedReport)"
            type="button"
            class="ghost-btn"
            @click="openEditor(selectedReport)"
          >
            编辑
          </button>
          <button
            v-if="isStudent && selectedReport && canSubmit(selectedReport)"
            type="button"
            class="primary-btn"
            @click="handleSubmit(selectedReport)"
          >
            提交审核
          </button>
          <button
            v-if="isTeacher && selectedReport?.status === 2"
            type="button"
            class="primary-btn"
            @click="openReview(selectedReport)"
          >
            审核
          </button>
          <button v-if="selectedReport" type="button" class="ghost-btn" @click="handleDownload(selectedReport)">
            下载 Word
          </button>
        </div>
      </div>
    </div>

    <div v-if="editorVisible" class="dialog-mask" @click.self="closeEditor">
      <div class="dialog-card editor-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-tag">{{ editingId ? '编辑报告' : '新建报告' }}</p>
            <h3>实验报告</h3>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeEditor">关闭</button>
        </div>
        <div class="form-grid">
          <label><span>实验名称</span><input v-model.trim="form.experimentName" /></label>
          <label><span>实验日期</span><input v-model="form.experimentDate" type="date" /></label>
          <label>
            <span>实验室</span>
            <select v-model.number="form.labId" @change="handleLabChange">
              <option v-for="lab in labOptions" :key="lab.value" :value="lab.value">{{ lab.label }}</option>
            </select>
          </label>
          <label>
            <span>指导教师</span>
            <select v-model.number="form.teacherId">
              <option v-for="teacher in teacherOptions" :key="teacher.value" :value="teacher.value">
                {{ teacher.label }}
              </option>
            </select>
          </label>
          <label class="full-width"><span>实验目的</span><textarea v-model.trim="form.purpose" rows="3" /></label>
          <label class="full-width"><span>实验原理</span><textarea v-model.trim="form.principle" rows="3" /></label>
          <label class="full-width"><span>实验步骤</span><textarea v-model.trim="form.steps" rows="4" /></label>
          <label class="full-width"><span>实验数据/现象</span><textarea v-model.trim="form.resultData" rows="4" /></label>
          <label class="full-width"><span>实验结论</span><textarea v-model.trim="form.conclusion" rows="3" /></label>
        </div>

        <div class="consumable-editor">
          <div class="section-head">
            <h4>耗材使用</h4>
            <button type="button" class="ghost-btn small-btn" @click="addConsumable">添加耗材</button>
          </div>
          <div v-for="(item, index) in form.consumables" :key="index" class="consumable-row">
            <select v-model.number="item.consumableId" @change="syncConsumableRow(item)">
              <option :value="0">请选择耗材</option>
              <option v-for="option in consumableOptions" :key="option.id" :value="option.id">
                {{ option.consumableName }} / {{ option.specification || '无规格' }} / 库存 {{ option.stockQuantity }}{{ option.unit }}
              </option>
            </select>
            <input
              v-model.number="item.quantity"
              type="number"
              min="1"
              :max="consumableStock(item.consumableId)"
              placeholder="数量"
            />
            <input :value="consumableUnit(item.consumableId)" placeholder="单位" disabled />
            <input v-model.trim="item.remark" placeholder="备注" />
            <button type="button" class="ghost-btn small-btn" @click="removeConsumable(index)">删除</button>
          </div>
        </div>

        <div class="dialog-actions">
          <button type="button" class="ghost-btn" @click="closeEditor">取消</button>
          <button type="button" class="primary-btn" :disabled="saving" @click="handleSaveDraft">
            {{ saving ? '保存中...' : '保存草稿' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="reviewVisible" class="dialog-mask" @click.self="closeReview">
      <div class="dialog-card review-dialog">
        <div class="dialog-head">
          <div>
            <p class="dialog-tag">教师审核</p>
            <h3>{{ selectedReport?.experimentName || '--' }}</h3>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeReview">关闭</button>
        </div>
        <label class="review-field">
          <span>教师评语</span>
          <textarea v-model.trim="reviewComment" rows="5" placeholder="填写通过意见或退回原因" />
        </label>
        <div class="dialog-actions">
          <button type="button" class="ghost-btn" :disabled="saving" @click="handleReview(4)">退回</button>
          <button type="button" class="primary-btn" :disabled="saving" @click="handleReview(3)">通过</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { getPrimaryRole } from '../access';
import {
  createExperimentReportDraft,
  downloadExperimentReportWord,
  fetchExperimentReportById,
  fetchExperimentReports,
  reviewExperimentReport,
  submitExperimentReport,
  updateExperimentReportDraft,
} from '../api/experimentReports';
import { fetchConsumableOptions } from '../api/consumables';
import { fetchLabOptions } from '../api/labs';
import { fetchTeacherOptions } from '../api/users';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { useGlobalToast } from '../composables/useGlobalToast';
import { useAuthStore } from '../stores/auth';
import { getBadgeClass } from '../utils/format';
import type {
  ExperimentReportConsumableDto,
  ExperimentReportDto,
  ExperimentReportSavePayload,
  ConsumableDto,
  OptionItem,
  PageData,
} from '../types';

const auth = useAuthStore();
const { showToast } = useGlobalToast();
const role = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));
const isStudent = computed(() => role.value === 'STUDENT');
const isTeacher = computed(() => role.value === 'TEACHER');
const isAdmin = computed(() => role.value === 'ADMIN');
const pageTitle = computed(() => (isTeacher.value ? '报告审核' : isAdmin.value ? '实验报告管理' : '实验报告'));
const headers = computed(() =>
  isStudent.value
    ? ['实验名称', '指导教师', '实验室', '实验日期', '状态', '操作']
    : ['实验名称', '学生', '指导教师', '实验室', '实验日期', '状态', '操作'],
);

const reportState = ref<PageData<ExperimentReportDto>>({ list: [], total: 0, pageNum: 1, pageSize: 10 });
const labOptions = ref<OptionItem[]>([]);
const teacherOptions = ref<OptionItem[]>([]);
const consumableOptions = ref<ConsumableDto[]>([]);
const loading = ref(false);
const saving = ref(false);
const message = ref('');
const keyword = ref('');
const statusFilter = ref(0);
const selectedReport = ref<ExperimentReportDto | null>(null);
const detailVisible = ref(false);
const editorVisible = ref(false);
const reviewVisible = ref(false);
const editingId = ref<number | null>(null);
const reviewComment = ref('');

const form = reactive<ExperimentReportSavePayload>({
  teacherId: 0,
  labId: 0,
  reservationId: null,
  experimentName: '',
  experimentDate: '',
  purpose: '',
  principle: '',
  steps: '',
  resultData: '',
  analysis: '',
  conclusion: '',
  consumables: [],
});

const totalPages = computed(() => Math.max(1, Math.ceil(reportState.value.total / reportState.value.pageSize)));
const detailSections = computed(() => [
  { label: '实验目的', value: selectedReport.value?.purpose },
  { label: '实验原理', value: selectedReport.value?.principle },
  { label: '实验步骤', value: selectedReport.value?.steps },
  { label: '实验数据/现象', value: selectedReport.value?.resultData },
  { label: '实验结论', value: selectedReport.value?.conclusion },
]);

onMounted(async () => {
  await Promise.all([loadOptions(), loadReports(1)]);
});

watch(
  () => form.labId,
  async (labId) => {
    if (editorVisible.value && labId) {
      await loadConsumableOptions(labId);
    }
  },
);

async function loadOptions(): Promise<void> {
  const token = auth.token.value;
  const departmentId = auth.currentUser.value?.departmentId ?? undefined;
  const [labs, teachers] = await Promise.all([
    fetchLabOptions(departmentId ? { departmentId } : {}, token),
    fetchTeacherOptions(token),
  ]);
  labOptions.value = labs;
  teacherOptions.value = teachers;
}

async function loadConsumableOptions(labId: number): Promise<void> {
  consumableOptions.value = labId ? await fetchConsumableOptions(labId, auth.token.value) : [];
}

async function loadReports(pageNum = 1): Promise<void> {
  loading.value = true;
  message.value = '';
  try {
    reportState.value = await fetchExperimentReports(
      {
        pageNum,
        pageSize: reportState.value.pageSize,
        status: statusFilter.value || undefined,
        keyword: keyword.value || undefined,
      },
      auth.token.value,
    );
  } catch (error) {
    message.value = error instanceof Error ? error.message : '实验报告加载失败';
  } finally {
    loading.value = false;
  }
}

function resetFilters(): void {
  keyword.value = '';
  statusFilter.value = 0;
  void loadReports(1);
}

function statusText(status: number): string {
  if (status === 2) return '待审核';
  if (status === 3) return '已通过';
  if (status === 4) return '已退回';
  return '草稿';
}

function consumableUsageStatusText(status?: number): string {
  if (status === 2) return '已出库';
  if (status === 3) return '异常';
  return '待提交';
}

function canEdit(report: ExperimentReportDto): boolean {
  return report.status === 1 || report.status === 4;
}

function canSubmit(report: ExperimentReportDto): boolean {
  return report.status === 1 || report.status === 4;
}

async function openDetail(report: ExperimentReportDto): Promise<void> {
  selectedReport.value = await fetchExperimentReportById(report.id, auth.token.value);
  detailVisible.value = true;
}

function closeDetail(): void {
  detailVisible.value = false;
}

async function openEditor(report?: ExperimentReportDto): Promise<void> {
  detailVisible.value = false;
  reviewVisible.value = false;
  const data = report ? await fetchExperimentReportById(report.id, auth.token.value) : null;
  editingId.value = data?.id ?? null;
  form.teacherId = data?.teacherId ?? teacherOptions.value[0]?.value ?? 0;
  form.labId = data?.labId ?? labOptions.value[0]?.value ?? 0;
  form.reservationId = data?.reservationId ?? null;
  form.experimentName = data?.experimentName ?? '';
  form.experimentDate = data?.experimentDate ?? new Date().toISOString().slice(0, 10);
  form.purpose = data?.purpose ?? '';
  form.principle = data?.principle ?? '';
  form.steps = data?.steps ?? '';
  form.resultData = data?.resultData ?? '';
  form.analysis = data?.analysis ?? '';
  form.conclusion = data?.conclusion ?? '';
  await loadConsumableOptions(form.labId);
  form.consumables = data?.consumables?.length
    ? data.consumables.map((item) => ({
        consumableId: item.consumableId,
        consumableName: item.consumableName,
        specification: item.specification,
        quantity: item.quantity ?? 1,
        unit: item.unit,
        remark: item.remark,
      }))
    : [];
  editorVisible.value = true;
}

function closeEditor(): void {
  editorVisible.value = false;
}

function addConsumable(): void {
  form.consumables.push({ consumableId: 0, consumableName: '', specification: '', quantity: 1, unit: '', remark: '' });
}

function removeConsumable(index: number): void {
  form.consumables.splice(index, 1);
}

function handleLabChange(): void {
  form.consumables = [];
  void loadConsumableOptions(form.labId);
}

function findConsumableOption(consumableId?: number): ConsumableDto | undefined {
  return consumableOptions.value.find((item) => item.id === consumableId);
}

function syncConsumableRow(item: ExperimentReportConsumableDto): void {
  const option = findConsumableOption(item.consumableId);
  item.consumableName = option?.consumableName ?? '';
  item.specification = option?.specification;
  item.unit = option?.unit;
  if (!item.quantity || item.quantity < 1) {
    item.quantity = 1;
  }
}

function consumableStock(consumableId?: number): number {
  return findConsumableOption(consumableId)?.stockQuantity ?? 0;
}

function consumableUnit(consumableId?: number): string {
  return findConsumableOption(consumableId)?.unit ?? '';
}

function validateConsumables(): boolean {
  const selected = new Set<number>();
  for (const item of form.consumables) {
    if (!item.consumableId) {
      showToast('error', '请选择耗材');
      return false;
    }
    if (selected.has(item.consumableId)) {
      showToast('error', '同一耗材不能重复选择');
      return false;
    }
    selected.add(item.consumableId);
    const stock = consumableStock(item.consumableId);
    if (!item.quantity || item.quantity <= 0) {
      showToast('error', '耗材使用数量必须大于 0');
      return false;
    }
    if (item.quantity > stock) {
      showToast('error', '耗材使用数量不能超过当前库存');
      return false;
    }
  }
  return true;
}

async function handleSaveDraft(): Promise<void> {
  if (!form.experimentName || !form.teacherId || !form.labId || !form.experimentDate) {
    showToast('error', '请填写实验名称、实验室、指导教师和实验日期');
    return;
  }
  if (!validateConsumables()) {
    return;
  }
  saving.value = true;
  try {
    const payload = normalizedPayload();
    const report = editingId.value
      ? await updateExperimentReportDraft(editingId.value, payload, auth.token.value)
      : await createExperimentReportDraft(payload, auth.token.value);
    showToast('success', '报告草稿已保存');
    editorVisible.value = false;
    selectedReport.value = report;
    await loadReports(reportState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '保存报告失败');
  } finally {
    saving.value = false;
  }
}

async function handleSubmit(report: ExperimentReportDto): Promise<void> {
  saving.value = true;
  try {
    selectedReport.value = await submitExperimentReport(report.id, auth.token.value);
    showToast('success', '报告已提交，耗材已自动出库');
    await loadReports(reportState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '提交报告失败');
  } finally {
    saving.value = false;
  }
}

async function openReview(report: ExperimentReportDto): Promise<void> {
  detailVisible.value = false;
  selectedReport.value = await fetchExperimentReportById(report.id, auth.token.value);
  reviewComment.value = selectedReport.value.teacherComment ?? '';
  reviewVisible.value = true;
}

function closeReview(): void {
  reviewVisible.value = false;
}

async function handleReview(status: number): Promise<void> {
  if (!selectedReport.value) return;
  saving.value = true;
  try {
    selectedReport.value = await reviewExperimentReport(
      selectedReport.value.id,
      { status, teacherComment: reviewComment.value || undefined },
      auth.token.value,
    );
    showToast('success', status === 3 ? '报告已通过' : '报告已退回');
    reviewVisible.value = false;
    await loadReports(reportState.value.pageNum);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '审核失败');
  } finally {
    saving.value = false;
  }
}

async function handleDownload(report: ExperimentReportDto): Promise<void> {
  try {
    const blob = await downloadExperimentReportWord(report.id, auth.token.value);
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `实验报告-${report.studentName || '学生'}-${report.experimentName}.docx`;
    link.click();
    URL.revokeObjectURL(url);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '下载失败');
  }
}

function normalizedPayload(): ExperimentReportSavePayload {
  const consumables: ExperimentReportConsumableDto[] = form.consumables
    .filter((item) => item.consumableId)
    .map((item) => ({
      consumableId: item.consumableId,
      consumableName: item.consumableName || findConsumableOption(item.consumableId)?.consumableName || '',
      specification: item.specification || findConsumableOption(item.consumableId)?.specification || undefined,
      quantity: item.quantity ?? 0,
      unit: item.unit || findConsumableOption(item.consumableId)?.unit || undefined,
      remark: item.remark?.trim() || undefined,
    }));
  return {
    ...form,
    reservationId: form.reservationId || null,
    consumables,
  };
}
</script>

<style scoped>
.report-page {
  display: grid;
  gap: 18px;
}

.toolbar,
.row-actions,
.pagination-wrap,
.dialog-actions,
.section-head {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.toolbar {
  justify-content: flex-end;
  margin-bottom: 16px;
}

.toolbar-input,
.toolbar-select,
.form-grid input,
.form-grid select,
.form-grid textarea,
.review-field textarea,
.consumable-row select,
.consumable-row input {
  border: 1px solid #dbe4ee;
  border-radius: 14px;
  padding: 11px 13px;
  background: #fff;
  color: #0f172a;
  font: inherit;
}

.toolbar-input {
  width: 240px;
}

.toolbar-select {
  width: 150px;
}

.ghost-btn,
.primary-btn {
  border-radius: 999px;
  padding: 10px 17px;
  font-weight: 600;
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

.small-btn {
  padding: 8px 13px;
  font-weight: 500;
}

.row-actions {
  justify-content: center;
}

.row-clickable {
  cursor: pointer;
}

.empty-cell {
  padding: 24px 12px;
  text-align: center;
  color: #64748b;
}

.pagination-wrap {
  justify-content: flex-end;
  margin-top: 14px;
  color: #64748b;
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
  width: min(1080px, 96vw);
  max-height: 90vh;
  overflow-y: auto;
  border-radius: 24px;
  background: #fff;
  padding: 24px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}

.detail-dialog {
  display: flex;
  flex-direction: column;
}

.review-dialog {
  width: min(640px, 94vw);
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-tag {
  margin: 0 0 8px;
  color: #2563eb;
  font-weight: 800;
}

.dialog-head h3 {
  margin: 0;
  color: #0f172a;
}

.detail-grid,
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-grid {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.detail-grid div,
.form-grid label,
.review-field {
  display: grid;
  gap: 8px;
}

.detail-grid span,
.form-grid span,
.review-field span {
  color: #64748b;
  font-size: 14px;
}

.full-width {
  grid-column: 1 / -1;
}

.form-grid textarea,
.review-field textarea {
  resize: vertical;
}

.report-sections {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-top: 16px;
}

.report-sections article {
  min-height: 148px;
  padding: 18px 20px;
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  background:
    linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(255, 255, 255, 0.96)),
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.08), transparent 42%);
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.06);
}

.report-sections h4,
.section-head h4 {
  margin: 0 0 10px;
  color: #0f172a;
  font-size: 17px;
}

.report-sections p {
  margin: 0;
  color: #334155;
  line-height: 1.65;
  white-space: pre-wrap;
}

.mini-table {
  width: 100%;
  border-collapse: collapse;
  overflow: hidden;
  border-radius: 14px;
  background: #fff;
}

.mini-table th,
.mini-table td {
  border-bottom: 1px solid #e2e8f0;
  padding: 10px;
  text-align: left;
}

.mini-table th {
  background: #eff6ff;
  color: #1d4ed8;
  font-weight: 700;
}

.consumable-editor {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.section-head {
  justify-content: space-between;
}

.consumable-row {
  display: grid;
  grid-template-columns: minmax(220px, 1.5fr) 110px 90px minmax(160px, 1fr) auto;
  gap: 10px;
}

.info-text {
  color: #64748b;
}

.dialog-actions {
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-actions {
  position: sticky;
  bottom: -24px;
  z-index: 2;
  margin: 20px -24px -24px;
  padding: 18px 24px 24px;
  border-top: 1px solid #e2e8f0;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.82), #ffffff 38%);
  backdrop-filter: blur(10px);
}

@media (max-width: 960px) {
  .toolbar {
    justify-content: flex-start;
  }

  .toolbar-input,
  .toolbar-select {
    width: 100%;
  }

  .detail-grid,
  .form-grid,
  .consumable-row,
  .report-sections {
    grid-template-columns: 1fr;
  }

  .report-sections article {
    min-height: auto;
  }
}
</style>
