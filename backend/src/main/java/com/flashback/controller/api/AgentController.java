package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentConversationIntentRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.dto.AgentToolCallConfirmRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 多轮对话接口（C1 + C2）。
 *
 * 鉴权：路径位于 /api/** 之下，由 WebMvcConfig 注册的 JWT 拦截器统一拦截。
 * 归属：会话归属校验落在 service + SQL 双层，跨用户访问返回安全的未找到。
 * C2：新增工具确认端点，C1 四个端点的既有契约保持不变。
 */
@Validated
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentChatService agentChatService;

    public AgentController(AgentChatService agentChatService) {
        this.agentChatService = agentChatService;
    }

    @PostMapping("/sessions")
    public ApiResponse<AgentSessionVO> startOrResume(
            @CurrentUser AuthUser authUser,
            @RequestBody(required = false) AgentSessionStartRequest request) {
        return ApiResponse.success(agentChatService.startOrResume(authUser.getUserId(), request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ApiResponse<AgentSessionVO> getSession(
            @CurrentUser AuthUser authUser,
            @PathVariable Long sessionId) {
        return ApiResponse.success(agentChatService.getSession(authUser.getUserId(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<AgentSessionVO> sendMessage(
            @CurrentUser AuthUser authUser,
            @PathVariable Long sessionId,
            @Valid @RequestBody AgentMessageRequest request) {
        return ApiResponse.success(agentChatService.sendMessage(authUser.getUserId(), sessionId, request));
    }

    @PostMapping("/sessions/{sessionId}/finish")
    public ApiResponse<AgentSessionVO> finish(
            @CurrentUser AuthUser authUser,
            @PathVariable Long sessionId) {
        return ApiResponse.success(agentChatService.finish(authUser.getUserId(), sessionId));
    }

    @PutMapping("/sessions/{sessionId}/intent")
    public ApiResponse<AgentSessionVO> switchConversationIntent(
            @CurrentUser AuthUser authUser,
            @PathVariable Long sessionId,
            @Valid @RequestBody AgentConversationIntentRequest request) {
        return ApiResponse.success(agentChatService.switchConversationIntent(
                authUser.getUserId(), sessionId, request.getConversationIntent()));
    }

    /**
     * C2：确认（接受或拒绝）一条工具提议。
     *
     * 这是工具执行的唯一入口——Agent 在生成回复时只能提议，
     * 任何写操作都必须经过用户在此处的显式确认（design.md 决策 2）。
     */
    @PostMapping("/sessions/{sessionId}/tool-calls/{toolCallId}/confirm")
    public ApiResponse<AgentSessionVO> confirmToolCall(
            @CurrentUser AuthUser authUser,
            @PathVariable Long sessionId,
            @PathVariable Long toolCallId,
            @Valid @RequestBody AgentToolCallConfirmRequest request) {
        return ApiResponse.success(agentChatService.confirmToolCall(
                authUser.getUserId(), sessionId, toolCallId, request.getDecision()));
    }
}
