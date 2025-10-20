#!/usr/bin/env node

/**
 * Quick test for the mobile app user authentication
 */

import HttpClient from './tests/utils/http-client.js'

const TEST_BASE_URL = 'https://andoid-app-kotlin.vercel.app'
const client = new HttpClient(TEST_BASE_URL)

async function testMobileUserLogin() {
  console.log('🧪 Testing mobile user login...')
  console.log('Base URL:', TEST_BASE_URL)
  
  try {
    // Test the newly created and confirmed user
    const response = await client.post('/api/auth/login', {
      email: 'testuser.1760826150808@grocery.com',
      password: 'TestPassword123!'
    })
    
    console.log('\n📊 Response Status:', response.status)
    console.log('📊 Response Data:', JSON.stringify(response.data, null, 2))
    
    if (response.status === 200 && response.data.success) {
      console.log('\n✅ SUCCESS: Mobile user login works!')
      console.log('✅ User ID:', response.data.data.user.id)
      console.log('✅ Email:', response.data.data.user.email)
      console.log('✅ Profile:', response.data.data.user.profile ? 'Found' : 'Missing')
      console.log('✅ Access Token:', response.data.data.tokens.access_token ? 'Present' : 'Missing')
      console.log('\n🎉 AUTHENTICATION FIX SUCCESSFUL!')
    } else {
      console.log('\n❌ FAILED: Login still not working')
      console.log('❌ Status:', response.status)
      console.log('❌ Error:', response.data.error)
    }
    
  } catch (error) {
    console.error('\n❌ ERROR:', error.message)
    if (error.response) {
      console.error('❌ Response Status:', error.response.status)
      console.error('❌ Response Data:', JSON.stringify(error.response.data, null, 2))
    }
  }
}

testMobileUserLogin()