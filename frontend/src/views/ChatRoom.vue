<template>
  <div class="chat-page">
    <!-- 左侧边栏 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3>历史对话</h3>
        <a-button 
          type="link" 
          @click="createNewChat"
          class="new-chat-btn"
        >
          + 新对话
        </a-button>
      </div>

      <div class="history-list">
        <div v-if="chatHistories.length === 0" class="empty-history">
          暂无对话记录
        </div>
        <div
          v-for="(history, index) in chatHistories"
          :key="history.roomId"
          class="history-item"
          :class="{ active: currentRoomId === history.roomId }"
          @click="selectHistory(history)"
        >
          <div class="history-title">
            {{ history.roomId === currentRoomId ? '当前对话' : `房间 ${history.roomId}` }}
          </div>
          <div class="history-preview">{{ getHistoryPreview(history) }}</div>
          <div class="history-time">{{ formatTime(history.timestamp) }}</div>
        </div>
      </div>
    </div>

    <!-- 右侧聊天区域 -->
    <div class="chat-room">
      <div class="header">
        <h2>AI 脑筋急转弯</h2>
        <div class="room-info">
          房间号: {{ currentRoomId }}
          <span v-if="isViewingHistory" class="history-tag">（历史记录）</span>
          <span v-else-if="gameEnded" class="ended-tag">（游戏结束）</span>
          <span v-else-if="gameStarted" class="playing-tag">（游戏中）</span>
          <span v-else class="ready-tag">（准备开始）</span>
        </div>
      </div>

      <div class="chat-container">
        <div class="messages" ref="messagesRef">
          <!-- 显示当前游戏轮次 -->
          <div v-if="currentGameRound > 0" class="game-round">
            第 {{ currentGameRound }} 轮游戏
          </div>
          <ChatMessage
            v-for="(message, index) in messages"
            :key="index"
            :content="message.content"
            :isUser="message.isUser"
          />
        </div>
      </div>

      <!-- 输入区域占满右侧底部 -->
      <div class="input-area">
        <!-- 开始/结束按钮在输入框上方左侧 -->
        <div class="game-controls">
          <a-button 
            type="primary" 
            @click="startChat"
            :disabled="gameStarted  && !gameEnded"
            class="start-btn"
          >
            {{ gameEnded ? '重新开始' : '开始游戏' }}
          </a-button>
          <a-button 
            @click="endGame"
            :disabled="!gameStarted || gameEnded"
            class="end-btn"
          >
            结束游戏
          </a-button>
          <a-button 
            v-if="isViewingHistory"
            @click="createNewChat"
            class="new-chat-control-btn"
          >
            返回新对话
          </a-button>
        </div>
        
        <div class="input-group">
          <a-input
            v-model:value="inputMessage"
            placeholder="请输入内容"
            @press-enter="sendMessage"
            :disabled="!gameStarted || gameEnded"
            class="message-input"
          />
          <a-button 
            type="primary" 
            @click="sendMessage" 
            :disabled="!gameStarted || gameEnded"
            class="send-btn"
          >
            发送
          </a-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ChatMessage from '../components/ChatMessage.vue'
import { chatAPI } from '../services/api'

const route = useRoute()
const router = useRouter()
const messagesRef = ref(null)

// 通用的房间号生成函数 - 6位数字
const generateRoomId = () => {
  let roomId = parseInt(Date.now().toString().slice(-6))
  if (roomId < 100000) {
    roomId += 100000
  }
  return roomId
}

// 当前房间ID和状态
const currentRoomId = ref(route.params.roomId || generateRoomId()) 
const inputMessage = ref('')
const messages = ref([])
const gameStarted = ref(false)
const gameEnded = ref(false)
const currentGameRound = ref(0) // 当前游戏轮次

// 历史记录相关
const chatHistories = ref([])

// 计算属性
const isViewingHistory = computed(() => {
  return messages.value.length > 0 && !gameStarted.value && currentGameRound.value === 0
})

// 监听消息变化，自动保存到历史记录
watch([messages, gameStarted, gameEnded], () => {
  if (messages.value.length > 0) {
    saveCurrentChat()
  }
}, { deep: true })

// 加载历史记录
const loadChatHistories = () => {
  try {
    const stored = localStorage.getItem('aiChatHistories')
    if (stored) {
      chatHistories.value = JSON.parse(stored)
    }
  } catch (error) {
    console.error('加载历史记录失败:', error)
  }
}

// 保存历史记录
const saveChatHistories = () => {
  try {
    localStorage.setItem('aiChatHistories', JSON.stringify(chatHistories.value))
  } catch (error) {
    console.error('保存历史记录失败:', error)
  }
}

// 创建新对话
const createNewChat = () => {
  const newRoomId = generateRoomId()  
  
  // 先保存当前对话到历史记录（如果有内容的话）
  if (messages.value.length > 0) {
    saveCurrentChat()
  }
  
  router.push(`/chat/${newRoomId}`)
  resetChatState()
  currentRoomId.value = newRoomId
  
  // 立即创建一个空的历史记录
  const emptyHistory = {
    roomId: newRoomId,
    messages: [],
    ended: false,
    gameRound: 0,
    timestamp: new Date().getTime()
  }
  
  // 添加到历史记录开头
  chatHistories.value.unshift(emptyHistory)
  
  // 只保留最近20条记录
  if (chatHistories.value.length > 20) {
    chatHistories.value = chatHistories.value.slice(0, 20)
  }
  
  saveChatHistories()
}

// 重置游戏状态（开始新游戏时使用）
const resetGameState = () => {
  gameStarted.value = true
  gameEnded.value = false
  inputMessage.value = ''
  currentGameRound.value += 1
}

// 重置整个聊天状态（创建新对话时使用）
const resetChatState = () => {
  messages.value = []
  gameStarted.value = false
  gameEnded.value = false
  inputMessage.value = ''
  currentGameRound.value = 0
}

// 选择历史记录
const selectHistory = (history) => {
  if (history.roomId === currentRoomId.value) return
  
  // 加载历史消息
  messages.value = [...history.messages]
  gameStarted.value = history.ended ? false : true
  gameEnded.value = history.ended || false
  currentRoomId.value = history.roomId
  currentGameRound.value = history.gameRound || 1
  
  // 更新URL但不导航
  router.replace(`/chat/${history.roomId}`)
  scrollToBottom()
}

// 获取历史记录预览
const getHistoryPreview = (history) => {
  const lastUserMessage = history.messages.filter(msg => msg.isUser).pop()?.content || ''
  const lastAIMessage = history.messages.filter(msg => !msg.isUser).pop()?.content || ''
  
  if (lastUserMessage) {
    return lastUserMessage.length > 25 ? lastUserMessage.substring(0, 25) + '...' : lastUserMessage
  }
  return lastAIMessage.length > 25 ? lastAIMessage.substring(0, 25) + '...' : lastAIMessage
}

// 格式化时间
const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('zh-CN', { 
    hour: '2-digit', 
    minute: '2-digit' 
  })
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

const addMessage = (content, isUser = false) => {
  messages.value.push({ content, isUser })
  scrollToBottom()
}

const startChat = async () => {
  if (gameStarted.value && !gameEnded.value) return
  
  // 如果游戏已结束，开始新的一轮
  if (gameEnded.value) {
    addMessage('--- 开始新游戏 ---', false)
  }
  
  resetGameState()
  addMessage('开始', true)
  
  try {
    const response = await chatAPI.sendMessage(currentRoomId.value, '开始')
    addMessage(response.data)
    checkGameEnd(response.data)
  } catch (error) {
    message.error('发送消息失败')
    gameStarted.value = false
  }
}

const sendMessage = async () => {
  if (!inputMessage.value.trim() || !gameStarted.value || gameEnded.value) return

  const userMessage = inputMessage.value.trim()
  
  // 如果用户输入"开始"，触发重新开始游戏
  if (userMessage === '开始' && gameEnded.value) {
    inputMessage.value = ''
    await startChat()
    return
  }
  
  addMessage(userMessage, true)
  inputMessage.value = ''

  try {
    const response = await chatAPI.sendMessage(currentRoomId.value, userMessage)
    addMessage(response.data)
    checkGameEnd(response.data)
  } catch (error) {
    message.error('发送消息失败')
  }
}

const endGame = () => {
  gameEnded.value = true
  addMessage('用户结束了游戏', true)
  addMessage('游戏已结束')
}

const checkGameEnd = (aiResponse) => {
  if (aiResponse.includes('游戏已结束') || aiResponse.includes('游戏结束')) {
    gameEnded.value = true
  }
}

// 保存当前对话到历史记录
const saveCurrentChat = () => {
  if (messages.value.length > 0) {
    const existingIndex = chatHistories.value.findIndex(
      history => history.roomId === currentRoomId.value
    )
    
    const history = {
      roomId: currentRoomId.value,
      messages: [...messages.value],
      ended: gameEnded.value,
      gameRound: currentGameRound.value,
      timestamp: new Date().getTime()
    }
    
    if (existingIndex !== -1) {
      chatHistories.value[existingIndex] = history
    } else {
      chatHistories.value.unshift(history)
    }
    
    // 只保留最近20条记录
    if (chatHistories.value.length > 20) {
      chatHistories.value = chatHistories.value.slice(0, 20)
    }
    
    saveChatHistories()
  }
}

// 自动滚动到底部
onMounted(() => {
  loadChatHistories()
  
  // 如果是新房间（从首页跳转过来），创建新对话记录
  if (!route.params.roomId) {
    resetChatState()
    createNewHistory(currentRoomId.value)
  } else {
    // 如果有房间ID，检查是否已存在历史记录，如果不存在则创建
    const existingHistory = chatHistories.value.find(history => history.roomId === currentRoomId.value)
    if (!existingHistory) {
      createNewHistory(currentRoomId.value)
    }
  }
  scrollToBottom()
})

// 创建新的历史记录
const createNewHistory = (roomId) => {
  const newHistory = {
    roomId: roomId,
    messages: [],
    ended: false,
    gameRound: 0,
    timestamp: new Date().getTime()
  }
  
  chatHistories.value.unshift(newHistory)
  
  if (chatHistories.value.length > 20) {
    chatHistories.value = chatHistories.value.slice(0, 20)
  }
  
  saveChatHistories()
  console.log('创建新对话记录:', roomId)
}
</script>

<style scoped>
/* 定义紫色主题变量 */
:deep(:root) {
  --primary-color: #7b2cbf; /* 主紫色 */
  --primary-light: #9d4edd; /* 浅紫色 */
  --primary-dark: #5a189a; /* 深紫色 */
  --primary-lightest: #f3e8ff; /* 最浅紫色 */
  --primary-shadow: rgba(123, 44, 191, 0.3); /* 紫色阴影 */
  
  --secondary-orange: #ffa940; /* 辅助橙色（历史标签） */
  --danger-red: #ff4d4f; /* 危险红（结束标签） */
  --success-green: #52c41a; /* 成功绿（游戏中标签） */
}

.chat-page {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  background: var(--primary-lightest);
  margin: 0;
  padding: 0;
  overflow: hidden;
  z-index: 9999;
}

/* 左侧边栏样式 */
.sidebar {
  width: 300px;
  background: white;
  border-right: 1px solid #e8e8e8;
  display: flex;
  flex-direction: column;
  height: 100vh;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
}

.sidebar-header h3 {
  margin: 0;
  color: var(--primary-dark);
  font-size: 16px;
  font-weight: 600;
}

.new-chat-btn {
  color: var(--primary-color);
  font-weight: 500;
  transition: color 0.2s ease;
}

.new-chat-btn:hover {
  color: var(--primary-dark);
}

/* 历史记录列表 */
.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.empty-history {
  text-align: center;
  color: #999;
  padding: 40px 20px;
  font-size: 14px;
}

.history-item {
  padding: 12px;
  margin-bottom: 8px;
  background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%);
  border-radius: 8px;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(252, 182, 159, 0.2);
}

.history-item:hover {
  background: linear-gradient(135deg, #fdeb71 0%, #f8d800 100%);
  border-color: transparent;
  transform: translateX(4px);
  box-shadow: 0 4px 8px rgba(248, 216, 0, 0.3);
}

.history-item.active {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  color: white;
  border-color: transparent;
  box-shadow: 0 4px 12px rgba(250, 112, 154, 0.4);
}

.history-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
}

.history-preview {
  font-size: 12px;
  color: #666;
  opacity: 0.8;
  margin-bottom: 4px;
}

.history-item.active .history-preview {
  color: rgba(255, 255, 255, 0.9);
}

.history-time {
  font-size: 11px;
  color: #999;
}

.history-item.active .history-time {
  color: rgba(255, 255, 255, 0.8);
}

/* 标签样式 */
.history-tag {
  background: var(--secondary-orange);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
}

.ended-tag {
  background: var(--danger-red);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
}

.playing-tag {
  background: var(--success-green);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
}

.ready-tag {
  background: var(--primary-color);
  color: white;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 12px;
  margin-left: 8px;
}

/* 游戏轮次显示 */
.game-round {
  text-align: center;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
  padding: 8px 16px;
  border-radius: 20px;
  margin: 10px auto;
  font-size: 14px;
  font-weight: 500;
  display: inline-block;
  box-shadow: 0 2px 8px var(--primary-shadow);
}

/* 右侧聊天区域样式 */
.chat-room {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  margin: 0;
  padding: 0;
}

.header {
  text-align: center;
  margin-bottom: 0;
  padding: 20px;
  border-bottom: 1px solid #e8e8e8;
  background: white;
}

.header h2 {
  margin: 0 0 8px 0;
  color: var(--primary-dark);
  font-size: 24px;
  font-weight: 600;
}

.room-info {
  color: #666;
  font-size: 14px;
}

.chat-container {
  flex: 1;
  overflow: hidden;
  margin-bottom: 0;
  background: var(--primary-lightest);
  width: 100%;
}

.messages {
  height: 100%;
  overflow-y: auto;
  padding: 20px;
  width: 100%;
  max-width: none;
  margin: 0;
  text-align: center;
}

/* 输入区域 - 占满右侧底部 */
.input-area {
  border-top: 1px solid #e8e8e8;
  padding: 20px;
  background: white;
  width: 100%;
}

/* 游戏控制按钮 - 在输入框上方左侧 */
.game-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

/* 紫色主题按钮 */
.start-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  border: none;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
  transition: all 0.3s ease;
}

.start-btn:not(:disabled):hover {
  background: linear-gradient(135deg, var(--primary-dark) 0%, #4a148c 100%);
  box-shadow: 0 4px 12px var(--primary-shadow);
  transform: translateY(-1px);
}

.start-btn:disabled {
  background: #f0f0f0;
  color: #bfbfbf;
  cursor: not-allowed;
}

.end-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid var(--danger-red);
  background: white;
  color: var(--danger-red);
  transition: all 0.3s ease;
}

.end-btn:not(:disabled):hover {
  background: var(--danger-red);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3);
  border-color: var(--danger-red);
}

.end-btn:disabled {
  border-color: #d9d9d9;
  color: #bfbfbf;
  cursor: not-allowed;
}

.new-chat-control-btn {
  height: 36px;
  padding: 0 20px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid var(--primary-color);
  background: white;
  color: var(--primary-color);
  transition: all 0.3s ease;
}

.new-chat-control-btn:hover {
  background: var(--primary-color);
  color: white;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px var(--primary-shadow);
}

/* 输入组 */
.input-group {
  display: flex;
  gap: 12px;
  width: 100%;
  margin: 0;
}

.message-input {
  flex: 1;
  height: 48px;
  border-radius: 8px;
  font-size: 16px;
  border: 1px solid #d9d9d9;
  transition: all 0.2s ease;
}

.message-input:focus {
  border-color: var(--primary-color);
  box-shadow: 0 0 0 2px rgba(123, 44, 191, 0.2);
  outline: none;
}

.send-btn {
  height: 48px;
  padding: 0 32px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  border: none;
  background: linear-gradient(135deg, var(--primary-color) 0%, var(--primary-dark) 100%);
  color: white;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.send-btn:not(:disabled):hover {
  background: linear-gradient(135deg, var(--primary-dark) 0%, #4a148c 100%);
  transform: translateY(-2px);
  box-shadow: 0 4px 12px var(--primary-shadow);
}

.send-btn:disabled {
  background: #f0f0f0;
  color: #bfbfbf;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-page {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    height: 300px;
    border-right: none;
    border-bottom: 1px solid #e8e8e8;
  }
  
  .history-list {
    max-height: 150px;
  }
  
  .messages {
    padding: 16px;
  }
  
  .input-area {
    padding: 16px;
  }
  
  .chat-room {
    height: calc(100vh - 300px);
  }
  
  .game-controls {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .input-group {
    flex-direction: column;
  }
  
  .send-btn {
    width: 100%;
  }
}
</style>