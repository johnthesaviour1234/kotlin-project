import { supabase, handleError, handleSuccess } from '../../../lib/supabase';

export default async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(handleError({ message: 'Method not allowed' }));
  }

  try {
    // Get all active categories with parent-child relationships
    const { data: categories, error } = await supabase
      .from('product_categories')
      .select(`
        id,
        name,
        description,
        parent_category_id,
        is_active,
        created_at
      `)
      .eq('is_active', true)
      .order('name');

    if (error) {
      throw error;
    }

    // Organize categories into hierarchical structure
    const rootCategories = categories.filter(cat => !cat.parent_category_id);
    const subCategories = categories.filter(cat => cat.parent_category_id);

    const hierarchicalCategories = rootCategories.map(root => ({
      ...root,
      subcategories: subCategories.filter(sub => sub.parent_category_id === root.id)
    }));

    res.status(200).json(handleSuccess({
      categories: hierarchicalCategories,
      total_count: categories.length,
      root_categories: rootCategories.length,
      subcategories: subCategories.length
    }, 'Categories retrieved successfully'));

  } catch (error) {
    console.error('Categories API error:', error);
    res.status(500).json(handleError(error));
  }
}