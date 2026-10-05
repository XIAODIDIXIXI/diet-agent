package com.diet.model;

import java.util.Map;

import com.diet.enums.SourceMode;
import com.fasterxml.jackson.annotation.JsonAutoDetect;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@Accessors(fluent = true)
@AllArgsConstructor
@NoArgsConstructor

/**
 * 请求对象
 */
public class ChatRequest {
    private String sessionId;               //首次为空，后续携带实现多轮对话，多轮槽位、历史推荐和消息记录都绑定在sesionid上
    private String message;                 //用户当前输入
    private SourceMode sourceMode;          //public or oersonal
    private Map<String, Object> context;    //预留上下文
}