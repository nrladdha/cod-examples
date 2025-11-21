package com.cloudera.cod.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

public class ApplicationPropertyLoader {
    private static ApplicationPropertyLoader instance=null;

    private static final Logger logger = LoggerFactory.getLogger(ApplicationPropertyLoader.class);

    public static final String COD_JDBC_URL="cod.jdbc.url";
    public static final String TABLE_SALT_BUCKETS="table.salt.buckets";
    public static final String CLIENT_ENDPOINT_URL="client.endpoint.url";

    private static final String APP_PROPERTIES_FILE_PATH="filepath";



    ArrayList<String> requiredKeys=null;

    private ApplicationPropertyLoader(){
        requiredKeys=new ArrayList<String>();
        requiredKeys.add(COD_JDBC_URL);
        requiredKeys.add(TABLE_SALT_BUCKETS);
        requiredKeys.add(CLIENT_ENDPOINT_URL);


    }
    public static  ApplicationPropertyLoader builder(){
        if (instance == null) {
            return new ApplicationPropertyLoader();
        }else {
            return instance;
        }
    }
    public Properties build() {
        Properties prop=new Properties();
        String filePath=System.getProperty(APP_PROPERTIES_FILE_PATH);
        logger.info("filePath : "+filePath);

        if (filePath ==null || filePath.trim().length()==0){
            logger.error("ERROR - filepath variable is not set. use -Dfilepath=<file-to-application.properties file>.");
            System.exit(-1);
        }
        try {
            InputStream inputStream = new FileInputStream(filePath);
        if (inputStream==null) {
            logger.error("ERROR - Could not load application.properties file.");
            System.exit(-1);
        }

            prop.load(inputStream);
        }catch (FileNotFoundException fnfException){
            logger.error("ERROR - Failed to find application.properties file. "+fnfException.getMessage());
            fnfException.printStackTrace();
            System.exit(-1);
        }catch (IOException ioException){
            logger.error("ERROR - Failed to build property object using application.properties file. "+ioException.getMessage());
            ioException.printStackTrace();
            System.exit(-1);
        }


        for (String requiredKey : requiredKeys){
            if (prop.containsKey(requiredKey)){
                if (prop.getProperty(requiredKey)==null || prop.getProperty(requiredKey).trim().length()==0){
                    logger.error("ERROR - Property is not defined in application.properties file : "+requiredKey);
                    System.exit(-1);
                }
            }else{
                logger.error("ERROR - Property is not defined in application.properties file : "+requiredKey);
                System.exit(-1);
            }
        }
        return prop;
    }
}
