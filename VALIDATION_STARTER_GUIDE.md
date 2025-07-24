# FHIR Bridge Validation - Complete Starter Guide

## 🎯 What You'll Accomplish
By the end of this guide, you will have successfully tested HL7-to-FHIR transformation using a simple point-and-click interface. No programming knowledge required.

## 📋 Prerequisites Checklist
Before starting, ensure you have:
- [ ] **Java 17** installed (check: `java -version`)
- [ ] **PostgreSQL** running (we'll verify this)
- [ ] **Postman** installed (free download from postman.com)
- [ ] **This project folder** open in your file explorer

## 🚀 Step 1: Start the Application (5 minutes)

### 1.1 Open Terminal/Command Prompt
**Windows**: Press `Windows Key + R`, type `cmd`, press Enter
**Mac**: Press `Command + Space`, type `terminal`, press Enter

### 1.2 Navigate to Project Folder
```bash
cd d:\fhir-bridge\fhir-bridge
```

### 1.3 Start PostgreSQL (if not running)
**Windows**:
```bash
# Check if PostgreSQL is running
pg_ctl status -D "C:\Program Files\PostgreSQL\14\data"

# If not running, start it
pg_ctl start -D "C:\Program Files\PostgreSQL\14\data"
```

**Mac**:
```bash
# Check if PostgreSQL is running
brew services list | grep postgresql

# If not running, start it
brew services start postgresql
```

### 1.4 Start the FHIR Bridge Application

#### Windows Users - Use Built-in Scripts
**Option 1: Double-click Method (Easiest)**
1. Open File Explorer
2. Navigate to: `d:\fhir-bridge\fhir-bridge\scripts`
3. **Double-click**: `dev-setup.bat`
4. **Wait**: A black command window will open and run automatically

**Option 2: Command Prompt**
```cmd
cd d:\fhir-bridge\fhir-bridge
scripts\dev-setup.bat
```

**Option 3: PowerShell**
```powershell
cd d:\fhir-bridge\fhir-bridge
.\scripts\dev-setup.bat
```

**Option 4: Docker Direct (if scripts fail)**
```cmd
cd d:\fhir-bridge\fhir-bridge
docker-compose up --build -d
```

#### Mac/Linux Users
```bash
cd /path/to/fhir-bridge
chmod +x scripts/dev-setup.sh
./scripts/dev-setup.sh
```

**Expected Output**:
```
Starting FHIR Bridge Application...
✅ PostgreSQL connection successful
✅ Database migrations applied
✅ Application started on http://localhost:8080
✅ Health check passed
```

### 1.5 Verify Application is Running
Open your web browser and go to: `http://localhost:8080/actuator/health`

**You should see**: `{"status":"UP"}`

## 📥 Step 2: Install Postman (3 minutes)

### 2.1 Choose Your Postman Option

#### ✅ Option 1: Postman VS Code Extension (Recommended)
**Best for: VS Code users who want integrated experience**
1. **Open VS Code**
2. **Go to Extensions** (Ctrl+Shift+X or Cmd+Shift+X)
3. **Search**: "Postman"
4. **Install**: "Postman - REST Client" by Postman
5. **Reload VS Code** when prompted

**Using the Extension**:
- **Open Postman panel**: Click Postman icon in sidebar
- **Import collection**: Drag and drop files into Postman panel
- **Run tests**: Click "Send" buttons directly in VS Code

#### ✅ Option 2: Standalone Postman Application
**Best for: Users who prefer separate application**
1. **Download**: https://www.postman.com/downloads/
2. **Install**: Run installer with default settings
3. **Open**: Start Menu → Postman (Windows) or Applications → Postman (Mac)

### 2.2 Both Options Work Equally Well
**VS Code Extension Benefits**:
- ✅ Integrated with your development environment
- ✅ No separate application to switch between
- ✅ Same import/export functionality
- ✅ Identical testing capabilities

**Standalone Benefits**:
- ✅ Works independently of VS Code
- ✅ Can be used on any computer
- ✅ Same import/export functionality
- ✅ Identical testing capabilities

### 2.3 No Difference in Testing
Both options support:
- ✅ Importing collections and environments
- ✅ Running all test cases
- ✅ JWT token management
- ✅ TEFCA compliance validation
- ✅ Performance testing
- ✅ All features needed for FHIR Bridge validation

## 📂 Step 3: Import Testing Files (2 minutes)

### 3.1 Import Collection
1. In Postman, click **"Import"** button (top-left)
2. Click **"Upload Files"**
3. Navigate to: `d:\fhir-bridge\fhir-bridge\postman`
4. Select: `FHIR_Bridge_Testing_Collection.json`
5. Click **"Import"**

### 3.2 Import Environment
1. Click **"Import"** again
2. Select: `FHIR_Bridge_Environment.json`
3. Click **"Import"**

### 3.3 Select Environment
1. In the top-right dropdown, select: **"FHIR Bridge Testing Environment"**
2. You should see variables like `base_url`, `jwt_token`, etc.

## 🔐 Step 4: Get Authentication Token (2 minutes)

### 4.1 Run Authentication Request
1. In Postman left sidebar, expand **"FHIR Bridge API Testing Collection"**
2. Expand **"Authentication"**
3. Click **"Get JWT Token"**
4. Click the blue **"Send"** button

### 4.2 Verify Token Received
**Expected Response**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 1800,
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

**The token is automatically saved** - no action needed!

## 🧪 Step 5: Run Your First Test (3 minutes)

### 5.1 Test Health Check
1. Expand **"Health Checks"**
2. Click **"Health Check"**
3. Click **"Send"**
4. **Expected**: `{"status":"UP"}`

### 5.2 Test FHIR Server
1. Click **"FHIR Capability Statement"**
2. Click **"Send"**
3. **Expected**: JSON response with FHIR server details

## 🔄 Step 6: Transform Your First HL7 Message (3 minutes)

### 6.1 Select Transformation Test
1. Expand **"HL7 to FHIR Transformation"**
2. Click **"Transform ADT_A01 - Patient Admission"**

### 6.2 Execute Transformation
1. Click **"Send"**
2. **Expected**: Large JSON response starting with:
```json
{
  "resourceType": "Bundle",
  "type": "transaction",
  "entry": [...]
}
```

### 6.3 Verify Results
Look for these key elements in the response:
- **Patient resource** with name "Alex Jordan Smith"
- **Encounter resource** with status "in-progress"
- **Observation resources** with lab values

## ✅ Step 7: Validate TEFCA Compliance (2 minutes)

### 7.1 Run Compliance Test
1. Expand **"TEFCA Compliance Testing"**
2. Click **"Validate TEFCA Format"**
3. Click **"Send"**

### 7.2 Check Compliance Headers
In the response headers, verify:
- `X-TEFCA-Compliant: true`
- `X-HIPAA-Compliant: true`

## 📊 Step 8: View FHIR Resources (2 minutes)

### 8.1 Get Patient by ID
1. Expand **"FHIR Resource Management"**
2. Click **"Get Patient by ID"**
3. Click **"Send"**
4. **Expected**: Patient details for PAT-000001

### 8.2 Search Patients
1. Click **"Search Patients"**
2. Click **"Send"**
3. **Expected**: List of patients matching search criteria

## 🎯 Step 9: Complete Validation Checklist

### 9.1 Basic Functionality
- [ ] Application starts successfully
- [ ] Health check returns "UP"
- [ ] Authentication works (JWT token received)
- [ ] HL7 transformation returns FHIR Bundle

### 9.2 TEFCA Compliance
- [ ] US Core Patient profile validated
- [ ] US Core Observation profile validated
- [ ] Security headers present
- [ ] Audit logging active

### 9.3 Data Accuracy
- [ ] Patient demographics match source
- [ ] Lab values correctly transformed
- [ ] Encounter details accurate
- [ ] Document references valid

## 🚨 Troubleshooting Quick Fixes

### Issue: "Port 8080 already in use"
**Solution**:
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID [PID] /F

# Mac
lsof -i :8080
kill -9 [PID]
```

### Issue: "Database connection failed"
**Solution**:
```bash
# Check PostgreSQL
psql -U postgres -c "SELECT 1"

# If fails, start PostgreSQL:
# Windows: pg_ctl start -D "C:\Program Files\PostgreSQL\14\data"
# Mac: brew services start postgresql
```

### Issue: "401 Unauthorized"
**Solution**:
1. Re-run "Get JWT Token" request
2. Verify token is saved in environment variables
3. Check Postman environment is selected

## 📈 Step 10: Performance Test (Optional - 2 minutes)

### 10.1 Run Load Test
1. Expand **"Performance Testing"**
2. Click **"Load Test - 100 Messages"**
3. Click **"Send"** (this will take ~30 seconds)
4. **Expected**: All 100 requests successful

## 🎉 Success Indicators

You have successfully validated the FHIR Bridge when:
- ✅ All Postman requests return 200 status
- ✅ FHIR Bundle contains correct patient data
- ✅ TEFCA compliance headers are present
- ✅ No errors in application logs

## 📞 Next Steps

### For Advanced Testing:
1. **Modify test data**: Edit HL7 messages in Postman
2. **Test different patients**: Change `patient_id` in environment
3. **Performance testing**: Use Collection Runner for batch tests
4. **Custom validation**: Add your own test assertions

### For Production:
1. **Change base URL**: Update `base_url` in Postman environment
2. **Use real credentials**: Replace test username/password
3. **Monitor logs**: Check `logs/fhir-bridge.log` for issues

## 🆘 Quick Help

**Stuck?** Check these resources:
- **Application logs**: `tail -f logs/fhir-bridge.log`
- **Postman console**: View → Show Postman Console
- **Health check**: `http://localhost:8080/actuator/health`
- **Support**: Open GitHub issue with error screenshots