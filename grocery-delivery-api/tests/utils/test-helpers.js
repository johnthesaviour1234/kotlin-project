/**
 * Test utilities and helpers for authentication testing
 */

import HttpClient from './http-client.js'

// Test configuration
export const TEST_CONFIG = {
  BASE_URL: process.env.TEST_BASE_URL || 'http://localhost:3000',
  TIMEOUT: parseInt(process.env.TEST_TIMEOUT) || 30000,
  DEBUG: process.env.TEST_DEBUG === 'true'
}

// Test data
export const TEST_DATA = {
  VALID_USER: {
    email: 'test@example.com',
    password: 'TestPassword123!'
  },
  UNVERIFIED_USER: {
    email: 'unverified@example.com',
    password: 'TestPassword123!'
  },
  INVALID_CREDENTIALS: {
    email: 'invalid@example.com',
    password: 'wrongpassword'
  },
  INVALID_EMAIL: {
    email: 'invalid-email',
    password: 'TestPassword123!'
  },
  MISSING_PASSWORD: {
    email: 'test@example.com'
  },
  EMPTY_FIELDS: {
    email: '',
    password: ''
  }
}

/**
 * Create HTTP client for testing
 */
export function createTestClient() {
  return new HttpClient(TEST_CONFIG.BASE_URL)
}

/**
 * Test assertion helper
 */
export class TestAssertion {
  constructor(name) {
    this.name = name
    this.passed = 0
    this.failed = 0
    this.errors = []
  }

  /**
   * Assert equality
   */
  assertEqual(actual, expected, message = '') {
    const testMessage = message || `Expected ${expected}, got ${actual}`
    if (actual === expected) {
      this.passed++
      if (TEST_CONFIG.DEBUG) {
        console.log(`✓ ${testMessage}`)
      }
    } else {
      this.failed++
      const error = `✗ ${testMessage}`
      this.errors.push(error)
      console.error(error)
    }
  }

  /**
   * Assert truthiness
   */
  assertTrue(actual, message = '') {
    const testMessage = message || `Expected truthy value, got ${actual}`
    if (actual) {
      this.passed++
      if (TEST_CONFIG.DEBUG) {
        console.log(`✓ ${testMessage}`)
      }
    } else {
      this.failed++
      const error = `✗ ${testMessage}`
      this.errors.push(error)
      console.error(error)
    }
  }

  /**
   * Assert object has property
   */
  assertHasProperty(obj, property, message = '') {
    const testMessage = message || `Expected object to have property '${property}'`
    if (obj && typeof obj === 'object' && obj.hasOwnProperty(property)) {
      this.passed++
      if (TEST_CONFIG.DEBUG) {
        console.log(`✓ ${testMessage}`)
      }
    } else {
      this.failed++
      const error = `✗ ${testMessage}`
      this.errors.push(error)
      console.error(error)
    }
  }

  /**
   * Assert HTTP status code
   */
  assertStatus(response, expectedStatus, message = '') {
    const testMessage = message || `Expected status ${expectedStatus}, got ${response.status}`
    this.assertEqual(response.status, expectedStatus, testMessage)
  }

  /**
   * Print test results
   */
  printResults() {
    const total = this.passed + this.failed
    console.log(`\n=== ${this.name} Results ===`)
    console.log(`Tests: ${total}`)
    console.log(`Passed: ${this.passed}`)
    console.log(`Failed: ${this.failed}`)
    
    if (this.failed > 0) {
      console.log(`\nFailures:`)
      this.errors.forEach(error => console.log(`  ${error}`))
    }
    
    return this.failed === 0
  }
}

/**
 * Validate authentication response structure
 */
export function validateAuthResponse(response, assertion) {
  const { data } = response
  
  // Check main response structure
  assertion.assertHasProperty(data, 'success', 'Response should have success property')
  assertion.assertHasProperty(data, 'message', 'Response should have message property')
  assertion.assertHasProperty(data, 'data', 'Response should have data property')
  
  if (data.success) {
    const responseData = data.data
    
    // Check user object
    assertion.assertHasProperty(responseData, 'user', 'Response data should have user object')
    if (responseData.user) {
      assertion.assertHasProperty(responseData.user, 'id', 'User should have id')
      assertion.assertHasProperty(responseData.user, 'email', 'User should have email')
      assertion.assertHasProperty(responseData.user, 'email_confirmed', 'User should have email_confirmed')
      assertion.assertHasProperty(responseData.user, 'profile', 'User should have profile')
    }
    
    // Check tokens object
    assertion.assertHasProperty(responseData, 'tokens', 'Response data should have tokens object')
    if (responseData.tokens) {
      assertion.assertHasProperty(responseData.tokens, 'access_token', 'Tokens should have access_token')
      assertion.assertHasProperty(responseData.tokens, 'refresh_token', 'Tokens should have refresh_token')
      assertion.assertHasProperty(responseData.tokens, 'expires_at', 'Tokens should have expires_at')
      assertion.assertHasProperty(responseData.tokens, 'expires_in', 'Tokens should have expires_in')
    }
    
    // Check session object
    assertion.assertHasProperty(responseData, 'session', 'Response data should have session object')
    if (responseData.session) {
      assertion.assertHasProperty(responseData.session, 'token_type', 'Session should have token_type')
    }
  }
}

/**
 * Wait for server to be ready
 */
export async function waitForServer(client, maxAttempts = 30, delay = 1000) {
  for (let i = 0; i < maxAttempts; i++) {
    try {
      const response = await client.get('/api/health')
      if (response.status === 200) {
        console.log('✓ Server is ready')
        return true
      }
    } catch (error) {
      // Server not ready, continue waiting
    }
    
    console.log(`Waiting for server... (${i + 1}/${maxAttempts})`)
    await new Promise(resolve => setTimeout(resolve, delay))
  }
  
  throw new Error('Server did not become ready within timeout period')
}

/**
 * Log test start
 */
export function logTestStart(testName) {
  console.log(`\n🧪 Starting ${testName}...`)
  console.log('=' .repeat(50))
}

/**
 * Log test completion
 */
export function logTestComplete(testName, success) {
  console.log('=' .repeat(50))
  console.log(`${success ? '✅' : '❌'} ${testName} ${success ? 'PASSED' : 'FAILED'}`)
}