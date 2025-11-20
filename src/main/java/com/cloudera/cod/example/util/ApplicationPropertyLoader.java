package com.cloudera.cod.example.util;

import org.apache.phoenix.shaded.com.sun.jersey.json.impl.provider.entity.JSONArrayProvider;
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

    ArrayList<String> requiredKeys=null;

    private ApplicationPropertyLoader(){
        requiredKeys=new ArrayList<String>();
        requiredKeys.add("cod.jdbc.url");
        requiredKeys.add("table.salt.buckets");


    }
    public static  ApplicationPropertyLoader builder(){
        if (instance == null) {
            return new ApplicationPropertyLoader();
        }else {
            return instance;
        }
    }
    public Properties build() throws IOException {
        Properties prop=new Properties();
        String filePath=System.getProperty("filepath");
        logger.info("filePath : "+filePath);

        if (filePath ==null || filePath.trim().length()==0){
            logger.error("ERROR - filepath variable is not set. use -Dfilepath=<file-to-application.properties file>.");
            System.exit(-1);
        }
        InputStream inputStream = new FileInputStream(filePath);
        if (inputStream==null) {
            logger.error("ERROR - Could not load application.properties file.");
            System.exit(-1);
        }
        prop.load(inputStream);

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
