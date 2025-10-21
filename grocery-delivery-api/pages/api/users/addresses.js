import { supabase, supabaseClient, getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization']
  if (!h || typeof h !== 'string') return null
  if (!h.toLowerCase().startsWith('bearer ')) return null
  return h.slice(7).trim()
}

function validateAddress(body) {
  const errors = []
  if (!body.name || typeof body.name !== 'string' || body.name.trim().length < 2) {
    errors.push({ field: 'name', message: 'Name must be at least 2 characters long' })
  }
  if (!body.street_address || typeof body.street_address !== 'string' || body.street_address.trim().length < 5) {
    errors.push({ field: 'street_address', message: 'Street address must be at least 5 characters long' })
  }
  if (!body.city || typeof body.city !== 'string' || body.city.trim().length < 2) {
    errors.push({ field: 'city', message: 'City must be at least 2 characters long' })
  }
  if (!body.state || typeof body.state !== 'string' || body.state.trim().length < 2) {
    errors.push({ field: 'state', message: 'State must be at least 2 characters long' })
  }
  if (!body.postal_code || typeof body.postal_code !== 'string' || body.postal_code.trim().length < 3) {
    errors.push({ field: 'postal_code', message: 'Postal code must be at least 3 characters long' })
  }
  return errors
}

export default async function handler(req, res) {
  const token = getBearer(req)
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'))
  }

  try {
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token)
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'))
    }
    const userId = userData.user.id
    const client = getAuthenticatedClient(token)

    // GET - Fetch all addresses for user
    if (req.method === 'GET') {
      const { data, error } = await client
        .from('user_addresses')
        .select('*')
        .eq('user_id', userId)
        .order('is_default', { ascending: false })
        .order('created_at', { ascending: true })

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      return res.status(200).json(formatSuccessResponse({ items: data || [] }))
    }

    // POST - Create new address
    if (req.method === 'POST') {
      const body = req.body || {}
      const validationErrors = validateAddress(body)
      if (validationErrors.length > 0) {
        return res.status(400).json(formatErrorResponse('Validation failed', validationErrors))
      }

      const addressData = {
        user_id: userId,
        name: body.name.trim().slice(0, 100),
        street_address: body.street_address.trim(),
        apartment: body.apartment ? body.apartment.trim().slice(0, 100) : null,
        city: body.city.trim().slice(0, 100),
        state: body.state.trim().slice(0, 100),
        postal_code: body.postal_code.trim().slice(0, 20),
        landmark: body.landmark ? body.landmark.trim().slice(0, 200) : null,
        address_type: ['home', 'work', 'other'].includes(body.address_type) ? body.address_type : 'home',
        is_default: Boolean(body.is_default)
      }

      // If this is set as default, unset other defaults first
      if (addressData.is_default) {
        await client
          .from('user_addresses')
          .update({ is_default: false })
          .eq('user_id', userId)
      }

      const { data, error } = await client
        .from('user_addresses')
        .insert(addressData)
        .select('*')
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      return res.status(201).json(formatSuccessResponse(data))
    }

    // PUT - Update address (requires address ID in query)
    if (req.method === 'PUT') {
      const addressId = req.query.id
      if (!addressId) {
        return res.status(400).json(formatErrorResponse('Address ID is required in query parameters'))
      }

      const body = req.body || {}
      const validationErrors = validateAddress(body)
      if (validationErrors.length > 0) {
        return res.status(400).json(formatErrorResponse('Validation failed', validationErrors))
      }

      const updateData = {
        name: body.name.trim().slice(0, 100),
        street_address: body.street_address.trim(),
        apartment: body.apartment ? body.apartment.trim().slice(0, 100) : null,
        city: body.city.trim().slice(0, 100),
        state: body.state.trim().slice(0, 100),
        postal_code: body.postal_code.trim().slice(0, 20),
        landmark: body.landmark ? body.landmark.trim().slice(0, 200) : null,
        address_type: ['home', 'work', 'other'].includes(body.address_type) ? body.address_type : 'home',
        is_default: Boolean(body.is_default),
        updated_at: new Date().toISOString()
      }

      // If this is set as default, unset other defaults first
      if (updateData.is_default) {
        await client
          .from('user_addresses')
          .update({ is_default: false })
          .eq('user_id', userId)
      }

      const { data, error } = await client
        .from('user_addresses')
        .update(updateData)
        .eq('id', addressId)
        .eq('user_id', userId)
        .select('*')
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      if (!data) {
        return res.status(404).json(formatErrorResponse('Address not found'))
      }
      return res.status(200).json(formatSuccessResponse(data))
    }

    // DELETE - Delete address
    if (req.method === 'DELETE') {
      const addressId = req.query.id
      if (!addressId) {
        return res.status(400).json(formatErrorResponse('Address ID is required in query parameters'))
      }

      const { data, error } = await client
        .from('user_addresses')
        .delete()
        .eq('id', addressId)
        .eq('user_id', userId)
        .select('*')
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Database error', [{ field: 'database', message: error.message }]))
      }
      if (!data) {
        return res.status(404).json(formatErrorResponse('Address not found'))
      }
      return res.status(200).json(formatSuccessResponse({ message: 'Address deleted successfully', deletedAddress: data }))
    }

    return res.status(405).json(formatErrorResponse('Method not allowed'))
  } catch (e) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}