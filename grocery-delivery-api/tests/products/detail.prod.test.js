#!/usr/bin/env node

import HttpClient from '../utils/http-client.js'
import { TestAssertion, logTestStart, logTestComplete } from '../utils/test-helpers.js'

const BASE_URL = process.env.TEST_BASE_URL || 'https://kotlin-project.vercel.app'

async function testProductDetailProd() {
  const testName = 'Products: Detail (prod)'
  logTestStart(testName)

  const client = new HttpClient(BASE_URL)
  const assertion = new TestAssertion(testName)

  try {
    // Fetch a single product from list
    const listRes = await client.get('/api/products/list?limit=1')
    assertion.assertStatus(listRes, 200, 'GET /api/products/list should return 200')
    const listItems = listRes.data?.data?.items || []
    assertion.assertTrue(Array.isArray(listItems) && listItems.length >= 1, 'Should return at least 1 product in list')

    const productId = listItems[0]?.id
    assertion.assertTrue(!!productId, 'First product should have an id')

    // Fetch product detail
    const detailRes = await client.get(`/api/products/${productId}`)
    assertion.assertStatus(detailRes, 200, 'GET /api/products/[id] should return 200')
    assertion.assertTrue(detailRes.data?.success === true, 'Response.success should be true')

    const product = detailRes.data?.data || {}
    assertion.assertHasProperty(product, 'id', 'Product should have id')
    assertion.assertHasProperty(product, 'name', 'Product should have name')
    assertion.assertHasProperty(product, 'price', 'Product should have price')
  } catch (e) {
    assertion.failed++
    assertion.errors.push(e.message)
  }

  const success = assertion.printResults()
  logTestComplete(testName, success)
  return success
}

if (import.meta.url === `file://${process.argv[1]}`) {
  const ok = await testProductDetailProd()
  process.exit(ok ? 0 : 1)
}

export default testProductDetailProd
