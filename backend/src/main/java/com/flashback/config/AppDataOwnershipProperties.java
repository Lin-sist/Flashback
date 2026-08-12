package com.flashback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.data-ownership")
public class AppDataOwnershipProperties {
    private String artifactDirectory = System.getProperty("java.io.tmpdir") + "/flashback-data-ownership";
    private long artifactTtlHours = 24;
    private long confirmationTtlMinutes = 10;

    public String getArtifactDirectory() { return artifactDirectory; }
    public void setArtifactDirectory(String artifactDirectory) { this.artifactDirectory = artifactDirectory; }
    public long getArtifactTtlHours() { return artifactTtlHours; }
    public void setArtifactTtlHours(long artifactTtlHours) { this.artifactTtlHours = artifactTtlHours; }
    public long getConfirmationTtlMinutes() { return confirmationTtlMinutes; }
    public void setConfirmationTtlMinutes(long confirmationTtlMinutes) { this.confirmationTtlMinutes = confirmationTtlMinutes; }
}
