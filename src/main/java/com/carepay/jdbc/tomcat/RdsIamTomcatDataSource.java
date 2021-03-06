package com.carepay.jdbc.tomcat;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.carepay.jdbc.RdsAWS4Signer;
import com.carepay.jdbc.pem.PemKeyStoreProvider;
import com.carepay.jdbc.util.DaemonThreadFactory;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.PoolConfiguration;

/**
 * DataSource based on Tomcat connection pool that supports IAM authentication to RDS
 */
public class RdsIamTomcatDataSource extends org.apache.tomcat.jdbc.pool.DataSource {

    static {
        PemKeyStoreProvider.register();
    }

    private final ScheduledExecutorService scheduledExectorService;
    private final RdsAWS4Signer tokenGenerator;

    public RdsIamTomcatDataSource() {
        this(new RdsAWS4Signer(), Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory()));
    }

    public RdsIamTomcatDataSource(final RdsAWS4Signer tokenGenerator, final ScheduledExecutorService scheduledExecutorService) {
        this.tokenGenerator = tokenGenerator;
        this.scheduledExectorService = scheduledExecutorService;
        PemKeyStoreProvider.register();
    }

    public RdsIamTomcatDataSource(final RdsAWS4Signer tokenGenerator,
                                  final ScheduledExecutorService scheduledExecutorService,
                                  final PoolConfiguration poolProperties) {
        super(poolProperties);
        this.tokenGenerator = tokenGenerator;
        this.scheduledExectorService = scheduledExecutorService;
    }

    /**
     * Creates a new Connection Pool once. Overridden so we can change the underlying pool.
     *
     * @return the cached pool or the newly created pool.
     */
    @Override
    public ConnectionPool createPool() throws SQLException {
        return pool != null ? pool : createPoolImpl();
    }

    /**
     * Creates a new RDS IAM backed pool.
     */
    protected synchronized ConnectionPool createPoolImpl() throws SQLException {
        if (pool == null) {
            pool = new RdsIamAuthConnectionPool(tokenGenerator, poolProperties, scheduledExectorService);
        }
        return pool;
    }

}
