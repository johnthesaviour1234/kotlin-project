#!/usr/bin/env node

import HttpClient from '../utils/http-client.js'
import { TestAssertion, logTestStart, logTestComplete } from '../utils/test-helpers.js'

const BASE_URL = process.env.TEST_BASE_URL || 'https://andoid-app-kotlin.vercel.app'
const TEST_EMAIL = process.env.TEST_USER_EMAIL
const TEST_PASSWORD = process.env.TEST_USER_PASSWORD

async function testUserProfileProd() {
  const testName = 'Users: Profile (prod)'
  logTestStart(testName)

  const client = new HttpClient(BASE_URL)
  const assertion = new TestAssertion(testName)

  try {
    if (!TEST_EMAIL || !TEST_PASSWORD) {
      console.log('Skipping profile test: TEST_USER_EMAIL and TEST_USER_PASSWORD env vars not set')
      return true
    }

    // Login to obtain access token
    const loginRes = await client.post('/api/auth/login', {
      email: TEST_EMAIL,
      password: TEST_PASSWORD,
    })
    assertion.assertStatus(loginRes, 200, 'POST /api/auth/login should return 200')
    const accessToken = loginRes.data?.data?.tokens?.access_token
    const userId = loginRes.data?.data?.user?.id
    assertion.assertTrue(!!accessToken, 'Login should return access_token')

    const authHeader = { Authorization: `Bearer ${accessToken}` }

    // GET profile
    const getRes = await client.get('/api/users/profile', authHeader)
    assertion.assertStatus(getRes, 200, 'GET /api/users/profile should return 200')
    assertion.assertTrue(getRes.data?.success === true, 'GET profile response.success should be true')
    assertion.assertTrue(getRes.data?.data?.id === userId, 'Profile id should match logged-in user id')

    // PUT profile update
    const newName = `Prod Test User ${Date.now()}`
    const putRes = await client.put('/api/users/profile', { full_name: newName }, authHeader)
    assertion.assertStatus(putRes, 200, 'PUT /api/users/profile should return 200')
    assertion.assertTrue(putRes.data?.success === true, 'PUT profile response.success should be true')
    assertion.assertTrue(putRes.data?.data?.full_name === newName, 'Updated full_name should be returned')
  } catch (e) {
    assertion.failed++
    assertion.errors.push(e.message)
  }

  const success = assertion.printResults()
  logTestComplete(testName, success)
  return success
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const ok = await testUserProfileProd()
  process.exit(ok ? 0 : 1)
}

export default testUserProfileProd
