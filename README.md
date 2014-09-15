# SQL Server JDBC library with SSO

This JDBC driver allows you to connect to a SQL Server instance by using the Windows authentication (NTLM).

It is based on the [jTDS driver](http://jtds.sourceforge.net/). The main difference is that the native library used to perform the authentication is auto-loaded at runtime (no need to put it on the lib path).

## How to build

This project uses [Maven](http://maven.apache.org/) as a project builder.

To build this driver, you need to:

1. Download the latest source code
2. Download the latest [jTDS distribution file](http://sourceforge.net/projects/jtds/files/)
3. Set the property `jtds-dist-path` in `pom.xml` to the right path (for example `C:\temp\jtds-1.3.1-dist.zip`)
4. Run `mvn clean build`
5. The generated library is in the `target` folder