package com.cloudera.cod.example.util;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.AppConfigurationEntry;
import java.util.HashMap;
import java.util.Map;
import java.security.PrivilegedAction;

public class KeytabLogin {

    // --- Configuration Details ---
    private static final String PRINCIPAL_NAME = "srv_nl_machineuser@PS-SANDB.A465-9Q4K.CLOUDERA.SITE";
    private static final String KEYTAB_FILE_PATH = "ps-sandbox-aws-srv_nl_machineuser.keytab";
    private static final String LOGIN_CONFIG_NAME = "KeytabLogin"; // A custom name for your configuration

    public static void main(String[] args) {
        // Optional: Ensure Kerberos config is found, if not in a default location
        System.setProperty("java.security.krb5.conf", "krb5.conf");

        try {
            Subject subject = loginWithKeytab();
            System.out.println("Kerberos Login Successful for Subject: " + subject.getPrincipals());

            // --- Execute privileged action ---
            Subject.doAs(subject, (PrivilegedAction<Void>) () -> {
                System.out.println("Running code with Kerberos credentials...");
                // Place your Kerberos-enabled code here (e.g., GSS-API calls,
                // Hadoop access, etc.)
                return null;
            });

        } catch (LoginException e) {
            System.err.println("Kerberos Login Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Performs the Kerberos login using the keytab and returns the authenticated Subject.
     */
    public static Subject loginWithKeytab() throws LoginException {
        // 1. Define the options for the Krb5LoginModule
        Map<String, String> options = new HashMap<>();
        options.put("useKeyTab", "true");      // Use the keytab file for credentials
        options.put("keyTab", KEYTAB_FILE_PATH); // Path to your keytab file
        options.put("principal", PRINCIPAL_NAME); // The principal in the keytab
        options.put("storeKey", "true");       // Store the secret key in the Subject
        options.put("doNotPrompt", "true");    // Do not prompt the user for password
        options.put("refreshKrb5Config", "true"); // Re-read krb5.conf before login

        // Optional for debugging:
        // options.put("debug", "true");

        // 2. Create the AppConfigurationEntry
        AppConfigurationEntry keytabLoginEntry = new AppConfigurationEntry(
                "com.sun.security.auth.module.Krb5LoginModule",
                AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                options
        );

        // 3. Create a custom JAAS Configuration
        Configuration jaasConfig = new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                // Ensure the name matches the one used in LoginContext constructor
                if (LOGIN_CONFIG_NAME.equals(name)) {
                    return new AppConfigurationEntry[]{keytabLoginEntry};
                }
                return null;
            }
        };

        // 4. Create the LoginContext and perform the login
        LoginContext loginContext = new LoginContext(LOGIN_CONFIG_NAME, (Subject)null, null, jaasConfig);
        loginContext.login();

        return loginContext.getSubject();
    }
}







