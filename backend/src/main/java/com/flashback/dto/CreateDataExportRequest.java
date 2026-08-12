package com.flashback.dto;

import com.flashback.domain.SealedContentPolicy;
import jakarta.validation.constraints.NotNull;

public class CreateDataExportRequest {
    @NotNull
    private SealedContentPolicy sealedContentPolicy = SealedContentPolicy.RESPECT_SEAL;
    public SealedContentPolicy getSealedContentPolicy() { return sealedContentPolicy; }
    public void setSealedContentPolicy(SealedContentPolicy sealedContentPolicy) { this.sealedContentPolicy = sealedContentPolicy; }
}
