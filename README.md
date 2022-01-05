# carepool-aws-jdbc

Adds supports for RDS IAM authentication to a JDBC DataSource. Supports Tomcat and Hikari connection pools.

## 1. Tomcat DataSource
To use with Apache Tomcat JDBC ConnectionPool (default in Spring Boot 1.x):

```
spring:
  datasource:
    url: jdbc:mysql://dbhost.domain.com/dbname
    username: iam_username
    type: com.carepay.jdbc.tomcat.RdsIamTomcatDataSource

```

## 2. Hikari (default in Spring Boot 2.x)
```
spring:
  datasource:
    url: jdbc:mysql://dbhost.domain.com/dbname
    username: iam_username
    type: com.carepay.jdbc.hikari.RdsIamHikariDataSource

```

Please note that this library does not automatically add the Tomcat or Hikari dependency. So you will still need include the correct library:
```
implementation 'com.carepay:carepool-jdbc:2.1.14'
implementation 'com.zaxxer:HikariCP:3.2.0'`
//implementation 'org.apache.tomcat:tomcat-jdbc:9.0.21'
```

## 3. MySQL Plugin (Connector/J v8.0.10+)
To add support for IAM authentication using the MySQL Connector/J driver, you need to specify a
specific authentication plugin: `jdbc:mysql://test-db.cluster-xxxxxxx.eu-west-1.rds.amazonaws.com/mydb?authenticationPlugins=com.carepay.jdbc.mysql.RdsIamPasswordPlugin`
You can specify a specific AWS profile using `&awsProfile=xxx` parameter.

## 4. MariaDB Plugin
To use the MariaDB Plugin, simply add `carepool-aws-jdbc-2.1.14-all.jar` to the classpath of the JDBC driver. To use the IAM plugin, you need to specify the credentials type: `credentialType=AWS4RDS`. Example JDBC URL:

`jdbc:mariadb://db.cluster-xx.eu-west-1.rds.amazonaws.com/dbname?credentialType=AWS4RDS`
