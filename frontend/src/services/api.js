import axios from 'axios'

// 使用代理路径，避免 CORS 问题
const API_BASE_URL = '/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000
})

export const chatAPI = {
  sendMessage(roomId, userPrompt) {
    return api.post(`/${roomId}/chat`, null, {
      params: { userPrompt }
    })
  },
  
  getRooms() {
    return api.get('/rooms')
  }
}

export default api