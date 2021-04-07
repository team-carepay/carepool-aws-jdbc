package com.carepay.jdbc.mariadb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.regex.Pattern;

import com.carepay.aws.auth.Credentials;
import com.carepay.aws.auth.CredentialsProvider;
import com.carepay.aws.auth.DefaultCredentialsProviderChain;
import com.carepay.aws.auth.RegionProvider;
import com.carepay.aws.region.DefaultRegionProviderChain;
import com.carepay.aws.util.URLOpener;
import com.carepay.jdbc.RdsAWS4Signer;
import com.carepay.jdbc.pem.PemKeyStoreProvider;
import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.credential.Credential;
import org.mariadb.jdbc.credential.CredentialPlugin;
import org.mariadb.jdbc.util.Options;

import static com.carepay.jdbc.RdsIamConstants.BUNDLE_DOWNLOAD_URL;
import static com.carepay.jdbc.RdsIamConstants.CA_BUNDLE_URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;

/**
 * MariaDB Credential Plugin which supports Amazon IAM authentication. Multiple credentials providers
 * are supported: Environment, System Properties, Profile, SSO, EC2 Metadata.
 */
public class AmazonRdsIamCredentialPlugin implements CredentialPlugin {
    private static final Pattern URL_PATTERN = Pattern.compile("^https?://.*");

    private final CredentialsProvider credentialsProvider;
    private final RegionProvider regionProvider;
    private final Clock clock;
    private final URLOpener opener;

    private RdsAWS4Signer signer;
    private LocalDateTime expiryDate;
    private HostAddress hostAddress;
    private String username;
    private Credential authToken;

    @Override
    public String type() {
        return "AWS4RDS";
    }

    @Override
    public String name() {
        return "Amazon RDS IAM Authentication plugin";
    }

    @Override
    public boolean mustUseSsl() {
        return true;
    }

    public AmazonRdsIamCredentialPlugin() {
        this(new DefaultCredentialsProviderChain(), new DefaultRegionProviderChain(), Clock.systemUTC(), new URLOpener.Default());
    }

    public AmazonRdsIamCredentialPlugin(final CredentialsProvider credentialsProvider, final RegionProvider regionProvider, final Clock clock, final URLOpener opener) {
        this.clock = clock;
        this.credentialsProvider = credentialsProvider;
        this.regionProvider = regionProvider;
        this.opener = opener;
        PemKeyStoreProvider.register();
    }

    @Override
    public CredentialPlugin initialize(final Options options, final String username, final HostAddress hostAddress) {
        this.hostAddress = hostAddress;
        this.username = username;
        configureServerSslCert(options);
        final Properties nonMappedOptions = options.nonMappedOptions;
        this.signer = new RdsAWS4Signer(getCredentialsProvider(nonMappedOptions), getRegionProvider(nonMappedOptions), this.clock);
        return this;
    }

    private void configureServerSslCert(final Options options) {
        try {
            if (options.serverSslCert == null) {
                options.serverSslCert = downloadCertBundle();
            } else if (URL_PATTERN.matcher(options.serverSslCert).matches()) {
                options.serverSslCert = downloadCertBundle(options.serverSslCert);
            }
        } catch (IOException e) {
            options.serverSslCert = CA_BUNDLE_URL;
        }
    }

    private String downloadCertBundle() throws IOException {
        return downloadCertBundle(BUNDLE_DOWNLOAD_URL);
    }

    private String downloadCertBundle(final String url) throws IOException {
        final HttpURLConnection uc = opener.open(URLOpener.create(url));
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream(), UTF_8));
             final PrintWriter pw = new PrintWriter(new StringWriter())) {
            String line;
            while ((line = in.readLine()) != null) {
                pw.println(line);
            }
            return pw.toString();
        }
    }

    private RegionProvider getRegionProvider(final Properties properties) {
        final String region = properties.getProperty("region");
        return region != null ? () -> region : this.regionProvider;
    }

    private CredentialsProvider getCredentialsProvider(final Properties properties) {
        final Credentials propertyCredentials = new Credentials(properties.getProperty("accessKeyId"), properties.getProperty("secretKey"), null);
        return propertyCredentials.isPresent() ? () -> propertyCredentials : this.credentialsProvider;
    }

    @Override
    public Credential get() {
        final LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), UTC);
        if (this.expiryDate == null || expiryDate.isBefore(now)) {
            this.authToken = new Credential(this.username, signer.generateToken(hostAddress.host, hostAddress.port, username));
            this.expiryDate = now.plusMinutes(10L); // Token expires after 15 min, so renew after 10 min
        }
        return authToken;
    }
}
