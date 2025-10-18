# Authentication Tests

This directory contains comprehensive tests for the authentication endpoints, specifically focusing on the login functionality.

## 📁 Directory Structure

```
tests/
├── auth/                    # Authentication-specific tests
│   ├── login-normal.test.js        # Normal login flow tests
│   ├── login-bypass.test.js        # Development bypass tests
│   └── login-errors.test.js        # Error handling tests
├── utils/                   # Test utilities and helpers
│   ├── http-client.js              # HTTP client for API requests
│   └── test-helpers.js             # Test utilities and assertion helpers
├── api/                     # Future API tests
├── run-auth-tests.js       # Main test runner
└── README.md               # This file
```

## 🚀 Running Tests

### Prerequisites

1. **Environment Variables**: Ensure your `.env` file contains:
   ```env
   SUPABASE_URL=your_supabase_url
   SUPABASE_ANON_KEY=your_anon_key
   SUPABASE_SERVICE_ROLE_KEY=your_service_role_key
   ```

2. **Development Bypass Testing** (optional):
   ```env
   NODE_ENV=development
   ALLOW_UNVERIFIED_LOGIN=true
   ```

### Run All Tests

The main test runner automatically starts the development server and runs all tests:

```bash
# Run all authentication tests
node tests/run-auth-tests.js
```

### Run Individual Test Suites

```bash
# Normal login flow
node tests/auth/login-normal.test.js

# Development bypass
node tests/auth/login-bypass.test.js

# Error handling
node tests/auth/login-errors.test.js
```

### Package.json Scripts

Add these scripts to your `package.json`:

```json
{
  "scripts": {
    "test:auth": "node tests/run-auth-tests.js",
    "test:auth:normal": "node tests/auth/login-normal.test.js",
    "test:auth:bypass": "node tests/auth/login-bypass.test.js",
    "test:auth:errors": "node tests/auth/login-errors.test.js"
  }
}
```

## 🧪 Test Suites

### 1. Normal Login Tests (`login-normal.test.js`)

Tests the standard authentication flow:
- ✅ Valid credentials authentication
- ✅ Response structure validation
- ✅ Token generation verification
- ✅ Response time performance
- ✅ HTTP method validation

### 2. Development Bypass Tests (`login-bypass.test.js`)

Tests the development bypass functionality:
- ✅ Environment variable validation
- ✅ Bypass for unverified users (development only)
- ✅ Mock session token generation
- ✅ Production mode prevention
- ✅ Non-existent user handling

### 3. Error Handling Tests (`login-errors.test.js`)

Comprehensive error scenario testing:
- ❌ Invalid credentials
- ❌ Invalid email format
- ❌ Missing password
- ❌ Empty fields
- ❌ No request body
- ❌ Malformed JSON
- ❌ Wrong content type
- ❌ Very long credentials (security)
- ❌ SQL injection attempts (security)
- ❌ XSS attempts (security)
- ❌ Rate limiting validation

## 📊 Test Output

The test runner provides colorized output with:
- 🔍 Environment variable validation
- 🚀 Server startup status
- ⏳ Server readiness checks
- 🧪 Individual test results
- 📊 Comprehensive test summary
- 🎉 Pass/fail statistics

### Example Output

```
🧪 Authentication Test Suite
============================================================
Testing the login.js endpoint with comprehensive scenarios

🔍 Environment Check
==============================
✓ SUPABASE_URL: Set
✓ SUPABASE_ANON_KEY: Set
✓ SUPABASE_SERVICE_ROLE_KEY: Set
• NODE_ENV: development
• ALLOW_UNVERIFIED_LOGIN: true

✅ Environment check passed!

🚀 Starting Next.js development server...
✅ Development server started successfully!

⏳ Waiting for server to be ready for requests...
✓ Server is ready

🧪 Running Authentication Test Suite
============================================================

▶️  Running Normal Login tests...
✅ Normal Login PASSED (1247ms)

▶️  Running Development Bypass tests...
✅ Development Bypass PASSED (892ms)

▶️  Running Error Handling tests...
✅ Error Handling PASSED (3156ms)

📊 Test Suite Summary
============================================================

Total Tests: 3
Passed: 3
Failed: 0
Total Duration: 5295ms

🎉 All tests passed!
```

## 🔧 Configuration

### Test Configuration

Modify `utils/test-helpers.js` for:
- Base URL configuration
- Timeout settings
- Debug mode
- Test data

### Runner Configuration

Modify `run-auth-tests.js` for:
- Server startup timeout
- Test execution order
- Color themes
- Reporting format

## 🛡️ Security Tests

The test suite includes security validation for:

- **Input Validation**: Email format, required fields
- **SQL Injection**: Malicious SQL in credentials
- **XSS Prevention**: Script injection attempts
- **Rate Limiting**: Multiple rapid requests
- **Data Sanitization**: Output escaping

## 🐛 Troubleshooting

### Common Issues

1. **Server won't start**
   - Check if port 3000 is available
   - Verify `package.json` has `dev` script
   - Ensure all dependencies are installed

2. **Environment variables not loaded**
   - Verify `.env` file exists in project root
   - Check variable names match exactly
   - Restart development server

3. **Tests timeout**
   - Increase timeout values in test configuration
   - Check database connectivity
   - Verify Supabase service status

4. **Development bypass not working**
   - Ensure `NODE_ENV=development`
   - Verify `ALLOW_UNVERIFIED_LOGIN=true`
   - Check test user exists in database

### Debug Mode

Enable debug output:
```bash
TEST_DEBUG=true node tests/run-auth-tests.js
```

## 📝 Adding New Tests

1. **Create test file** in appropriate directory
2. **Import test helpers** from `utils/test-helpers.js`
3. **Export test function** for runner integration
4. **Add to runner** in `run-auth-tests.js`
5. **Update documentation**

### Example Test Structure

```javascript
import { 
  createTestClient, 
  TestAssertion, 
  logTestStart,
  logTestComplete
} from '../utils/test-helpers.js'

async function testNewFeature() {
  const testName = 'New Feature Test'
  logTestStart(testName)
  
  const client = createTestClient()
  const assertion = new TestAssertion(testName)
  
  // Your tests here...
  
  const success = assertion.printResults()
  logTestComplete(testName, success)
  return success
}

export default testNewFeature
```

## 📈 Future Enhancements

- [ ] Integration with CI/CD pipelines
- [ ] Database state management
- [ ] Performance benchmarking
- [ ] Load testing capabilities
- [ ] Screenshot/video recording
- [ ] Test data factories
- [ ] Mock service integration