<template>
  <section class="profile-page">
    <!-- 顶部用户卡片：把信用分放到视觉中心，其他信息作为辅助层级 -->
    <article v-if="profile" class="profile-hero-card">
      <div class="profile-hero-main">
        <div class="profile-identity">
          <span class="profile-eyebrow">个人资料</span>
          <h2>{{ profile.realName }}</h2>
          <p>{{ identityLabel }}：{{ profile.userNo }}</p>
        </div>

        <div class="profile-credit-card" :class="creditTone">
          <span>信用分</span>
          <strong>{{ profile.creditScore }}</strong>
          <small>{{ creditHint }}</small>
        </div>

        <div class="profile-violation-card">
          <span>违纪次数</span>
          <strong>{{ profile.violationCount }}</strong>
          <small>{{ violationHint }}</small>
        </div>
      </div>
    </article>

    <!-- 主内容区：左侧信息分组，右侧轻量操作区 -->
    <section class="profile-content-grid">
      <div class="profile-info-column">
        <article class="info-card">
          <div class="info-card-head">
            <h3>基本信息</h3>
            <p>用于识别当前账号身份</p>
          </div>
          <div v-if="profile" class="info-list">
            <div class="info-item">
              <span>姓名</span>
              <strong>{{ profile.realName }}</strong>
            </div>
            <div class="info-item">
              <span>{{ identityLabel }}</span>
              <strong>{{ profile.userNo }}</strong>
            </div>
            <div class="info-item">
              <span>性别</span>
              <strong>{{ genderText(profile.gender) }}</strong>
            </div>
          </div>
        </article>

        <article class="info-card">
          <div class="info-card-head">
            <h3>联系方式</h3>
            <p>手机号可修改，邮箱仅支持查看</p>
          </div>
          <div v-if="profile" class="info-list">
            <div class="info-item">
              <span>手机号</span>
              <strong>{{ profile.phone || '未填写' }}</strong>
            </div>
            <div class="info-item">
              <span>邮箱</span>
              <strong>{{ profile.email || '未填写' }}</strong>
            </div>
          </div>
        </article>

        <article class="info-card">
          <div class="info-card-head">
            <h3>组织信息</h3>
            <p>展示所属学院与账号状态</p>
          </div>
          <div v-if="profile" class="info-list">
            <div class="info-item">
              <span>所属学院 / 部门</span>
              <strong>{{ departmentName }}</strong>
            </div>
            <div class="info-item">
              <span>账号类型</span>
              <strong>{{ roleLabel }}</strong>
            </div>
            <div class="info-item">
              <span>资料状态</span>
              <strong>{{ profile.phone ? '手机号已完善' : '建议补充手机号' }}</strong>
            </div>
          </div>
        </article>
      </div>

      <aside class="profile-action-column">
        <article class="action-card">
          <div class="action-card-head">
            <h3>账号操作</h3>
            <p>只保留高频、轻量的资料维护动作</p>
          </div>

          <div class="action-list">
            <button type="button" class="action-button" @click="openPhoneDialog">
              <div>
                <strong>修改手机号</strong>
                <span>更新预约联系号码</span>
              </div>
              <em>{{ profile?.phone || '去完善' }}</em>
            </button>

            <button type="button" class="action-button" @click="openPasswordDialog">
              <div>
                <strong>修改密码</strong>
                <span>定期更新密码以提升账号安全</span>
              </div>
              <em>立即修改</em>
            </button>
          </div>

          <div class="action-note">
            邮箱、姓名、{{ identityLabel }}、信用分等核心资料当前不开放在个人资料页修改。
          </div>
        </article>
      </aside>
    </section>
  </section>

  <div v-if="phoneDialogVisible" class="dialog-mask" @click.self="closePhoneDialog">
    <div class="dialog-card profile-dialog" role="dialog" aria-modal="true" aria-labelledby="phone-dialog-title">
      <div class="dialog-head">
        <div>
          <span class="dialog-tag">资料修改</span>
          <h3 id="phone-dialog-title">修改手机号</h3>
          <p>邮箱仅支持查看，手机号修改后会用于预约联系。</p>
        </div>
        <button type="button" class="ghost-btn small-btn" :disabled="phoneSaving" @click="closePhoneDialog">关闭</button>
      </div>

      <form class="dialog-form" @submit.prevent="handleProfileUpdate">
        <label class="dialog-field">
          <span>手机号</span>
          <input v-model.trim="profileForm.phone" placeholder="请输入手机号" />
        </label>

        <label class="dialog-field">
          <span>邮箱</span>
          <input :value="profile?.email || '未填写'" disabled />
        </label>

        <div class="dialog-actions">
          <button type="button" class="ghost-btn" :disabled="phoneSaving" @click="closePhoneDialog">取消</button>
          <button type="submit" class="primary-btn" :disabled="phoneSaving">
            {{ phoneSaving ? '保存中...' : '保存手机号' }}
          </button>
        </div>
      </form>
    </div>
  </div>

  <div v-if="passwordDialogVisible" class="dialog-mask" @click.self="closePasswordDialog">
    <div class="dialog-card profile-dialog" role="dialog" aria-modal="true" aria-labelledby="password-dialog-title">
      <div class="dialog-head">
        <div>
          <span class="dialog-tag">密码修改</span>
          <h3 id="password-dialog-title">修改登录密码</h3>
          <p>请确保新密码与旧密码不同，并且两次输入一致。</p>
        </div>
        <button type="button" class="ghost-btn small-btn" :disabled="passwordSaving" @click="closePasswordDialog">关闭</button>
      </div>

      <form class="dialog-form" @submit.prevent="handlePasswordUpdate">
        <label class="dialog-field">
          <span>旧密码</span>
          <input v-model="passwordForm.oldPassword" type="password" />
        </label>
        <label class="dialog-field">
          <span>新密码</span>
          <input v-model="passwordForm.newPassword" type="password" />
        </label>
        <label class="dialog-field">
          <span>确认新密码</span>
          <input v-model="passwordForm.confirmPassword" type="password" />
        </label>

        <div class="dialog-actions">
          <button type="button" class="ghost-btn" :disabled="passwordSaving" @click="closePasswordDialog">取消</button>
          <button type="submit" class="primary-btn" :disabled="passwordSaving">
            {{ passwordSaving ? '修改中...' : '修改密码' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { getPrimaryRole } from '../access';
import { fetchDepartmentOptions } from '../api/departments';
import { fetchMyProfile, updateMyPassword, updateMyProfile } from '../api/users';
import { useGlobalToast } from '../composables/useGlobalToast';
import { useAuthStore } from '../stores/auth';
import type { OptionItem, UserVO } from '../types';

const auth = useAuthStore();
const { showToast } = useGlobalToast();
const profile = ref<UserVO | null>(null);
const departments = ref<OptionItem[]>([]);
const primaryRole = computed(() => getPrimaryRole(auth.currentUser.value?.roleCodes));

const phoneDialogVisible = ref(false);
const passwordDialogVisible = ref(false);
const phoneSaving = ref(false);
const passwordSaving = ref(false);

const profileForm = reactive({
  phone: '',
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const identityLabel = computed(() => (primaryRole.value === 'STUDENT' ? '学号' : '工号'));
const roleLabel = computed(() => {
  if (primaryRole.value === 'STUDENT') return '学生';
  if (primaryRole.value === 'TEACHER') return '教师';
  if (primaryRole.value === 'ADMIN') return '管理员';
  return '未识别';
});

const departmentName = computed(() => {
  if (!profile.value?.departmentId) {
    return '未分配';
  }
  return departments.value.find((item) => item.value === profile.value?.departmentId)?.label ?? '未分配';
});

const creditTone = computed(() => {
  const score = profile.value?.creditScore ?? 100;
  if (score >= 90) return 'good';
  if (score >= 80) return 'warn';
  return 'danger';
});

const creditHint = computed(() => {
  const score = profile.value?.creditScore ?? 100;
  if (score >= 90) return '当前信用状态良好';
  if (score >= 80) return '请继续保持规范使用';
  return '信用分偏低，请注意违规影响';
});

const violationHint = computed(() => {
  const count = profile.value?.violationCount ?? 0;
  if (count === 0) return '当前无违纪记录';
  if (count === 1) return '已有 1 次违纪记录';
  return `已有 ${count} 次违纪记录`;
});

function genderText(gender?: number): string {
  if (gender === 1) return '男';
  if (gender === 2) return '女';
  return '未填写';
}

async function loadProfile(): Promise<void> {
  const [profileData, departmentOptions] = await Promise.all([
    fetchMyProfile(auth.token.value),
    fetchDepartmentOptions(auth.token.value),
  ]);

  profile.value = profileData;
  departments.value = departmentOptions;
  profileForm.phone = profileData.phone ?? '';
}

function openPhoneDialog(): void {
  profileForm.phone = profile.value?.phone ?? '';
  phoneDialogVisible.value = true;
}

function closePhoneDialog(): void {
  if (phoneSaving.value) {
    return;
  }
  phoneDialogVisible.value = false;
}

function openPasswordDialog(): void {
  passwordForm.oldPassword = '';
  passwordForm.newPassword = '';
  passwordForm.confirmPassword = '';
  passwordDialogVisible.value = true;
}

function closePasswordDialog(): void {
  if (passwordSaving.value) {
    return;
  }
  passwordDialogVisible.value = false;
}

async function handleProfileUpdate(): Promise<void> {
  if (!profileForm.phone.trim()) {
    showToast('error', '请输入手机号');
    return;
  }
  phoneSaving.value = true;
  try {
    const updated = await updateMyProfile(
      {
        phone: profileForm.phone,
      },
      auth.token.value,
    );
    profile.value = updated;
    profileForm.phone = updated.phone ?? '';
    showToast('success', '手机号已更新');
    window.setTimeout(() => {
      phoneDialogVisible.value = false;
    }, 900);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '手机号更新失败，请稍后重试。');
  } finally {
    phoneSaving.value = false;
  }
}

async function handlePasswordUpdate(): Promise<void> {
  if (!passwordForm.oldPassword.trim() || !passwordForm.newPassword.trim() || !passwordForm.confirmPassword.trim()) {
    showToast('error', '请完整填写旧密码、新密码和确认新密码');
    return;
  }
  if (false && passwordForm.oldPassword === passwordForm.newPassword) {
    showToast('error', '新密码不能与旧密码相同');
    return;
  }
  if (false && passwordForm.newPassword !== passwordForm.confirmPassword) {
    showToast('error', '两次输入的新密码不一致');
    return;
  }
  passwordSaving.value = true;
  try {
    await updateMyPassword(
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      },
      auth.token.value,
    );
    showToast('success', '密码修改成功，请牢记新的登录密码');
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
    window.setTimeout(() => {
      passwordDialogVisible.value = false;
    }, 900);
  } catch (error) {
    showToast('error', error instanceof Error ? error.message : '密码修改失败，请稍后重试。');
  } finally {
    passwordSaving.value = false;
  }
}

onMounted(() => {
  void loadProfile();
});
</script>

<style scoped>
.profile-page {
  display: grid;
  gap: 22px;
}

.profile-hero-card,
.info-card,
.action-card {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.96)),
    rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.18s ease,
    box-shadow 0.18s ease,
    border-color 0.18s ease;
}

.profile-hero-card:hover,
.info-card:hover,
.action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(37, 99, 235, 0.18);
  box-shadow: 0 24px 48px rgba(15, 23, 42, 0.1);
}

.profile-hero-card {
  padding: 24px 28px;
}

.profile-hero-main {
  display: grid;
  grid-template-columns: minmax(220px, 1fr) minmax(220px, 0.9fr) minmax(180px, 0.7fr);
  gap: 18px;
  align-items: stretch;
}

.profile-identity {
  display: grid;
  align-content: center;
  gap: 8px;
}

.profile-eyebrow,
.dialog-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(37, 99, 235, 0.08);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.profile-identity h2 {
  margin: 0;
  color: #0f172a;
  font-size: 34px;
  line-height: 1.1;
}

.profile-identity p {
  margin: 0;
  color: #64748b;
  font-size: 15px;
}

.profile-credit-card,
.profile-violation-card {
  display: grid;
  align-content: center;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 20px;
  border: 1px solid rgba(148, 163, 184, 0.14);
  background: rgba(255, 255, 255, 0.9);
}

.profile-credit-card span,
.profile-violation-card span {
  color: #64748b;
  font-size: 13px;
}

.profile-credit-card strong,
.profile-violation-card strong {
  color: #0f172a;
  font-size: 38px;
  line-height: 1;
}

.profile-credit-card small,
.profile-violation-card small {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.profile-credit-card.good {
  background: linear-gradient(180deg, rgba(236, 253, 245, 0.96), rgba(255, 255, 255, 0.96));
}

.profile-credit-card.good strong {
  color: #059669;
}

.profile-credit-card.warn {
  background: linear-gradient(180deg, rgba(255, 251, 235, 0.96), rgba(255, 255, 255, 0.96));
}

.profile-credit-card.warn strong {
  color: #d97706;
}

.profile-credit-card.danger {
  background: linear-gradient(180deg, rgba(254, 242, 242, 0.96), rgba(255, 255, 255, 0.96));
}

.profile-credit-card.danger strong {
  color: #dc2626;
}

.profile-content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.85fr);
  gap: 22px;
  align-items: start;
}

.profile-info-column,
.profile-action-column {
  display: grid;
  gap: 18px;
}

.info-card,
.action-card {
  padding: 22px 24px;
}

.info-card-head,
.action-card-head {
  display: grid;
  gap: 6px;
  margin-bottom: 16px;
}

.info-card-head h3,
.action-card-head h3,
.dialog-head h3 {
  margin: 0;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.15;
}

.info-card-head p,
.action-card-head p,
.dialog-head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.6;
}

.info-list {
  display: grid;
  gap: 12px;
}

.info-item {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.info-item span {
  color: #64748b;
  font-size: 14px;
}

.info-item strong {
  color: #0f172a;
  font-size: 15px;
  text-align: right;
}

.profile-action-column {
  position: sticky;
  top: 20px;
}

.action-list {
  display: grid;
  gap: 12px;
}

.action-button {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: #ffffff;
  text-align: left;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease;
}

.action-button:hover {
  transform: translateY(-1px);
  border-color: rgba(37, 99, 235, 0.22);
  box-shadow: 0 14px 28px rgba(15, 23, 42, 0.08);
}

.action-button strong {
  display: block;
  margin-bottom: 4px;
  color: #0f172a;
  font-size: 15px;
}

.action-button span,
.action-note {
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

.action-button em {
  color: #1d4ed8;
  font-style: normal;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.action-note {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: rgba(248, 250, 252, 0.92);
  border: 1px dashed rgba(148, 163, 184, 0.2);
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
  width: min(560px, 94vw);
  border-radius: 24px;
  background: #ffffff;
  padding: 24px;
  box-shadow: 0 28px 80px rgba(15, 23, 42, 0.24);
}

.dialog-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.dialog-form {
  display: grid;
  gap: 14px;
}

.dialog-field {
  display: grid;
  gap: 8px;
}

.dialog-field span {
  color: #475569;
  font-size: 14px;
  font-weight: 600;
}

.dialog-field input {
  width: 100%;
  height: 46px;
  padding: 0 14px;
  border: 1px solid #dbe4ee;
  border-radius: 16px;
  background: #ffffff;
  color: #0f172a;
  font: inherit;
}

.dialog-field input:disabled {
  background: #f8fafc;
  color: #64748b;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 4px;
}

@media (max-width: 1100px) {
  .profile-hero-main,
  .profile-content-grid {
    grid-template-columns: 1fr;
  }

  .profile-action-column {
    position: static;
  }
}

@media (max-width: 720px) {
  .profile-hero-card,
  .info-card,
  .action-card,
  .dialog-card {
    padding: 18px;
  }

  .profile-identity h2 {
    font-size: 28px;
  }

  .profile-credit-card strong,
  .profile-violation-card strong {
    font-size: 32px;
  }

  .info-item,
  .action-button {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-item strong {
    text-align: left;
  }

  .dialog-head {
    flex-direction: column;
  }

  .dialog-actions {
    flex-direction: column-reverse;
  }

  .dialog-actions .ghost-btn,
  .dialog-actions .primary-btn {
    width: 100%;
  }
}
</style>
