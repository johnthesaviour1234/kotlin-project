import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  const { method } = req

  switch (method) {
    case 'GET':
      return await listProducts(req, res)
    case 'POST':
      return await createProduct(req, res)
    default:
      return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
}

async function listProducts(req, res) {
  try {
    const {
      page = '1',
      limit = '20',
      search = '',
      category = '',
      is_active = '',
      featured = '',
      sort_by = 'created_at',
      sort_order = 'desc'
    } = req.query

    const pageNum = parseInt(page)
    const limitNum = Math.min(parseInt(limit), 100)
    const from = (pageNum - 1) * limitNum
    const to = from + limitNum - 1

    let query = supabase
      .from('products')
      .select(`
        id,
        name,
        description,
        price,
        image_url,
        category_id,
        featured,
        is_active,
        created_at,
        updated_at,
        product_categories(id, name),
        inventory(stock)
      `, { count: 'exact' })

    // Apply filters
    if (search) {
      query = query.or(`name.ilike.%${search}%,description.ilike.%${search}%`)
    }
    if (category) {
      query = query.eq('category_id', category)
    }
    if (is_active !== '') {
      query = query.eq('is_active', is_active === 'true')
    }
    if (featured !== '') {
      query = query.eq('featured', featured === 'true')
    }

    // Apply sorting
    query = query.order(sort_by, { ascending: sort_order === 'asc' })
    query = query.range(from, to)

    const { data, error, count } = await query

    if (error) {
      console.error('Error listing products:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch products'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      pagination: {
        page: pageNum,
        limit: limitNum,
        total: count || 0,
        total_pages: Math.ceil((count || 0) / limitNum),
        has_more: to + 1 < (count || 0)
      }
    }))
  } catch (error) {
    console.error('List products error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function createProduct(req, res) {
  try {
    const {
      name,
      description,
      price,
      image_url,
      category_id,
      featured = false,
      is_active = true,
      initial_stock = 0
    } = req.body

    // Validation
    if (!name || !price || !category_id) {
      return res.status(400).json(formatErrorResponse('Missing required fields: name, price, category_id'))
    }

    if (parseFloat(price) < 0) {
      return res.status(400).json(formatErrorResponse('Price must be non-negative'))
    }

    // Create product
    const { data: product, error: productError } = await supabase
      .from('products')
      .insert({
        name: name.trim(),
        description: description?.trim() || null,
        price: parseFloat(price),
        image_url: image_url || null,
        category_id,
        featured,
        is_active
      })
      .select()
      .single()

    if (productError) {
      console.error('Error creating product:', productError)
      return res.status(500).json(formatErrorResponse('Failed to create product'))
    }

    // Create inventory record
    const { error: inventoryError } = await supabase
      .from('inventory')
      .insert({
        product_id: product.id,
        stock: parseInt(initial_stock) || 0
      })

    if (inventoryError) {
      console.error('Error creating inventory:', inventoryError)
      // Rollback product creation
      await supabase.from('products').delete().eq('id', product.id)
      return res.status(500).json(formatErrorResponse('Failed to create inventory record'))
    }

    // Log admin activity
    await logAdminActivity(
      req.user.id,
      'product_created',
      'product',
      product.id,
      { product_name: product.name }
    )

    res.status(201).json(formatSuccessResponse(product, 'Product created successfully'))
  } catch (error) {
    console.error('Create product error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
