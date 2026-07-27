package com.flashback.controller.api;

import com.flashback.common.response.ApiResponse;
import com.flashback.dto.AgentMessageRequest;
import com.flashback.dto.AgentSessionStartRequest;
import com.flashback.security.auth.AuthUser;
import com.flashback.security.auth.CurrentUser;
import com.flashback.service.AgentChatService;
import com.flashback.vo.AgentSessionVO;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 多轮对话接口（C1）。
 *
 * 鉴权：路径位于 /api/** 之下，由 WebMvcConfig 注册的 JWT 拦截器统一拦截。
 * 归属：会话归属校验落在 service + SQL 双层，跨用户访问返回安全的未找到。
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
}
