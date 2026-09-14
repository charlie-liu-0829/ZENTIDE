<template>
  <div class="top-panel">
    <el-form :model="searchForm" @submit.prevent>
      <el-row :gutter="10">
        <el-col :span="5">
          <el-form-item label="用户昵称">
            <el-input clearable placeholder="输入用户昵称" v-model="searchForm.nickNameFuzzy"></el-input>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-form-item label="状态" prop="">
            <el-select clearable placeholder="请选择状态" v-model="searchForm.status">
              <el-option :value="0" label="禁用"></el-option>
              <el-option :value="1" label="启用"></el-option>
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="5">
          <el-button type="primary" @click="loadDataList">搜索</el-button>
        </el-col>
      </el-row>
    </el-form>
  </div>
  <el-card class="table-data-card">
    <div class="table-panel">
      <Table ref="tableInfoRef" :columns="columns" :fetch="loadDataList" :dataSource="tableData">
        <template #slotAvatar="{ row }">
          <Avatar :avatar="row.avatar || undefined" :width="50"></Avatar>
        </template>

        <template #slotNickName="{ row }"> {{ row.nickName }} ({{ SEX_MAP[row.sex] || '未知' }}) </template>

        <template #slotJoinTime="{ row }">
          <div>加入时间：{{ row.joinTime }}</div>
          <div>最后登录时间：{{ row.lastLoginTime }}</div>
        </template>

        <template #slotStatus="{ row }">
          <el-tag v-if="row.status == 0" effect="dark" type="danger">已禁用</el-tag>
          <el-tag v-if="row.status == 1" effect="dark" type="success">正常</el-tag>
        </template>

        <template #slotOperation="{ row }">
          <a href="javascript:void(0)" class="a-link" @click="changeStatus(row)">{{
            row.status == 0 ? '启用' : '禁用'
          }}</a>
        </template>
      </Table>
    </div>
  </el-card>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
const { proxy } = getCurrentInstance()

const SEX_MAP = {
  0: '女',
  1: '男',
  2: '保密',
}

const columns = [
  {
    label: '头像',
    prop: 'avatar',
    scopedSlots: 'slotAvatar',
  },
  {
    label: '昵称',
    prop: 'nickName',
    scopedSlots: 'slotNickName',
  },
  {
    label: '邮箱',
    prop: 'email',
  },
  {
    label: '加入时间',
    prop: 'joinTime',
    scopedSlots: 'slotJoinTime',
  },
  {
    label: '最后登录IP',
    prop: 'lastLoginIp',
  },
  {
    label: '状态',
    prop: 'status',
    scopedSlots: 'slotStatus',
  },
  {
    label: '操作',
    prop: 'operation',
    width: 80,
    scopedSlots: 'slotOperation',
  },
]

const tableInfoRef = ref()
const searchForm = ref({})
const tableData = ref({})
const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  }
  Object.assign(params, searchForm.value)
  let result = await proxy.Request({
    url: proxy.Api.loadUser,
    params: params,
  })
  if (!result) {
    return
  }
  Object.assign(tableData.value, result.data)
}

const changeStatus = (row) => {
  proxy.Confirm({
    message: `确定要${row.status == 0 ? '启用' : '禁用'}吗？`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.changeStatus,
        params: {
          userId: row.userId,
          status: row.status == 0 ? 1 : 0,
        },
      })
      if (!result) {
        return
      }
      proxy.Message.success('操作成功')
      loadDataList()
    },
  })
}
</script>

<style lang="scss" scoped>
.table-panel {
  height: calc(100vh - 135px);
}
</style>
