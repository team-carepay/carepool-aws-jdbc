package com.carepay.jdbc.util;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DaemonThreadFactoryTest {

    @Test
    public void newThread() {
        assertThat(new DaemonThreadFactory().newThread(() -> {})).isNotNull();
    }
}