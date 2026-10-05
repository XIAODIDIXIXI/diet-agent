package com.diet.controller.chat;

import com.diet.constants.DietConstants;
import com.diet.model.ChatRequest;
import com.diet.model.ChatResponse;
import com.diet.service.orchestrator.DietOrchestratorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 饮食推荐对话 HTTP 入口。
 * 本层只做参数透传，完整状态机由 {@link DietOrchestratorService#dietChat} 驱动。
 */
@RestController
@RequestMapping("/api/v1/diet")
public class DietChatController {

    /** 多 Agent 编排服务，注入后用于处理每轮对话。 */
    private final DietOrchestratorService orchestratorService;

    /** Spring 构造器注入 Orchestrator。 */
    public DietChatController(DietOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    /**
     * POST /api/v1/diet/chat — 同步对话接口。
     * 接收用户消息，返回澄清追问或推荐结果（含餐食卡片）。
     */
    @PostMapping("/chat")
    public ChatResponse dietChat(
            // 从请求头 X-User-Id 读取用户 ID，缺省为 1 便于本地调试
            @RequestHeader(value = DietConstants.USER_ID, defaultValue = "1") Long userId,
            // 从请求体反序列化 ChatRequest（sessionId、message、sourceMode）
            @RequestBody ChatRequest request
    ) {
        // 委托 Orchestrator 执行完整状态机，直接返回 ChatResponse
        return orchestratorService.dietChat(userId, request);
    }
}

/*
1. @RestController

本质：@Controller + @ResponseBody 的组合注解。

作用：标识当前类是一个 Web 控制器，且类中所有方法的返回值（如对象、集合）会自动序列化为 JSON/XML，直接写入 HTTP 响应体，不走视图解析器（不做页面跳转）。

适用：纯 RESTful API 开发。

2. @RequestMapping("/api/v1/diet")

本质：通用的请求映射注解（可修饰类或方法）。

作用：在类级别定义基础路径（Base Path）。当前类下所有方法的映射路径都必须以此为前缀。

关键：此处只指定路径，未指定请求方法（Method），具体请求类型交由子注解细化。

3. @PostMapping("/chat")

本质：@RequestMapping(method = RequestMethod.POST) 的快捷版（组合注解）。

作用：将方法绑定到子路径 /chat，且严格限定仅接收 HTTP POST 请求。

1. @RequestHeader

本质：参数绑定注解（方法形参上用）。

作用：从 HTTP 请求头中提取指定键的值，自动赋值给方法参数。

关键：支持 required（是否必须）和 defaultValue（缺省默认值）。如代码中从 X-User-Id 取值，若无则用 "1" 兜底。

2. @RequestBody

本质：参数绑定注解（方法形参上用）。

作用：将 HTTP 请求体（Body） 中的 JSON/XML 数据，自动反序列化为 Java 对象（如 ChatRequest）。

关键：依赖 HttpMessageConverter（如 MappingJackson2HttpMessageConverter）完成转换。一个请求只能有一个 @RequestBody。

3. @Controller + @ResponseBody

本质：@RestController 的底层原始组合。

作用拆解：

@Controller：声明该类为控制器 Bean，默认方法返回值是视图名（用于页面跳转）。

@ResponseBody：加在方法或类上，强制将返回值直接写入 HTTP 响应体（序列化为 JSON），不再解析为视图。

关键：@RestController 就等于在类上同时贴了 @Controller 和 @ResponseBody，省去每个方法都加 @ResponseBody 的繁琐。

完整路径：结合类上的 @RequestMapping，最终暴露的端点即为 POST /api/v1/diet/chat。
 */