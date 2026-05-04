<template>
  <section class="content-grid users-page">
    <BasePanel panel-class="users-panel">
      <div class="toolbar">
        <div class="toolbar-top">
          <div class="toolbar-title">用户管理</div>
        </div>
        <div class="toolbar-row">
          <div class="toolbar-filters">
            <input v-model="keyword" class="toolbar-input" placeholder="搜索学号/工号或姓名"/>
            <select v-model="filters.roleCode" class="toolbar-select">
              <option :value="null">全部角色</option>
              <option value="TEACHER">教师</option>
              <option value="STUDENT">学生</option>
            </select>
            <select v-model="filters.status" class="toolbar-select">
              <option :value="null">全部状态</option>
              <option :value="1">启用</option>
              <option :value="0">禁用</option>
            </select>
          </div>
          <div class="toolbar-actions">
            <button type="button" class="ghost-btn toolbar-ghost-btn" @click="loadUsers">查询</button>
            <button type="button" class="ghost-btn toolbar-ghost-btn" @click="handleReset">重置</button>
            <button type="button" class="ghost-btn" @click="openImportDialog">批量导入</button>
            <button type="button" class="primary-btn" @click="openCreateDialog">新增用户</button>
          </div>
        </div>
      </div>

      <p v-if="message" class="info-text">{{ message }}</p>

      <BaseTable
        class="users-table"
        :headers="['学号/工号', '姓名', '部门', '角色', '账号状态', '性别', '手机号', '信用分', '违纪次数', '操作']"
      >
        <tr v-if="loading">
          <td :colspan="10" class="table-empty">数据加载中...</td>
        </tr>
        <tr v-else-if="users.length === 0">
          <td :colspan="10" class="table-empty">暂无用户数据</td>
        </tr>
        <tr
          v-for="user in users"
          :key="user.id"
          class="user-row"
          @click="openEditDialog(user)"
        >
          <td><span class="cell-text cell-text-wide">{{ user.userNo }}</span></td>
          <td><span class="cell-text">{{ user.realName }}</span></td>
          <td><span class="cell-text">{{ departmentLabel(user.departmentId) }}</span></td>
          <td><span class="cell-text">{{ roleLabel(user.roleIds) }}</span></td>
          <td>
            <span :class="getStatusBadgeClass(user.status)">{{ user.status === 1 ? '启用' : '禁用' }}</span>
          </td>
          <td><span class="cell-text cell-text-tight">{{ genderLabel(user.gender) }}</span></td>
          <td><span class="cell-text">{{ user.phone || '--' }}</span></td>
          <td>{{ user.creditScore }}</td>
          <td>{{ user.violationCount }}</td>
          <td>
            <div class="row-actions">
              <button type="button" class="ghost-btn small-btn" @click.stop="openEditDialog(user)">编辑</button>
            </div>
          </td>
        </tr>
      </BaseTable>

      <div class="pagination-wrap">
        <span class="pagination-total">共 {{ total }} 条</span>
        <button type="button" class="ghost-btn small-btn" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">
          上一页
        </button>
        <span class="pagination-text">第 {{ pageNum }} / {{ totalPages }} 页</span>
        <button type="button" class="ghost-btn small-btn" :disabled="pageNum >= totalPages"
                @click="changePage(pageNum + 1)">
          下一页
        </button>
      </div>
    </BasePanel>
  </section>

  <teleport to="body">
    <div v-if="userDialogVisible" class="dialog-mask" @click.self="closeDialog">
      <div class="dialog-card">
        <div class="dialog-head">
          <div class="dialog-head-left">
            <p class="dialog-tag">{{ dialogMode === 'create' ? '新增用户' : '编辑用户' }}</p>
            <h3>{{ dialogMode === 'create' ? '创建新账号' : '修改用户信息' }}</h3>
            <p class="dialog-subtitle">点击列表条目也可以直接打开这个弹窗进行编辑。</p>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeDialog">关闭</button>
        </div>

        <div class="dialog-body">
          <div class="dialog-preview">
            <div class="avatar-block">
              <span>{{ previewInitial }}</span>
            </div>
            <div class="preview-name">{{ userForm.realName || '未填写姓名' }}</div>
            <div class="preview-meta">{{ roleSummary || '未分配角色' }}</div>
            <div class="preview-tags">
              <span class="chip">{{ departmentSummary }}</span>
              <span class="chip" :class="getStatusBadgeClass(userForm.status)">
                {{ userForm.status === 1 ? '启用' : '禁用' }}
              </span>
            </div>
            <div class="preview-stats">
              <div>
                <strong>{{ userForm.creditScore }}</strong>
                <span>信用分</span>
              </div>
              <div>
                <strong>{{ userForm.violationCount }}</strong>
                <span>违纪次数</span>
              </div>
            </div>
            <div v-if="dialogMode === 'edit' && userForm.id != null" class="dialog-delete-row">
              <button
                type="button"
                class="danger-chip"
                :disabled="saving"
                @click="handleDeleteCurrent"
              >
                删除当前用户
              </button>
            </div>
          </div>

          <form class="user-form" @submit.prevent="handleSubmit">
            <label>
              <span>学号 / 工号</span>
              <input
                v-model.trim="userForm.userNo"
                :disabled="dialogMode === 'edit'"
                :class="{ 'readonly-field': dialogMode === 'edit' }"
                placeholder="请输入学号或工号"
              />
            </label>
            <label v-if="dialogMode === 'create'">
              <span>初始密码</span>
              <input v-model.trim="userForm.password" type="password" placeholder="请设置初始密码"/>
            </label>
            <label>
              <span>真实姓名</span>
              <input v-model.trim="userForm.realName" placeholder="请输入真实姓名"/>
            </label>
            <label>
              <span>所属部门</span>
              <select
                v-model="userForm.departmentId"
                :disabled="isDepartmentLocked"
                :class="{ 'readonly-field': isDepartmentLocked }"
              >
                <option v-if="!isDepartmentLocked" :value="null">请选择部门</option>
                <option v-for="dept in visibleDepartmentOptions" :key="dept.value" :value="dept.value">{{
                    dept.label
                  }}
                </option>
              </select>
            </label>
            <label>
              <span>角色</span>
              <select v-model="selectedRoleId">
                <option :value="null">请选择角色</option>
                <option v-for="role in roleOptions" :key="role.value" :value="role.value">{{ role.label }}</option>
              </select>
            </label>
            <label>
              <span>性别</span>
              <select v-model="userForm.gender">
                <option :value="null">未知</option>
                <option :value="1">男</option>
                <option :value="2">女</option>
              </select>
            </label>
            <label>
              <span>手机号码</span>
              <input v-model.trim="userForm.phone" placeholder="请输入手机号码"/>
            </label>
            <label>
              <span>邮箱</span>
              <input v-model.trim="userForm.email" placeholder="请输入邮箱地址"/>
            </label>
            <label>
              <span>账号状态</span>
              <select v-model="userForm.status">
                <option :value="1">启用</option>
                <option :value="0">禁用</option>
              </select>
            </label>

            <div class="dialog-actions">
              <button type="button" class="ghost-btn" @click="closeDialog">取消</button>
              <button type="submit" class="primary-btn" :disabled="saving">
                {{ saving ? '保存中...' : '保存' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </teleport>

  <teleport to="body">
    <div v-if="deleteConfirmVisible && deleteTarget" class="dialog-mask" @click.self="closeDeleteConfirm">
      <div class="dialog-card delete-confirm-dialog">
        <div class="delete-confirm-head">
          <span class="delete-confirm-badge">删除确认</span>
          <h3>确认删除当前用户？</h3>
          <p>
            删除后将无法恢复，
            <strong>{{ deleteTarget.realName }}</strong>
            的账号数据会被移除。
          </p>
        </div>

        <div class="delete-confirm-preview">
          <div>
            <span>学号 / 工号</span>
            <strong>{{ deleteTarget.userNo }}</strong>
          </div>
          <div>
            <span>所属部门</span>
            <strong>{{ departmentLabel(deleteTarget.departmentId) }}</strong>
          </div>
          <div>
            <span>角色</span>
            <strong>{{ roleLabel(deleteTarget.roleIds) }}</strong>
          </div>
        </div>

        <div class="delete-confirm-actions">
          <button type="button" class="ghost-btn" :disabled="deleting" @click="closeDeleteConfirm">取消</button>
          <button type="button" class="danger-chip delete-confirm-btn" :disabled="deleting" @click="confirmDelete">
            {{ deleting ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </teleport>

  <teleport to="body">
    <div v-if="importDialogVisible" class="dialog-mask" @click.self="closeImportDialog">
      <div class="dialog-card import-dialog-card">
        <div class="dialog-head">
          <div class="dialog-head-left">
            <p class="dialog-tag">批量导入</p>
            <h3>Excel 用户导入</h3>
            <p class="dialog-subtitle">支持 `.xlsx` 文件，逐行校验并返回导入结果明细。</p>
          </div>
          <button type="button" class="ghost-btn small-btn" @click="closeImportDialog">关闭</button>
        </div>

        <div class="import-body">
          <aside class="import-side">
            <div class="import-side-title">导入说明</div>
            <ul class="import-guide">
              <li>模板字段必须保持固定顺序</li>
              <li>部门名称和角色名称会按名称匹配</li>
              <li>默认密码统一为 123456</li>
              <li>单元格为空或格式不合法时会逐行提示</li>
            </ul>
            <button type="button" class="ghost-btn import-template-btn" @click="handleDownloadTemplate">
              下载导入模板
            </button>
          </aside>

          <section class="import-main">
            <div class="import-actions">
              <button type="button" class="ghost-btn" @click="pickImportFile">选择文件</button>
              <button type="button" class="primary-btn" :disabled="importSubmitting" @click="handleImportSubmit">
                {{ importSubmitting ? '导入中...' : '开始导入' }}
              </button>
              <span class="import-file-name">{{ importFileName }}</span>
            </div>
            <input
              ref="importFileInput"
              type="file"
              accept=".xlsx"
              class="import-file-input"
              @change="handleImportFileChange"
            />

            <div class="import-result-card">
              <div class="import-summary">
                <div class="import-summary-item">
                  <strong>{{ importResult?.total ?? 0 }}</strong>
                  <span>总行数</span>
                </div>
                <div class="import-summary-item success">
                  <strong>{{ importResult?.success ?? 0 }}</strong>
                  <span>成功</span>
                </div>
                <div class="import-summary-item danger">
                  <strong>{{ importResult?.fail ?? 0 }}</strong>
                  <span>失败</span>
                </div>
              </div>

              <div v-if="importResult?.failDetails?.length" class="import-fail-list">
                <div class="import-fail-head">
                  <span>失败行</span>
                  <span>失败原因</span>
                </div>
                <div v-for="item in importResult.failDetails" :key="item.row" class="import-fail-row">
                  <span class="import-fail-row-num">第 {{ item.row }} 行</span>
                  <span class="import-fail-row-reason">{{ item.reason }}</span>
                </div>
              </div>
              <div v-else class="import-empty">上传后会在这里显示导入结果。</div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue';
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import {useGlobalToast} from '../composables/useGlobalToast';
import {fetchDepartmentOptions} from '../api/departments';
import {createUser, deleteUser, downloadImportTemplate, fetchUsers, importUsers, updateUser} from '../api/users';
import {fetchRoleOptions} from '../api/roles';
import {useAuthStore} from '../stores/auth';
import type {OptionItem, UserImportResult, UserVO} from '../types';

type DialogMode = 'create' | 'edit';

const auth = useAuthStore();
const {showToast} = useGlobalToast();
const users = ref<UserVO[]>([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const loading = ref(false);
const message = ref('');
const dialogMode = ref<DialogMode>('create');
const userDialogVisible = ref(false);
const saving = ref(false);
const deleteConfirmVisible = ref(false);
const deleting = ref(false);
const importDialogVisible = ref(false);
const importSubmitting = ref(false);
const importFileInput = ref<HTMLInputElement | null>(null);
const importFile = ref<File | null>(null);
const importResult = ref<UserImportResult | null>(null);
const deleteTarget = ref<UserVO | null>(null);
const departmentOptions = ref<OptionItem[]>([]);
const roleOptions = ref<OptionItem[]>([]);
const keyword = ref('');
const filters = reactive<{
  roleCode: string | null;
  status: number | null;
}>({
  roleCode: null,
  status: null,
});
const userForm = reactive<{
  id: number | null;
  userNo: string;
  password: string;
  realName: string;
  departmentId: number | null;
  roleIds: number[];
  gender: number | null;
  phone: string;
  email: string;
  status: number;
  creditScore: number;
  violationCount: number;
}>({
  id: null,
  userNo: '',
  password: '',
  realName: '',
  departmentId: null,
  roleIds: [],
  gender: null,
  phone: '',
  email: '',
  status: 1,
  creditScore: 0,
  violationCount: 0,
});

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));
const previewInitial = computed(() => (userForm.realName.trim().slice(0, 1) || userForm.userNo.trim().slice(0, 1) || 'U'));
const currentDepartmentId = computed<number | null>(() => auth.currentUser.value?.departmentId ?? null);
const isCreateDepartmentLocked = computed(() => dialogMode.value === 'create' && currentDepartmentId.value != null);
const isEditDepartmentLocked = computed(() => dialogMode.value === 'edit');
const isDepartmentLocked = computed(() => isCreateDepartmentLocked.value || isEditDepartmentLocked.value);
const visibleDepartmentOptions = computed(() => {
  if (!isCreateDepartmentLocked.value) {
    return departmentOptions.value;
  }
  return departmentOptions.value.filter((item) => item.value === currentDepartmentId.value);
});
const selectedRoleId = computed<number | null>({
  get: () => userForm.roleIds[0] ?? null,
  set: (value) => {
    userForm.roleIds = value == null ? [] : [value];
  },
});
const roleSummary = computed(() => {
  if (userForm.roleIds.length === 0) {
    return '';
  }
  const map = new Map(roleOptions.value.map((item) => [item.value, item.label]));
  return userForm.roleIds.map((id) => map.get(id) ?? `角色${id}`).join(' / ');
});
const departmentSummary = computed(() => {
  if (!userForm.departmentId) {
    return '未分配部门';
  }
  return departmentOptions.value.find((item) => item.value === userForm.departmentId)?.label ?? '未分配部门';
});
const importFileName = computed(() => importFile.value?.name ?? '未选择文件');

let queryTimer: number | null = null;

function departmentLabel(departmentId?: number): string {
  if (!departmentId) {
    return '--';
  }
  return departmentOptions.value.find((item) => item.value === departmentId)?.label ?? '--';
}

function roleLabel(roleIds: number[]): string {
  if (!roleIds || roleIds.length === 0) {
    return '--';
  }
  const map = new Map(roleOptions.value.map((item) => [item.value, item.label]));
  return roleIds.map((id) => map.get(id) ?? `角色${id}`).join(' / ');
}

function genderLabel(gender?: number): string {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '--';
}


function getStatusBadgeClass(status: number): string {
  return status === 1 ? 'badge success' : 'badge danger';
}

function resetForm(): void {
  userForm.id = null;
  userForm.userNo = '';
  userForm.password = '';
  userForm.realName = '';
  userForm.departmentId = null;
  userForm.roleIds = [];
  userForm.gender = null;
  userForm.phone = '';
  userForm.email = '';
  userForm.status = 1;
  userForm.creditScore = 0;
  userForm.violationCount = 0;
}

function resetImportState(): void {
  importFile.value = null;
  importResult.value = null;
  if (importFileInput.value) {
    importFileInput.value.value = '';
  }
}

async function loadOptions(): Promise<void> {
  const [deptData, roleData] = await Promise.all([
    fetchDepartmentOptions(auth.token.value),
    fetchRoleOptions(auth.token.value),
  ]);
  departmentOptions.value = deptData;
  roleOptions.value = roleData;
}

async function loadUsers(): Promise<void> {
  loading.value = true;
  message.value = '';
  try {
    const data = await fetchUsers(
      {
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        userNo: keyword.value || undefined,
        realName: keyword.value || undefined,
        roleCode: filters.roleCode ?? undefined,
        status: filters.status ?? undefined,
      },
      auth.token.value,
    );
    users.value = data.list;
    total.value = data.total;
  } catch (error) {
    message.value = error instanceof Error ? error.message : '用户列表加载失败。';
  } finally {
    loading.value = false;
  }
}

function scheduleReload(): void {
  if (queryTimer != null) {
    window.clearTimeout(queryTimer);
  }
  queryTimer = window.setTimeout(() => {
    pageNum.value = 1;
    void loadUsers();
  }, 250);
}

function handleReset(): void {
  keyword.value = '';
  filters.roleCode = null;
  filters.status = null;
  pageNum.value = 1;
  void loadUsers();
}

function changePage(nextPage: number): void {
  if (nextPage < 1 || nextPage > totalPages.value) {
    return;
  }
  pageNum.value = nextPage;
  void loadUsers();
}

function openCreateDialog(): void {
  resetForm();
  dialogMode.value = 'create';
  userForm.departmentId = currentDepartmentId.value;
  userDialogVisible.value = true;
}

function openImportDialog(): void {
  resetImportState();
  importDialogVisible.value = true;
}

function closeImportDialog(): void {
  importDialogVisible.value = false;
  resetImportState();
}

function openEditDialog(user: UserVO): void {
  dialogMode.value = 'edit';
  userForm.id = user.id;
  userForm.userNo = user.userNo;
  userForm.password = '';
  userForm.realName = user.realName ?? '';
  userForm.departmentId = user.departmentId ?? null;
  userForm.roleIds = [...(user.roleIds ?? [])];
  userForm.gender = user.gender ?? null;
  userForm.phone = user.phone ?? '';
  userForm.email = user.email ?? '';
  userForm.status = user.status ?? 1;
  userForm.creditScore = user.creditScore ?? 0;
  userForm.violationCount = user.violationCount ?? 0;
  userDialogVisible.value = true;
}

function closeDialog(): void {
  if (saving.value) {
    return;
  }
  userDialogVisible.value = false;
}

function closeDialogAfterSuccess(): void {
  userDialogVisible.value = false;
  resetForm();
}

function getErrorMessage(error: unknown, fallback: string): string {
  return error instanceof Error && error.message ? error.message : fallback;
}

function closeDeleteConfirm(): void {
  if (deleting.value) {
    return;
  }
  deleteConfirmVisible.value = false;
  deleteTarget.value = null;
}

function pickImportFile(): void {
  importFileInput.value?.click();
}

function handleImportFileChange(event: Event): void {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0] ?? null;
  importFile.value = file;
  importResult.value = null;
}

async function handleDownloadTemplate(): Promise<void> {
  try {
    const blob = await downloadImportTemplate(auth.token.value);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = '用户导入模板.xlsx';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    showToast('success', '模板下载成功', 2200);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '模板下载失败。', 2600);
  }
}

async function handleImportSubmit(): Promise<void> {
  if (!importFile.value) {
    showToast('error', '请先选择 Excel 文件。', 2400);
    return;
  }
  if (!importFile.value.name.toLowerCase().endsWith('.xlsx')) {
    showToast('error', '只支持 .xlsx 文件。', 2400);
    return;
  }

  importSubmitting.value = true;
  try {
    const result = await importUsers(importFile.value, auth.token.value);
    importResult.value = result;
    if (result.success > 0) {
      showToast('success', `导入完成，成功 ${result.success} 条，失败 ${result.fail} 条`, 2600);
    } else {
      showToast('error', `导入失败，失败 ${result.fail} 条`, 2600);
    }
    try {
      await loadUsers();
    } catch {
      message.value = '导入已完成，列表刷新失败，请手动刷新页面。';
    }
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '批量导入失败。', 2600);
  } finally {
    importSubmitting.value = false;
  }
}

function validateUserForm(): boolean {
  if (!userForm.userNo.trim() && dialogMode.value === 'create') {
    showToast('error', '请先填写学号/工号。', 2400);
    return false;
  }
  if (!userForm.password.trim() && dialogMode.value === 'create') {
    showToast('error', '请先填写初始密码。', 2400);
    return false;
  }
  if (!userForm.realName.trim()) {
    showToast('error', '请先填写真实姓名。', 2400);
    return false;
  }
  if (userForm.departmentId == null) {
    showToast('error', '请选择所属部门。', 2400);
    return false;
  }
  if (userForm.roleIds.length === 0) {
    showToast('error', '请至少选择一个角色。', 2400);
    return false;
  }
  if (userForm.gender == null) {
    showToast('error', '请选择性别。', 2400);
    return false;
  }
  return true;
}

function buildCreatePayload() {
  return {
    departmentId: userForm.departmentId ?? undefined,
    userNo: userForm.userNo.trim(),
    password: userForm.password.trim(),
    realName: userForm.realName.trim(),
    gender: userForm.gender ?? undefined,
    phone: userForm.phone.trim() || undefined,
    email: userForm.email.trim() || undefined,
    status: userForm.status,
    roleIds: [...userForm.roleIds],
  };
}

function buildUpdatePayload() {
  return {
    realName: userForm.realName.trim(),
    gender: userForm.gender ?? undefined,
    phone: userForm.phone.trim() || undefined,
    email: userForm.email.trim() || undefined,
    status: userForm.status,
    roleIds: [...userForm.roleIds],
  };
}

async function refreshUsersAfterSuccess(successMessage: string): Promise<void> {
  closeDialogAfterSuccess();
  showToast('success', successMessage, 2400);

  try {
    await loadUsers();
  } catch {
    message.value = `${successMessage}，但列表刷新失败，请手动刷新页面。`;
  }
}

async function handleSubmit(): Promise<void> {
  if (!validateUserForm() || saving.value) {
    return;
  }

  saving.value = true;

  try {
    if (dialogMode.value === 'create') {
      await createUser(buildCreatePayload(), auth.token.value);
      await refreshUsersAfterSuccess('用户创建成功');
      return;
    }

    if (userForm.id == null) {
      showToast('error', '缺少用户 ID，无法更新。', 2600);
      return;
    }

    await updateUser(userForm.id, buildUpdatePayload(), auth.token.value);
    await refreshUsersAfterSuccess('用户信息已更新');
  } catch (error) {
    // 失败时不要关闭弹窗，也不要 resetForm，保留用户已经填写的内容。
    showToast('error', getErrorMessage(error, '保存用户失败。'), 2600);
  } finally {
    saving.value = false;
  }
}

function handleDeleteCurrent(): Promise<void> {
  if (userForm.id == null) {
    return Promise.resolve();
  }
  handleDelete({
    id: userForm.id,
    userNo: userForm.userNo,
    realName: userForm.realName,
    departmentId: userForm.departmentId,
    roleIds: [...userForm.roleIds],
    gender: userForm.gender,
    phone: userForm.phone,
    email: userForm.email,
    status: userForm.status,
    creditScore: userForm.creditScore,
    violationCount: userForm.violationCount,
  } as UserVO);
  return Promise.resolve();
}

function handleDelete(user: UserVO): void {
  if (deleting.value) {
    return;
  }
  deleteTarget.value = user;
  deleteConfirmVisible.value = true;
}

async function confirmDelete(): Promise<void> {
  if (!deleteTarget.value || deleting.value) {
    return;
  }
  deleting.value = true;
  try {
    await deleteUser(deleteTarget.value.id, auth.token.value);
    message.value = '';
    showToast('success', '用户已删除。', 2400);
    try {
      await loadUsers();
    } catch {
      message.value = '删除已完成，列表刷新失败，请手动刷新页面。';
    }
    deleteConfirmVisible.value = false;
    deleteTarget.value = null;
    closeDialog();
    resetForm();
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '删除用户失败。', 2600);
  } finally {
    deleting.value = false;
  }
}

watch([keyword, () => filters.roleCode, () => filters.status], () => {
  scheduleReload();
});

onMounted(async () => {
  await loadOptions();
  await loadUsers();
});

onUnmounted(() => {
  if (queryTimer != null) {
    window.clearTimeout(queryTimer);
    queryTimer = null;
  }
});
</script>

<style scoped>
.users-page {
  grid-template-columns: 1fr;
}

.users-panel {
  min-height: calc(100vh - 160px);
}

.toolbar {
  margin-bottom: 18px;
}

.toolbar-top {
  display: flex;
  justify-content: flex-start;
}

.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  align-items: center;
  gap: 8px;
}

.toolbar-filters {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.toolbar-input,
.toolbar-select {
  border: 1px solid #dbe4ee;
  border-radius: 16px;
  background: #fff;
  color: #0f172a;
  height: 42px;
  padding: 0 14px;
  width: 100%;
  min-width: 0;
}

.toolbar-input {
  width: 240px;
}

.toolbar-select {
  width: 150px;
}

.toolbar-title {
  color: #0f172a;
  font-size: 28px;
  line-height: 1.15;
  font-weight: 700;
}

.user-row {
  cursor: pointer;
}

.user-row:hover {
  background: rgba(56, 102, 219, 0.04);
}

.toolbar-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.toolbar-actions .ghost-btn {
  min-width: 86px;
}

.cell-text {
  display: block;
  max-width: 132px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-text-wide {
  max-width: 148px;
}

.cell-text-tight {
  max-width: 44px;
}

.cell-text-email {
  max-width: 220px;
}

.row-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.danger-chip {
  border: 0;
  border-radius: 999px;
  padding: 10px 18px;
  background: linear-gradient(135deg, #ef4444, #dc2626);
  color: #fff;
  box-shadow: 0 10px 24px rgba(220, 38, 38, 0.22);
  cursor: pointer;
  font-weight: 700;
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}

.danger-chip:hover {
  transform: translateY(-1px);
  box-shadow: 0 14px 28px rgba(220, 38, 38, 0.28);
  filter: saturate(1.05);
}

.table-empty {
  text-align: center;
  color: var(--muted, #6c7b95);
  padding: 24px 12px;
}

.users-table :deep(table) {
  width: 100%;
  min-width: 0;
  table-layout: fixed;
}

.users-table :deep(th),
.users-table :deep(td) {
  white-space: nowrap;
}

.users-table :deep(th:nth-child(1)),
.users-table :deep(td:nth-child(1)) {
  width: 128px;
}

.users-table :deep(th:nth-child(2)),
.users-table :deep(td:nth-child(2)) {
  width: 86px;
}

.users-table :deep(th:nth-child(3)),
.users-table :deep(td:nth-child(3)) {
  width: 92px;
}

.users-table :deep(th:nth-child(4)),
.users-table :deep(td:nth-child(4)) {
  width: 78px;
}

.users-table :deep(th:nth-child(5)),
.users-table :deep(td:nth-child(5)) {
  width: 84px;
}

.users-table :deep(th:nth-child(6)),
.users-table :deep(td:nth-child(6)) {
  width: 52px;
}

.users-table :deep(th:nth-child(7)),
.users-table :deep(td:nth-child(7)) {
  width: 112px;
}

.users-table :deep(th:nth-child(8)),
.users-table :deep(td:nth-child(8)) {
  width: 66px;
}

.users-table :deep(th:nth-child(9)),
.users-table :deep(td:nth-child(9)) {
  width: 74px;
}

.users-table :deep(th:nth-child(10)),
.users-table :deep(td:nth-child(10)) {
  width: 114px;
}

.dialog-mask {
  position: fixed;
  inset: 0;
  background: radial-gradient(circle at top, rgba(37, 99, 235, 0.12), transparent 36%),
  rgba(15, 23, 42, 0.44);
  backdrop-filter: blur(10px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 28px;
  z-index: 48;
}

.dialog-card {
  position: relative;
  width: min(1040px, 100%);
  max-height: min(88vh, 920px);
  overflow: auto;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 250, 255, 0.98)),
  #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 32px;
  box-shadow: 0 32px 80px rgba(15, 23, 42, 0.24),
  inset 0 1px 0 rgba(255, 255, 255, 0.92);
  padding: 22px;
  animation: dialog-enter 0.22s ease;
}

.dialog-card::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 110px;
  border-radius: 32px 32px 0 0;
  background: radial-gradient(circle at 12% 18%, rgba(56, 102, 219, 0.16), transparent 24%),
  radial-gradient(circle at 92% 12%, rgba(15, 140, 127, 0.12), transparent 20%),
  linear-gradient(180deg, rgba(240, 246, 255, 0.94), rgba(255, 255, 255, 0));
  pointer-events: none;
}

.dialog-head {
  position: sticky;
  top: -26px;
  z-index: 2;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  margin: -22px -22px 14px;
  padding: 22px 22px 14px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.74));
  backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 32px 32px 0 0;
}

.dialog-tag {
  display: inline-flex;
  align-items: center;
  padding: 6px 11px;
  border-radius: 999px;
  background: rgba(56, 102, 219, 0.1);
  color: #3866db;
  font-size: 0.74rem;
  letter-spacing: 0.08em;
  font-weight: 700;
  margin: 0 0 8px;
}

.dialog-head h3 {
  margin: 0;
  font-size: 1.72rem;
  line-height: 1.15;
  color: var(--ink, #1b2b49);
}

.dialog-subtitle {
  margin: 8px 0 0;
  color: #70819e;
  font-size: 0.88rem;
}

.dialog-body {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 16px;
}

.dialog-preview {
  position: sticky;
  top: 108px;
  align-self: start;
  border-radius: 26px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(234, 242, 255, 0.96), rgba(248, 250, 255, 0.98));
  border: 1px solid rgba(56, 102, 219, 0.16);
  display: flex;
  flex-direction: column;
  gap: 12px;
  box-shadow: 0 18px 40px rgba(56, 102, 219, 0.08);
}

.avatar-block {
  width: 64px;
  height: 64px;
  border-radius: 20px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #3866db, #6d8ff0);
  color: #fff;
  font-size: 1.55rem;
  font-weight: 800;
  box-shadow: 0 18px 30px rgba(56, 102, 219, 0.24);
}

.preview-name {
  font-size: 1.28rem;
  font-weight: 800;
  color: var(--ink, #1b2b49);
}

.preview-meta {
  color: #5a6b8b;
  line-height: 1.4;
  font-size: 0.9rem;
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.22);
  color: #42557a;
  font-size: 0.84rem;
}

.preview-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.preview-stats div {
  border-radius: 18px;
  padding: 12px 13px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: grid;
  gap: 4px;
}

.preview-stats strong {
  font-size: 1.2rem;
  color: var(--ink, #1b2b49);
}

.preview-stats span {
  color: #637592;
  font-size: 0.82rem;
}

.user-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-content: start;
  padding: 2px;
}

.user-form label {
  display: grid;
  gap: 7px;
}

.user-form label > span {
  font-size: 0.88rem;
  color: #4b5d7f;
  font-weight: 600;
}

.user-form input,
.user-form select {
  min-height: 46px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.32);
  background: rgba(250, 252, 255, 0.98);
  padding: 10px 14px;
  font-size: 0.92rem;
  color: var(--ink, #1b2b49);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.85),
  0 8px 18px rgba(15, 23, 42, 0.03);
  transition: border-color 0.18s ease,
  box-shadow 0.18s ease,
  background 0.18s ease,
  transform 0.18s ease;
}

.user-form select {
  appearance: none;
  -webkit-appearance: none;
  -moz-appearance: none;
  background-image: none;
}

.user-form input:focus,
.user-form select:focus {
  outline: none;
  border-color: rgba(56, 102, 219, 0.55);
  background: #fff;
  box-shadow: 0 0 0 4px rgba(56, 102, 219, 0.12),
  0 16px 28px rgba(56, 102, 219, 0.08);
  transform: translateY(-1px);
}

.user-form .readonly-field:disabled {
  background: rgba(226, 232, 240, 0.58);
  border-color: rgba(148, 163, 184, 0.3);
  color: rgba(71, 85, 105, 0.95);
  box-shadow: none;
  cursor: not-allowed;
  opacity: 1;
  -webkit-text-fill-color: rgba(71, 85, 105, 0.95);
}

.dialog-actions {
  grid-column: 1 / -1;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 6px;
  padding-top: 4px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.dialog-delete-row {
  display: flex;
  justify-content: center;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.delete-confirm-dialog {
  width: min(520px, 92vw);
  display: grid;
  gap: 18px;
}

.delete-confirm-dialog::before {
  display: none;
}

.delete-confirm-head {
  position: relative;
  z-index: 1;
  display: grid;
  gap: 10px;
}

.delete-confirm-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 7px 14px;
  border-radius: 999px;
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #111827;
  font-size: 0.82rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.delete-confirm-head h3 {
  margin: 0;
  color: var(--ink, #1b2b49);
  font-size: 1.5rem;
}

.delete-confirm-head p {
  margin: 0;
  color: #5b6d8d;
  line-height: 1.6;
}

.delete-confirm-head strong {
  color: #dc2626;
}

.delete-confirm-preview {
  display: grid;
  gap: 10px;
}

.delete-confirm-preview div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.96);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.delete-confirm-preview span {
  color: #64748b;
  font-size: 0.9rem;
}

.delete-confirm-preview strong {
  color: var(--ink, #1b2b49);
  text-align: right;
}

.delete-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.delete-confirm-btn {
  min-width: 116px;
  justify-content: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
  flex-wrap: wrap;
}

.pagination-total,
.pagination-text {
  color: #4b5d7f;
}

.info-text {
  margin: 6px 0 14px;
}

.import-dialog-card {
  width: min(1080px, 94vw);
}

.import-body {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
}

.import-side {
  border-radius: 24px;
  padding: 18px;
  background: linear-gradient(180deg, rgba(234, 242, 255, 0.96), rgba(247, 250, 255, 0.98));
  border: 1px solid rgba(56, 102, 219, 0.14);
  display: grid;
  gap: 14px;
  align-content: start;
  box-shadow: 0 18px 36px rgba(56, 102, 219, 0.08);
}

.import-side-title {
  font-size: 1rem;
  font-weight: 800;
  color: var(--ink, #1b2b49);
}

.import-guide {
  margin: 0;
  padding-left: 18px;
  color: #5b6d8d;
  display: grid;
  gap: 8px;
  line-height: 1.5;
  font-size: 0.92rem;
}

.import-template-btn {
  justify-self: start;
}

.import-main {
  border-radius: 24px;
  padding: 18px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 16px;
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.05);
}

.import-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.import-file-name {
  color: #5b6d8d;
  font-size: 0.92rem;
}

.import-file-input {
  display: none;
}

.import-result-card {
  border-radius: 22px;
  padding: 16px;
  background: rgba(250, 252, 255, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.16);
  display: grid;
  gap: 14px;
}

.import-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.import-summary-item {
  border-radius: 18px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(148, 163, 184, 0.14);
  display: grid;
  gap: 4px;
}

.import-summary-item strong {
  font-size: 1.3rem;
  color: var(--ink, #1b2b49);
}

.import-summary-item span {
  color: #617393;
  font-size: 0.86rem;
}

.import-summary-item.success {
  border-color: rgba(16, 185, 129, 0.18);
  background: rgba(236, 253, 245, 0.92);
}

.import-summary-item.success strong {
  color: #059669;
}

.import-summary-item.danger {
  border-color: rgba(239, 68, 68, 0.16);
  background: rgba(254, 242, 242, 0.92);
}

.import-summary-item.danger strong {
  color: #dc2626;
}

.import-fail-list {
  display: grid;
  gap: 10px;
}

.import-fail-head,
.import-fail-row {
  display: grid;
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 12px;
  align-items: center;
}

.import-fail-head {
  color: #627391;
  font-size: 0.84rem;
  font-weight: 700;
}

.import-fail-row {
  border-radius: 16px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(248, 113, 113, 0.14);
}

.import-fail-row-num {
  font-weight: 700;
  color: #1f2f4d;
}

.import-fail-row-reason {
  color: #b42318;
  line-height: 1.5;
}

.import-empty {
  color: #71829e;
  font-size: 0.92rem;
}

@media (max-width: 1100px) {
  .dialog-body {
    grid-template-columns: 1fr;
  }

  .dialog-preview {
    position: static;
  }

  .import-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .toolbar-actions {
    justify-content: flex-start;
  }

  .user-form {
    grid-template-columns: 1fr;
  }
}

@keyframes dialog-enter {
  from {
    opacity: 0;
    transform: translateY(10px) scale(0.985);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>
