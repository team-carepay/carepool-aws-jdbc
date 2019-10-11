package com.carepay.jdbc;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Security;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.function.Consumer;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;
import com.amazonaws.services.rds.auth.GetIamAuthTokenRequest;
import com.amazonaws.services.rds.auth.RdsIamAuthTokenGenerator;
import de.dentrassi.crypto.pem.PemKeyStoreProvider;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;
import org.apache.tomcat.jdbc.pool.PoolUtilities;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DataSource based on Tomcat connection pool that supports IAM authentication to RDS
 */
public class RdsIamTomcatDataSource extends org.apache.tomcat.jdbc.pool.DataSource implements RdsIamConstants {

    private static final Logger LOG = LoggerFactory.getLogger(RdsIamTomcatDataSource.class);

    static long DEFAULT_TIMEOUT = 600000L; // renew every 10 minutes, since token expires after 15m
    public static final int DEFAULT_PORT = 3306;

    static {
        Security.addProvider(new PemKeyStoreProvider());
    }

    /**
     * Creates a new Connection Pool once. Overridden so we can change the underlying pool.
     * @return the cached pool or the newly created pool.
     * @throws SQLException
     */
    @Override
    public ConnectionPool createPool() throws SQLException {
        return pool != null ? pool : createPoolImpl();
    }

    /**
     * Creates a new RDS IAM backed pool.
     *
     * @return
     * @throws SQLException
     */
    protected synchronized ConnectionPool createPoolImpl() throws SQLException {
        if (pool == null) {
            pool = new RdsIamAuthConnectionPool(poolProperties);
        }
        return pool;
    }

    /**
     * Extends the default pool. This implementation will generate a new IAM token every 10 min.
     */
    public static class RdsIamAuthConnectionPool extends ConnectionPool implements Runnable {

        private RdsIamAuthTokenGenerator rdsIamAuthTokenGenerator;
        private GetIamAuthTokenRequest rdsIamAuthTokenRequest;
        private Thread tokenThread;
        private BlockingQueue<PooledConnection> busyConnections;
        private BlockingQueue<PooledConnection> idleConnections;

        public RdsIamAuthConnectionPool(PoolConfiguration prop) throws SQLException {
            super(prop);
        }

        /**
         * Initializes the pool.
         * @param prop the pool configuration
         * @throws SQLException
         */
        @Override
        protected void init(PoolConfiguration prop) throws SQLException {
            try {
                final URI uri = new URI(prop.getUrl().substring(5)); // jdbc:
                final String host = uri.getHost();
                final int port = uri.getPort() > 0 ? uri.getPort() : DEFAULT_PORT;
                final String region = host.endsWith(".rds.amazonaws.com") ? StringUtils.split(host, '.')[2] :  new DefaultAwsRegionProviderChain().getRegion();
                this.rdsIamAuthTokenGenerator = getRdsIamAuthTokenGenerator(region);
                this.rdsIamAuthTokenRequest = getIamAuthTokenRequest(host, port, prop.getUsername());
                updatePassword(prop);

                final Properties props = prop.getDbProperties();
                props.setProperty(REQUIRE_SSL,"true"); // for MySQL 5.x and before
                props.setProperty(VERIFY_SERVER_CERTIFICATE, "true");
                props.setProperty(SSL_MODE,VERIFY_CA); // for MySQL 8.x and higher
                props.setProperty(TRUST_CERTIFICATE_KEY_STORE_URL,CA_BUNDLE_URL);
                props.setProperty(TRUST_CERTIFICATE_KEY_STORE_TYPE, PEM);

                super.init(prop);
                this.busyConnections = getPrivateConnectionListField("busy");
                this.idleConnections = getPrivateConnectionListField("idle");
                createBackgroundThread();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void createBackgroundThread() {
            this.tokenThread = new Thread(this, "RdsIamAuthDataSourceTokenThread");
            this.tokenThread.setDaemon(true);
            this.tokenThread.start();
        }

        protected static GetIamAuthTokenRequest getIamAuthTokenRequest(String host, int port, String username) {
            return GetIamAuthTokenRequest.builder().hostname(host).port(port).userName(username).build();
        }

        protected static RdsIamAuthTokenGenerator getRdsIamAuthTokenGenerator(String region) {
            return RdsIamAuthTokenGenerator.builder().credentials(new DefaultAWSCredentialsProviderChain()).region(region).build();
        }

        @SuppressWarnings("unchecked")
        private BlockingQueue<PooledConnection> getPrivateConnectionListField(final String fieldName) {
            try {
                Field queueField = ConnectionPool.class.getDeclaredField(fieldName);
                queueField.setAccessible(true);
                return (BlockingQueue<PooledConnection>)queueField.get(this);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new IllegalStateException(e.getMessage(),e);
            }
        }

        @Override
        public void run() {
            /**
             * Removes the cached password from the PooledConnection
             */
            Consumer<PooledConnection> consumer = (pc) -> pc.getAttributes().remove(PoolUtilities.PROP_PASSWORD);
            try {
                do {
                    // wait for 10 minutes, then recreate the token
                    Thread.sleep(DEFAULT_TIMEOUT);
                    updatePassword(getPoolProperties());
                    idleConnections.forEach(consumer);
                    busyConnections.forEach(consumer);
                } while (this.tokenThread != null);
            } catch (InterruptedException e) {
                LOG.trace("Interrupted",e);
                Thread.currentThread().interrupt();
            }
        }

        @Override
        protected void close(boolean force) {
            super.close(force);
            Thread t = tokenThread;
            tokenThread = null;
            if (t != null) {
                t.interrupt();
            }
        }

        /**
         * Updates the password in the pool by generating a new token
         * @param poolConfiguration
         */
        private void updatePassword(PoolConfiguration poolConfiguration) {
            String token = rdsIamAuthTokenGenerator.getAuthToken(rdsIamAuthTokenRequest);
            LOG.debug("Updated IAM token for connection pool");
            poolConfiguration.setPassword(token);
        }
    }
}
