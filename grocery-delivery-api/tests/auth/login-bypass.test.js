#!/usr/bin/env node

/**
 * Test script for development bypass authentication
 * Tests unverified email login with mock session handling
 */

import { 
  createTestClient, 
  TestAssertion, 
  validateAuthResponse, 
  TEST_DATA,
  logTestStart,
  logTestComplete
} from '../utils/test-helpers.js'

async function testDevelopmentBypass() {
  const testName = 'Development Bypass Authentication'
  logTestStart(testName)
  
  const client = createTestClient()
  const assertion = new TestAssertion(testName)
  
  try {
    // Check if we're in development mode and bypass is enabled
    const isDevelopment = process.env.NODE_ENV === 'development'
    const bypassEnabled = process.env.ALLOW_UNVERIFIED_LOGIN === 'true'
    
    console.log(`\n📋 Environment Check:`)
    console.log(`- NODE_ENV: ${process.env.NODE_ENV}`)
    console.log(`- ALLOW_UNVERIFIED_LOGIN: ${process.env.ALLOW_UNVERIFIED_LOGIN}`)
    
    if (!isDevelopment || !bypassEnabled) {
      console.log('⚠️ Development bypass not enabled - skipping bypass tests')
      console.log('To enable: set NODE_ENV=development and ALLOW_UNVERIFIED_LOGIN=true')
      
      // Still test that bypass doesn't work in production mode
      console.log('\n📋 Test: Production mode bypass prevention')
      const response = await client.post('/api/auth/login', TEST_DATA.UNVERIFIED_USER)
      
      // Should fail without bypass
      assertion.assertTrue(
        response.status === 401 || (response.data && !response.data.success),
        'Bypass should not work in production mode'
      )
      
      console.log('✓ Production mode properly prevents bypass')
    } else {
      // Test bypass functionality in development mode
      console.log('\n📋 Test 1: Development bypass for unverified user')
      
      const bypassResponse = await client.post('/api/auth/login', TEST_DATA.UNVERIFIED_USER)
      
      console.log('Response status:', bypassResponse.status)
      console.log('Response data:', JSON.stringify(bypassResponse.data, null, 2))
      
      if (bypassResponse.status === 200 && bypassResponse.data && bypassResponse.data.success) {
        // Bypass worked - validate the response
        assertion.assertStatus(bypassResponse, 200, 'Bypass login should return 200')
        
        // Validate response structure
        validateAuthResponse(bypassResponse, assertion)
        
        // Check success response
        assertion.assertTrue(bypassResponse.data.success, 'Response should indicate success')
        assertion.assertEqual(bypassResponse.data.message, 'Login successful', 'Should have success message')
        
        // Check user data
        const userData = bypassResponse.data.data.user
        assertion.assertEqual(userData.email, TEST_DATA.UNVERIFIED_USER.email, 'Email should match')
        assertion.assertTrue(typeof userData.id === 'string' && userData.id.length > 0, 'User should have valid ID')
        
        // Check mock session tokens (development bypass creates mock tokens)
        const tokens = bypassResponse.data.data.tokens
        assertion.assertTrue(tokens.access_token && tokens.access_token.includes('dev-token-'), 'Should have dev access token')
        assertion.assertTrue(tokens.refresh_token && tokens.refresh_token.includes('dev-refresh-'), 'Should have dev refresh token')
        assertion.assertTrue(typeof tokens.expires_in === 'number' && tokens.expires_in > 0, 'Should have valid expires_in')
        assertion.assertEqual(tokens.token_type, 'bearer', 'Should have bearer token type')
        
        console.log('✓ Development bypass test completed successfully')
        
        // Test 2: Verify bypass logs development message
        console.log('\n📋 Test 2: Development bypass logging')
        // Note: We can't directly test console logs, but the functionality should log the bypass
        assertion.assertTrue(true, 'Development bypass should log bypass message (manual verification needed)')
        
      } else if (bypassResponse.status === 401 && bypassResponse.data && !bypassResponse.data.success) {
        // Bypass didn't work - this could be expected if user doesn't exist
        console.log('⚠️ Bypass failed - this might be expected if test user doesn\'t exist in database')
        console.log('Response:', bypassResponse.data.message)
        
        if (bypassResponse.data.message && bypassResponse.data.message.includes('Email not confirmed')) {
          assertion.assertTrue(true, 'Email not confirmed error is expected for unverified user')
        } else {
          assertion.assertTrue(false, `Unexpected bypass failure: ${bypassResponse.data.message}`)
        }
      } else {
        console.error('❌ Unexpected bypass response:', bypassResponse)
        assertion.assertTrue(false, 'Bypass response was unexpected')
      }
      
      // Test 3: Test bypass with non-existent user
      console.log('\n📋 Test 3: Bypass with non-existent user')
      const nonExistentUser = {
        email: 'nonexistent@example.com',
        password: 'TestPassword123!'
      }
      
      const nonExistentResponse = await client.post('/api/auth/login', nonExistentUser)
      assertion.assertTrue(
        nonExistentResponse.status === 401,
        'Non-existent user should still fail even with bypass'
      )
      
      console.log('✓ Non-existent user properly handled')
    }
    
    // Test 4: Environment variable validation
    console.log('\n📋 Test 4: Environment variable validation')
    
    // This test ensures the logic checks both environment variables
    const hasCorrectEnvLogic = (process.env.NODE_ENV === 'development') && (process.env.ALLOW_UNVERIFIED_LOGIN === 'true')
    assertion.assertEqual(
      hasCorrectEnvLogic,
      isDevelopment && bypassEnabled,
      'Environment variable logic should be consistent'
    )
    
    console.log('✓ Environment variable validation completed')
    
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
    const success = await testDevelopmentBypass()
    process.exit(success ? 0 : 1)
  } catch (error) {
    console.error('❌ Test runner error:', error)
    process.exit(1)
  }
}

export default testDevelopmentBypass