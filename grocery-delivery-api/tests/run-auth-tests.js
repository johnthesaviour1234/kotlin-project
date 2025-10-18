#!/usr/bin/env node

/**
 * Main test runner for authentication tests
 * Runs all auth tests with proper reporting and environment setup
 */

import { spawn } from 'child_process'
import path from 'path'
import { fileURLToPath } from 'url'
import { 
  createTestClient, 
  waitForServer,
  TEST_CONFIG,
  logTestStart,
  logTestComplete
} from './utils/test-helpers.js'

// Import test functions
import testNormalLogin from './auth/login-normal.test.js'
import testDevelopmentBypass from './auth/login-bypass.test.js'
import testLoginErrors from './auth/login-errors.test.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

/**
 * Configuration for test runner
 */
const RUNNER_CONFIG = {
  SERVER_START_TIMEOUT: 60000, // 60 seconds
  SERVER_READY_TIMEOUT: 30000,  // 30 seconds
  COLORS: {
    GREEN: '\x1b[32m',
    RED: '\x1b[31m',
    YELLOW: '\x1b[33m',
    BLUE: '\x1b[34m',
    CYAN: '\x1b[36m',
    RESET: '\x1b[0m'
  }
}

/**
 * Log with color
 */
function colorLog(message, color = 'RESET') {
  console.log(`${RUNNER_CONFIG.COLORS[color]}${message}${RUNNER_CONFIG.COLORS.RESET}`)
}

/**
 * Start Next.js development server
 */
async function startDevServer() {
  return new Promise((resolve, reject) => {
    colorLog('\n🚀 Starting Next.js development server...', 'BLUE')
    
    const serverProcess = spawn('npm', ['run', 'dev'], {
      cwd: path.resolve(__dirname, '..'),
      stdio: ['inherit', 'pipe', 'pipe'],
      shell: true
    })
    
    let output = ''
    let serverReady = false
    
    const timeout = setTimeout(() => {
      if (!serverReady) {
        serverProcess.kill()
        reject(new Error('Server start timeout'))
      }
    }, RUNNER_CONFIG.SERVER_START_TIMEOUT)
    
    serverProcess.stdout.on('data', (data) => {
      output += data.toString()
      process.stdout.write(data)
      
      // Check if server is ready
      if (output.includes('Ready') || output.includes('started server') || output.includes('Local:')) {
        serverReady = true
        clearTimeout(timeout)
        colorLog('✅ Development server started successfully!', 'GREEN')
        resolve(serverProcess)
      }
    })
    
    serverProcess.stderr.on('data', (data) => {
      process.stderr.write(data)
    })
    
    serverProcess.on('error', (error) => {
      clearTimeout(timeout)
      reject(new Error(`Failed to start server: ${error.message}`))
    })
    
    serverProcess.on('exit', (code) => {
      clearTimeout(timeout)
      if (code !== 0 && !serverReady) {
        reject(new Error(`Server exited with code ${code}`))
      }
    })
  })
}

/**
 * Wait for server to be ready for requests
 */
async function waitForServerReady() {
  colorLog('⏳ Waiting for server to be ready for requests...', 'YELLOW')
  
  const client = createTestClient()
  try {
    await waitForServer(client, 30, 1000)
    return true
  } catch (error) {
    colorLog(`❌ Server readiness check failed: ${error.message}`, 'RED')
    return false
  }
}

/**
 * Run all authentication tests
 */
async function runAllTests() {
  const testResults = []
  const tests = [
    { name: 'Normal Login', fn: testNormalLogin },
    { name: 'Development Bypass', fn: testDevelopmentBypass },
    { name: 'Error Handling', fn: testLoginErrors }
  ]
  
  colorLog('\n🧪 Running Authentication Test Suite', 'CYAN')
  colorLog('=' .repeat(60), 'CYAN')
  
  for (const test of tests) {
    try {
      colorLog(`\n▶️  Running ${test.name} tests...`, 'BLUE')
      const startTime = Date.now()
      
      const result = await test.fn()
      const duration = Date.now() - startTime
      
      testResults.push({
        name: test.name,
        passed: result,
        duration,
        error: null
      })
      
      colorLog(`${result ? '✅' : '❌'} ${test.name} ${result ? 'PASSED' : 'FAILED'} (${duration}ms)`, result ? 'GREEN' : 'RED')
      
    } catch (error) {
      const duration = Date.now() - Date.now()
      testResults.push({
        name: test.name,
        passed: false,
        duration,
        error: error.message
      })
      
      colorLog(`❌ ${test.name} FAILED - Error: ${error.message}`, 'RED')
    }
  }
  
  return testResults
}

/**
 * Print final test summary
 */
function printTestSummary(results) {
  colorLog('\n📊 Test Suite Summary', 'CYAN')
  colorLog('=' .repeat(60), 'CYAN')
  
  const totalTests = results.length
  const passedTests = results.filter(r => r.passed).length
  const failedTests = totalTests - passedTests
  const totalDuration = results.reduce((sum, r) => sum + r.duration, 0)
  
  colorLog(`\nTotal Tests: ${totalTests}`, 'BLUE')
  colorLog(`Passed: ${passedTests}`, 'GREEN')
  colorLog(`Failed: ${failedTests}`, failedTests > 0 ? 'RED' : 'GREEN')
  colorLog(`Total Duration: ${totalDuration}ms`, 'BLUE')
  
  if (failedTests > 0) {
    colorLog('\n❌ Failed Tests:', 'RED')
    results
      .filter(r => !r.passed)
      .forEach(r => {
        colorLog(`  • ${r.name}${r.error ? ` - ${r.error}` : ''}`, 'RED')
      })
  }
  
  colorLog(`\n${passedTests === totalTests ? '🎉 All tests passed!' : '💥 Some tests failed!'}`, passedTests === totalTests ? 'GREEN' : 'RED')
  
  return passedTests === totalTests
}

/**
 * Check environment setup
 */
function checkEnvironment() {
  colorLog('\n🔍 Environment Check', 'CYAN')
  colorLog('=' .repeat(30), 'CYAN')
  
  const requiredEnvVars = [
    'SUPABASE_URL',
    'SUPABASE_ANON_KEY',
    'SUPABASE_SERVICE_ROLE_KEY'
  ]
  
  const missingVars = []
  
  requiredEnvVars.forEach(envVar => {
    if (process.env[envVar]) {
      colorLog(`✓ ${envVar}: Set`, 'GREEN')
    } else {
      colorLog(`✗ ${envVar}: Missing`, 'RED')
      missingVars.push(envVar)
    }
  })
  
  // Optional environment variables for development
  const optionalVars = ['NODE_ENV', 'ALLOW_UNVERIFIED_LOGIN']
  optionalVars.forEach(envVar => {
    const value = process.env[envVar] || 'not set'
    colorLog(`• ${envVar}: ${value}`, 'YELLOW')
  })
  
  if (missingVars.length > 0) {
    colorLog(`\n❌ Missing required environment variables: ${missingVars.join(', ')}`, 'RED')
    colorLog('Please set these variables in your .env file', 'RED')
    return false
  }
  
  colorLog('\n✅ Environment check passed!', 'GREEN')
  return true
}

/**
 * Main test runner function
 */
async function main() {
  let serverProcess = null
  
  try {
    // Print header
    colorLog('\n🧪 Authentication Test Suite', 'CYAN')
    colorLog('=' .repeat(60), 'CYAN')
    colorLog('Testing the login.js endpoint with comprehensive scenarios', 'CYAN')
    
    // Check environment
    if (!checkEnvironment()) {
      process.exit(1)
    }
    
    // Start development server
    serverProcess = await startDevServer()
    
    // Wait for server to be ready
    const serverReady = await waitForServerReady()
    if (!serverReady) {
      throw new Error('Server failed to become ready for requests')
    }
    
    // Run all tests
    const results = await runAllTests()
    
    // Print summary
    const allTestsPassed = printTestSummary(results)
    
    // Cleanup
    if (serverProcess) {
      colorLog('\n🛑 Stopping development server...', 'YELLOW')
      serverProcess.kill()
      
      // Wait a bit for graceful shutdown
      await new Promise(resolve => setTimeout(resolve, 2000))
    }
    
    // Exit with appropriate code
    process.exit(allTestsPassed ? 0 : 1)
    
  } catch (error) {
    colorLog(`\n❌ Test runner failed: ${error.message}`, 'RED')
    
    // Cleanup on error
    if (serverProcess) {
      colorLog('🛑 Stopping development server...', 'YELLOW')
      serverProcess.kill()
    }
    
    process.exit(1)
  }
}

// Handle process signals for cleanup
process.on('SIGINT', () => {
  colorLog('\n🛑 Received SIGINT, cleaning up...', 'YELLOW')
  process.exit(1)
})

process.on('SIGTERM', () => {
  colorLog('\n🛑 Received SIGTERM, cleaning up...', 'YELLOW')
  process.exit(1)
})

// Run main function if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
  main().catch(error => {
    colorLog(`\n💥 Unhandled error: ${error.message}`, 'RED')
    process.exit(1)
  })
}