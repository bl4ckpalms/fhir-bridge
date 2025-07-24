import json
import boto3
import gzip
import base64
from datetime import datetime
import os

s3_client = boto3.client('s3')
logs_client = boto3.client('logs')

def handler(event, context):
    """
    Process CloudWatch Logs for analysis and alerting
    """
    try:
        # Decode and decompress the log data
        compressed_payload = base64.b64decode(event['awslogs']['data'])
        uncompressed_payload = gzip.decompress(compressed_payload)
        log_data = json.loads(uncompressed_payload)
        
        log_group = log_data['logGroup']
        log_stream = log_data['logStream']
        log_events = log_data['logEvents']
        
        # Process logs based on log group
        if 'application' in log_group:
            process_application_logs(log_events)
        elif 'security' in log_group:
            process_security_logs(log_events)
        elif 'audit' in log_group:
            process_audit_logs(log_events)
        
        # Store processed logs in S3
        store_processed_logs(log_data)
        
        return {
            'statusCode': 200,
            'body': json.dumps('Log processing completed successfully')
        }
        
    except Exception as e:
        print(f"Error processing logs: {str(e)}")
        return {
            'statusCode': 500,
            'body': json.dumps(f'Error processing logs: {str(e)}')
        }

def process_application_logs(log_events):
    """Process application logs for metrics and alerts"""
    error_count = 0
    performance_metrics = []
    
    for event in log_events:
        message = event['message']
        
        # Count errors
        if 'ERROR' in message:
            error_count += 1
            
        # Extract performance metrics
        if 'Request completed' in message:
            # Parse duration from message
            try:
                duration = extract_duration(message)
                performance_metrics.append({
                    'timestamp': event['timestamp'],
                    'duration': duration,
                    'message': message
                })
            except:
                pass
    
    # Send custom metrics to CloudWatch
    if error_count > 0:
        send_custom_metric('ApplicationErrors', error_count, 'FHIRBridge/Application')
    
    if performance_metrics:
        avg_duration = sum(m['duration'] for m in performance_metrics) / len(performance_metrics)
        send_custom_metric('AverageResponseTime', avg_duration, 'FHIRBridge/Performance')

def process_security_logs(log_events):
    """Process security logs for threat detection"""
    failed_auth_count = 0
    suspicious_activities = []
    
    for event in log_events:
        message = event['message']
        
        if 'authentication failed' in message.lower():
            failed_auth_count += 1
            
        if any(keyword in message.lower() for keyword in ['unauthorized', 'forbidden', 'blocked']):
            suspicious_activities.append({
                'timestamp': event['timestamp'],
                'message': message
            })
    
    if failed_auth_count > 0:
        send_custom_metric('FailedAuthentications', failed_auth_count, 'FHIRBridge/Security')
    
    if suspicious_activities:
        # Send to security team
        send_security_alert(suspicious_activities)

def process_audit_logs(log_events):
    """Process audit logs for compliance"""
    audit_events = []
    
    for event in log_events:
        try:
            audit_data = json.loads(event['message'])
            audit_events.append({
                'timestamp': event['timestamp'],
                'userId': audit_data.get('userId'),
                'action': audit_data.get('action'),
                'resourceType': audit_data.get('resourceType'),
                'resourceId': audit_data.get('resourceId'),
                'outcome': audit_data.get('outcome')
            })
        except:
            pass
    
    if audit_events:
        store_audit_summary(audit_events)

def extract_duration(message):
    """Extract duration from log message"""
    import re
    match = re.search(r'(\d+)ms', message)
    return int(match.group(1)) if match else 0

def send_custom_metric(metric_name, value, namespace):
    """Send custom metric to CloudWatch"""
    cloudwatch = boto3.client('cloudwatch')
    cloudwatch.put_metric_data(
        Namespace=namespace,
        MetricData=[
            {
                'MetricName': metric_name,
                'Value': value,
                'Unit': 'Count',
                'Timestamp': datetime.utcnow()
            }
        ]
    )

def send_security_alert(activities):
    """Send security alert to SNS"""
    sns = boto3.client('sns')
    message = {
        'alert_type': 'security',
        'activities': activities,
        'timestamp': datetime.utcnow().isoformat()
    }
    
    sns.publish(
        TopicArn=os.environ['SECURITY_ALERT_TOPIC_ARN'],
        Message=json.dumps(message),
        Subject='FHIR Bridge Security Alert'
    )

def store_processed_logs(log_data):
    """Store processed logs in S3"""
    bucket = os.environ['LOG_ANALYSIS_BUCKET']
    key = f"processed/{datetime.utcnow().strftime('%Y/%m/%d')}/{log_data['logStream']}.json"
    
    s3_client.put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(log_data),
        ContentType='application/json'
    )

def store_audit_summary(audit_events):
    """Store audit summary for compliance reporting"""
    bucket = os.environ['LOG_ANALYSIS_BUCKET']
    key = f"audit-summary/{datetime.utcnow().strftime('%Y/%m/%d')}/summary.json"
    
    summary = {
        'date': datetime.utcnow().isoformat(),
        'total_events': len(audit_events),
        'events': audit_events
    }
    
    s3_client.put_object(
        Bucket=bucket,
        Key=key,
        Body=json.dumps(summary),
        ContentType='application/json'
    )