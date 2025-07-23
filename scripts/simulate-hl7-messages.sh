#!/bin/bash
# =============================================================================
# HL7 Message Simulation Script
# Simulates sending HL7 v2 messages to FHIR Bridge for testing
# =============================================================================

set -e

echo "🚀 Starting HL7 message simulation..."

# Configuration
FHIR_BRIDGE_URL="http://fhir-bridge-test:8080"
API_ENDPOINT="$FHIR_BRIDGE_URL/api/v1/transform"
HL7_MESSAGES_DIR="/hl7-messages"

# Wait for FHIR Bridge to be ready
echo "⏳ Waiting for FHIR Bridge to be ready..."
for i in {1..30}; do
    if curl -f "$FHIR_BRIDGE_URL/actuator/health" >/dev/null 2>&1; then
        echo "✅ FHIR Bridge is ready"
        break
    fi
    echo "   Attempt $i/30 - waiting 5 seconds..."
    sleep 5
done

# Function to send HL7 message
send_hl7_message() {
    local message_file=$1
    local message_type=$2
    
    echo "📤 Sending $message_type message: $(basename $message_file)"
    
    # Read the HL7 message and convert line endings
    local hl7_content=$(cat "$message_file" | tr '\n' '\r')
    
    # Send the message to FHIR Bridge
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Content-Type: application/x-hl7-v2" \
        -H "Accept: application/fhir+json" \
        --data-raw "$hl7_content" \
        "$API_ENDPOINT" || echo "000")
    
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo "   ✅ Success (HTTP $http_code)"
        echo "   📄 Response: $(echo "$response_body" | jq -r '.resourceType // "Unknown"') resource created"
    else
        echo "   ❌ Failed (HTTP $http_code)"
        echo "   📄 Error: $response_body"
    fi
    
    echo ""
    sleep 2
}

# Simulate message sending
echo "🔄 Starting message simulation..."
echo ""

# Check if HL7 messages directory exists
if [ ! -d "$HL7_MESSAGES_DIR" ]; then
    echo "❌ HL7 messages directory not found: $HL7_MESSAGES_DIR"
    exit 1
fi

# Send ADT message (Patient Admission)
if [ -f "$HL7_MESSAGES_DIR/adt-a01-sample.hl7" ]; then
    send_hl7_message "$HL7_MESSAGES_DIR/adt-a01-sample.hl7" "ADT^A01 (Patient Admission)"
fi

# Send ORM message (Order)
if [ -f "$HL7_MESSAGES_DIR/orm-o01-sample.hl7" ]; then
    send_hl7_message "$HL7_MESSAGES_DIR/orm-o01-sample.hl7" "ORM^O01 (Order Message)"
fi

# Send ORU message (Observation Result)
if [ -f "$HL7_MESSAGES_DIR/oru-r01-sample.hl7" ]; then
    send_hl7_message "$HL7_MESSAGES_DIR/oru-r01-sample.hl7" "ORU^R01 (Observation Result)"
fi

# Continuous simulation mode (optional)
if [ "$1" = "--continuous" ]; then
    echo "🔄 Starting continuous simulation mode..."
    echo "   Sending messages every 30 seconds..."
    echo "   Press Ctrl+C to stop"
    
    while true; do
        echo "📅 $(date): Sending batch of test messages..."
        
        # Randomize patient IDs to create variety
        PATIENT_ID="PATIENT$(printf "%03d" $((RANDOM % 100 + 1)))"
        
        # Send messages with randomized patient data
        for message_file in "$HL7_MESSAGES_DIR"/*.hl7; do
            if [ -f "$message_file" ]; then
                # Replace patient ID in message
                temp_file="/tmp/$(basename $message_file)"
                sed "s/PATIENT[0-9][0-9][0-9]/$PATIENT_ID/g" "$message_file" > "$temp_file"
                send_hl7_message "$temp_file" "$(basename $message_file)"
                rm -f "$temp_file"
            fi
        done
        
        echo "⏰ Waiting 30 seconds before next batch..."
        sleep 30
    done
fi

echo "🎉 HL7 message simulation completed!"
echo ""
echo "📊 Simulation summary:"
echo "   • Sent ADT^A01 (Patient Admission) message"
echo "   • Sent ORM^O01 (Order) message"  
echo "   • Sent ORU^R01 (Observation Result) message"
echo ""
echo "🔍 Check the following for results:"
echo "   • FHIR Bridge logs: docker logs fhir-bridge-test-app"
echo "   • Database records: Connect to PostgreSQL on port 5433"
echo "   • Redis cache: Connect to Redis on port 6380"
echo "   • API responses: Check the output above"