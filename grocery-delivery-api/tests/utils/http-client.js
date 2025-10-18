/**
 * HTTP Client utilities for API testing
 */

import https from 'https'
import http from 'http'

class HttpClient {
  constructor(baseUrl = 'http://localhost:3000') {
    this.baseUrl = baseUrl
    this.defaultHeaders = {
      'Content-Type': 'application/json',
      'Accept': 'application/json'
    }
  }

  /**
   * Make HTTP request
   * @param {string} method - HTTP method
   * @param {string} path - API path
   * @param {Object} data - Request body data
   * @param {Object} headers - Additional headers
   * @returns {Promise<Object>} Response object with status, headers, and data
   */
  async request(method, path, data = null, headers = {}) {
    return new Promise((resolve, reject) => {
      const url = new URL(path, this.baseUrl)
      const isHttps = url.protocol === 'https:'
      const httpModule = isHttps ? https : http
      
      const requestHeaders = {
        ...this.defaultHeaders,
        ...headers
      }

      const postData = data ? JSON.stringify(data) : null
      if (postData) {
        requestHeaders['Content-Length'] = Buffer.byteLength(postData)
      }

      const options = {
        hostname: url.hostname,
        port: url.port || (isHttps ? 443 : 80),
        path: url.pathname + url.search,
        method: method.toUpperCase(),
        headers: requestHeaders
      }

      const req = httpModule.request(options, (res) => {
        let responseData = ''
        
        res.on('data', (chunk) => {
          responseData += chunk
        })
        
        res.on('end', () => {
          try {
            const parsedData = responseData ? JSON.parse(responseData) : null
            resolve({
              status: res.statusCode,
              statusText: res.statusMessage,
              headers: res.headers,
              data: parsedData,
              raw: responseData
            })
          } catch (parseError) {
            resolve({
              status: res.statusCode,
              statusText: res.statusMessage,
              headers: res.headers,
              data: responseData,
              raw: responseData,
              parseError: parseError.message
            })
          }
        })
      })

      req.on('error', (error) => {
        reject(new Error(`Request failed: ${error.message}`))
      })

      // Set timeout
      req.setTimeout(30000, () => {
        req.destroy()
        reject(new Error('Request timeout'))
      })

      if (postData) {
        req.write(postData)
      }
      
      req.end()
    })
  }

  /**
   * GET request
   */
  async get(path, headers = {}) {
    return this.request('GET', path, null, headers)
  }

  /**
   * POST request
   */
  async post(path, data, headers = {}) {
    return this.request('POST', path, data, headers)
  }

  /**
   * PUT request
   */
  async put(path, data, headers = {}) {
    return this.request('PUT', path, data, headers)
  }

  /**
   * DELETE request
   */
  async delete(path, headers = {}) {
    return this.request('DELETE', path, null, headers)
  }
}

export default HttpClient