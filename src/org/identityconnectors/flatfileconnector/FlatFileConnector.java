package org.identityconnectors.flatfileconnector;

import com.jscape.inet.ssh.SshException;
import com.jscape.inet.ssh.SshSession;
import com.sun.corba.se.impl.io.InputStreamHook;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.ScriptContext;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.flatfileconnector.util.FlatFileUtil;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.impl.api.local.operations.FilteredResultsHandler;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.ScriptOnResourceOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

@ConnectorClass(configurationClass=FlatFileConnectorConfiguration.class, displayNameKey="connector.display")
public class FlatFileConnector implements PoolableConnector,CreateOp,UpdateOp,DeleteOp,SearchOp<String>,SchemaOp,TestOp {   
    private static final Log LOG = Log.getLog(FlatFileConnector.class); 
    private FlatFileConnectorConfiguration config;
    private boolean isAlive;
    private SshSession session;
    public void init(Configuration cfg) {
        //get the config object
        this.config = (FlatFileConnectorConfiguration)cfg;        
        //this creates a connection to the target system
        createSshConnection();       
        //connection is now alive
        this.isAlive = true;
        System.out.println("Finished init..!");
    }
    private void createSshConnection(){
        SshSession returnSession = null;
        FlatFileConnector ffc = this;
        this.config.getPassword().access(new GuardedString.Accessor() {        
                @Override
                public void access(char[] arg0) {
                    try {
                        session = new SshSession(config.getHostName(),config.getUserName(),new String(arg0));
                    } catch (SshException e) {
                        throw new RuntimeException(e);
                    }
                }
        }); 
        System.out.println("Successfully connected to the target system "+this.config.getHostName()+" with user name "+this.config.getUserName());
    }
    /**
     * {@inheritDoc}
     */
    public void dispose() {
        //disconnect the connection
        this.session.disconnect();
        //connection is no more alive
        this.isAlive = false;
        System.out.println("Finished dispose..!");
    }
    /**
     * {@inheritDoc}
     */
    public void checkAlive() {
        if(!isAlive){
            throw new IllegalStateException("This connector is dead!!");
        }
    } 
    /**
     * {@inheritDoc}
     */
    public Configuration getConfiguration() {
        return this.config;
    }
    
    /**
     * {@inheritDoc}
     */
    public Uid create(ObjectClass oclass, Set<Attribute> attrs, OperationOptions options) {
        Uid returnUid = null;                          
        File targetFile = this.config.getTargetFile();        
        String uidAttribute = this.config.getUniqueAttribute();       
        File customScriptFile = this.config.getCustomScriptForProvisioning();
        // if customScriptFile is not null, means the user provided a custom script when the IT resource was created in OIM advanced console,
        // use this for provisioning.
        if(customScriptFile != null){               
            String id=""; String lastName=""; String firstName=""; String email="";
            for(Attribute attr:attrs){
                if(attr.getName().equalsIgnoreCase("AccountId"))
                    id = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("lastName"))
                    lastName = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("firstName"))
                    firstName = attr.getValue().get(0).toString();
                if(attr.getName().equalsIgnoreCase("email"))
                    email = attr.getValue().get(0).toString();            
            }              
            ProcessBuilder pb = new ProcessBuilder(customScriptFile.getAbsolutePath(),id,firstName,lastName,email);
            BufferedReader bufferedReader = null;
            try {        
                pb = pb.redirectErrorStream(true);
                Process process = pb.start();                
                InputStream inputStream = process.getInputStream();
               InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
              bufferedReader  = new BufferedReader(inputStreamReader);
               String line = null;
                StringBuilder sb = new StringBuilder();
               while ((line = bufferedReader.readLine()) != null) {
                 sb.append(line);
                 sb.append("\n");
               }
                if(sb.length() > 0){
                    String response = sb.toString();                          
                    if (response.contains(FlatFileUtil.ERROR_MESSAGE)){
                        throw new RuntimeException("Error while provisioning "+sb);
                    }
                }                
            } catch (IOException e) {
                throw new RuntimeException("Error while executing script "+customScriptFile+". Error is "+e.getMessage());
            }            
            returnUid = FlatFileUtil.createUid(attrs, uidAttribute);
        }else{               
            if((!FlatFileUtil.accountExists(attrs, targetFile))){                    
                    returnUid = FlatFileUtil.createAccount(attrs,targetFile,uidAttribute);
            }     
        }return returnUid;      
    }
    /**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass oclass, Uid uid, Set<Attribute> replaceAttributes,
            OperationOptions options) { 
        FlatFileConnectorConfiguration config = (FlatFileConnectorConfiguration)this.getConfiguration();
        File targetFile = config.getTargetFile();
        StringBuffer updatedContent = FlatFileUtil.checkAndGetUpdateAttributes(uid, replaceAttributes, targetFile);
        
        FlatFileUtil.writeUpdatedContent(updatedContent, targetFile);   
        return new Uid(uid.getUidValue());
    }
    /**
     * {@inheritDoc}
     */
    public void delete(ObjectClass oclass, Uid uid, OperationOptions options) {
        FlatFileConnectorConfiguration config = (FlatFileConnectorConfiguration)this.getConfiguration();
                File targetFile = config.getTargetFile();
                StringBuffer updatedContent = FlatFileUtil.deleteAttribute(uid.getUidValue(), targetFile);     
                FlatFileUtil.writeUpdatedContent(updatedContent, targetFile);
        
    }


    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass,
                                                   OperationOptions operationOptions) {
        return new AbstractFilterTranslator(){};
    }

    public void executeQuery(ObjectClass objectClass, String string,
                             ResultsHandler resultsHandler,
                             OperationOptions operationOptions) {        

        FlatFileConnectorConfiguration config = (FlatFileConnectorConfiguration)this.getConfiguration();
        File targetFile = config.getTargetFile();
        File lookupReconFirl = config.getLookupReconFile();
        System.out.println(targetFile);
        System.out.println(lookupReconFirl);
        File customScriptFile = config.getCustomScriptForRecon();
        String uniqueAttribute = config.getUniqueAttribute();        
        BufferedReader reader = FlatFileUtil.getReader(targetFile);        
               
        //In ICF we perform search operation for any type of recon... be it lookup recon, normal recon 
        //operationOptions will help us to find out the same
        //first get the attributes... attributes will have clue
        String [] options = operationOptions.getAttributesToGet();               
        List<String> optionsList = Arrays.asList(options);
        System.out.println("optionsList = "+optionsList);
        String reconType = FlatFileUtil.Normal_RECON;
        if(optionsList.contains(FlatFileUtil.ROLES)){
            reconType = FlatFileUtil.LOOKUP_RECON;
        }
       
        
        // full recon but with script
        if(reconType.equalsIgnoreCase(FlatFileUtil.Normal_RECON) && customScriptFile != null){
            System.out.println("****Searching using custom script****");            
            ProcessBuilder pb = new ProcessBuilder(customScriptFile.getAbsolutePath());
            Process process;
            try {
                process = pb.start();
                InputStream is = process.getInputStream();
                InputStreamReader isReader = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isReader);
                String line = null;
                StringBuilder sb = new StringBuilder();
                while((line = br.readLine())!=null){                    
                    sb.append(line);                   
                }
                String s = sb.toString();
                Scanner sc = new Scanner(s).useDelimiter(",");
                while(sc.hasNext()){
                    String token = sc.next();                    
                    if(token.startsWith("AccountId")){
                        String[] tokenArray = token.split(":");                        
                        ConnectorObjectBuilder cObjBuilder = new ConnectorObjectBuilder();             
                        cObjBuilder.addAttribute(uniqueAttribute,tokenArray[1]);                           
                        cObjBuilder.setUid(uniqueAttribute);
                        cObjBuilder.setName(uniqueAttribute); 
                        ConnectorObject cb = cObjBuilder.build();                           
                        resultsHandler.handle(cb);
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception..!! "+e.getMessage());
            }
            
        }
        //normal recon but without scripts!
        // here I need to check for operation option 'LatestToken', if this is present and its value not null
        //then need to get data from target based on 'LatestToken' in other words incremental recon
        else if(reconType.equalsIgnoreCase(FlatFileUtil.Normal_RECON) && customScriptFile == null){  
            Map<String,HashMap<String,List<String>>> searchResult = null;
            if(optionsList.contains(FlatFileUtil.LatestToken)){
                System.out.println("Performing incremental recon");
                searchResult = getIncrementalSearchResult(reader, targetFile, uniqueAttribute);
            }else{
                System.out.println("Performing full recon");
                searchResult = getSearchResult(reader, targetFile, uniqueAttribute);                    
            }
            Set<String> accountIds = searchResult.keySet();
            for(String accountID:accountIds){
                ConnectorObjectBuilder cObjBuilder = new ConnectorObjectBuilder();             
                cObjBuilder.addAttribute(uniqueAttribute,accountID);                
                //cObjBuilder.setUid(uniqueAttribute);
                
                HashMap<String,List<String>> attrNameValues = searchResult.get(accountID);
                Set<String> attrNames = attrNameValues.keySet();
                for(String attrName:attrNames)
                {
                    System.out.println("Adding attr "+attrName+" with value "+attrNameValues.get(attrName)+" for accountID "+accountID);
                    
                    //Special Case to handle status attribute
                    if(attrName.equalsIgnoreCase(FlatFileUtil.Status))
                    {
                        String statusValue = attrNameValues.get(attrName).get(0);
                        boolean isEnabled = true; //enable by default
                        
                        if(statusValue.equalsIgnoreCase("false"))
                        {
                            isEnabled = false;
                        }
                        
                        else
                        {
                            isEnabled = true;
                        }
                        
                        cObjBuilder.addAttribute(AttributeBuilder.buildEnabled(isEnabled));
                    }
                    
                    //Other attribute
                    else
                    {
                        cObjBuilder.addAttribute(attrName,attrNameValues.get(attrName));
                    }
                }
                
                //Case: Handle accounts with no status attribute; Enabled by default
                if(!attrNames.contains(FlatFileUtil.Status))
                {
                    cObjBuilder.addAttribute(AttributeBuilder.buildEnabled(true));
                }
                
                cObjBuilder.addAttribute(FlatFileUtil.Incremental_Recon_Attribute, new Date().getTime());
                cObjBuilder.addAttribute(FlatFileUtil.LatestToken, new Date().getTime());
                Uid uid = new Uid(accountID);
                cObjBuilder.setUid(uid);
                cObjBuilder.setName(uid.getUidValue());
                ConnectorObject cb = cObjBuilder.build();  
                System.out.println("User Account Object" + cb);
                resultsHandler.handle(cb);
            }            
        }
        
        else if(reconType.equalsIgnoreCase(FlatFileUtil.LOOKUP_RECON)){
            //here is the place where we need write code which OIM is expecting for look up recon
            //we can have many lookup recons running, get which lookup recon we need to run
            //this is given in the scheduled tasks in OIM as 'Decode Attribute'            
            System.out.println(config.getLookupReconFile());
            File lookupReconFile = config.getLookupReconFile();
            FileReader fReader = null;
            BufferedReader bReader = null;
            try {
                fReader = new FileReader(lookupReconFile);
                bReader = new BufferedReader(fReader);
                String line = null;
                while((line = bReader.readLine()) != null){
                    ConnectorObjectBuilder cObjBuilder = new ConnectorObjectBuilder();
                    cObjBuilder.addAttribute(FlatFileUtil.ROLES,line);
                    cObjBuilder.setUid(FlatFileUtil.ROLES);
                    cObjBuilder.setName(FlatFileUtil.ROLES); 
                    ConnectorObject cb = cObjBuilder.build();                   
                    resultsHandler.handle(cb); 
                }
            } catch (FileNotFoundException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            } catch (IOException e) {
                System.out.println(e.getMessage());
                throw new RuntimeException(e);
            }
        }
        
    }
    private Map<String,HashMap<String,List<String>>> getIncrementalSearchResult(BufferedReader reader,File targetFile,String uniqueAttribute){
        return getSearchResult(reader, targetFile, uniqueAttribute); 
    }
    private Map<String,HashMap<String,List<String>>> getSearchResult(BufferedReader reader,File targetFile,String uniqueAttribute){
        List<String> result = new ArrayList<String>();
        
        Map<String,HashMap<String,List<String>>> searchResult = new HashMap<String, HashMap<String,List<String>>>();
              
        String line = null;        
        try {
            while((line = reader.readLine()) != null){
                HashMap<String,List<String>> values = new HashMap<String,List<String>>();  
                Scanner scanner = new Scanner(line).useDelimiter(";");
                String uniqueAttrValue = null;
                while(scanner.hasNext()){
                    List<String> valuesList = new ArrayList<String>();
                    String  str = scanner.next();
                    String[] strSplit = str.split(":");
                    if(strSplit[0].equalsIgnoreCase(uniqueAttribute)){
                        uniqueAttrValue =  strSplit[1];                        
                    }else{
                        if(strSplit[0].equalsIgnoreCase(FlatFileUtil.Role)){
                            String[] valuesArray = strSplit[1].split(",");
                            valuesList.addAll(Arrays.asList(valuesArray));
                            values.put(FlatFileUtil.Role, valuesList);                            
                        }else if(strSplit[0].equalsIgnoreCase(FlatFileUtil.email)){                            
                            valuesList.add(strSplit[1]);
                            values.put(FlatFileUtil.email, valuesList);                            
                        }else if(strSplit[0].equalsIgnoreCase(FlatFileUtil.lastName)){                            
                            valuesList.add(strSplit[1]);
                            values.put(FlatFileUtil.lastName, valuesList);                            
                        }else if(strSplit[0].equalsIgnoreCase(FlatFileUtil.FirstName)){                            
                            valuesList.add(strSplit[1]);
                            values.put(FlatFileUtil.FirstName, valuesList);                            
                        }else if(strSplit[0].equalsIgnoreCase(FlatFileUtil.Status)){                            
                            valuesList.add(strSplit[1]);
                            values.put(FlatFileUtil.Status, valuesList);                            
                        } else if(strSplit[0].equalsIgnoreCase(FlatFileUtil.Salutation)){                            
                            valuesList.add(strSplit[1]);
                            values.put(FlatFileUtil.Salutation, valuesList);                            
                        }
                            
                    }
                }
        
                searchResult.put(uniqueAttrValue, values);
            }        
        } catch (IOException e) {
            throw new RuntimeException("Exception while reading the target file "+e.getMessage());
        }
        
        
        System.out.println("Account Ids "+searchResult.keySet());
        
        Set<String> ids = searchResult.keySet();
        for(String id:ids){
            System.out.println("Account ID = "+id);
            Map<String,List<String>> attrNameValues = searchResult.get(id);
            Set<String> attrNames = attrNameValues.keySet();
            for(String attrName:attrNames){
                System.out.println("Attr name = "+attrName+" Attr value = "+attrNameValues.get(attrName));
            }
        }
        return searchResult;
    }

    public Schema schema() {
        Set<AttributeInfo> attrInfoSet = new HashSet<AttributeInfo>();
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.AccountId, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.FirstName, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.lastName, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.email, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.Role, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.Salutation, String.class));
        attrInfoSet.add(AttributeInfoBuilder.build(FlatFileUtil.Incremental_Recon_Attribute, String.class));
        SchemaBuilder schemaBld = new SchemaBuilder(FlatFileConnector.class);
        schemaBld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfoSet);
        Schema schema = schemaBld.build();
        return schema;
    }

    public void test() {
        createSshConnection();
    }
}