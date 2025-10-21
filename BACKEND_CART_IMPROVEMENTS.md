# Backend Cart API Improvements

## 1. Cart Endpoint Consistency

### Current Issues:
- Inconsistent response structures across cart endpoints
- Missing product details in cart responses
- Potential race conditions in cart operations

### Required Changes:

#### A. Enhanced Cart GET Endpoint
```javascript
// api/cart/index.js - GET handler
export default async function handler(req, res) {
  if (req.method === 'GET') {
    try {
      // Join cart with products table to get complete product info
      const { data: cartItems, error } = await supabase
        .from('cart')
        .select(`
          id,
          product_id,
          quantity,
          price,
          created_at,
          products:product_id (
            id,
            name,
            image_url,
            category_id
          )
        `)
        .eq('user_id', user.id)
        .order('created_at', { ascending: false });

      if (error) throw error;

      // Transform to expected format with product details
      const items = cartItems.map(item => ({
        id: item.id,
        product_id: item.product_id,
        product_name: item.products?.name || 'Unknown Product',
        image_url: item.products?.image_url || '',
        quantity: item.quantity,
        price: item.price,
        created_at: item.created_at
      }));

      return res.json({
        success: true,
        data: { items },
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      return res.status(500).json({
        success: false,
        error: error.message,
        timestamp: new Date().toISOString()
      });
    }
  }
}
```

#### B. Atomic Cart Operations
```javascript
// Ensure cart operations are atomic and return consistent data
export async function clearUserCart(userId) {
  const { error } = await supabase
    .from('cart')
    .delete()
    .eq('user_id', userId);
  
  if (error) throw error;
  
  // Return empty cart structure
  return {
    items: [],
    total_items: 0,
    total_price: 0
  };
}
```

#### C. Cart Item Removal Fix
```javascript
// api/cart/[productId].js - DELETE handler
export default async function handler(req, res) {
  if (req.method === 'DELETE') {
    const { productId } = req.query;
    
    try {
      const { error } = await supabase
        .from('cart')
        .delete()
        .eq('user_id', user.id)
        .eq('product_id', productId);
        
      if (error) throw error;
      
      return res.json({
        success: true,
        data: { message: 'Item removed from cart' },
        timestamp: new Date().toISOString()
      });
    } catch (error) {
      return res.status(500).json({
        success: false,
        error: error.message,
        timestamp: new Date().toISOString()
      });
    }
  }
}
```

## 2. Order Completion Integration

### Post-Order Cart Clearing
```javascript
// api/orders/create.js - After successful order creation
export default async function handler(req, res) {
  // ... order creation logic ...
  
  try {
    // Create order
    const order = await createOrder(orderData);
    
    // Clear user's cart atomically
    await clearUserCart(user.id);
    
    return res.json({
      success: true,
      data: {
        order,
        cart_cleared: true
      },
      timestamp: new Date().toISOString()
    });
  } catch (error) {
    // Handle rollback if needed
  }
}
```

## 3. Database Optimizations

### Add Indexes for Performance
```sql
-- Improve cart query performance
CREATE INDEX IF NOT EXISTS idx_cart_user_id ON cart(user_id);
CREATE INDEX IF NOT EXISTS idx_cart_user_product ON cart(user_id, product_id);
```

### Add Database Triggers
```sql
-- Auto-update cart timestamps
CREATE OR REPLACE FUNCTION update_cart_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_cart_updated_at_trigger
    BEFORE UPDATE ON cart
    FOR EACH ROW
    EXECUTE FUNCTION update_cart_updated_at();
```