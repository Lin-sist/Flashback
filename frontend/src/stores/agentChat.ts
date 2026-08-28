import { defineStore } from 'pinia'
import {
    agentService,
    type AgentMessage,
    type AgentConversationIntent,
    type AgentSession,
    type AgentToolCall,
    type AgentToolDecision,
} from '../services'

interface AgentChatState {
    session: AgentSession | null
    loading: boolean
    sending: boolean
    finishing: boolean
    switchingIntent: boolean
    switchingMemoryAuthorization: boolean
    /** C2：工具确认进行中，用于防抖，避免重复点击重复提交。 */
    confirmingToolCall: boolean
    errorMessage: string
}

const errorText = (error: unknown) => error instanceof Error ? error.message : '暂时无法继续对话'

export const useAgentChatStore = defineStore('agentChat', {
    state: (): AgentChatState => ({
        session: null,
        loading: false,
        sending: false,
        finishing: false,
        switchingIntent: false,
        switchingMemoryAuthorization: false,
        confirmingToolCall: false,
        errorMessage: '',
    }),
    getters: {
        messages: (state): AgentMessage[] => state.session?.messages || [],
        materialDraft: (state): string => state.session?.materialDraft?.trim() || '',
        canContinue: (state): boolean => Boolean(state.session?.canContinue),
        /** C2：仅当提议处于待确认状态时才展示确认条。 */
        pendingToolCall: (state): AgentToolCall | null => {
            const pending = state.session?.pendingToolCall
            return pending && pending.status === 'PROPOSED' ? pending : null
        },
        lastToolCallResult: (state): AgentToolCall | null => state.session?.lastToolCallResult || null,
    },
    actions: {
        clear() {
            this.session = null
            this.loading = false
            this.sending = false
            this.finishing = false
            this.switchingIntent = false
            this.switchingMemoryAuthorization = false
            this.confirmingToolCall = false
            this.errorMessage = ''
        },
        applySession(session: AgentSession) {
            this.session = session
            this.errorMessage = session.status === 'SUCCESS' ? '' : session.message || '暂时无法继续对话'
            return session
        },
        async startOrResume(conversationIntent: AgentConversationIntent, recordId?: number | null) {
            this.loading = true
            this.errorMessage = ''
            try {
                return this.applySession(await agentService.startOrResume(conversationIntent, recordId))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.loading = false
            }
        },
        async send(content: string) {
            if (!this.session || this.sending) return null
            const normalized = content.trim()
            if (!normalized) return null
            this.sending = true
            this.errorMessage = ''
            try {
                return this.applySession(await agentService.sendMessage(this.session.sessionId, normalized))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.sending = false
            }
        },
        async finish() {
            if (!this.session || this.finishing) return null
            this.finishing = true
            this.errorMessage = ''
            try {
                return this.applySession(await agentService.finish(this.session.sessionId))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.finishing = false
            }
        },
        async switchConversationIntent(conversationIntent: AgentConversationIntent) {
            if (!this.session || this.switchingIntent) return null
            if (this.session.conversationIntent === conversationIntent) return this.session
            this.switchingIntent = true
            this.errorMessage = ''
            try {
                // 不做本地乐观更新：只展示 backend 返回的权威 intent。
                return this.applySession(await agentService.switchConversationIntent(
                    this.session.sessionId,
                    conversationIntent,
                ))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.switchingIntent = false
            }
        },
        async switchMemoryAuthorization(crossRecordMemoryEnabled: boolean) {
            if (!this.session || this.switchingMemoryAuthorization) return null
            if (Boolean(this.session.crossRecordMemoryEnabled) === crossRecordMemoryEnabled) {
                return this.session
            }
            this.switchingMemoryAuthorization = true
            try {
                return this.applySession(await agentService.switchMemoryAuthorization(
                    this.session.sessionId,
                    crossRecordMemoryEnabled,
                ))
            } catch (error) {
                throw error
            } finally {
                this.switchingMemoryAuthorization = false
            }
        },
        /**
         * C2：确认或拒绝当前工具提议。
         *
         * 返回本次执行结果，供页面在成功后局部刷新草稿表单。
         * confirmingToolCall 作为防抖闸：重复点击不会重复提交
         * （后端也有幂等保护，这里只是少一次无谓请求）。
         */
        async confirmToolCall(decision: AgentToolDecision) {
            const pending = this.session?.pendingToolCall
            if (!this.session || !pending || this.confirmingToolCall) return null
            if (pending.status !== 'PROPOSED') return null

            this.confirmingToolCall = true
            this.errorMessage = ''
            try {
                const session = await agentService.confirmToolCall(
                    this.session.sessionId,
                    pending.toolCallId,
                    decision,
                )
                this.applySession(session)
                return session.lastToolCallResult || null
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.confirmingToolCall = false
            }
        },
        async retry(conversationIntent: AgentConversationIntent, recordId?: number | null) {
            this.loading = true
            this.errorMessage = ''
            try {
                const current = this.session
                if (current?.sessionId) {
                    const latest = current.messages[current.messages.length - 1]
                    // provider 失败后后端保留用户消息；相同内容重试不会重复落库。
                    if (current.sessionStatus === 'ACTIVE' && latest?.role === 'USER') {
                        this.sending = true
                        try {
                            return this.applySession(await agentService.sendMessage(current.sessionId, latest.content))
                        } finally {
                            this.sending = false
                        }
                    }
                }
                // 开场失败（无消息）时重新触发 startOrResume，后端复用空的 ACTIVE 会话。
                return this.applySession(await agentService.startOrResume(conversationIntent, recordId))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.loading = false
            }
        },
    },
})
