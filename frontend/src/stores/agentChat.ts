import { defineStore } from 'pinia'
import { agentService, type AgentMessage, type AgentSession } from '../services'

interface AgentChatState {
    session: AgentSession | null
    loading: boolean
    sending: boolean
    finishing: boolean
    errorMessage: string
}

const errorText = (error: unknown) => error instanceof Error ? error.message : '暂时无法继续对话'

export const useAgentChatStore = defineStore('agentChat', {
    state: (): AgentChatState => ({
        session: null,
        loading: false,
        sending: false,
        finishing: false,
        errorMessage: '',
    }),
    getters: {
        messages: (state): AgentMessage[] => state.session?.messages || [],
        materialDraft: (state): string => state.session?.materialDraft?.trim() || '',
        canContinue: (state): boolean => Boolean(state.session?.canContinue),
    },
    actions: {
        clear() {
            this.session = null
            this.loading = false
            this.sending = false
            this.finishing = false
            this.errorMessage = ''
        },
        applySession(session: AgentSession) {
            this.session = session
            this.errorMessage = session.status === 'SUCCESS' ? '' : session.message || '暂时无法继续对话'
            return session
        },
        async startOrResume(recordId?: number | null) {
            this.loading = true
            this.errorMessage = ''
            try {
                return this.applySession(await agentService.startOrResume(recordId))
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
        async retry(recordId?: number | null) {
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
                return this.applySession(await agentService.startOrResume(recordId))
            } catch (error) {
                this.errorMessage = errorText(error)
                throw error
            } finally {
                this.loading = false
            }
        },
    },
})
