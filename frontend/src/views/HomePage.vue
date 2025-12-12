<template>
  <div class="home-page">
    <div class="content">
      <h1 class="title">AI 脑筋急转弯</h1>
      <p class="subtitle">与 AI 对话，享受智力竞技的快乐</p>
      <a-button
        type="primary"
        size="large"
        class="start-button"
        @click="startGame"
        :loading="loading"
      >
        开始游戏
      </a-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { Button as AButton } from 'ant-design-vue'

// 路由实例
const router = useRouter()
// 加载状态
const loading = ref(false)

/**
 * 生成6位数字房间号
 * @returns {number} 6位数字房间号
 */
const generateRoomId = () => {
  let roomId = parseInt(Date.now().toString().slice(-6))
  // 确保是6位数字
  if (roomId < 100000) {
    roomId += 100000
  }
  return roomId
}

/**
 * 开始游戏 - 跳转至聊天页面
 */
const startGame = async () => {
  loading.value = true
  try {
    const roomId = generateRoomId()
    router.push(`/chat/${roomId}`)
  } catch (error) {
    console.error('启动游戏失败:', error)
    message.error('启动游戏失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 全局样式重置 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  width: 100%;
  height: 100%;
  overflow: hidden; /* 禁止页面滚动 */
}

/* 页面容器 */
.home-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f3e8ff; /* 浅紫色背景 */
  position: fixed;
  top: 0;
  left: 0;
}

/* 中间内容卡片 */
.content {
  width: 90%; /* 占满90%宽度，减少左右留白 */
  max-width: 400px; /* 最大宽度限制 */
  background-color: #ffffff;
  border-radius: 16px; /* 圆角 */
  box-shadow: 0 8px 24px rgba(123, 44, 191, 0.15); /* 柔和阴影 */
  padding: 36px 24px; /* 紧凑内边距 */
  text-align: center;
}

/* 标题样式 */
.title {
  font-size: 28px;
  font-weight: 700;
  color: #5a189a; /* 深紫色标题 */
  margin-bottom: 12px; /* 标题下方紧凑间距 */
  line-height: 1.2;
}

/* 副标题样式 */
.subtitle {
  font-size: 15px;
  color: #666666;
  margin-bottom: 24px; /* 副标题与按钮间距 */
  line-height: 1.4;
}

/* 开始游戏按钮 */
.start-button {
  width: 100%; /* 按钮全屏宽，消除左右留白 */
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  background-color: #7b2cbf !important; /* 主紫色按钮 */
  border: none !important;
  color: #ffffff !important;
  transition: all 0.2s ease;
}

.start-button:hover:not(:disabled) {
  background-color: #5a189a !important; /* 深紫色hover效果 */
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(123, 44, 191, 0.3);
}

.start-button:active {
  transform: translateY(0);
}

/* 小屏设备适配 */
@media (max-width: 375px) {
  .content {
    padding: 28px 18px; /* 更小的内边距 */
  }
  
  .title {
    font-size: 24px;
  }
  
  .subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }
  
  .start-button {
    height: 44px;
    font-size: 15px;
  }
}
</style>