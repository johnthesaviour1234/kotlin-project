import { withAdminAuth } from '../../../../lib/adminMiddleware.js'
import { supabase } from '../../../../lib/supabase.js'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation.js'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const timeRange = req.query.range || '7d' // 1d, 7d, 30d, 90d

    // Calculate date range
    const now = new Date()
    let startDate = new Date()
    
    switch (timeRange) {
      case '1d':
        startDate.setDate(now.getDate() - 1)
        break
      case '7d':
        startDate.setDate(now.getDate() - 7)
        break
      case '30d':
        startDate.setDate(now.getDate() - 30)
        break
      case '90d':
        startDate.setDate(now.getDate() - 90)
        break
      default:
        startDate.setDate(now.getDate() - 7)
    }

    // Fetch metrics in parallel
    const [
      ordersResult,
      revenueResult,
      customersResult,
      deliveryPersonnelResult,
      pendingOrdersResult,
      lowStockResult
    ] = await Promise.all([
      // Total orders in time range
      supabase
        .from('orders')
        .select('id, total_amount, status, created_at', { count: 'exact' })
        .gte('created_at', startDate.toISOString()),
      
      // Revenue calculation
      supabase
        .from('orders')
        .select('total_amount')
        .eq('payment_status', 'paid')
        .gte('created_at', startDate.toISOString()),
      
      // Active customers
      supabase
        .from('user_profiles')
        .select('id', { count: 'exact' })
        .eq('user_type', 'customer')
        .eq('is_active', true),
      
      // Active delivery personnel
      supabase
        .from('user_profiles')
        .select('id', { count: 'exact' })
        .eq('user_type', 'delivery_driver')
        .eq('is_active', true),
      
      // Pending orders
      supabase
        .from('orders')
        .select('id', { count: 'exact' })
        .eq('status', 'pending'),
      
      // Low stock items
      supabase
        .from('inventory')
        .select('product_id, stock, products!inner(name)', { count: 'exact' })
        .lte('stock', 10)
    ])

    // Process results
    const totalOrders = ordersResult.count || 0
    const totalRevenue = revenueResult.data?.reduce((sum, order) => sum + parseFloat(order.total_amount || 0), 0) || 0
    const activeCustomers = customersResult.count || 0
    const activeDeliveryPersonnel = deliveryPersonnelResult.count || 0
    const pendingOrders = pendingOrdersResult.count || 0
    const lowStockItems = lowStockResult.count || 0

    // Calculate order status breakdown
    const ordersByStatus = {}
    ordersResult.data?.forEach(order => {
      ordersByStatus[order.status] = (ordersByStatus[order.status] || 0) + 1
    })

    // Calculate average order value
    const averageOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0

    // Recent orders for timeline
    const recentOrders = ordersResult.data?.slice(0, 5).map(order => ({
      id: order.id,
      total: order.total_amount,
      status: order.status,
      created_at: order.created_at
    })) || []

    res.status(200).json(formatSuccessResponse({
      summary: {
        total_orders: totalOrders,
        total_revenue: parseFloat(totalRevenue.toFixed(2)),
        average_order_value: parseFloat(averageOrderValue.toFixed(2)),
        active_customers: activeCustomers,
        active_delivery_personnel: activeDeliveryPersonnel,
        pending_orders: pendingOrders,
        low_stock_items: lowStockItems
      },
      orders_by_status: ordersByStatus,
      recent_orders: recentOrders,
      time_range: timeRange
    }))

  } catch (error) {
    console.error('Dashboard metrics error:', error)
    res.status(500).json(formatErrorResponse('Failed to fetch dashboard metrics'))
  }
}

export default withAdminAuth(handler)
