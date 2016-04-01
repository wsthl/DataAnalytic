package com.ibm.cstl.sport.aa.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;

import com.ibm.team.repository.common.IContent;
import com.ibm.team.repository.common.util.ObfuscationHelper;

/**
 * if isCreateRTCLoginFile()== false 
 * 		get the userId and passWord from RTC and Retain File 
 * else if isCreateRTCLoginFile() == true  
 * 		create RTC LoginFile and Retain LoginFile 
 *
 */


public class PasswordHelper {
    
    public static String[] getPassword(String client, File passwordFile) throws GeneralSecurityException, IOException {
        if (!passwordFile.exists()) {
            throw new FileNotFoundException(client + " Password File dosen't exist!");
        }
        if (passwordFile.isDirectory()) {
            throw new FileNotFoundException(client + " Password File is Directory!");
        }
        String user = null;
        String password = null;
        FileInputStream inputStream = new FileInputStream(passwordFile);
        try {
            Properties properties = new Properties();
            properties.loadFromXML(inputStream);
            user = properties.getProperty("user");
            if (user == null) {
                throw new IOException("Invalid "+ client + " User!");
            }
            password = properties.getProperty("password");
            if (password == null) {
                throw new IOException("Invalid "+ client + " Password!");
            }
            password = ObfuscationHelper.decryptString(password);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return new String[]{user, password};
    }
    
    public static void createPasswordFile(File passwordFile, String user, String password) throws IOException,
            GeneralSecurityException {
        FileOutputStream outputStream = new FileOutputStream(passwordFile);
        Properties properties = new Properties();
        properties.put("user", user);
        properties.put("password", ObfuscationHelper.encryptString(password));
        properties.storeToXML(outputStream, "4.0", IContent.ENCODING_UTF_8);
        outputStream.close();
    }
    
}