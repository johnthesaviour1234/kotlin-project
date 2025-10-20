#!/usr/bin/env node

import HttpClient from '../utils/http-client.js'
import { TestAssertion, logTestStart, logTestComplete } from '../utils/test-helpers.js'

const BASE_URL = process.env.TEST_BASE_URL || 'https://andoid-app-kotlin.vercel.app'

async function testListProd() {
  const testName = 'Products: List (prod)'
  logTestStart(testName)

  const client = new HttpClient(BASE_URL)
  const assertion = new TestAssertion(testName)

  try {
    const res = await client.get('/api/products/list?featured=true&limit=5')
    assertion.assertStatus(res, 200, 'GET /api/products/list should return 200')
    assertion.assertTrue(res.data?.success === true, 'Response.success should be true')
    const items = res.data?.data?.items || []
    assertion.assertTrue(Array.isArray(items) && items.length >= 1, 'Should return at least 1 product')
    assertion.assertHasProperty(items[0], 'id', 'Product should have id')
    assertion.assertHasProperty(items[0], 'name', 'Product should have name')
    assertion.assertHasProperty(items[0], 'price', 'Product should have price')
  } catch (e) {
    assertion.failed++
    assertion.errors.push(e.message)
  }

  const success = assertion.printResults()
  logTestComplete(testName, success)
  return success
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const ok = await testListProd()
  process.exit(ok ? 0 : 1)
}

export default testListProd
