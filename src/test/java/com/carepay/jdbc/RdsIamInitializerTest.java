package com.carepay.jdbc;

import java.io.IOException;
import java.net.URL;
import java.security.Security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RdsIamInitializerTest {
    @Test
    public void testCaBundleNotFound() {
        try {
            Security.removeProvider("PEM");
            RdsIamInitializer.init("classpath", "blabla");
            fail();
        } catch (IllegalStateException e) {
            assertThat(e.getMessage()).isEqualTo("Resource not found: classpath:blabla");
        }
    }


    @Test
    public void testCustomResourceHandler() throws IOException {
        Security.removeProvider("PEM");
        RdsIamInitializer.init();
        assertThat(new URL(RdsIamConstants.CA_BUNDLE_URL).openStream()).isNotNull();
        try {
            new URL("classpath:blabla").openConnection();
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Resource not found: classpath:blabla");
        }
    }
}
