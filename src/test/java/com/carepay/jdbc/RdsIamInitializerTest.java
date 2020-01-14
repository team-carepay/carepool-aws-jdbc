package com.carepay.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.security.Security;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RdsIamInitializerTest {
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

    @Test
    public void testCaBundleNotFound() throws IllegalAccessException, NoSuchFieldException, IOException {
        Field field = URL.class.getDeclaredField("factory");
        field.setAccessible(true);
        URLStreamHandlerFactory existing = (URLStreamHandlerFactory) field.get(null);
        try {
            field.set(null,null);
            Security.removeProvider("PEM");
            System.out.println("TestingCaBundleNotFound");
            RdsIamInitializer.init("classpath", "blabla");
            try (InputStream is = new URL("classpath:blegbleg").openStream()) {
            }
            fail();
        } catch (IOException e) {
            assertThat(e.getMessage()).isEqualTo("Resource not found: classpath:blegbleg");
        }
        finally {
            field.set(null,existing);
        }
    }


}
