import json
import logging
import os
from datetime import datetime
import phoenixdb
from database import Database


logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s.%(msecs)03d - %(levelname)s - %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)

# Global connection variable for connection reuse across Lambda invocations
_connection = None

def log_and_print(message, level="info"):
    timestamp = datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
    full_message = f"{timestamp} - {level.upper()} - {message}"
    print(full_message)

def format_response(status_code, body_dict):
    return {
        "isBase64Encoded": False,
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(body_dict)
    }



def get_phoenix_connection():
    """
    Get or create Phoenix database connection
    Reuses connection across Lambda invocations for better performance
    """
    global _connection    
    try:

        # Check if existing connection is still valid
        if _connection is not None:
            try:
                # Test connection with a simple query
                #cursor = _connection.cursor()
                #cursor.execute("SELECT 1")
                #cursor.close()
               # print("Reusing existing Phoenix connection")
                return _connection
            except Exception as e:
                print(f"Existing connection invalid: {e}",'error')
                _connection = None
        
        # Create new connection
        db = Database()
        _connection = db.connect()
        print("New Phoenix connection established successfully")
        return _connection
        
    except ImportError:
        print("Module not found. Please include it in deployment package.",'error')
        raise
    except Exception as e:
        print(f"Failed to connect to Phoenix: {str(e)}",'error')
        raise


def lambda_handler(event, context):
    print(f"Received event: {json.dumps(event)}")


    # Extract CUST_ID from various possible event formats
    cust_id = None
    
    # Direct invocation
    if 'cust_id' in event:
        cust_id = event['cust_id']
    # API Gateway query parameter
    elif 'queryStringParameters' in event and event['queryStringParameters']:
        cust_id = event['queryStringParameters'].get('cust_id')
    # API Gateway path parameter
    elif 'pathParameters' in event and event['pathParameters']:
        cust_id = event['pathParameters'].get('cust_id')
    # Body (for POST requests)
    elif 'body' in event:
        try:
            body = json.loads(event['body']) if isinstance(event['body'], str) else event['body']
            cust_id = body.get('cust_id')
        except json.JSONDecodeError:
            pass
    
    print(f"CUST_ID: {cust_id}")

    if not cust_id:
        return format_response(400, {"error": "Missing 'cust_id' in request"})
   
    try:
        
        conn = get_phoenix_connection()
        cursor = conn.cursor()
        query_start = datetime.now()
        cursor.execute(f"SELECT * FROM TEST1.PROMOTIONS WHERE CUST_ID = '{cust_id}'")
        output=cursor.fetchone()
        cursor.close()
        query_end = datetime.now()
        if not output:
            output_cust_id = output
            output_promotions = None
            #print(f"Query executed: No records found for customer cust_id: {cust_id}")
        else:
            output_cust_id = output[0]
            output_promotions = output[1]
            #print(f"Query executed: {output_cust_id}")
     
        query_duration = (query_end - query_start).total_seconds()
        log_and_print(f"For customer cust_id : {cust_id} - PhoenixDB query completed in {query_duration:.6f} seconds")

      
        if not output_cust_id:
            log_and_print(f"No records found for customer cust_id: {cust_id}", level="warning")
            return format_response(404, {"cust_id": f"{cust_id}","promotions": "No promotions found.","CODreq_starttime": f"{query_start}","CODreq_endtime": f"{query_end}","CODreq_duration": f"{query_duration}"})

        log_and_print(f"Found customer cust_id: {cust_id}", level="info")   
        return format_response(200, {"cust_id": f"{output_cust_id}","promotions": f"{output_promotions}","CODreq_starttime": f"{query_start}","CODreq_endtime": f"{query_end}","CODreq_duration": f"{query_duration}"})

    except Exception as e:
        log_and_print(f"Error accessing cod : {str(e)}", level="error")
        return format_response(500, {"error": f"Failed to access cod database: {str(e)}"})
