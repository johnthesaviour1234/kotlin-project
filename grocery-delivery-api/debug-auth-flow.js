#!/usr/bin/env node

/**
 * Debug authentication flow to identify the issue
 */

import HttpClient from './tests/utils/http-client.js'

const TEST_BASE_URL = 'https://kotlin-project.vercel.app'
const client = new HttpClient(TEST_BASE_URL)

async function debugAuthFlow() {
  console.log('🔍 Debugging authentication flow...')
  console.log('Base URL:', TEST_BASE_URL)
  
  try {
    // Test 1: Check if user exists and is verified
    console.log('\n1️⃣ Testing with existing verified user...')
    const response = await client.post('/api/auth/login', {
      email: 'testuser.1760826150808@grocery.com',
      password: 'TestPassword123!'
    })
    
    console.log('Status:', response.status)
    console.log('Success:', response.data?.success)
    console.log('Error:', response.data?.error)
    
    if (response.data?.data?.user) {
      console.log('User ID:', response.data.data.user.id)
      console.log('Email confirmed:', response.data.data.user.email_confirmed)
    }
    
    // Test 2: Try with a different approach - check if it's the development bypass
    console.log('\n2️⃣ Testing with a non-existent user to see error handling...')
    const response2 = await client.post('/api/auth/login', {
      email: 'nonexistent@test.com',
      password: 'TestPassword123!'
    })
    
    console.log('Non-existent user status:', response2.status)
    console.log('Non-existent user error:', response2.data?.error)
    
  } catch (error) {
    console.error('\n❌ ERROR:', error.message)
    if (error.response) {
      console.error('Status:', error.response.status)
      console.error('Data:', JSON.stringify(error.response.data, null, 2))
    }
  }
}

debugAuthFlow()