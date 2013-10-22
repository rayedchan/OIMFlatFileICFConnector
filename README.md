OIMFlatFileICFConnector
=======================
Version: Oracle Idenity Manager 11g R1
This is a sample ICF Connector from Oracle. I have modified the project
to be used in Netbeans. You can find the original source code and documentation under the original-jdev directory.

Required Jars
-icf-oim-intg.jar located in $MW_HOME/Oracle_IDM1/server/icf/intg
-connector-framework.jar
-connector-framework-internal.jar
-sshFactory.jar (http://www.jscape.com/downloads/ssh-factory)

Java Connector Server
-Connector Servers allow to you to execute the connector bundle outside of OIM. Here is a guide to setup a Java Connector Server in a Linux environment: "http://rayedchan.wordpress.com/2013/08/06/installing-and-configuring-a-java-connector-server/". 
-Add the ICF FlatFile Connector jar to the "bundles" directory of your Java Connector Server.
-Include sshFactory.jar in your Java Connector Server lib directory.
-Restart Java Connector Server when making changes such as modifying config files or adding new jars.
-[Optional] Use standalone java client to test ICF FlatFile Connector. The Java Connector must be ruuning. 
 https://bitbucket.org/rayedchan/icf-flat-file-connector-test-client