<template>
  <div class="layout">
    <div class="left-side">
      <div class="left-side-content">
        <div class="logo"><span>知潮</span><strong>ZENTIDE</strong><small>社区治理中心</small></div>
        <template v-for="item in menuList" :key="item.name">
          <div :class="['menu-item', route.path == item.path ? 'active' : '']" @click="jump(item)">
            <div :class="['iconfont', `icon-${item.icon}`]"></div>
            <div class="menu-name">{{ item.name }}</div>
            <div
              :class="[
                'iconfont',
                'icon-right',
                'icon-down',
                item.opened ? 'icon-right-opened' : 'icon-right-closed',
              ]"
              v-if="item.children"
            ></div>
          </div>
          <div
            v-if="item.children"
            :class="['submenu-container', item.opened ? 'submenu-opened' : 'submenu-closed']"
          >
            <div
              :key="sub.path"
              :class="['submenu-item', route.path == sub.path ? 'active' : '']"
              v-for="sub in item.children"
              @click="jump(sub)"
            >
              {{ sub.name }}
            </div>
          </div>
        </template>
      </div>
    </div>
    <div class="right">
      <div class="top">
        <div class="breadcrumb">
          <el-breadcrumb>
            <el-breadcrumb-item v-for="item in route.meta.itemList" :key="item">{{
              item
            }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="user-info">欢迎，管理员 <span class="logout" @click="logout">退出</span></div>
      </div>
      <div class="right-body">
        <router-view></router-view>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, getCurrentInstance } from 'vue'
import { useRouter, useRoute } from 'vue-router'
const { proxy } = getCurrentInstance()
const router = useRouter()
const route = useRoute()

const menuList = ref([
  {
    name: '社区运营',
    icon: 'community',
    opened: true,
    children: [
      {
        name: '社区总览',
        path: '/community/overview',
      },
      {
        name: '兴趣现场',
        path: '/community/hubs',
      },
      {
        name: '帖子审核',
        path: '/community/posts',
      },
      {
        name: '加入申请',
        path: '/community/members',
      },
      {
        name: '评论审核',
        path: '/community/comments',
      },
      {
        name: '智能治理',
        path: '/community/governance',
      },
    ],
  },
  {
    name: '社区配置',
    icon: 'setting',
    opened: true,
    children: [
      {
        name: '固定帖子类型',
        path: '/community/post-types',
      },
      {
        name: '兴趣现场方向',
        path: '/community/directions',
      },
      {
        name: '话题管理',
        path: '/community/topics',
      },
      {
        name: '治理规则',
        path: '/community/governance-rules',
      },
    ],
  },
  {
    name: '用户管理',
    icon: 'user',
    opened: true,
    children: [
      {
        name: '用户列表',
        path: '/user/userList',
      },
    ],
  },
])

const jump = (item) => {
  if (item.children) {
    item.opened = !item.opened
    return
  }
  router.push(item.path)
}

const logout = () => {
  proxy.Confirm({
    message: '确定要退出吗?',
    okfun: async () => {
      try {
        await proxy.Request({
          url: proxy.Api.logout,
          showError: false,
        })
      } finally {
        localStorage.removeItem('adminToken')
        window.location.reload()
      }
    },
  })
}
</script>

<style lang="scss" scoped>
.layout {
  display: flex;
  min-width: 1040px;
  min-height: 100vh;
  background: #f3f2ed;

  .left-side {
    position: relative;
    width: 224px;
    height: 100vh;
    overflow: auto;
    background: #202722;
    border-right: 1px solid rgba(255, 255, 255, 0.07);

    .left-side-content {
      position: relative;
      width: 100%;
      min-height: 100%;
      padding: 0 10px 24px;
      box-sizing: border-box;

      .logo {
        display: grid;
        grid-template-columns: auto 1fr;
        align-items: baseline;
        column-gap: 7px;
        padding: 25px 12px 28px;
        color: #f8f5ec;
        font-size: 17px;
        letter-spacing: -0.02em;

        span {
          font-family: serif;
          font-weight: 700;
        }

        strong {
          font-size: 10px;
          letter-spacing: 1.4px;
          color: #d98570;
        }

        small {
          grid-column: 1/-1;
          margin-top: 5px;
          color: rgba(255, 255, 255, 0.44);
          font-size: 10px;
          letter-spacing: 1px;
        }
      }

      .active {
        background: #f3f2ed;
        color: #202722 !important;
      }

      .menu-item {
        display: flex;
        align-items: center;
        color: rgba(255, 255, 255, 0.62);
        height: 44px;
        margin-top: 4px;
        border-radius: 5px;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        padding: 0 12px;
        transition: background 0.15s, color 0.15s;

        &:hover {
          background: rgba(255, 255, 255, 0.07);
          color: #fff;
        }

        .menu-name {
          margin-left: 5px;
          flex: 1;
          width: 0;
        }

        .icon-right {
          font-size: 12px;
          transition: all 0.3s;
        }

        .icon-right-opened {
          transform: rotate(180deg);
        }

        .icon-right-closed {
          transform: rotate(0deg);
        }
      }

      .submenu-container {
        transition: all 0.3s ease;

        .submenu-item {
          display: flex;
          align-items: center;
          color: rgba(255, 255, 255, 0.5);
          height: 36px;
          margin: 2px 0;
          border-radius: 5px;
          font-size: 12px;
          cursor: pointer;
          padding: 0 12px 0 36px;
          transition: background 0.15s, color 0.15s;

          &:hover {
            background: rgba(255, 255, 255, 0.07);
            color: #fff;
          }
        }
      }

      .submenu-opened {
        max-height: 500px;
        opacity: 1;
      }

      .submenu-closed {
        max-height: 0;
        opacity: 0;
      }
    }

  }

  .right {
    flex: 1;
    width: 0;
    height: 100vh;
    overflow: auto;
    background: #f3f2ed;

    .top {
      height: 54px;
      padding: 0 24px;
      background: rgba(243, 242, 237, 0.94);
      border-bottom: 1px solid #dedfd9;
      display: flex;
      align-items: center;
      position: sticky;
      top: 0;
      z-index: 5;

      .breadcrumb {
        flex: 1;

        :deep(.el-breadcrumb) {
          line-height: 54px;
          font-size: 12px;
        }
      }

      .user-info {
        color: var(--text2);
        padding: 0;
        font-size: 12px;
        display: flex;
        align-items: center;

        .logout {
          color: var(--link2);
          cursor: pointer;
          margin-left: 12px;
        }

        .tool {
          margin-left: 5px;
          color: var(--pink);
          cursor: pointer;
        }
      }
    }

    .right-body {
      width: min(1380px, 100%);
      min-height: calc(100% - 54px);
      margin: 0 auto;
      background: transparent;
      overflow: visible;
    }
  }
}
</style>
