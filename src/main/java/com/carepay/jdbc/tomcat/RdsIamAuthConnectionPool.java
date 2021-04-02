package com.carepay.jdbc.tomcat;

import java.lang.reflect.Field;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.carepay.jdbc.RdsAWS4Signer;
import com.carepay.jdbc.util.JdbcUrlUtils;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolUtilities;
import org.apache.tomcat.jdbc.pool.PooledConnection;

import static com.carepay.jdbc.RdsIamConstants.CA_BUNDLE_URL;
import static com.carepay.jdbc.RdsIamConstants.PEM;
import static com.carepay.jdbc.RdsIamConstants.REQUIRE_SSL;
import static com.carepay.jdbc.RdsIamConstants.SSL_MODE;
import static com.carepay.jdbc.RdsIamConstants.TRUST_CERTIFICATE_KEY_STORE_TYPE;
import static com.carepay.jdbc.RdsIamConstants.TRUST_CERTIFICATE_KEY_STORE_URL;
import static com.carepay.jdbc.RdsIamConstants.USE_SSL;
import static com.carepay.jdbc.RdsIamConstants.VERIFY_CA;
import static com.carepay.jdbc.RdsIamConstants.VERIFY_SERVER_CERTIFICATE;

/**
 * Extends the default pool. This implementation will generate a new IAM token every 10 min.
 */
public class RdsIamAuthConnectionPool extends ConnectionPool {

    private final RdsAWS4Signer tokenGenerator;
    private final ScheduledExecutorService scheduledExectorService;
    private String host;
    private int port;
    private ScheduledFuture<?> backgroundFuture;

    public RdsIamAuthConnectionPool(final RdsAWS4Signer tokenGenerator, PoolConfiguration prop, ScheduledExecutorService scheduledExecutorService) throws SQLException {
        super(prop);
        this.tokenGenerator = tokenGenerator;
        this.scheduledExectorService = scheduledExecutorService;
        BlockingQueue<PooledConnection> busyConnections = getPrivateConnectionListField("busy");
        BlockingQueue<PooledConnection> idleConnections = getPrivateConnectionListField("idle");
        Consumer<PooledConnection> consumer = pc -> pc.getAttributes().remove(PoolUtilities.PROP_PASSWORD);

        this.backgroundFuture = scheduledExectorService.scheduleWithFixedDelay(
                () -> {
                    updatePassword(prop);
                    idleConnections.forEach(consumer);
                    busyConnections.forEach(consumer);
                },
                0, 10, TimeUnit.MINUTES
        );
    }

    /**
     * Initializes the pool.
     *
     * @param prop the pool configuration
     */
    @Override
    protected void init(PoolConfiguration prop) throws SQLException {
        final URL uri = JdbcUrlUtils.extractJdbcURL(prop.getUrl());
        host = uri.getHost();
        port = uri.getPort();

        final Properties props = prop.getDbProperties();
        props.setProperty(USE_SSL, "true");      // for MySQL 5.x and before
        props.setProperty(REQUIRE_SSL, "true");  // for MySQL 5.x and before
        props.setProperty(VERIFY_SERVER_CERTIFICATE, "true");
        props.setProperty(SSL_MODE, VERIFY_CA); // for MySQL 8.x and higher
        props.setProperty(TRUST_CERTIFICATE_KEY_STORE_URL, CA_BUNDLE_URL);
        props.setProperty(TRUST_CERTIFICATE_KEY_STORE_TYPE, PEM);

        prop.setInitialSize(0);
        super.init(prop);
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<PooledConnection> getPrivateConnectionListField(final String fieldName) {
        try {
            Field queueField = ConnectionPool.class.getDeclaredField(fieldName);
            queueField.setAccessible(true); //NOSONAR
            return (BlockingQueue<PooledConnection>) queueField.get(this);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * Updates the password in the pool by generating a new token
     */
    private void updatePassword(PoolConfiguration poolConfiguration) {
        String token = tokenGenerator.generateToken(host, port, poolConfiguration.getUsername());
        poolConfiguration.setPassword(token);
    }

    @Override
    protected void close(boolean force) {
        super.close(force);
        if (backgroundFuture != null) {
            backgroundFuture.cancel(force);
        }
    }
}
