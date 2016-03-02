CAPortal 
====================================

A portal interface to an OpenCA DB that provides: 
- Host and client certificate request and renewal with CSR creation using either:
  -> ForgeJS - JavaScript that creates CSR on client or 
  -> BouncyCastle - CSR creation runs on server and is sent back down to client
- .p12 certificate creation using ForgeJS  
- RA certificate management interface: approving/delete/revoke CSR/CRR requests. 
- RA operator list/details.
- TODO: Does not yet provide an interface for CA Operators for managing RA operators,  
  RA details, exporting/signing requests, promoting certs to RA operator, managing 
  bulk requests.    


Note on ForgeJS
----------------
http://digitalbazaar.com/forge/
https://github.com/digitalbazaar/forge 
If you are just interested in creating CSRs using the ForgeJS library, then 
take a look at the following html files that show examples of how to use 
ForgeJS without running the portal: 'src\test\resources\forgeJStests\*.html'. 
These html files can be simply opened in your browser from disk. 
In a number of cases, you will need to use the browser's JavaScript console/debugger
(e.g. Firebug or Chrome console) to view the output.   


Pre-requisites 
---------------
- Servlet container supporting Servlet/JSP spec 2.5/2.1 (e.g. Tomcat v6 or v7). 
- Minimum JavaSE version 1.6 
- This portal uses a legacy OpenCA DB that has some extensions for processing a 
  'bulk' of many certificates. The CA db needs to be deployed and available (Postgres). 
  A 'create_openca_ddl.sql' script is provided to create the DB from scratch. 
  Note, if creating an empty DB, you will probably need to seed the DB with RA details. 
- maven (install maven and configure maven with your http proxy settings, if any) 
  see: http://maven.apache.org/guides/mini/guide-proxies.html 

Configuration 
-------------
- Copy 'WEB-INF/project.propertiesTEMPLATE' to 'WEB-INF/project.properties' and update  
  'WEB-INF/project.properties' to specify your details. This file defines DB 
  connection details and a path to the mutableproperties.properties file. 
- Copy 'WEB-INF/mutableproperties.propertiesTEMPLATE' to 'WEB-INF/mutableproperties.properties' and update  
  to specify your details.
- Configure your WebServer and servlet container/server for client certificate authentication. This will vary 
  according to your server and can be quite complex (but is just a one time setup). 
  See below for info on how to configure Apache and Tomcat. 
- Specify your preferred logging level in 'src/main/resources/log4j.xml'
- Build


Building
---------
Run the following maven targets to clean/build/package. Note, if this is the
first time you have run mvn, it will need to download/install various
maven plugins and dependencies into your local maven repo (~/.m2) and so may take some time: 

- 'mvn clean'
- 'mvn compile'  
- 'mvn package' (note, the war file is built in 'target/caportal-<versionSNAPSHOT>.war
                Rename this file to 'caportal.war' and deploy to your Servlet container). 

Deploy
-------
- Deploy 'target/caportal.war' to your servlet container. This normally requires 
  copying the war file to the servers 'webapps' directory. 
- MUST see Note below on BouncyCastle provided jar files. 


Note on BouncyCastle Provided Jar files
----------------------------------------
The 'pom.xml' file defines two BouncyCastle <dependency> elements for the following 
jar files: 'bcprov-jdk15on.jar' and 'bcpkix-jdk15on.jar'. 
After building, these jar files will be downloaded and will be available in your 
local maven repo (e.g. '$HOME/.m2/repository/org/bouncycastle/'). 

For deployment, you can either:   
- a) package these jars with your application when building, in which case 
  remove the '<scope>provided</scope>' for each these jar's <dependency> elements
  in 'pom.xml', then re-build OR 
- b) manually copy these jars into the server's shared/common lib directory, in which case 
  do specify the '<scope>provided</scope>' in pom.xml so that these jars are NOT 
  packaged into the final .war file (i.e. they are provided by the server). 

It is strongly recommended to go with option b) i.e. manually copy these jars in the server's 
common/shared lib. This means other webapps will be able to use them; 
Since these are special security provider jars, the class loader only allows
them to be loaded once by the first webapp that uses them. Therefore if 
you have another webapp that uses BC, they will not be able to load these jars. 
For Apache Tomcat, the shared/common lib dir is '<TOMCAT_HOME>/lib' 





Configuring Front end WebServer for Client Cert Auth
========================================
Basically, you can either configure Tomcat as the front-end webserver (not recommended) or run Tomcat behind Apache HTTPD
(apache acts as a reverse proxy) and let Apache perform the client certificate prompting/validation (strongly recommended). Both options are 
detailed below. 

##Use Apache HTTPD as front-end WebServer and configure Apache for SSL (apache acts as reverse proxy)
This is the recommended approach because apache is more configurable for SSL and client cert authentication. In your apache config, use mod_proxy to pass/reverse-pass requests to/from the tomcat instance via Tomcat's ajp connector. You will also need to use <Location> directives to specify the URLs that require client certificate verification. 
Your apache installation will require configuring with the CA certificates for your CA and the host cert/key.

See: [Tricks to do client cert auth behind reverse proxy](http://www.zeitoun.net/articles/client-certificate-x509-authentication-behind-reverse-proxy/start)

Sample apache config fragment (not all shown): 
```
#   SSL Engine Switch:
#   Enable/Disable SSL for this virtual host.
SSLEngine on

# Use mod_proxy to pass all requests to/from the target Tomcat's ajp connector
ProxyPass         /  ajp://localhost:8009/
ProxyPassReverse  /  ajp://localhost:8009/

# Specify which webapp URLs need client certificate verification
# (note we also specify the urls with trailing forward slash so that all 
# child-urls also require client-cert auth, e.g. /caporta/cert_owner/) 
 
# URLs for Cert owner pages
<Location /caportal/cert_owner>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /caportal/cert_owner/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# URLs for RA ops pages
<Location /caportal/raop>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /caportal/raop/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# URLs for CA operator pages
<Location /caportal/caop>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /caportal/caop/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# this option is mandatory to force apache to forward the client cert data to tomcat
SSLOptions +ExportCertData

#   SSL Protocol support:
# List the enable protocol levels with which clients will be able to
# connect.  Disable SSLv2 access by default:
SSLProtocol all -SSLv2 -SSLv3
SSLCipherSuite EECDH+AESGCM:EDH+AESGCM:AES256+EECDH:AES256+EDH

# Specify host cert/key/CA certs etc 
SSLCertificateFile /etc/grid-security/portal.ca.grid-support.ac.uk.crt
SSLCertificateKeyFile /etc/grid-security/portal.ca.grid-support.ac.uk.key
SSLCertificateChainFile /etc/grid-security/quovadis-chain.crt
SSLCACertificatePath /etc/grid-security/certificates/
SSLCARevocationPath  /etc/grid-security/certificates/
```

Sample tomcat server.xml fragment (not all shown):
```
<!-- Define an AJP 1.3 Connector on port 8009 -->
<Connector port="8009" protocol="AJP/1.3" redirectPort="8443" />
```

##Use Tomcat as front-end WebServer and configure Tomcat for SSL
Not recommended. Basically, you need to configure the Tomcat <Connector> (in server.xml) for SSL by specifying 
both a server certificate and trust (CA) certificates. Importantly, config of this element varies depending on  
whether you choose to use the Tomcat APR (apache portable runtime) Connector or the 
Tomcat JSSE Connector (the <Connector> supports different attributes depending on this choice
as shown below!). 

- See: http://tomcat.apache.org/tomcat-6.0-doc/ssl-howto.html
- See: http://tomcat.apache.org/tomcat-6.0-doc/connectors.html
- See JSSE Connector: http://tomcat.apache.org/tomcat-6.0-doc/config/http.html#SSL%20Support
- See APR Connector: http://tomcat.apache.org/tomcat-6.0-doc/apr.html


###JSSE Connector SSL and Client Cert authentication
- See JSSE Connector: http://tomcat.apache.org/tomcat-6.0-doc/config/http.html#SSL%20Support

The validity of the client cert needs to be verified against the cert authority which 
created it. Therefore, we need Tomcat to trust the client's cert issuer. In 
production, we would want to have the UK eScience certs only (ukescienceRoot and 2B certs) while
for the Dev CA we require the Dev CA cert only. To do this, 
export the issuer cert from a .p12 and import into a separate trust store file: 

```
$ keytool -exportcert -alias DevelopmentCA -keystore someKeystore.p12 -storetype PKCS12 -file DevelopmentCA.pem 
$ keytool -importcert -alias DevelopmentCA -keystore tomcatTrustStoreDevCA.jks -file DevelopmentCA.pem
```
You can then list the trust store using: 
```
$keytool -list -keystore tomcatTrustStoreDevCa.jks
 Enter keystore password:
 Keystore type: JKS
 Keystore provider: SUN
 Your keystore contains 1 entry
 developmentca, 05-Dec-2012, trustedCertEntry,
 Certificate fingerprint (MD5): B8:8F:67:4A:19:10:82:66:99:CB:93:57:9D:59:EA:B9
```

This new trust store file can then be used to configure the JSSE connector's truststoreFile 
setting, e.g: 

```xml
 <Connector 
   protocol="org.apache.coyote.http11.Http11Protocol"
   SSLEnabled="true" 
   clientAuth="want" 
   keystoreFile="C:/Program Files (x86)/Apache Software Foundation/apache-tomcat-6.0.35/conf/djm76DellHostCert.p12" 
   keystorePass="somepassword" 
   keystoreType="PKCS12" 
   maxThreads="150" 
   port="8443" 
   scheme="https" 
   secure="true" 
   <!-- sslProtocol="TLS"--> 
   sslProtocols = "TLSv1,TLSv1.1,TLSv1.2"        <!-- Tomcat 5 and 6 (prior to 6.0.38) -->
   sslEnabledProtocols = "TLSv1,TLSv1.1,TLSv1.2" <!-- Tomcat 6 (6.0.38 and later) and 7 -->
   truststoreFile="C:/Program Files (x86)/Apache Software Foundation/apache-tomcat-6.0.35/conf/tomcatTrustStoreDevCA.jks" 
   truststorePass="somepassword" 
   truststoreType="JKS"/>
``` 

  See: https://access.redhat.com/solutions/1232233
  If the sslEnabledProtocols or sslProtocols attributes are specified, only protocols that are listed and supported 
  by the SSL implementation will be enabled. If not specified, the JVM default is used. The permitted values may be 
  obtained from the JVM documentation for the allowed values for algorithm when creating an SSLContext instance e.g.
  Oracle Java 6 and Oracle Java 7.
 
### APR Connector SSL and Client Cert authentication
- See APR Connector: http://tomcat.apache.org/tomcat-6.0-doc/apr.html
- See: https://hynek.me/articles/hardening-your-web-servers-ssl-ciphers/ 

```xml
 <Connector
        protocol="org.apache.coyote.http11.Http11AprProtocol"
        port="8443"
        maxThreads="200"
        scheme="https"
        secure="true"
        SSLEnabled="true"
        SSLVerifyClient="optional"
        SSLCertificateFile="/etc/grid-security/hostcert.pem"
        SSLCertificateKeyFile="/etc/grid-security/hostkey.pem"
        SSLCertificateChainFile="/etc/grid-security/TerenaTrustChain.pem"
        SSLCACertificateFile="/etc/grid-security/e-ScienceTrustChain.pem"
        SSLCipherSuite="ECDH+AESGCM:DH+AESGCM:ECDH+AES256:DH+AES256:ECDH+AES128:DH+AES:ECDH+3DES:DH+3DES:RSA+AESGCM:RSA+AES:RSA+3DES:!aNULL:!MD5:!DSS"
        SSLProtocol="TLSv1" />
```
- See: https://access.redhat.com/solutions/1232233
  The default is for the SSLProtocol attribute to be set to ALL, with other acceptable values being SSLv2, SSLv3, TLSv1 and SSLv2+SSLv3. 
  Starting with version 1.1.21 of the Tomcat native library any combination of the three protocols concatenated with a plus sign will be supported

###Notes
- By default, if the tomcat installation is native APR, then it will use the openssl 
implementation, otherwise it will use the Java JSSE implementation. To avoid auto 
configuration you can define which implementation to use by specifying a classname in 
the 'protocol' attribute of the Connector.  

- Note, we specify clientAuth="want" (JSSE) or SSLVerifyClient="optional" (APR) because the CAPortal 
application supports dual mode authentication where parts of the webapp do not require client 
auth using certs while other parts of the webapp DO require client cert auth. Using 'want|optional' 
means that the SSL stack will request a client certificate, but it will not fail if one 
isn't presented. It is then left to the webapp to fail if user tries to access protected areas 
that require client cert auth (this is handled nicely by Spring Security).             

- Note, If you are using the native APR connector, then you will almost certainly require 
the 'SSLCACertificateFile' attribute rather than 'SSLCACertificatePath'. With the 
latter, we couldn't get the browser to prompt the user for client certs issued 
from only the supported CA(s), rather, the browser prompted for all 
the clients certs from any CA. Using the 'SSLCACertificateFile' we don't have 
this problem.  

- Note, you MUST Disable SSLv2: 
If you are deploying on Tomcat using the native APR connector, be aware of the 
following (related) bugs that affects versions <6.0.35 and <7.0.19. The bugs don't 
allow 'SSLv3+TLSv1' for the SSLProtocol attribute (APR). In this case, the 
SSLProtocol defaults to the value 'All'.   
 
https://issues.apache.org/bugzilla/show_bug.cgi?id=53344 
https://issues.apache.org/bugzilla/show_bug.cgi?id=51477 

Workaround: If you are using the tomcat APR connector you can disable SSLv2 by disabling
all of the SSLv2 ciphers (which effectively disables sslv2). This can be confirmed 
using the SSLLabs server query tool which confirms this! You can also use 
curl or openssl s_client as below to test sslv2

```
> curl --verbose --sslv2 https://ca-dev2.ca.ngs.ac.uk ...
> curl: (35) Unsupported SSL protocol version
> 
> openssl s_client -ssl2 -connect ca-dev2.ca.ngs.ac.uk:443 ...
> 2675716:error:1406D0B8:SSL routines:GET_SERVER_HELLO:no cipher
```
