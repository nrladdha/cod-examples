import phoenixdb
import configparser
import requests

import urllib3
urllib3.disable_warnings(category=urllib3.exceptions.InsecureRequestWarning)

class Database:

    def connect(self):
        REQUIRED_OPTS = ['Username', 'Password', 'Url']
        config = configparser.ConfigParser()
        config.read('config.ini')
        if not 'COD' in config:
            raise Exception("Could not find section for COD in config.ini")
        cod_config = config['COD']
        opts = {}

        # Validate the configuration
        for required_opt in REQUIRED_OPTS:
            if not required_opt in cod_config:
                raise Exception("Did not find %s in configuration" % (required_opt))    
  
      # Provide non-required options
        if 'Truststore' in cod_config:
            opts['verify'] = cod_config['Truststore']
        else:
            opts['verify'] = False
        if 'Authentication' in cod_config:
            opts['authentication'] = cod_config['Authentication']
        else:
            opts['authentication'] = 'BASIC'

        # Read required options
        opts['avatica_user'] = cod_config['Username']
        opts['avatica_password'] = cod_config['Password']
        return phoenixdb.connect(cod_config['Url'], autocommit=True, **opts)

