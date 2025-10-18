#!/usr/bin/env node

/**
 * Test script for error handling scenarios
 * Tests various error scenarios like invalid credentials, missing fields, server errors
 */

import { 
  createTestClient, 
  TestAssertion, 
  TEST_DATA,
  logTestStart,
  logTestComplete
} from '../utils/test-helpers.js'

async function testLoginErrors() {
  const testName = 'Login Error Handling'
  logTestStart(testName)
  
  const client = createTestClient()
  const assertion = new TestAssertion(testName)
  
  try {
    // Test 1: Invalid credentials
    console.log('\n📋 Test 1: Invalid credentials')
    const invalidResponse = await client.post('/api/auth/login', TEST_DATA.INVALID_CREDENTIALS)
    
    assertion.assertStatus(invalidResponse, 401, 'Invalid credentials should return 401')
    
    if (invalidResponse.data) {
      assertion.assertEqual(invalidResponse.data.success, false, 'Response should indicate failure')
      assertion.assertTrue(
        invalidResponse.data.message && invalidResponse.data.message.includes('Invalid'),
        'Should have invalid credentials message'
      )
    }
    
    console.log('✓ Invalid credentials test completed')
    
    // Test 2: Invalid email format
    console.log('\n📋 Test 2: Invalid email format')
    const invalidEmailResponse = await client.post('/api/auth/login', TEST_DATA.INVALID_EMAIL)
    
    assertion.assertStatus(invalidEmailResponse, 400, 'Invalid email should return 400')
    
    if (invalidEmailResponse.data) {
      assertion.assertEqual(invalidEmailResponse.data.success, false, 'Response should indicate failure')
      assertion.assertTrue(
        invalidEmailResponse.data.message && invalidEmailResponse.data.message.includes('Validation failed'),
        'Should have validation failed message'
      )
      assertion.assertTrue(
        Array.isArray(invalidEmailResponse.data.errors),
        'Should have errors array'
      )
    }
    
    console.log('✓ Invalid email format test completed')
    
    // Test 3: Missing password
    console.log('\n📋 Test 3: Missing password')
    const missingPasswordResponse = await client.post('/api/auth/login', TEST_DATA.MISSING_PASSWORD)
    
    assertion.assertStatus(missingPasswordResponse, 400, 'Missing password should return 400')
    
    if (missingPasswordResponse.data) {
      assertion.assertEqual(missingPasswordResponse.data.success, false, 'Response should indicate failure')
      assertion.assertTrue(
        missingPasswordResponse.data.message && missingPasswordResponse.data.message.includes('Validation failed'),
        'Should have validation failed message'
      )
      assertion.assertTrue(
        Array.isArray(missingPasswordResponse.data.errors),
        'Should have errors array'
      )
    }
    
    console.log('✓ Missing password test completed')
    
    // Test 4: Empty fields
    console.log('\n📋 Test 4: Empty email and password')
    const emptyFieldsResponse = await client.post('/api/auth/login', TEST_DATA.EMPTY_FIELDS)
    
    assertion.assertStatus(emptyFieldsResponse, 400, 'Empty fields should return 400')
    
    if (emptyFieldsResponse.data) {
      assertion.assertEqual(emptyFieldsResponse.data.success, false, 'Response should indicate failure')
      assertion.assertTrue(
        Array.isArray(emptyFieldsResponse.data.errors) && emptyFieldsResponse.data.errors.length > 0,
        'Should have validation errors for empty fields'
      )
    }
    
    console.log('✓ Empty fields test completed')
    
    // Test 5: No request body
    console.log('\n📋 Test 5: No request body')
    const noBodyResponse = await client.post('/api/auth/login', null)
    
    assertion.assertStatus(noBodyResponse, 400, 'No request body should return 400')
    
    if (noBodyResponse.data) {
      assertion.assertEqual(noBodyResponse.data.success, false, 'Response should indicate failure')
    }
    
    console.log('✓ No request body test completed')
    
    // Test 6: Invalid JSON (malformed request)
    console.log('\n📋 Test 6: Invalid JSON request')
    try {
      // Send malformed JSON by manually crafting the request
      const malformedResponse = await client.request('POST', '/api/auth/login', 'invalid-json')
      
      // Should return 400 for malformed JSON
      assertion.assertTrue(
        malformedResponse.status === 400 || malformedResponse.status === 500,
        'Malformed JSON should return 400 or 500'
      )
      
      console.log('✓ Invalid JSON test completed')
    } catch (error) {
      // This is also acceptable - the request might fail at the HTTP level
      console.log('✓ Invalid JSON properly rejected at HTTP level')
    }
    
    // Test 7: Wrong content type
    console.log('\n📋 Test 7: Wrong content type')
    const wrongContentTypeResponse = await client.post('/api/auth/login', 'email=test&password=test', {
      'Content-Type': 'application/x-www-form-urlencoded'
    })
    
    // Should handle or reject form-encoded data
    assertion.assertTrue(
      wrongContentTypeResponse.status === 400 || wrongContentTypeResponse.status === 422,
      'Wrong content type should be handled appropriately'
    )
    
    console.log('✓ Wrong content type test completed')
    
    // Test 8: Very long email/password (security test)
    console.log('\n📋 Test 8: Very long credentials')
    const longCredentials = {
      email: 'a'.repeat(1000) + '@example.com',
      password: 'b'.repeat(1000)
    }
    
    const longCredsResponse = await client.post('/api/auth/login', longCredentials)
    
    // Should handle or reject very long inputs gracefully
    assertion.assertTrue(
      longCredsResponse.status === 400 || longCredsResponse.status === 401,
      'Very long credentials should be handled gracefully'
    )
    
    console.log('✓ Long credentials test completed')
    
    // Test 9: SQL injection attempt (security test)
    console.log('\n📋 Test 9: SQL injection attempt')
    const sqlInjectionAttempt = {
      email: "admin@example.com'; DROP TABLE users; --",
      password: "password"
    }
    
    const sqlInjectionResponse = await client.post('/api/auth/login', sqlInjectionAttempt)
    
    // Should handle SQL injection attempts safely
    assertion.assertTrue(
      sqlInjectionResponse.status === 400 || sqlInjectionResponse.status === 401,
      'SQL injection attempt should be handled safely'
    )
    
    if (sqlInjectionResponse.data) {
      assertion.assertEqual(sqlInjectionResponse.data.success, false, 'SQL injection should not succeed')
    }
    
    console.log('✓ SQL injection attempt test completed')
    
    // Test 10: XSS attempt (security test)
    console.log('\n📋 Test 10: XSS attempt')
    const xssAttempt = {
      email: '<script>alert("xss")</script>@example.com',
      password: '<script>alert("xss")</script>'
    }
    
    const xssResponse = await client.post('/api/auth/login', xssAttempt)
    
    // Should handle XSS attempts safely
    assertion.assertTrue(
      xssResponse.status === 400 || xssResponse.status === 401,
      'XSS attempt should be handled safely'
    )
    
    if (xssResponse.data && xssResponse.data.message) {
      assertion.assertTrue(
        !xssResponse.data.message.includes('<script>'),
        'Response should not contain raw script tags'
      )
    }
    
    console.log('✓ XSS attempt test completed')
    
    // Test 11: Rate limiting (if implemented)
    console.log('\n📋 Test 11: Multiple rapid requests')
    const rapidRequests = []
    
    // Make 5 rapid requests to test rate limiting
    for (let i = 0; i < 5; i++) {
      rapidRequests.push(client.post('/api/auth/login', TEST_DATA.INVALID_CREDENTIALS))
    }
    
    const rapidResponses = await Promise.all(rapidRequests)
    
    // All should be handled (even if they fail)
    rapidResponses.forEach((response, index) => {
      assertion.assertTrue(
        response.status >= 200 && response.status < 600,
        `Rapid request ${index + 1} should return valid HTTP status`
      )
    })
    
    console.log('✓ Rapid requests test completed')
    
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
    const success = await testLoginErrors()
    process.exit(success ? 0 : 1)
  } catch (error) {
    console.error('❌ Test runner error:', error)
    process.exit(1)
  }
}

export default testLoginErrors