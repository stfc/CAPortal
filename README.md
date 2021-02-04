CAPortal 
========

A portal interface to an OpenCA DB that provides: 
- Host and client certificate request and renewal with CSR creation using either:
  -> ForgeJS - JavaScript that creates CSR on client or 
  -> BouncyCastle - CSR creation runs on server and is sent back down to client
- .p12 certificate creation using ForgeJS  
- RA certificate management interface: approving/delete/revoke CSR/CRR requests. 
- RA operator list/details.


Note on ForgeJS
----------------
http://digitalbazaar.com/forge/

https://github.com/digitalbazaar/forge 

If you are just interested in creating CSRs using the ForgeJS library, then 
take a look at the following html files that show examples of how to use 
ForgeJS without running the portal: 'src\test\resources\forgeJStests\*.html'. 

These html files can be simply opened in your browser from disk. 
In a number of cases, you will need to use the browser's JavaScript console/debugger to view the output.   


Pre-requisites 
---------------
- Java 11 or above
- This portal uses a legacy OpenCA DB that has some extensions for processing a 
  'bulk' of many certificates. The CA db needs to be deployed and available (Postgres). 
  A 'create_openca_ddl.sql' script is provided to create the DB from scratch. 
  Note, if creating an empty DB, you will probably need to seed the DB with RA details. 
- Maven

Configuration 
-------------
- Copy `application.propertiesTEMPLATE` to the same folder you will be deploying and update to specify your details. This file defines DB 
  connection details and a path to the `mutableproperties.properties` file. 
- Copy `mutableproperties.propertiesTEMPLATE` to the same folder you will be deploying and update  
  to specify your details.
- Remove TEMPLATE from the end of the files
- Configure your web server for client certificate authentication. This will vary 
  according to your server and can be quite complex (but is just a one time setup). 
  See below for info on how to configure Apache.
- Build


Building
---------
Run the following maven targets to clean/build/package. Note, if this is the
first time you have run mvn, it will need to download/install various
maven plugins and dependencies into your local maven repo (~/.m2) and so may take some time: 

- 'mvn clean'
- 'mvn compile'  
- 'mvn package'

Deploy
-------
- Copy 'target/caportal.war' to your target system. This is an executable Java file, and will run an embedded web server.
- Copy `caportal.service` to your systems systemd service location to run it on boot/as a service.


Configuring Front end WebServer  for Client Cert Auth
-----------------------------------------------------

In your apache config, use mod_proxy to pass/reverse-pass requests to/from the 
You will also need to use <Location> directives to specify the URLs that require client certificate verification. 
Your apache installation will require configuring with the CA certificates for your CA and the host cert/key.

Sample apache config fragment (not all shown): 
```
#   SSL Engine Switch:
#   Enable/Disable SSL for this virtual host.
SSLEngine on

# Use mod_proxy to pass all requests to/from CAPortals ajp connector
# This is REQUIRED for Client Certificate authentication to work. 
# Normal HTTP proxying will NOT work.
ProxyPass         /  ajp://localhost:8009/
ProxyPassReverse  /  ajp://localhost:8009/

# Specify which webapp URLs need client certificate verification
# (note we also specify the urls with trailing forward slash so that all 
# child-urls also require client-cert auth, e.g. /caporta/cert_owner/) 
 
# URLs for Cert owner pages
<Location /cert_owner>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /cert_owner/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# URLs for RA ops pages
<Location /raop>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /raop/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# URLs for CA operator pages
<Location /caop>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>
<Location /caop/>
  SSLVerifyClient require
  SSLVerifyDepth 10
</Location>

# this option is mandatory to force apache to forward the client cert data to embedded Tomcat
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