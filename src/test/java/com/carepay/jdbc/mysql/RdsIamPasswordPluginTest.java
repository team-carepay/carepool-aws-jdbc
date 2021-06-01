package com.carepay.jdbc.mysql;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.carepay.aws.auth.Credentials;
import com.mysql.cj.conf.DefaultPropertySet;
import com.mysql.cj.conf.PropertyDefinitions;
import com.mysql.cj.conf.PropertyKey;
import com.mysql.cj.conf.PropertySet;
import com.mysql.cj.exceptions.ExceptionInterceptor;
import com.mysql.cj.exceptions.FeatureNotAvailableException;
import com.mysql.cj.exceptions.SSLParamsException;
import com.mysql.cj.log.Log;
import com.mysql.cj.protocol.AbstractSocketConnection;
import com.mysql.cj.protocol.Protocol;
import com.mysql.cj.protocol.ServerSession;
import com.mysql.cj.protocol.a.NativePacketPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RdsIamPasswordPluginTest {

    private Clock clock = Clock.fixed(Instant.parse("2021-10-12T12:13:14Z"), ZoneId.of("UTC"));
    private RdsIamPasswordPlugin plugin;
    private Protocol<NativePacketPayload> protocol;
    private DefaultPropertySet propertySet;

    @BeforeEach
    void setUp() {
        plugin = new RdsIamPasswordPlugin(() -> new Credentials("ABC", "DEF", "GHI"), () -> "eu-west-1", clock);
        //noinspection unchecked
        protocol = mock(Protocol.class);
        propertySet = new DefaultPropertySet();
        when(protocol.getPropertySet()).thenReturn(propertySet);
        when(protocol.getSocketConnection()).thenReturn(new FakeSocketConnection("cp-ke-test-fake-db.cluster-c3u8jposmdov.eu-west-1.rds.amazonaws.com", 3306));
    }

    @Test
    void testInit() {
        plugin.init(protocol, null);
        assertThat(propertySet.getEnumProperty(PropertyKey.sslMode).getValue()).isEqualTo(PropertyDefinitions.SslMode.VERIFY_CA);
        assertThat(propertySet.getStringProperty(PropertyKey.trustCertificateKeyStoreType).getValue()).isEqualTo("PEM");
    }

    @Test
    void getProtocolPluginName() {
        assertThat(plugin.getProtocolPluginName()).isEqualTo("mysql_clear_password");
    }

    @Test
    void requiresConfidentiality() {
        assertThat(plugin.requiresConfidentiality()).isTrue();
    }

    @Test
    void isReusable() {
        assertThat(plugin.isReusable()).isTrue();
    }

    @Test
    void setAuthentication() {
        plugin.setAuthenticationParameters("johndoe", "secret");
        assertThat(plugin.username).isEqualTo("johndoe");
    }

    @Test
    void nextAuthenticationStep() {
        plugin.init(protocol, null);
        NativePacketPayload fromServer = new NativePacketPayload(2000);
        List<NativePacketPayload> toServer = new ArrayList<>();
        plugin.nextAuthenticationStep(fromServer, toServer);
        assertThat(toServer).isNotEmpty();
        NativePacketPayload loginPacket = toServer.get(0);
        String str = new String(loginPacket.getByteBuffer(),0, loginPacket.getPayloadLength()-1);
        assertThat(str).isEqualTo("cp-ke-test-fake-db.cluster-c3u8jposmdov.eu-west-1.rds.amazonaws.com:3306/?Action=connect&DBUser=null&X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=ABC%2F20211012%2Feu-west-1%2Frds-db%2Faws4_request&X-Amz-Date=20211012T121314Z&X-Amz-Expires=900&X-Amz-Security-Token=GHI&X-Amz-SignedHeaders=host&X-Amz-Signature=c51622f22daffcfaa6707878fb009853f3764ec02246dd2d0a2c9e0c1c6bad37");
    }

    static class FakeSocketConnection extends AbstractSocketConnection {

        public FakeSocketConnection(final String host, final int port) {
            this.host = host;
            this.port = port;
        }

        @Override
        public void connect(String host, int port, PropertySet propertySet, ExceptionInterceptor exceptionInterceptor, Log log, int loginTimeout) {
        }

        @Override
        public void performTlsHandshake(ServerSession serverSession) throws SSLParamsException, FeatureNotAvailableException, IOException {
        }
    }
}
