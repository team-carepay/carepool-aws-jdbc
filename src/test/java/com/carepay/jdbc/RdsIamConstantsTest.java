package com.carepay.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RdsIamConstantsTest {
    @Test
    public void testConstructor() {
        assertThat(new RdsIamConstants()).isNotNull();
    }
}