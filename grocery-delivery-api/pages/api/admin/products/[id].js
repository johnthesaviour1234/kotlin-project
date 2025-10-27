import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  const { method } = req
  const { id } = req.query

  switch (method) {
    case 'GET':
      return await getProduct(req, res, id)
    case 'PUT':
      return await updateProduct(req, res, id)
    case 'DELETE':
      return await deleteProduct(req, res, id)
    default:
      return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
}

async function getProduct(req, res, id) {
  try {
    const { data, error } = await supabase
      .from('products')
      .select(`
        *,
        product_categories(id, name),
        inventory(stock),
        product_images(id, image_url, display_order, is_primary, alt_text)
      `)
      .eq('id', id)
      .single()

    if (error || !data) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    res.status(200).json(formatSuccessResponse(data))
  } catch (error) {
    console.error('Get product error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function updateProduct(req, res, id) {
  try {
    const {
      name,
      description,
      price,
      image_url,
      category_id,
      featured,
      is_active
    } = req.body

    // Fetch current product
    const { data: currentProduct, error: fetchError } = await supabase
      .from('products')
      .select('*')
      .eq('id', id)
      .single()

    if (fetchError || !currentProduct) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    // Build update object
    const updateData = {
      updated_at: new Date().toISOString()
    }

    if (name !== undefined) updateData.name = name.trim()
    if (description !== undefined) updateData.description = description?.trim() || null
    if (price !== undefined) {
      if (parseFloat(price) < 0) {
        return res.status(400).json(formatErrorResponse('Price must be non-negative'))
      }
      updateData.price = parseFloat(price)
    }
    if (image_url !== undefined) updateData.image_url = image_url || null
    if (category_id !== undefined) updateData.category_id = category_id
    if (featured !== undefined) updateData.featured = featured
    if (is_active !== undefined) updateData.is_active = is_active

    // Update product
    const { data: updatedProduct, error: updateError } = await supabase
      .from('products')
      .update(updateData)
      .eq('id', id)
      .select()
      .single()

    if (updateError) {
      console.error('Error updating product:', updateError)
      return res.status(500).json(formatErrorResponse('Failed to update product'))
    }

    // Log admin activity
    await logAdminActivity(
      req.user.id,
      'product_updated',
      'product',
      id,
      {
        old_values: currentProduct,
        new_values: updatedProduct
      }
    )

    res.status(200).json(formatSuccessResponse(updatedProduct, 'Product updated successfully'))
  } catch (error) {
    console.error('Update product error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function deleteProduct(req, res, id) {
  try {
    // Fetch product first
    const { data: product, error: fetchError } = await supabase
      .from('products')
      .select('id, name')
      .eq('id', id)
      .single()

    if (fetchError || !product) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    // Delete product (cascade will handle inventory and images)
    const { error: deleteError } = await supabase
      .from('products')
      .delete()
      .eq('id', id)

    if (deleteError) {
      console.error('Error deleting product:', deleteError)
      return res.status(500).json(formatErrorResponse('Failed to delete product'))
    }

    // Log admin activity
    await logAdminActivity(
      req.user.id,
      'product_deleted',
      'product',
      id,
      { product_name: product.name }
    )

    res.status(200).json(formatSuccessResponse(null, 'Product deleted successfully'))
  } catch (error) {
    console.error('Delete product error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
