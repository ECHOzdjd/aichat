package com.ywang.aichat.service.impl;

import com.volcengine.ark.runtime.model.completion.chat.ChatMessage;
import com.volcengine.ark.runtime.model.completion.chat.ChatMessageRole;
import com.ywang.aichat.model.ChatRoom;
import com.ywang.aichat.service.AiManage;
import com.ywang.aichat.service.ChatService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service

public class ChatServiceImpl implements ChatService {
    @Resource
    private AiManage aiManage;
    //存储历史对话；
    //每次对话需要添加对话；
    final Map<Long, List<ChatMessage>> chatHistories = new HashMap<>();
    @Override
    public String doChat(long roomId, String userPrompt) {
        String systemPrompt = "你是一位脑筋急转弯游戏主持人，我们将进行一个是非问答推理游戏。\\n" +
                "\\n" +
                "游戏规则如下：\\n" +
                "\\n" +
                "当我说开始时，你要从你的题库中随机选择一道全新的、不重复的脑筋急转弯题目。\\n" +
                "要求：题干简短、有趣、需要逻辑推理或反向思考，每次必须出不同的题目。\\n" +
                "题目类型可以多样化，包括但不限于：情境推理、逻辑谜题、文字游戏、反常识思考等。\\n" +
                "\\n" +
                "出题后，你只负责回答我的提问，每次只能回答以下三种之一：\\n" +
                "是\\n" +
                "否\\n" +
                "与此无关\\n" +
                "\\n" +
                "在合适的时候，你可以适当引导我，比如说你离真相更近了或你可能忽略了某个细节。\\n" +
                "\\n" +
                "游戏结束条件满足任一即可：\\n" +
                "我说出不想玩了、告诉我答案、揭晓答案等类似表达\\n" +
                "我已经基本推理出真相、还原了故事，或所有关键问题都被询问到\\n" +
                "我输入退出\\n" +
                "已经问了15个问题，但我仍然没有接近真相或关键线索\\n" +
                "\\n" +
                "结束时你的任务：\\n" +
                "输出游戏结束，并给出本题的正确答案或完整解释。\\n" +
                "如果我表现得不错，可以适当给一句点评或鼓励。\\n" +
                "\\n" +
                "重要提示：每次游戏必须出不同的题目，不要重复使用相同的题目。\\n" +
                "准备好后，当我输入开始，游戏正式开始。";


        //1、准备好消息列表；
        List<ChatMessage> messages = new ArrayList<>();
        final ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content(systemPrompt).build();
        final ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(userPrompt).build();

        if(!chatHistories.containsKey(roomId) && userPrompt.equals("开始")) {
            messages.add(systemMessage);
            chatHistories.put(roomId, messages);
        }else{
            messages = chatHistories.get(roomId);
        }
        messages.add(userMessage);

        //2、调用ai;
        String answer = aiManage.doChat(messages);
        final  ChatMessage answerMessage = ChatMessage.builder().role(ChatMessageRole.ASSISTANT).content(answer).build();
        messages.add(answerMessage);

        if(answer.contains("游戏结束")){
            chatHistories.remove(roomId);
        }

        //3、返回消息；
        return answer;
    }

    @Override
    public List<ChatRoom> getChatRoomList() {
        List<ChatRoom> chatRoomList = new ArrayList<>();
        for(Map.Entry<Long, List<ChatMessage>> entry : chatHistories.entrySet()){
            ChatRoom chatRoom = new ChatRoom();
            chatRoom.setRoomId(entry.getKey());
            List<ChatMessage> messages = entry.getValue();
            chatRoom.setChatMessages(messages);
            chatRoomList.add(chatRoom);
        }
        return chatRoomList;
    }
}
