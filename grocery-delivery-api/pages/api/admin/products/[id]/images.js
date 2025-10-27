import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation.js'

async function handler(req, res) {
  const { method } = req
  const { id: productId } = req.query

  switch (method) {
    case 'GET':
      return await getProductImages(req, res, productId)
    case 'POST':
      return await addProductImage(req, res, productId)
    default:
      return res.status(405).json(formatErrorResponse('Method not allowed'))
  }
}

async function getProductImages(req, res, productId) {
  try {
    // Fetch all images for this product
    const { data, error } = await supabase
      .from('product_images')
      .select('*')
      .eq('product_id', productId)
      .order('display_order', { ascending: true })

    if (error) {
      console.error('Error fetching product images:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch product images'))
    }

    res.status(200).json(formatSuccessResponse({
      product_id: productId,
      images: data || [],
      count: data?.length || 0
    }))
  } catch (error) {
    console.error('Get product images error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

async function addProductImage(req, res, productId) {
  try {
    // Verify product exists
    const { data: product, error: productError } = await supabase
      .from('products')
      .select('id, name')
      .eq('id', productId)
      .single()

    if (productError || !product) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    const { image_url, is_primary = false, alt_text } = req.body

    if (!image_url) {
      return res.status(400).json(formatErrorResponse('image_url is required'))
    }

    // Get current max display order
    const { data: existingImages } = await supabase
      .from('product_images')
      .select('display_order')
      .eq('product_id', productId)
      .order('display_order', { ascending: false })
      .limit(1)

    const displayOrder = (existingImages?.[0]?.display_order || 0) + 1

    // If setting as primary, unset other primary images
    if (is_primary) {
      await supabase
        .from('product_images')
        .update({ is_primary: false })
        .eq('product_id', productId)
    }

    // Check if this is the first image
    const isFirstImage = !existingImages || existingImages.length === 0

    // Insert new image
    const { data: imageRecord, error: dbError } = await supabase
      .from('product_images')
      .insert({
        product_id: productId,
        image_url,
        display_order: displayOrder,
        is_primary: is_primary || isFirstImage,
        alt_text: alt_text || `${product.name} - Image ${displayOrder}`
      })
      .select()
      .single()

    if (dbError) {
      console.error('Database insert error:', dbError)
      return res.status(500).json(formatErrorResponse('Failed to add image'))
    }

    // Log admin activity
    await logAdminActivity(
      req.user.id,
      'product_image_added',
      'product',
      productId,
      { 
        product_name: product.name,
        image_id: imageRecord.id
      }
    )

    res.status(201).json(formatSuccessResponse(imageRecord, 'Image added successfully'))
  } catch (error) {
    console.error('Add product image error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
