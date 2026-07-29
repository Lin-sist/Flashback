import { httpRequest } from './httpClient'
import { hasPreviewSession } from '../features/preview/preview-session'
import { getToken } from '../utils'

export type AgentResultStatus = 'SUCCESS' | 'UNAVAILABLE' | 'FAILED'
export type AgentSessionStatus = 'ACTIVE' | 'ENDED'
export type AgentStage =
    | 'OPENING'
    | 'EMOTION'
    | 'CONFUSION'
    | 'CORE_QUESTION'
    | 'EXPECTATION'
    | 'CLOSING'
    /** C3b：友人回看对话的固定阶段。回看无阶段推进，恒为该值。 */
    | 'REVIEW'
    | 'ENDED'

/**
 * C3b 会话用途。
 *
 * 回看对话复用同一套会话端点（后端 design 决策 6）——读取、追加消息、结束
 * 三个端点在两种用途下语义完全一致，另开一套只会产生近乎重复的接口。
 * 缺省不传即写作引导，既有调用无需改动。
 */
export type AgentSessionPurpose = 'WRITING_GUIDANCE' | 'REVIEW_CHAT'
export type AgentMessageRole = 'USER' | 'ASSISTANT'

export interface AgentMessage {
    id: number
    role: AgentMessageRole
    turnNo: number
    stage: AgentStage
    content: string
    createdAt: string
}

/** C2 工具提议状态。 */
export type AgentToolCallStatus = 'PROPOSED' | 'EXECUTED' | 'FAILED' | 'REJECTED' | 'REJECTED_BY_GUARD'

/** C2 用户对提议的决定。只有接受或拒绝，没有免确认模式。 */
export type AgentToolDecision = 'ACCEPT' | 'REJECT'

export interface AgentToolCall {
    toolCallId: number
    tool: string
    status: AgentToolCallStatus
    /** Agent 的征询话术，用作确认条文案。 */
    askText?: string | null
    resultSummary?: string | null
    failureType?: string | null
    tagIds?: number[] | null
    unlockAt?: string | null
}

export interface AgentSession {
    sessionId: number
    recordId?: number | null
    /**
     * C3b：会话用途。
     * 回看会话恒不会带 pendingToolCall 与 materialDraft——后端完全不产出它们，
     * 不是前端隐藏（回看无工具、不产可回填素材）。
     */
    purpose?: AgentSessionPurpose
    stage: AgentStage
    sessionStatus: AgentSessionStatus
    turnCount: number
    maxTurns: number
    canContinue: boolean
    messages: AgentMessage[]
    materialDraft?: string | null
    source: string
    status: AgentResultStatus
    message?: string | null
    /** C2 新增：待用户确认的工具提议。 */
    pendingToolCall?: AgentToolCall | null
    /** C2 新增：最近一次工具执行结果。 */
    lastToolCallResult?: AgentToolCall | null
}

const shouldBlockRealIntegrationInPreview = () => !getToken() && hasPreviewSession()

const rejectPreviewAgentRequest = <T>() => Promise.reject<T>(new Error('演示模式不访问真实 Agent 服务'))

const requireRealSession = <T>(request: () => Promise<T>) => {
    if (shouldBlockRealIntegrationInPreview()) {
        return rejectPreviewAgentRequest<T>()
    }
    return request()
}

export const agentService = {
    startOrResume(recordId?: number | null) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: '/api/agent/sessions',
            method: 'POST',
            data: recordId ? { recordId } : {},
        }))
    },
    /**
     * C3b：在已解锁记录上开启或恢复回看对话。
     *
     * 与 startOrResume 分成两个方法而非加可选参数，是为了让调用点读起来就知道
     * 自己开的是哪种对话——两者的记录状态要求、是否有工具、是否产素材都不同。
     */
    startOrResumeReview(recordId: number) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: '/api/agent/sessions',
            method: 'POST',
            data: { recordId, purpose: 'REVIEW_CHAT' },
        }))
    },
    getSession(sessionId: number) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}`,
        }))
    },
    sendMessage(sessionId: number, content: string) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}/messages`,
            method: 'POST',
            data: { content },
        }))
    },
    finish(sessionId: number) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}/finish`,
            method: 'POST',
        }))
    },
    /**
     * C2：确认或拒绝一条工具提议。
     *
     * 刻意不传任何工具参数——执行入参一律取自后端持久化的提议，
     * 前端回传参数等于绕过白名单与校验。
     */
    confirmToolCall(sessionId: number, toolCallId: number, decision: AgentToolDecision) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}/tool-calls/${toolCallId}/confirm`,
            method: 'POST',
            data: { decision },
        }))
    },
}
