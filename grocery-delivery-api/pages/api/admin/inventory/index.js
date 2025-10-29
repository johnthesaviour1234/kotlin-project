import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'
import eventBroadcaster from '../../../../lib/eventBroadcaster.js'

async function handler(req, res) {
  const { method } = req

  switch (method) {
    case 'GET':
      return await getInventory(req, res)
    case 'PUT':
      return await updateStock(req, res)
    default:
      return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
}

async function getInventory(req, res) {
  try {
    const { low_stock = 'false', threshold = '10' } = req.query

    let query = supabase
      .from('inventory')
      .select(`
        product_id,
        stock,
        updated_at,
        products(id, name, price, image_url, is_active)
      `)

    if (low_stock === 'true') {
      query = query.lte('stock', parseInt(threshold))
    }

    query = query.order('stock', { ascending: true })

    const { data, error } = await query

    if (error) {
      console.error('Error fetching inventory:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch inventory'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      low_stock_count: data?.filter(item => item.stock <= parseInt(threshold)).length || 0
    }))
  } catch (error) {
    console.error('Get inventory error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function updateStock(req, res) {
  try {
    const { product_id, stock, adjustment_type = 'set' } = req.body

    if (!product_id || stock === undefined) {
      return res.status(400).json(formatErrorResponse('Missing required fields: product_id, stock'))
    }

    const stockValue = parseInt(stock)
    if (stockValue < 0) {
      return res.status(400).json(formatErrorResponse('Stock cannot be negative'))
    }

    let updateQuery
    if (adjustment_type === 'add') {
      // Add to existing stock
      const { data: current } = await supabase
        .from('inventory')
        .select('stock')
        .eq('product_id', product_id)
        .single()
      
      updateQuery = supabase
        .from('inventory')
        .update({ stock: (current?.stock || 0) + stockValue, updated_at: new Date().toISOString() })
        .eq('product_id', product_id)
    } else if (adjustment_type === 'subtract') {
      // Subtract from existing stock
      const { data: current } = await supabase
        .from('inventory')
        .select('stock')
        .eq('product_id', product_id)
        .single()
      
      const newStock = Math.max(0, (current?.stock || 0) - stockValue)
      updateQuery = supabase
        .from('inventory')
        .update({ stock: newStock, updated_at: new Date().toISOString() })
        .eq('product_id', product_id)
    } else {
      // Set stock to specific value
      updateQuery = supabase
        .from('inventory')
        .update({ stock: stockValue, updated_at: new Date().toISOString() })
        .eq('product_id', product_id)
    }

    const { data, error } = await updateQuery.select().single()

    if (error) {
      console.error('Error updating inventory:', error)
      return res.status(500).json(formatErrorResponse('Failed to update inventory'))
    }

    // Log activity
    await logAdminActivity(
      req.user.id,
      'inventory_updated',
      'inventory',
      product_id,
      { adjustment_type, stock: stockValue, new_stock: data.stock }
    )

    // ✅ NEW: Broadcast stock update event
    await eventBroadcaster.productStockChanged(product_id, data.stock)

    // ✅ NEW: Check for low stock and send alert
    const LOW_STOCK_THRESHOLD = 10
    if (data.stock <= LOW_STOCK_THRESHOLD) {
      await eventBroadcaster.lowStockAlert(product_id, data.stock, LOW_STOCK_THRESHOLD)
    }

    res.status(200).json(formatSuccessResponse(data, 'Inventory updated successfully'))
  } catch (error) {
    console.error('Update stock error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
