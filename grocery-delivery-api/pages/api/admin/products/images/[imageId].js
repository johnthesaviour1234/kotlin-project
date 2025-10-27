import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'DELETE') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { imageId } = req.query

    // Fetch image record
    const { data: imageRecord, error: fetchError } = await supabase
      .from('product_images')
      .select('id, product_id, image_url, is_primary')
      .eq('id', imageId)
      .single()

    if (fetchError || !imageRecord) {
      return res.status(404).json(formatErrorResponse('Image not found'))
    }

    // Delete from database
    const { error: dbError } = await supabase
      .from('product_images')
      .delete()
      .eq('id', imageId)

    if (dbError) {
      console.error('Database deletion error:', dbError)
      return res.status(500).json(formatErrorResponse('Failed to delete image'))
    }

    // If this was the primary image, set another image as primary
    if (imageRecord.is_primary) {
      const { data: nextImage } = await supabase
        .from('product_images')
        .select('id')
        .eq('product_id', imageRecord.product_id)
        .order('display_order', { ascending: true })
        .limit(1)
        .single()

      if (nextImage) {
        await supabase
          .from('product_images')
          .update({ is_primary: true })
          .eq('id', nextImage.id)
      }
    }

    // Log admin activity
    await logAdminActivity(
      req.user.id,
      'product_image_deleted',
      'product_image',
      imageId,
      { product_id: imageRecord.product_id }
    )

    res.status(200).json(formatSuccessResponse(null, 'Image deleted successfully'))
  } catch (error) {
    console.error('Delete image error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
