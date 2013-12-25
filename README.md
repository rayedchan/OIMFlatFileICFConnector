OIMFlatFileICFConnector
=======================
Version: Oracle Idenity Manager 11g R1  
This is a sample ICF Connector from Oracle. I have modified the project
to be used in Netbeans. You can find the original source code and documentation under the original-jdev directory.
  
__Quick Connector Installation__  
In the Connector-Installer-Package directory, I have included FlatFile.zip. The zip contains two directories you need to move around.   
The "jconnserv" is the Java Connector Server with everything included. The port number is 8759 and key is 12345. To start the java connector server, refer to Running the Java Connector Server section in "http://oraclestack.blogspot.com/2013/08/installing-and-configuring-java.html".   
The "FlatFile" is the connector installer package. Refer to "http://oraclestack.blogspot.com/2013/01/installing-oim-connector-on-11g.html" to install the connector in OIM.  
  
After installation, you need to update the FlatFileConnectorServer and FlatFileITResource IT Resources. Refer to "http://oraclestack.blogspot.com/2013/12/flat-file-connector-it-resources.html".  
Sample target system flat file can be found in sample-data directory.      
  
__Connector Server Requirements__  
This section does not need to be done if you are following the Quick Connector Installation section.
  
Required Jars  
-icf-oim-intg.jar located in $MW_HOME/Oracle_IDM1/server/icf/intg  
-connector-framework.jar  
-connector-framework-internal.jar  
-sshFactory.jar (http://www.jscape.com/downloads/ssh-factory)  
  
Java Connector Server  
-Connector Servers allow to you to execute the connector bundle outside of OIM. Here is a guide to setup a Java Connector Server in a Linux environment: "http://oraclestack.blogspot.com/2013/08/installing-and-configuring-java.html".   
-Add the ICF FlatFile Connector jar to the "bundles" directory of your Java Connector Server.  
-Include sshFactory.jar in your Java Connector Server lib directory.  
-Restart Java Connector Server when making changes such as modifying config files or adding new jars.  
-[Optional] Use standalone java client to test ICF FlatFile Connector. The Java Connector must be running.

__Side Note__
If you want to modify the functionality of the connector, update the code and build a new jar file. Replace the jar file in the bundles directory of the java connector server and restart the connector server. Depending on the changes you make in the code, you may have to update the metadata using design console.  