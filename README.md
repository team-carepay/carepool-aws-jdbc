# carepool-jdbc

Adds supports for RDS IAM authentication to a JDBC DataSource. Supports Tomcat and Hikari connection pools.

To use with Apache Tomcat JDBC ConnectionPool (default in Spring Boot 1.x):

```
spring:
  datasource:
    url: jdbc:mysql://dbhost.domain.com/dbname
    username: iam_username
    type: com.carepay.jdbc.tomcat.RdsIamTomcatDataSource

```

And with Hikari (default in Spring Boot 2.x)
```
spring:
  datasource:
    url: jdbc:mysql://dbhost.domain.com/dbname
    username: iam_username
    type: com.carepay.jdbc.hikari.RdsIamHikariDataSource

```

Please note that this library does not automatically add the Tomcat or Hikari dependency. So you will still need include the correct library:
```
implementation 'com.carepay:carepool-jdbc:1.0.0'
implementation 'com.zaxxer:HikariCP:3.2.0'`
//implementation 'org.apache.tomcat:tomcat-jdbc:9.0.21'
```
