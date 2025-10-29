import { supabase } from '../../../lib/supabase';
import { authMiddleware } from '../../../lib/authMiddleware';
import eventBroadcaster from '../../../lib/eventBroadcaster.js';

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json({ message: 'Method not allowed' });
  }

  try {
    // Authenticate user
    const user = await authMiddleware(req);
    if (!user) {
      return res.status(401).json({ message: 'Unauthorized' });
    }

    const {
      items,
      subtotal,
      taxAmount = 0,
      deliveryFee = 0,
      totalAmount,
      deliveryAddress,
      paymentMethod = 'cash',
      notes = '',
      estimatedDeliveryTime
    } = req.body;

    // Validate required fields
    if (!items || !Array.isArray(items) || items.length === 0) {
      return res.status(400).json({ message: 'Order items are required' });
    }

    if (!subtotal || !totalAmount || !deliveryAddress) {
      return res.status(400).json({ message: 'Missing required order details' });
    }

    // Get customer info
    const { data: customerProfile, error: profileError } = await supabase
      .from('user_profiles')
      .select('*')
      .eq('id', user.id)
      .single();

    if (profileError) {
      console.error('Error fetching customer profile:', profileError);
      return res.status(500).json({ message: 'Error fetching customer profile' });
    }

    const customerInfo = {
      name: customerProfile.full_name || 'Customer',
      email: customerProfile.email,
      phone: customerProfile.phone || ''
    };

    // Create the order
    const { data: order, error: orderError } = await supabase
      .from('orders')
      .insert([{
        customer_id: user.id,
        status: 'pending',
        subtotal: parseFloat(subtotal),
        tax_amount: parseFloat(taxAmount),
        delivery_fee: parseFloat(deliveryFee),
        total_amount: parseFloat(totalAmount),
        customer_info: customerInfo,
        delivery_address: deliveryAddress,
        payment_method: paymentMethod,
        payment_status: 'pending',
        notes: notes,
        estimated_delivery_time: estimatedDeliveryTime || null
      }])
      .select()
      .single();

    if (orderError) {
      console.error('Error creating order:', orderError);
      return res.status(500).json({ message: 'Error creating order' });
    }

    // Create order items
    const orderItems = items.map(item => ({
      order_id: order.id,
      product_id: item.productId,
      quantity: parseInt(item.quantity),
      unit_price: parseFloat(item.unitPrice),
      total_price: parseFloat(item.totalPrice),
      product_name: item.productName,
      product_image_url: item.productImageUrl || null
    }));

    const { error: itemsError } = await supabase
      .from('order_items')
      .insert(orderItems);

    if (itemsError) {
      console.error('Error creating order items:', itemsError);
      // Rollback order creation
      await supabase.from('orders').delete().eq('id', order.id);
      return res.status(500).json({ message: 'Error creating order items' });
    }

    // Update inventory (reduce stock)
    for (const item of items) {
      const { error: inventoryError } = await supabase.rpc('update_inventory_stock', {
        product_uuid: item.productId,
        quantity_change: -parseInt(item.quantity)
      });

      if (inventoryError) {
        console.error('Error updating inventory:', inventoryError);
        // Continue with order creation but log the error
        // In production, you might want to implement a more robust inventory management system
      }
    }

    // Return created order with items
    const { data: orderWithItems, error: fetchError } = await supabase
      .from('orders')
      .select(`
        *,
        order_items (
          id,
          product_id,
          quantity,
          unit_price,
          total_price,
          product_name,
          product_image_url
        )
      `)
      .eq('id', order.id)
      .single();

    if (fetchError) {
      console.error('Error fetching complete order:', fetchError);
      return res.status(500).json({ message: 'Order created but error fetching details' });
    }

    // Clear user's cart after successful order creation
    const { error: clearCartError } = await supabase
      .from('cart')
      .delete()
      .eq('user_id', user.id);

    if (clearCartError) {
      console.warn('Cart clearing failed after order creation:', clearCartError);
      // Log but don't fail the order - cart can be cleared manually
    }

    // ✅ Broadcast new order event to admins and customer (for multi-device sync)
    await eventBroadcaster.orderCreated(
      order.id,
      order.order_number,
      user.id,
      order.total_amount,
      orderWithItems
    );

    res.status(201).json({
      message: 'Order created successfully',
      order: orderWithItems,
      cart_cleared: !clearCartError
    });

  } catch (error) {
    console.error('Unexpected error creating order:', error);
    res.status(500).json({ message: 'Internal server error' });
  }
}