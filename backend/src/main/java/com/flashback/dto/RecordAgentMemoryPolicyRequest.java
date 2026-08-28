package com.flashback.dto;

import jakarta.validation.constraints.NotNull;

/** P4.2：记录级 Agent 记忆同意与用户说明，全量替换。 */
public class RecordAgentMemoryPolicyRequest {

    @NotNull(message = "excluded不能为空")
    private Boolean excluded;

    private String contextNote;

    public Boolean getExcluded() {
        return excluded;
    }

    public void setExcluded(Boolean excluded) {
        this.excluded = excluded;
    }

    public String getContextNote() {
        return contextNote;
    }

    public void setContextNote(String contextNote) {
        this.contextNote = contextNote;
    }
}
