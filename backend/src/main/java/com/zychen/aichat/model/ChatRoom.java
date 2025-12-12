package com.zychen.aichat.model;

import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import lombok.Data;

import java.util.List;

@Data
public class ChatRoom {
    private Long RoomId;
    private List<ChatMessage> ChatMessages;
}
