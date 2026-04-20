<template>
  <section class="content-grid two-columns">
    <BasePanel tag="个人信息" title="查看个人资料">
      <div v-if="profile" class="detail-list">
        <div><strong>姓名</strong><span>{{ profile.realName }}</span></div>
        <div><strong>学号 / 账号</strong><span>{{ profile.userNo }} / {{ profile.username }}</span></div>
        <div><strong>性别</strong><span>{{ genderText(profile.gender) }}</span></div>
        <div><strong>手机号</strong><span>{{ profile.phone || '未填写' }}</span></div>
        <div><strong>邮箱</strong><span>{{ profile.email || '未填写' }}</span></div>
        <div><strong>所属学院 / 部门</strong><span>{{ departmentName }}</span></div>
        <div><strong>信用分</strong><span>{{ profile.creditScore }}</span></div>
        <div><strong>违规次数</strong><span>{{ profile.violationCount }}</span></div>
      </div>
    </BasePanel>

    <BasePanel tag="资料修改" title="修改手机号与邮箱" panel-class="form-panel">
      <form class="stack-form" @submit.prevent="handleProfileUpdate">
        <label><span>手机号</span><input v-model="profileForm.phone" placeholder="请输入手机号" /></label>
        <label><span>邮箱</span><input v-model="profileForm.email" placeholder="请输入邮箱" /></label>
        <button type="submit" class="primary-btn wide">保存资料</button>
      </form>

      <p v-if="profileMessage" class="info-text">{{ profileMessage }}</p>
    </BasePanel>
  </section>

  <section class="content-grid two-columns">
    <BasePanel tag="密码修改" title="修改登录密码" panel-class="form-panel">
      <form class="stack-form" @submit.prevent="handlePasswordUpdate">
        <label><span>原密码</span><input v-model="passwordForm.oldPassword" type="password" /></label>
        <label><span>新密码</span><input v-model="passwordForm.newPassword" type="password" /></label>
        <label><span>确认新密码</span><input v-model="passwordForm.confirmPassword" type="password" /></label>
        <button type="submit" class="primary-btn wide">修改密码</button>
      </form>

      <p v-if="passwordMessage" class="info-text">{{ passwordMessage }}</p>
    </BasePanel>

    <BasePanel tag="说明" title="个人中心可修改范围">
      <ul class="bullet-list">
        <li>当前支持修改手机号、邮箱和登录密码。</li>
        <li>姓名、学号、角色和信用分属于系统基础信息，不在学生端开放修改。</li>
        <li>修改密码时请确保新旧密码不同，且两次输入保持一致。</li>
      </ul>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { fetchDepartmentOptions } from '../api/departments';
import { fetchMyProfile, updateMyPassword, updateMyProfile } from '../api/users';
import BasePanel from '../components/BasePanel.vue';
import { useAuthStore } from '../stores/auth';
import type { OptionItem, UserVO } from '../types';

const auth = useAuthStore();
const profile = ref<UserVO | null>(null);
const departments = ref<OptionItem[]>([]);
const profileMessage = ref('');
const passwordMessage = ref('');

const profileForm = reactive({
  phone: '',
  email: '',
});

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const departmentName = computed(() => {
  if (!profile.value?.departmentId) {
    return '未分配';
  }

  return departments.value.find((item) => item.value === profile.value?.departmentId)?.label ?? '未分配';
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
  profileForm.email = profileData.email ?? '';
}

async function handleProfileUpdate(): Promise<void> {
  try {
    const updated = await updateMyProfile(
      {
        phone: profileForm.phone,
        email: profileForm.email,
      },
      auth.token.value,
    );
    profile.value = updated;
    profileMessage.value = '个人资料已更新。';
  } catch (error) {
    profileMessage.value = error instanceof Error ? error.message : '个人资料更新失败。';
  }
}

async function handlePasswordUpdate(): Promise<void> {
  try {
    await updateMyPassword(
      {
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword,
        confirmPassword: passwordForm.confirmPassword,
      },
      auth.token.value,
    );
    passwordMessage.value = '密码修改成功，请牢记新的登录密码。';
    passwordForm.oldPassword = '';
    passwordForm.newPassword = '';
    passwordForm.confirmPassword = '';
  } catch (error) {
    passwordMessage.value = error instanceof Error ? error.message : '密码修改失败。';
  }
}

onMounted(() => {
  void loadProfile();
});
</script>
