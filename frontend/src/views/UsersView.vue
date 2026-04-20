<template>
  <section class="content-grid sidebar-layout">
    <BasePanel tag="用户表单" title="新增 / 编辑用户" panel-class="form-panel">
      <form class="stack-form">
        <label><span>用户名</span><input value="teacher_zhao" /></label>
        <label><span>真实姓名</span><input value="赵老师" /></label>
        <label>
          <span>角色</span>
          <select>
            <option>教师</option>
            <option>学生</option>
            <option>管理员</option>
          </select>
        </label>
        <label><span>所属部门</span><input value="计算机学院" /></label>
        <label><span>信誉分</span><input value="95" /></label>
        <button type="button" class="primary-btn wide">保存用户</button>
      </form>
    </BasePanel>

    <BasePanel tag="用户列表" title="账号与角色管理" :note="`共 ${users.length} 条`">
      <BaseTable :headers="['用户名', '姓名', '角色', '部门', '状态', '信誉分']">
        <tr v-for="user in users" :key="user.id">
          <td>{{ user.username }}</td>
          <td>{{ user.realName }}</td>
          <td>{{ user.role }}</td>
          <td>{{ user.department }}</td>
          <td><span :class="getBadgeClass(user.status)">{{ user.status }}</span></td>
          <td>{{ user.creditScore }}</td>
        </tr>
      </BaseTable>
    </BasePanel>
  </section>
</template>

<script setup lang="ts">
import BasePanel from '../components/BasePanel.vue';
import BaseTable from '../components/BaseTable.vue';
import { users } from '../data/mock';
import { getBadgeClass } from '../utils/format';
</script>

