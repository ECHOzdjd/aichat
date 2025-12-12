<template>
  <div :class="['message', { 'user-message': isUser, 'ai-message': !isUser }]">
    <div class="avatar">
      <a-avatar :size="32" :style="{ backgroundColor: isUser ? '#87d068' : '#108ee9' }">
        {{ isUser ? '我' : 'AI' }}
      </a-avatar>
    </div>
    <div class="content">
      <div class="bubble">
        {{ content }}
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  content: {
    type: String,
    required: true
  },
  isUser: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.message {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.user-message {
  flex-direction: row-reverse;
}

.avatar {
  margin: 0 8px;
}

.content {
  max-width: 70%;
}

.bubble {
  padding: 12px 16px;
  border-radius: 8px;
  background-color: #f0f0f0;
  word-wrap: break-word;
}

.user-message .bubble {
  background-color: #1890ff;
  color: white;
}

.ai-message .bubble {
  background-color: #f5f5f5;
  color: #333;
}
</style>