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

/**
 * Agent 对话请求超时（Type B 修复）。
 *
 * 为什么需要显式指定：httpClient 的默认超时是 10000ms，而后端一轮 Agent 对话包含
 * 一次真实 provider 调用。C5 闸门 3 实测 provider 单次耗时 4571~8467ms（均值 6476ms），
 * 加上编排、护栏判定与落库开销，一轮总耗时越过 10 秒是常态。
 * 于是前端在后端还在正常处理时就断开，用户看到的是 `request: fail timeout`。
 *
 * **关键是它必须大于后端的 app.ai.timeout-millis（20000）**，不只是「够大」：
 * 前端要给后端留出「自己先超时并返回 UNAVAILABLE / FAILED」的窗口。
 * 两者相等时前端总是先断，C1 设计的显式失败语义会被网络错误覆盖掉。
 *
 * 同类参照：stageSummaryService 同样调 AI，早已显式指定 15000ms。
 */
const AGENT_AI_TIMEOUT_MS = 30000

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
            // 开会话会生成 Agent 的开场，含一次 provider 调用。
            timeout: AGENT_AI_TIMEOUT_MS,
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
            timeout: AGENT_AI_TIMEOUT_MS,
        }))
    },
    getSession(sessionId: number) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}`,
            // 刻意沿用默认 10 秒：读会话是纯数据库操作，不调 provider。
            // 给它放宽只会让真正的网络故障晚 20 秒才暴露。
        }))
    },
    sendMessage(sessionId: number, content: string) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}/messages`,
            method: 'POST',
            data: { content },
            timeout: AGENT_AI_TIMEOUT_MS,
        }))
    },
    finish(sessionId: number) {
        return requireRealSession(() => httpRequest<AgentSession>({
            url: `/api/agent/sessions/${sessionId}/finish`,
            method: 'POST',
            // 写作引导的收束会触发素材生成（又一次 provider 调用）；回看的 finish 不调 AI。
            // 前端分不清这个区别，故统一取较宽的那个——回看白留余量不产生副作用，
            // 按 purpose 分流的额外复杂度不值得。
            timeout: AGENT_AI_TIMEOUT_MS,
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
            // 同 getSession：工具执行只动数据库，不调 provider，沿用默认 10 秒。
        }))
    },
}
