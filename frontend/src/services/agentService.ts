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
    | 'ENDED'
export type AgentMessageRole = 'USER' | 'ASSISTANT'

export interface AgentMessage {
    id: number
    role: AgentMessageRole
    turnNo: number
    stage: AgentStage
    content: string
    createdAt: string
}

export interface AgentSession {
    sessionId: number
    recordId?: number | null
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
}
