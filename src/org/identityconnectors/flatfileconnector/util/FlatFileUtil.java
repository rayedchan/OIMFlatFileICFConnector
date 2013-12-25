package org.identityconnectors.flatfileconnector.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Uid;


/**
 * The class FlatFileUtil contains utility methods such as createUid etc. 
 * 
 * @author manju
 */
public final class FlatFileUtil {
        public static String ERROR_MESSAGE = "account already exists";
        public static String LOOKUP_RECON_STRING = "Lookup.FF.UM.LookupRecon";
        public static String LOOKUP_RECON = "LOOKUP_RECON";
        public static String Normal_RECON = "Normal_RECON";
        public static String Incremental_RECON = "Incremental_RECON";
        public static String LatestToken="LatestToken";
        public static String ROLES = "Roles";
        public static String salutation = "salutation";
        public static String LOOKUP_RECON_FILE = "/home/mambuga/misc/roles.txt";
        public static String AccountId="AccountId";
        public static String Status = "__ENABLE__";
        public static String FirstName="FirstName";
        public static String lastName="lastName";
        public static String email="email";
        public static String Role="Role";
        public static String Salutation="Salutation";
        public static String Incremental_Recon_Attribute="LastModified";
	/**
	 * Creates and retunrs a Uid with the input value
	 * @param value
	 * @return Uid
	 */
	public static Uid getUid(String value){
		Uid uid = new Uid(value);
		return uid;
	}
	
	/**
	 * Returns a writer object for the input string
	 * @param file name
	 * @return buffered writer
	 */
	public static BufferedWriter getWriter(File file,boolean append){
		BufferedWriter writer = null;
		try {
			writer =  new BufferedWriter(new FileWriter(file,append));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to get a writer!");		
		}
		return writer;
	}
	
	/**
	 * Returns a reader object
	 * @param file
	 * @return
	 */
	public static BufferedReader getReader(File file){
		BufferedReader reader = null;
	    
		try {
			reader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to get a reader!");
		}
		return reader;
	}
	/**
	 * Closes the reader
	 * @param reader
	 */
	public static void closeReader(Reader reader){
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to close the reader "+e.getMessage());
		}
		
	}
	/**
	 * Closes the writer
	 * @param writer
	 */
	public static void closeWriter(Writer writer){
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Unable to close the writer "+e.getMessage());
		}
	}
	
	/**
	 * Creates a user on the target system, creates a corresponding Uid and returns it
	 * @param arg1
	 * @param targetFile
	 * @return
	 */
	public static Uid createAccount(Set<Attribute> arg1,File targetFile,String uidAttribute){	
		Uid returnUid = null;
		BufferedWriter writer = FlatFileUtil.getWriter(targetFile,true);
		try{
			for(Attribute thisAttribute:arg1){
				writer.write(thisAttribute.getName());
				writer.write(":");
				List<Object> valueList = thisAttribute.getValue(); 
				for(Object thisValue:valueList){
					writer.write(thisValue.toString());
					writer.write(";");
				}				
			}
			writer.write("\n");
			writer.flush();
		}catch(IOException e){
			throw new ConnectorIOException("IOException while creating "+e.getMessage());
		}
		Attribute thisAttribute = AttributeUtil.find(uidAttribute, arg1);
		String uidValue = thisAttribute.getValue().get(0).toString();
		returnUid = FlatFileUtil.getUid(uidValue);
		FlatFileUtil.closeWriter(writer);
		return returnUid;
	}
	
	private static String getAttributeValue(Set<Attribute> attributes, String name){
		String value = null;
		for(Attribute attr:attributes){
			if(attr.getName().equalsIgnoreCase(name)){
				value = attr.getValue().get(0).toString();
			}
		}
		return value;
	}
	
	/**
	 * Checks if account to be created on target system already exists
	 * @param arg1
	 * @param file
	 * @return
	 */
	public static boolean accountExists(Set<Attribute> arg1,File file){
		boolean accountExist = false;
		BufferedReader reader = FlatFileUtil.getReader(file);
		String s = null;
		String existingAccountID = getAttributeValue(arg1, "AccountId");
		
		try {
			while((s = reader.readLine()) != null){
				Scanner sc = new Scanner(s).useDelimiter(",");
                                System.out.println("s = "+s);                                
				while(sc.hasNext()){
					String nextStr = sc.next();
                                        System.out.println("nextStr = "+nextStr);
					String[] strArray = nextStr.split(":");
                                        System.out.println("strArray.length = "+strArray.length);
                                        for(String str:strArray){
                                            System.out.println("inside for str = "+str);                                            
                                        }
					if(existingAccountID.equalsIgnoreCase(strArray[strArray.length-1])){
						accountExist = true;
					}
				}
			}
		} catch (IOException e) {			
			e.printStackTrace();
			throw new RuntimeException("Unable to read from file "+file.getAbsolutePath());
		}
		FlatFileUtil.closeReader(reader);
		return accountExist;
	}
	
	/**
	 * Creates the ConnectorObject
	 * @param line
	 * @return
	 */
	public static ConnectorObject createConnectorObject(String line){
		ConnectorObject cb = null;			
			ConnectorObjectBuilder cBuilder = new ConnectorObjectBuilder();			
			Scanner sc = new Scanner(line).useDelimiter(",");
			while(sc.hasNext()){
				String nextStr = sc.next();
				String[] strArray = nextStr.split(":");
				cBuilder.addAttribute(strArray[0], strArray[1]);
				if(strArray[0].equalsIgnoreCase("AccountId")){
					cBuilder.setUid(strArray[0]);
					cBuilder.setName(strArray[0]);
				}
			}
			cb = cBuilder.build();
		return cb;
	}
	
	public static StringBuffer checkAndGetUpdateAttributes(Uid arg1,Set<Attribute> arg2,File targetFile){
		//boolean updatedAttributes = false;
                System.out.println("Inside checkAndGetUpdateAttributes...");
                
		boolean attributeFound = false;
		BufferedReader reader = FlatFileUtil.getReader(targetFile);
		String line = null;
		StringBuffer sb = new StringBuffer();
		try {
			while((line = reader.readLine()) != null){
				attributeFound = false;
				Scanner sc = new Scanner(line).useDelimiter(";");
				while(sc.hasNext()){
					String nextString = sc.next();
					String[] strArray = nextString.split(":");
					if(strArray[0].equals("AccountId") && strArray[1].equalsIgnoreCase(arg1.getUidValue())){					
						attributeFound = true;
						break;
					}
				}
				if(!attributeFound){
					sb.append(line);
					sb.append("\n");
				}else{
					StringBuffer updatedLine = new StringBuffer();
				    
					for(Attribute attr:arg2){
					    System.out.println("line = "+line);
					    System.out.println("attr.getValue() = "+attr.getValue());
                                            //we will enter here if line does not contains the replace attr value and name
                                            String[] strArray = null;
                                            //if(line.contains(attr.getName())){
                                                strArray = line.split(attr.getName());
                                            //}
                                            
                                            if(attr.getName().equalsIgnoreCase(FlatFileUtil.Role))
                                            {
                                                System.out.println("Updating role...");
                                                if(strArray[0] != null)
                                                    line = strArray[0];
                                                updatedLine.append(attr.getName());
                                                updatedLine.append(":");
                                                for(int i =0;i<attr.getValue().size();i++){
                                                    updatedLine.append(attr.getValue().get(i));
                                                    updatedLine.append(",");
                                                }
                                            }else{
                                                System.out.println("Updating "+attr.getName());
                                                if(strArray.length > 1 && strArray[1] != null){
                                                    line = "";
                                                    updatedLine.append(strArray[0]);
                                                    updatedLine.append(attr.getName());
                                                    updatedLine.append(":");
                                                    updatedLine.append(attr.getValue().get(0));
                                                    updatedLine.append(";");
                                                    Scanner sc1 = new Scanner(strArray[1]).useDelimiter(";");
                                                    int i = 0;
                                                    while(sc1.hasNext()){
                                                        if(i == 0){
                                                            sc1.next();
                                                            i++;
                                                        }else{
                                                            updatedLine.append(sc1.next());                                                            
                                                            updatedLine.append(";");
                                                        }
                                                    }
                                                    //updatedLine.append(strArray[1]);                                                    
                                                }else{
                                                    System.out.println("Updating to value "+attr.getValue().get(0));
                                                    updatedLine.append(attr.getName());
                                                    updatedLine.append(":");
                                                    updatedLine.append(attr.getValue().get(0));
                                                    updatedLine.append(";");
                                                }
                                            }
                                     //       if(!line.contains(attr.getValue().toString()) && !line.contains(attr.getName().toString())){
                                      //          updatedLine.append(attr.getName()).append(":").append(attr.getValue().get(attr.getValue().size()-1)).append(",");
                                       //     }//we come here if we have the attr name already in the line
                                       //     else if(!line.contains(attr.getValue().toString()) && line.contains(attr.getName().toString())){
                                       //         updatedLine.append(attr.getValue().get(attr.getValue().size()-1)).append(",");
                                       //     }
					}
                                    System.out.println("line = "+line);
				    System.out.println("updatedLine = "+updatedLine);
                                        sb.append(line);
					sb.append(updatedLine);
					sb.append("\n");
				}
			}
		} catch (IOException e) {			
			e.printStackTrace();
			throw new RuntimeException("Unable to read from file "+targetFile.getAbsolutePath());
		}
		System.out.println("Name: "+arg1.getName());
		System.out.println("UId value: "+arg1.getUidValue());
		System.out.println("value: "+arg1.getValue());
		FlatFileUtil.closeReader(reader);
		return sb;
	}
	
	public static void writeUpdatedContent(StringBuffer updatedContent,File file){
		BufferedWriter writer = FlatFileUtil.getWriter(file,false);
		try {			
			writer.write(updatedContent.toString());
			writer.flush();
		} catch (IOException e) {			
			e.printStackTrace();
			throw new RuntimeException("Unable to write to file "+file.getAbsolutePath());
		}		
		FlatFileUtil.closeWriter(writer);
	}
	
	public static StringBuffer deleteAttribute(String uidValue,File file){
		boolean accountExists = false;
		BufferedReader reader = FlatFileUtil.getReader(file);
		String line = null;
		StringBuffer newContent = new StringBuffer();
		try {
			while((line = reader.readLine()) != null){
				accountExists = false;				
				Scanner sc = new Scanner(line).useDelimiter(";");
				while(sc.hasNext()){					
					String[] strArray = sc.next().split(":");
					if(strArray[strArray.length-1].equalsIgnoreCase(uidValue)){
						accountExists = true;		
						break;
					}
				}
				if(!accountExists){
					newContent.append(line);
					newContent.append("\n");
				}
			}
		} catch (IOException e) {			
			e.printStackTrace();
			throw new RuntimeException("Cannot read from file "+file.getAbsolutePath());
		}	
		FlatFileUtil.closeReader(reader);
		return newContent;
	}
        
    public static Uid createUid(Set<Attribute> arg1,String uidAttribute){
        Uid returnValue = null;
        
        Attribute thisAttribute = AttributeUtil.find(uidAttribute, arg1);
        String uidValue = thisAttribute.getValue().get(0).toString();
        returnValue = FlatFileUtil.getUid(uidValue);       
        
        return returnValue;
    }
}