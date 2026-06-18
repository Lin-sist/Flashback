package com.flashback.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppMediaPropertiesTest {

    @Test
    void shouldExposeAcceptedDefaultMediaLimits() {
        AppMediaProperties properties = new AppMediaProperties();

        assertThat(properties.getMaxImageCountPerRecord()).isEqualTo(9);
        assertThat(properties.getMaxVoiceCountPerRecord()).isEqualTo(9);
        assertThat(properties.getMaxFileSizeBytes()).isEqualTo(41943040);
        assertThat(properties.getMaxTotalSizeBytesPerRecord()).isEqualTo(314572800);
    }
}
