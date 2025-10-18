#!/usr/bin/env node

/**
 * Test script for normal authentication flow
 * Tests valid credentials and proper response validation
 */

import { 
  createTestClient, 
  TestAssertion, 
  validateAuthResponse, 
  TEST_DATA,
  logTestStart,
  logTestComplete
} from '../utils/test-helpers.js'

async function testNormalLogin() {
  const testName = 'Normal Login Authentication'
  logTestStart(testName)
  
  const client = createTestClient()
  const assertion = new TestAssertion(testName)
  
  try {
    // Test 1: Valid login credentials
    console.log('\n📋 Test 1: Valid login credentials')
    const validResponse = await client.post('/api/auth/login', TEST_DATA.VALID_USER)
    
    // Check status code
    assertion.assertStatus(validResponse, 200, 'Valid login should return 200')
    
    // Validate response structure
    validateAuthResponse(validResponse, assertion)
    
    // Check success response
    if (validResponse.data && validResponse.data.success) {
      assertion.assertTrue(validResponse.data.success, 'Response should indicate success')
      assertion.assertEqual(validResponse.data.message, 'Login successful', 'Should have success message')
      
      // Check user data
      const userData = validResponse.data.data.user
      assertion.assertEqual(userData.email, TEST_DATA.VALID_USER.email, 'Email should match')
      assertion.assertTrue(typeof userData.id === 'string' && userData.id.length > 0, 'User should have valid ID')
      
      // Check tokens
      const tokens = validResponse.data.data.tokens
      assertion.assertTrue(tokens.access_token && tokens.access_token.length > 0, 'Should have access token')
      assertion.assertTrue(tokens.refresh_token && tokens.refresh_token.length > 0, 'Should have refresh token')
      assertion.assertTrue(typeof tokens.expires_in === 'number' && tokens.expires_in > 0, 'Should have valid expires_in')
      
      console.log('✓ Valid login test completed successfully')
    } else {
      console.error('❌ Valid login failed:', validResponse.data)
    }
    
    // Test 2: Check response time
    console.log('\\n📋 Test 2: Response time check')
    const startTime = Date.now()
    await client.post('/api/auth/login', TEST_DATA.VALID_USER)
    const responseTime = Date.now() - startTime
    
    assertion.assertTrue(responseTime < 5000, `Response time should be under 5s, got ${responseTime}ms`)
    console.log(`✓ Response time: ${responseTime}ms`)
    
    // Test 3: Method validation
    console.log('\\n📋 Test 3: HTTP method validation')
    const getResponse = await client.get('/api/auth/login')
    assertion.assertStatus(getResponse, 405, 'GET request should return 405 Method Not Allowed')
    
    if (getResponse.data) {
      assertion.assertEqual(getResponse.data.success, false, 'GET response should indicate failure')
      assertion.assertTrue(
        getResponse.data.message && getResponse.data.message.includes('Method not allowed'),
        'Should have method not allowed message'
      )
    }
    
    console.log('✓ Method validation test completed')
    
  } catch (error) {
    console.error('❌ Test execution error:', error.message)
    assertion.failed++
    assertion.errors.push(`Test execution error: ${error.message}`)
  }
  
  // Print results
  const success = assertion.printResults()
  logTestComplete(testName, success)
  
  return success
}

// Run test if called directly
if (import.meta.url === `file://${process.argv[1]}`) {
  try {
    const success = await testNormalLogin()
    process.exit(success ? 0 : 1)
  } catch (error) {
    console.error('❌ Test runner error:', error)
    process.exit(1)
  }
}

export default testNormalLogin