# Backend-First Implementation Plan: Admin & Delivery APIs

**Plan Created**: October 26, 2025, 08:25 UTC  
**Strategy**: API-First Development with End-to-End Flow Testing  
**Objective**: Build and verify complete backend flows before frontend development  
**Timeline**: 3-4 weeks (Backend only)

---

## 🎯 IMPLEMENTATION PHILOSOPHY

### **Why Backend-First?**
1. **Validate Business Logic Early**: Test complete order lifecycle before UI
2. **API Contract Definition**: Clear interfaces for frontend teams
3. **Independent Testing**: Test flows without UI dependencies
4. **Faster Iteration**: Fix backend issues without mobile app rebuilds
5. **Documentation First**: API contracts serve as living documentation

### **Testing Strategy:**
- **Postman/Insomnia Collections**: For manual testing
- **Automated API Tests**: Jest/Mocha test suites
- **Flow Testing**: Complete user journeys via API chains
- **Load Testing**: Performance validation with realistic data

---

## 📋 PHASE 1: DATABASE FOUNDATION (Week 1, Days 1-2)

### **Priority**: CRITICAL - Must complete first
**Duration**: 8-10 hours

### **Task 1.1: Create Missing Database Tables**

#### **A. Product Images Table**
```sql
-- Allow multiple images per product
CREATE TABLE product_images (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
  image_url TEXT NOT NULL,
  display_order INTEGER DEFAULT 0,
  is_primary BOOLEAN DEFAULT false,
  alt_text TEXT,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now()
);

-- RLS Policies
ALTER TABLE product_images ENABLE ROW LEVEL SECURITY;

-- Admins can do everything
CREATE POLICY "Admins can manage product images"
  ON product_images FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM user_profiles
      WHERE id = auth.uid() AND user_type = 'admin'
    )
  );

-- Everyone can view active product images
CREATE POLICY "Anyone can view product images"
  ON product_images FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM products
      WHERE products.id = product_images.product_id
      AND products.is_active = true
    )
  );

-- Indexes
CREATE INDEX idx_product_images_product_id ON product_images(product_id);
CREATE INDEX idx_product_images_display_order ON product_images(product_id, display_order);
```

#### **B. Delivery Assignments Table**
```sql
-- Track order-to-driver assignments
CREATE TABLE delivery_assignments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
  delivery_personnel_id UUID REFERENCES user_profiles(id),
  assigned_by UUID REFERENCES user_profiles(id), -- Admin who assigned
  assigned_at TIMESTAMPTZ DEFAULT now(),
  accepted_at TIMESTAMPTZ,
  declined_at TIMESTAMPTZ,
  status VARCHAR(50) DEFAULT 'pending' CHECK (
    status IN ('pending', 'accepted', 'declined', 'in_transit', 'completed', 'failed')
  ),
  notes TEXT,
  estimated_delivery_minutes INTEGER,
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE(order_id) -- One assignment per order
);

-- RLS Policies
ALTER TABLE delivery_assignments ENABLE ROW LEVEL SECURITY;

-- Admins can manage all assignments
CREATE POLICY "Admins can manage delivery assignments"
  ON delivery_assignments FOR ALL
  USING (
    EXISTS (
      SELECT 1 FROM user_profiles
      WHERE id = auth.uid() AND user_type = 'admin'
    )
  );

-- Delivery drivers can view their own assignments
CREATE POLICY "Delivery personnel can view their assignments"
  ON delivery_assignments FOR SELECT
  USING (
    delivery_personnel_id = auth.uid() OR
    EXISTS (
      SELECT 1 FROM user_profiles
      WHERE id = auth.uid() AND user_type IN ('admin', 'delivery_driver')
    )
  );

-- Delivery drivers can update their own assignments (accept/decline/status)
CREATE POLICY "Delivery personnel can update their assignments"
  ON delivery_assignments FOR UPDATE
  USING (delivery_personnel_id = auth.uid())
  WITH CHECK (delivery_personnel_id = auth.uid());

-- Indexes
CREATE INDEX idx_delivery_assignments_order_id ON delivery_assignments(order_id);
CREATE INDEX idx_delivery_assignments_personnel_id ON delivery_assignments(delivery_personnel_id);
CREATE INDEX idx_delivery_assignments_status ON delivery_assignments(status);
```

#### **C. Delivery Locations Table**
```sql
-- Track real-time driver GPS locations
CREATE TABLE delivery_locations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  delivery_personnel_id UUID NOT NULL REFERENCES user_profiles(id),
  order_id UUID REFERENCES orders(id),
  latitude DECIMAL(10, 8) NOT NULL,
  longitude DECIMAL(11, 8) NOT NULL,
  accuracy DECIMAL(10, 2), -- in meters
  speed DECIMAL(10, 2), -- in m/s
  heading DECIMAL(10, 2), -- in degrees
  timestamp TIMESTAMPTZ DEFAULT now(),
  created_at TIMESTAMPTZ DEFAULT now()
);

-- RLS Policies
ALTER TABLE delivery_locations ENABLE ROW LEVEL SECURITY;

-- Delivery drivers can insert their own locations
CREATE POLICY "Delivery personnel can insert their locations"
  ON delivery_locations FOR INSERT
  WITH CHECK (delivery_personnel_id = auth.uid());

-- Admins and assigned delivery personnel can view locations
CREATE POLICY "Admins and delivery personnel can view locations"
  ON delivery_locations FOR SELECT
  USING (
    delivery_personnel_id = auth.uid() OR
    EXISTS (
      SELECT 1 FROM user_profiles
      WHERE id = auth.uid() AND user_type = 'admin'
    ) OR
    EXISTS (
      SELECT 1 FROM orders
      WHERE orders.id = delivery_locations.order_id
      AND orders.customer_id = auth.uid()
    )
  );

-- Indexes
CREATE INDEX idx_delivery_locations_personnel_id ON delivery_locations(delivery_personnel_id);
CREATE INDEX idx_delivery_locations_order_id ON delivery_locations(order_id);
CREATE INDEX idx_delivery_locations_timestamp ON delivery_locations(timestamp DESC);

-- Partitioning for performance (optional, for high volume)
-- CREATE INDEX idx_delivery_locations_timestamp_personnel ON delivery_locations(delivery_personnel_id, timestamp DESC);
```

#### **D. Admin Activity Logs Table**
```sql
-- Track admin actions for audit trail
CREATE TABLE admin_activity_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  admin_id UUID NOT NULL REFERENCES user_profiles(id),
  action VARCHAR(100) NOT NULL, -- e.g., 'product_created', 'order_assigned', etc.
  entity_type VARCHAR(50) NOT NULL, -- e.g., 'product', 'order', 'user'
  entity_id UUID,
  changes JSONB, -- Store before/after values
  ip_address INET,
  user_agent TEXT,
  created_at TIMESTAMPTZ DEFAULT now()
);

-- RLS Policies
ALTER TABLE admin_activity_logs ENABLE ROW LEVEL SECURITY;

-- Only admins can view logs
CREATE POLICY "Admins can view activity logs"
  ON admin_activity_logs FOR SELECT
  USING (
    EXISTS (
      SELECT 1 FROM user_profiles
      WHERE id = auth.uid() AND user_type = 'admin'
    )
  );

-- System can insert logs (via service role)
CREATE POLICY "System can insert activity logs"
  ON admin_activity_logs FOR INSERT
  WITH CHECK (true);

-- Indexes
CREATE INDEX idx_admin_activity_logs_admin_id ON admin_activity_logs(admin_id);
CREATE INDEX idx_admin_activity_logs_created_at ON admin_activity_logs(created_at DESC);
CREATE INDEX idx_admin_activity_logs_action ON admin_activity_logs(action);
```

### **Task 1.2: Create Database Functions**

#### **A. Order Assignment Function**
```sql
-- Function to assign order to delivery personnel
CREATE OR REPLACE FUNCTION assign_order_to_delivery(
  p_order_id UUID,
  p_delivery_personnel_id UUID,
  p_assigned_by UUID,
  p_estimated_minutes INTEGER DEFAULT 30
)
RETURNS TABLE(
  assignment_id UUID,
  order_number VARCHAR,
  delivery_personnel_name TEXT,
  status VARCHAR
) AS $$
DECLARE
  v_assignment_id UUID;
  v_order_number VARCHAR;
  v_personnel_name TEXT;
BEGIN
  -- Verify order exists and is not already assigned
  IF NOT EXISTS (
    SELECT 1 FROM orders WHERE id = p_order_id AND status IN ('pending', 'confirmed')
  ) THEN
    RAISE EXCEPTION 'Order not found or cannot be assigned';
  END IF;

  -- Verify delivery personnel exists and is active
  IF NOT EXISTS (
    SELECT 1 FROM user_profiles 
    WHERE id = p_delivery_personnel_id 
    AND user_type = 'delivery_driver'
    AND is_active = true
  ) THEN
    RAISE EXCEPTION 'Delivery personnel not found or inactive';
  END IF;

  -- Create assignment
  INSERT INTO delivery_assignments (
    order_id,
    delivery_personnel_id,
    assigned_by,
    estimated_delivery_minutes,
    status
  ) VALUES (
    p_order_id,
    p_delivery_personnel_id,
    p_assigned_by,
    p_estimated_minutes,
    'pending'
  ) RETURNING id INTO v_assignment_id;

  -- Update order status
  UPDATE orders
  SET 
    status = 'confirmed',
    updated_at = now()
  WHERE id = p_order_id
  RETURNING order_number INTO v_order_number;

  -- Get delivery personnel name
  SELECT full_name INTO v_personnel_name
  FROM user_profiles
  WHERE id = p_delivery_personnel_id;

  -- Return result
  RETURN QUERY SELECT 
    v_assignment_id,
    v_order_number,
    v_personnel_name,
    'pending'::VARCHAR;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

#### **B. Accept/Decline Assignment Function**
```sql
CREATE OR REPLACE FUNCTION update_delivery_assignment_status(
  p_assignment_id UUID,
  p_personnel_id UUID,
  p_new_status VARCHAR,
  p_notes TEXT DEFAULT NULL
)
RETURNS BOOLEAN AS $$
DECLARE
  v_order_id UUID;
BEGIN
  -- Verify assignment belongs to this delivery person
  IF NOT EXISTS (
    SELECT 1 FROM delivery_assignments
    WHERE id = p_assignment_id
    AND delivery_personnel_id = p_personnel_id
  ) THEN
    RAISE EXCEPTION 'Assignment not found or unauthorized';
  END IF;

  -- Update assignment
  UPDATE delivery_assignments
  SET 
    status = p_new_status,
    accepted_at = CASE WHEN p_new_status = 'accepted' THEN now() ELSE accepted_at END,
    declined_at = CASE WHEN p_new_status = 'declined' THEN now() ELSE declined_at END,
    notes = COALESCE(p_notes, notes),
    updated_at = now()
  WHERE id = p_assignment_id
  RETURNING order_id INTO v_order_id;

  -- Update order status based on assignment status
  IF p_new_status = 'accepted' THEN
    UPDATE orders SET status = 'preparing', updated_at = now() WHERE id = v_order_id;
  ELSIF p_new_status = 'in_transit' THEN
    UPDATE orders SET status = 'out_for_delivery', updated_at = now() WHERE id = v_order_id;
  ELSIF p_new_status = 'completed' THEN
    UPDATE orders 
    SET 
      status = 'delivered',
      delivered_at = now(),
      updated_at = now()
    WHERE id = v_order_id;
  ELSIF p_new_status = 'declined' THEN
    UPDATE orders SET status = 'pending', updated_at = now() WHERE id = v_order_id;
  END IF;

  RETURN TRUE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### **Testing Task 1:**
- ✅ Verify all tables created successfully
- ✅ Test RLS policies with different user roles
- ✅ Insert sample data in each table
- ✅ Test database functions with various scenarios

**Completion Criteria**: All tables, policies, and functions working correctly

---

## 📋 PHASE 2: ADMIN AUTHENTICATION & AUTHORIZATION (Week 1, Days 2-3)

### **Priority**: HIGH
**Duration**: 8-10 hours

### **Task 2.1: Admin Middleware Enhancement**

**File**: `grocery-delivery-api/lib/adminMiddleware.js`

```javascript
import { supabase } from './supabase'

/**
 * Middleware to verify admin authentication and authorization
 * @param {Object} req - Request object
 * @param {Array} allowedRoles - Optional array of allowed roles
 * @returns {Object} - User object with profile
 */
export async function adminMiddleware(req, allowedRoles = ['admin']) {
  const authHeader = req.headers.authorization || req.headers.Authorization
  
  if (!authHeader || typeof authHeader !== 'string') {
    throw new Error('Missing authorization header')
  }

  if (!authHeader.toLowerCase().startsWith('bearer ')) {
    throw new Error('Invalid authorization format')
  }

  const token = authHeader.slice(7).trim()
  
  if (!token) {
    throw new Error('Missing access token')
  }

  // Verify token with Supabase
  const { data: { user }, error: authError } = await supabase.auth.getUser(token)
  
  if (authError || !user) {
    throw new Error('Invalid or expired token')
  }

  // Get user profile with role
  const { data: profile, error: profileError } = await supabase
    .from('user_profiles')
    .select('id, email, full_name, user_type, is_active')
    .eq('id', user.id)
    .single()

  if (profileError || !profile) {
    throw new Error('User profile not found')
  }

  // Check if user is active
  if (!profile.is_active) {
    throw new Error('User account is inactive')
  }

  // Check if user has required role
  if (!allowedRoles.includes(profile.user_type)) {
    throw new Error(`Insufficient permissions. Required: ${allowedRoles.join(' or ')}`)
  }

  return {
    ...user,
    profile
  }
}

/**
 * Wrapper for API handlers with admin auth
 */
export function withAdminAuth(handler, allowedRoles = ['admin']) {
  return async (req, res) => {
    try {
      const user = await adminMiddleware(req, allowedRoles)
      req.user = user
      return await handler(req, res)
    } catch (error) {
      return res.status(401).json({
        success: false,
        message: error.message || 'Unauthorized'
      })
    }
  }
}

/**
 * Log admin activity
 */
export async function logAdminActivity(adminId, action, entityType, entityId, changes = null) {
  try {
    await supabase
      .from('admin_activity_logs')
      .insert({
        admin_id: adminId,
        action,
        entity_type: entityType,
        entity_id: entityId,
        changes
      })
  } catch (error) {
    console.error('Failed to log admin activity:', error)
  }
}
```

### **Task 2.2: Admin Authentication Endpoints**

**File**: `grocery-delivery-api/pages/api/admin/auth/login.js`

```javascript
import { supabaseClient } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse, validateLoginRequest } from '../../../../lib/validation'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password } = req.body

    // Validate request
    const validation = validateLoginRequest({ email, password })
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    // Authenticate with Supabase
    const { data: authData, error: authError } = await supabaseClient.auth.signInWithPassword({
      email: validation.data.email,
      password: validation.data.password
    })

    if (authError) {
      console.error('Admin authentication error:', authError)
      return res.status(401).json(formatErrorResponse('Invalid credentials'))
    }

    if (!authData.user || !authData.session) {
      return res.status(401).json(formatErrorResponse('Authentication failed'))
    }

    // Get user profile and verify admin role
    const { data: profile, error: profileError } = await supabaseClient
      .from('user_profiles')
      .select('*')
      .eq('id', authData.user.id)
      .single()

    if (profileError || !profile) {
      return res.status(500).json(formatErrorResponse('Failed to fetch user profile'))
    }

    // Check if user is admin
    if (profile.user_type !== 'admin') {
      return res.status(403).json(formatErrorResponse('Access denied. Admin privileges required.'))
    }

    // Check if admin is active
    if (!profile.is_active) {
      return res.status(403).json(formatErrorResponse('Admin account is inactive'))
    }

    // Return successful response
    res.status(200).json(formatSuccessResponse({
      user: {
        id: authData.user.id,
        email: authData.user.email,
        profile: {
          full_name: profile.full_name,
          user_type: profile.user_type,
          is_active: profile.is_active
        }
      },
      tokens: {
        access_token: authData.session.access_token,
        refresh_token: authData.session.refresh_token,
        expires_at: authData.session.expires_at,
        expires_in: authData.session.expires_in
      }
    }, 'Admin login successful'))

  } catch (error) {
    console.error('Admin login endpoint error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**File**: `grocery-delivery-api/pages/api/admin/auth/logout.js`

```javascript
import { withAdminAuth } from '../../../../lib/adminMiddleware'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    // Token invalidation happens client-side
    // This endpoint is mainly for logging/tracking
    
    res.status(200).json(formatSuccessResponse(null, 'Logout successful'))
  } catch (error) {
    console.error('Admin logout error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Testing Task 2:**

**Postman Collection**: Create "Admin Authentication"

```json
{
  "name": "Admin Auth Flow",
  "requests": [
    {
      "name": "1. Admin Login - Valid Credentials",
      "method": "POST",
      "url": "{{base_url}}/api/admin/auth/login",
      "body": {
        "email": "admin@grocery.com",
        "password": "AdminPass123"
      },
      "tests": [
        "Status code is 200",
        "Response has access_token",
        "User type is 'admin'"
      ]
    },
    {
      "name": "2. Admin Login - Customer Credentials (Should Fail)",
      "method": "POST",
      "url": "{{base_url}}/api/admin/auth/login",
      "body": {
        "email": "abcd@gmail.com",
        "password": "Password123"
      },
      "tests": [
        "Status code is 403",
        "Error message mentions admin privileges"
      ]
    },
    {
      "name": "3. Admin Logout",
      "method": "POST",
      "url": "{{base_url}}/api/admin/auth/logout",
      "headers": {
        "Authorization": "Bearer {{admin_token}}"
      },
      "tests": [
        "Status code is 200"
      ]
    }
  ]
}
```

**Completion Criteria**: Admin authentication working with role validation

---

## 📋 PHASE 3: ADMIN DASHBOARD & METRICS (Week 1, Days 3-4)

### **Priority**: HIGH
**Duration**: 6-8 hours

### **Task 3.1: Dashboard Metrics Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/dashboard/metrics.js`

```javascript
import { withAdminAuth } from '../../../../lib/adminMiddleware'
import { supabase } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

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
```

### **Testing Task 3:**

**Postman Tests**:

```javascript
// Test: Dashboard Metrics
pm.test("Status code is 200", () => {
  pm.response.to.have.status(200)
})

pm.test("Response contains summary metrics", () => {
  const jsonData = pm.response.json()
  pm.expect(jsonData.data).to.have.property('summary')
  pm.expect(jsonData.data.summary).to.have.property('total_orders')
  pm.expect(jsonData.data.summary).to.have.property('total_revenue')
  pm.expect(jsonData.data.summary).to.have.property('active_customers')
})

pm.test("Metrics are numbers", () => {
  const summary = pm.response.json().data.summary
  pm.expect(summary.total_orders).to.be.a('number')
  pm.expect(summary.total_revenue).to.be.a('number')
})
```

**Completion Criteria**: Dashboard returns accurate real-time metrics

---

## 📋 PHASE 4: PRODUCT & INVENTORY MANAGEMENT APIs (Week 1, Day 5 - Week 2, Day 2)

### **Priority**: HIGH
**Duration**: 12-16 hours

### **Task 4.1: Setup Supabase Storage Bucket**

**Priority**: Must complete before image uploads

#### **A. Create Storage Bucket in Supabase Dashboard**

1. Go to Supabase Dashboard → Storage
2. Create new bucket: `product-images`
3. Configure bucket settings:
   - Public bucket: `true` (for public image access)
   - File size limit: `5MB` (max per image)
   - Allowed MIME types: `image/jpeg`, `image/png`, `image/webp`, `image/gif`

#### **B. Set Storage Policies**

```sql
-- Allow admins to upload images
CREATE POLICY "Admins can upload product images"
ON storage.objects FOR INSERT
WITH CHECK (
  bucket_id = 'product-images' AND
  EXISTS (
    SELECT 1 FROM user_profiles
    WHERE id = auth.uid() AND user_type = 'admin'
  )
);

-- Allow admins to update images
CREATE POLICY "Admins can update product images"
ON storage.objects FOR UPDATE
USING (
  bucket_id = 'product-images' AND
  EXISTS (
    SELECT 1 FROM user_profiles
    WHERE id = auth.uid() AND user_type = 'admin'
  )
);

-- Allow admins to delete images
CREATE POLICY "Admins can delete product images"
ON storage.objects FOR DELETE
USING (
  bucket_id = 'product-images' AND
  EXISTS (
    SELECT 1 FROM user_profiles
    WHERE id = auth.uid() AND user_type = 'admin'
  )
);

-- Allow public read access for all images
CREATE POLICY "Anyone can view product images"
ON storage.objects FOR SELECT
USING (bucket_id = 'product-images');
```

### **Task 4.2: Product Management Endpoints**

**Endpoints to implement:**
```
GET    /api/admin/products - List all products
POST   /api/admin/products - Create product
GET    /api/admin/products/[id] - Get product details
PUT    /api/admin/products/[id] - Update product
DELETE /api/admin/products/[id] - Delete product
POST   /api/admin/products/[id]/upload-image - Upload local image to Supabase Storage
GET    /api/admin/products/[id]/images - Get product images
DELETE /api/admin/products/images/[imageId] - Delete image
```

**File**: `grocery-delivery-api/pages/api/admin/products/index.js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware'
import { supabase } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

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
```

### **Task 4.3: Image Upload Endpoint (Supabase Storage)**

**File**: `grocery-delivery-api/pages/api/admin/products/[id]/upload-image.js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware'
import { supabase } from '../../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation'
import formidable from 'formidable'
import fs from 'fs'
import path from 'path'

// Disable Next.js body parser to handle multipart/form-data
export const config = {
  api: {
    bodyParser: false
  }
}

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id: productId } = req.query

    // Verify product exists
    const { data: product, error: productError } = await supabase
      .from('products')
      .select('id, name')
      .eq('id', productId)
      .single()

    if (productError || !product) {
      return res.status(404).json(formatErrorResponse('Product not found'))
    }

    // Parse multipart form data
    const form = formidable({
      maxFileSize: 5 * 1024 * 1024, // 5MB
      maxFiles: 5, // Max 5 images at once
      allowEmptyFiles: false,
      filter: function ({ mimetype }) {
        // Only allow images
        return mimetype && mimetype.includes('image')
      }
    })

    form.parse(req, async (err, fields, files) => {
      if (err) {
        console.error('Form parsing error:', err)
        return res.status(400).json(formatErrorResponse('Failed to parse image upload'))
      }

      const uploadedImages = []
      const imageFiles = Array.isArray(files.images) ? files.images : [files.images].filter(Boolean)

      if (imageFiles.length === 0) {
        return res.status(400).json(formatErrorResponse('No images provided'))
      }

      // Get current max display order for this product
      const { data: existingImages } = await supabase
        .from('product_images')
        .select('display_order')
        .eq('product_id', productId)
        .order('display_order', { ascending: false })
        .limit(1)

      let displayOrder = existingImages?.[0]?.display_order || 0

      // Upload each image to Supabase Storage
      for (const file of imageFiles) {
        try {
          // Read file buffer
          const fileBuffer = fs.readFileSync(file.filepath)
          const fileExt = path.extname(file.originalFilename || file.newFilename)
          const fileName = `${productId}/${Date.now()}-${Math.random().toString(36).substring(7)}${fileExt}`

          // Upload to Supabase Storage
          const { data: uploadData, error: uploadError } = await supabase.storage
            .from('product-images')
            .upload(fileName, fileBuffer, {
              contentType: file.mimetype,
              cacheControl: '3600',
              upsert: false
            })

          if (uploadError) {
            console.error('Supabase upload error:', uploadError)
            continue
          }

          // Get public URL
          const { data: urlData } = supabase.storage
            .from('product-images')
            .getPublicUrl(fileName)

          const publicUrl = urlData.publicUrl

          // Insert into product_images table
          displayOrder++
          const isPrimary = uploadedImages.length === 0 && existingImages?.length === 0

          const { data: imageRecord, error: dbError } = await supabase
            .from('product_images')
            .insert({
              product_id: productId,
              image_url: publicUrl,
              display_order: displayOrder,
              is_primary: isPrimary,
              alt_text: `${product.name} - Image ${displayOrder}`
            })
            .select()
            .single()

          if (dbError) {
            console.error('Database insert error:', dbError)
            // Try to delete uploaded file from storage
            await supabase.storage.from('product-images').remove([fileName])
            continue
          }

          uploadedImages.push(imageRecord)

          // Clean up temp file
          fs.unlinkSync(file.filepath)

        } catch (fileError) {
          console.error('File processing error:', fileError)
          continue
        }
      }

      if (uploadedImages.length === 0) {
        return res.status(500).json(formatErrorResponse('Failed to upload any images'))
      }

      // Log admin activity
      await logAdminActivity(
        req.user.id,
        'product_images_uploaded',
        'product',
        productId,
        { 
          product_name: product.name,
          images_count: uploadedImages.length
        }
      )

      res.status(201).json(formatSuccessResponse({
        uploaded: uploadedImages.length,
        images: uploadedImages
      }, `${uploadedImages.length} image(s) uploaded successfully`))
    })

  } catch (error) {
    console.error('Upload image error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Task 4.4: Get Product Images Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/products/[id]/images.js`

```javascript
import { withAdminAuth } from '../../../../../lib/adminMiddleware'
import { supabase } from '../../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation'

async function handler(req, res) {
  const { method } = req
  const { id: productId } = req.query

  switch (method) {
    case 'GET':
      return await getProductImages(req, res, productId)
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

export default withAdminAuth(handler)
```

### **Task 4.5: Delete Product Image Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/products/images/[imageId].js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware'
import { supabase } from '../../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation'

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

    // Extract storage path from URL
    const urlParts = imageRecord.image_url.split('/product-images/')
    if (urlParts.length < 2) {
      return res.status(400).json(formatErrorResponse('Invalid image URL'))
    }
    const storagePath = urlParts[1]

    // Delete from Supabase Storage
    const { error: storageError } = await supabase.storage
      .from('product-images')
      .remove([storagePath])

    if (storageError) {
      console.error('Storage deletion error:', storageError)
      // Continue with database deletion even if storage deletion fails
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
```

### **Task 4.6: Install Required Dependencies**

**Run in terminal:**
```bash
cd grocery-delivery-api
npm install formidable
```

### **Task 4.7: Inventory Management Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/inventory/index.js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware'
import { supabase } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

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

    res.status(200).json(formatSuccessResponse(data, 'Inventory updated successfully'))
  } catch (error) {
    console.error('Update stock error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Testing Task 4:**

**Complete Product Management Flow Test with Image Upload**:

```javascript
// Postman Collection: Product Management Flow

// 1. Create Product
POST /api/admin/products
{
  "name": "Fresh Apples",
  "description": "Organic red apples",
  "price": 4.99,
  "category_id": "{{category_id}}",
  "featured": true,
  "initial_stock": 100
}
// Save product_id to environment

// 2. Upload Product Images (Local Files)
POST /api/admin/products/{{product_id}}/upload-image
Headers: 
  Authorization: Bearer {{admin_token}}
  Content-Type: multipart/form-data
Body (form-data):
  images: [Select local image file 1]
  images: [Select local image file 2]
  images: [Select local image file 3]
// Response will include Supabase CDN URLs

// 3. Get Product Images (Verify Upload)
GET /api/admin/products/{{product_id}}/images
Headers: Authorization: Bearer {{admin_token}}
// Should return array of images with CDN URLs

// 4. List Products (verify creation with images)
GET /api/admin/products?page=1&limit=10

// 5. Update Product
PUT /api/admin/products/{{product_id}}
{
  "name": "Fresh Organic Apples",
  "price": 5.49
}

// 6. Get Low Stock Items
GET /api/admin/inventory?low_stock=true&threshold=20

// 7. Update Stock (Add)
PUT /api/admin/inventory
{
  "product_id": "{{product_id}}",
  "stock": 50,
  "adjustment_type": "add"
}

// 8. Update Stock (Subtract)
PUT /api/admin/inventory
{
  "product_id": "{{product_id}}",
  "stock": 20,
  "adjustment_type": "subtract"
}

// 9. Delete Product Image
DELETE /api/admin/products/images/{{image_id}}
Headers: Authorization: Bearer {{admin_token}}
// This also removes the image from Supabase Storage

// 10. Deactivate Product
PUT /api/admin/products/{{product_id}}
{
  "is_active": false
}
```

**Image Upload Test in Postman:**

1. Set request type to `POST`
2. URL: `{{base_url}}/api/admin/products/{{product_id}}/upload-image`
3. Headers: `Authorization: Bearer {{admin_token}}`
4. Body → select `form-data`
5. Add key `images` with type `File`
6. Select local image file(s) from your computer
7. Send request
8. Response will include Supabase CDN URLs like:
   ```json
   {
     "success": true,
     "message": "3 image(s) uploaded successfully",
     "data": {
       "uploaded": 3,
       "images": [
         {
           "id": "uuid",
           "product_id": "uuid",
           "image_url": "https://[project-id].supabase.co/storage/v1/object/public/product-images/[product-id]/[timestamp]-[random].jpg",
           "display_order": 1,
           "is_primary": true
         }
       ]
     }
   }
   ```

**Completion Criteria**: Full product CRUD + inventory management working

---

## 📋 PHASE 5: ORDER MANAGEMENT & ASSIGNMENT APIs (Week 2, Days 3-5)

### **Priority**: CRITICAL (Enables delivery flow)
**Duration**: 12-16 hours

### **Task 5.1: Admin Order Management**

**File**: `grocery-delivery-api/pages/api/admin/orders/index.js`

```javascript
import { withAdminAuth } from '../../../../lib/adminMiddleware'
import { supabase } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const {
      page = '1',
      limit = '20',
      status = '',
      search = '',
      date_from = '',
      date_to = '',
      sort_by = 'created_at',
      sort_order = 'desc'
    } = req.query

    const pageNum = parseInt(page)
    const limitNum = Math.min(parseInt(limit), 100)
    const from = (pageNum - 1) * limitNum
    const to = from + limitNum - 1

    let query = supabase
      .from('orders')
      .select(`
        id,
        order_number,
        customer_id,
        status,
        total_amount,
        subtotal,
        delivery_fee,
        payment_method,
        payment_status,
        delivery_address,
        customer_info,
        created_at,
        updated_at,
        order_items(
          id,
          product_name,
          quantity,
          unit_price,
          total_price
        ),
        delivery_assignments(
          id,
          status,
          delivery_personnel_id,
          assigned_at,
          accepted_at,
          user_profiles(full_name, phone)
        )
      `, { count: 'exact' })

    // Apply filters
    if (status) {
      query = query.eq('status', status)
    }
    if (search) {
      query = query.or(`order_number.ilike.%${search}%,customer_info->>email.ilike.%${search}%`)
    }
    if (date_from) {
      query = query.gte('created_at', date_from)
    }
    if (date_to) {
      query = query.lte('created_at', date_to)
    }

    // Apply sorting
    query = query.order(sort_by, { ascending: sort_order === 'asc' })
    query = query.range(from, to)

    const { data, error, count } = await query

    if (error) {
      console.error('Error listing orders:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch orders'))
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
    console.error('List orders error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Task 5.2: Order Assignment Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/orders/assign.js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../lib/adminMiddleware'
import { supabase } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { order_id, delivery_personnel_id, estimated_minutes = 30 } = req.body

    if (!order_id || !delivery_personnel_id) {
      return res.status(400).json(formatErrorResponse('Missing required fields: order_id, delivery_personnel_id'))
    }

    // Use database function to assign order
    const { data, error } = await supabase
      .rpc('assign_order_to_delivery', {
        p_order_id: order_id,
        p_delivery_personnel_id: delivery_personnel_id,
        p_assigned_by: req.user.id,
        p_estimated_minutes: parseInt(estimated_minutes)
      })

    if (error) {
      console.error('Error assigning order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to assign order'))
    }

    // Log activity
    await logAdminActivity(
      req.user.id,
      'order_assigned',
      'order',
      order_id,
      {
        delivery_personnel_id,
        estimated_minutes
      }
    )

    res.status(200).json(formatSuccessResponse(data[0], 'Order assigned successfully'))
  } catch (error) {
    console.error('Assign order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Task 5.3: Update Order Status Endpoint**

**File**: `grocery-delivery-api/pages/api/admin/orders/[id]/status.js`

```javascript
import { withAdminAuth, logAdminActivity } from '../../../../../lib/adminMiddleware'
import { supabase } from '../../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'PUT') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { id } = req.query
    const { status, notes = '' } = req.body

    const validStatuses = ['pending', 'confirmed', 'preparing', 'ready', 'out_for_delivery', 'delivered', 'cancelled']
    
    if (!status || !validStatuses.includes(status)) {
      return res.status(400).json(formatErrorResponse(`Invalid status. Must be one of: ${validStatuses.join(', ')}`))
    }

    // Fetch current order
    const { data: currentOrder, error: fetchError } = await supabase
      .from('orders')
      .select('id, status, order_number')
      .eq('id', id)
      .single()

    if (fetchError || !currentOrder) {
      return res.status(404).json(formatErrorResponse('Order not found'))
    }

    // Update order status
    const updateData = {
      status,
      updated_at: new Date().toISOString()
    }

    if (status === 'delivered') {
      updateData.delivered_at = new Date().toISOString()
    }

    if (notes) {
      updateData.notes = notes
    }

    const { data: updatedOrder, error: updateError } = await supabase
      .from('orders')
      .update(updateData)
      .eq('id', id)
      .select()
      .single()

    if (updateError) {
      console.error('Error updating order status:', updateError)
      return res.status(500).json(formatErrorResponse('Failed to update order status'))
    }

    // Log activity
    await logAdminActivity(
      req.user.id,
      'order_status_updated',
      'order',
      id,
      {
        old_status: currentOrder.status,
        new_status: status,
        order_number: currentOrder.order_number
      }
    )

    res.status(200).json(formatSuccessResponse(updatedOrder, 'Order status updated successfully'))
  } catch (error) {
    console.error('Update order status error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler)
```

### **Testing Task 5:**

**Complete Order Management Flow**:

```javascript
// Postman Collection: Admin Order Management Flow

// 1. List All Orders
GET /api/admin/orders?page=1&limit=10
// Save an order_id with status='pending'

// 2. Get Order Details
GET /api/admin/orders/{{order_id}}

// 3. List Pending Orders
GET /api/admin/orders?status=pending

// 4. Update Order Status to Confirmed
PUT /api/admin/orders/{{order_id}}/status
{
  "status": "confirmed",
  "notes": "Order verified and ready for assignment"
}

// 5. List Active Delivery Personnel
GET /api/admin/delivery-personnel?is_active=true
// Save delivery_personnel_id

// 6. Assign Order to Delivery Personnel
POST /api/admin/orders/assign
{
  "order_id": "{{order_id}}",
  "delivery_personnel_id": "{{delivery_personnel_id}}",
  "estimated_minutes": 30
}

// 7. Verify Assignment
GET /api/admin/orders/{{order_id}}
// Check delivery_assignments field

// 8. Update Order Status to Preparing
PUT /api/admin/orders/{{order_id}}/status
{
  "status": "preparing"
}
```

**Completion Criteria**: Complete order lifecycle management via APIs

---

## 📋 PHASE 6: DELIVERY PERSONNEL APIs (Week 3, Days 1-3)

### **Priority**: CRITICAL
**Duration**: 12-16 hours

### **Task 6.1: Delivery Authentication**

**File**: `grocery-delivery-api/pages/api/delivery/auth/login.js`

```javascript
import { supabaseClient } from '../../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse, validateLoginRequest } from '../../../../lib/validation'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { email, password } = req.body

    const validation = validateLoginRequest({ email, password })
    if (!validation.isValid) {
      return res.status(400).json(formatErrorResponse('Validation failed', validation.errors))
    }

    // Authenticate
    const { data: authData, error: authError } = await supabaseClient.auth.signInWithPassword({
      email: validation.data.email,
      password: validation.data.password
    })

    if (authError) {
      return res.status(401).json(formatErrorResponse('Invalid credentials'))
    }

    // Get profile and verify delivery driver role
    const { data: profile, error: profileError } = await supabaseClient
      .from('user_profiles')
      .select('*')
      .eq('id', authData.user.id)
      .single()

    if (profileError || !profile) {
      return res.status(500).json(formatErrorResponse('Failed to fetch user profile'))
    }

    if (profile.user_type !== 'delivery_driver') {
      return res.status(403).json(formatErrorResponse('Access denied. Delivery personnel privileges required.'))
    }

    if (!profile.is_active) {
      return res.status(403).json(formatErrorResponse('Delivery account is inactive'))
    }

    res.status(200).json(formatSuccessResponse({
      user: {
        id: authData.user.id,
        email: authData.user.email,
        profile: {
          full_name: profile.full_name,
          phone: profile.phone,
          user_type: profile.user_type,
          is_active: profile.is_active
        }
      },
      tokens: {
        access_token: authData.session.access_token,
        refresh_token: authData.session.refresh_token,
        expires_at: authData.session.expires_at,
        expires_in: authData.session.expires_in
      }
    }, 'Delivery login successful'))

  } catch (error) {
    console.error('Delivery login error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

### **Task 6.2: Available Orders Endpoint**

**File**: `grocery-delivery-api/pages/api/delivery/available-orders.js`

```javascript
import { withAdminAuth } from '../../../lib/adminMiddleware'
import { supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'GET') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    // Get orders assigned to this delivery person with status 'pending'
    const { data, error } = await supabase
      .from('delivery_assignments')
      .select(`
        id,
        order_id,
        status,
        assigned_at,
        estimated_delivery_minutes,
        notes,
        orders(
          id,
          order_number,
          total_amount,
          delivery_address,
          customer_info,
          notes,
          created_at
        )
      `)
      .eq('delivery_personnel_id', req.user.id)
      .eq('status', 'pending')
      .order('assigned_at', { ascending: true })

    if (error) {
      console.error('Error fetching available orders:', error)
      return res.status(500).json(formatErrorResponse('Failed to fetch available orders'))
    }

    res.status(200).json(formatSuccessResponse({
      items: data || [],
      count: data?.length || 0
    }))
  } catch (error) {
    console.error('Available orders error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
```

### **Task 6.3: Accept/Decline Order Endpoints**

**File**: `grocery-delivery-api/pages/api/delivery/accept.js`

```javascript
import { withAdminAuth } from '../../../lib/adminMiddleware'
import { supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, notes = '' } = req.body

    if (!assignment_id) {
      return res.status(400).json(formatErrorResponse('Missing required field: assignment_id'))
    }

    // Use database function to accept assignment
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: 'accepted',
        p_notes: notes
      })

    if (error) {
      console.error('Error accepting order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to accept order'))
    }

    res.status(200).json(formatSuccessResponse({ accepted: true }, 'Order accepted successfully'))
  } catch (error) {
    console.error('Accept order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
```

**File**: `grocery-delivery-api/pages/api/delivery/decline.js`

```javascript
import { withAdminAuth } from '../../../lib/adminMiddleware'
import { supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, reason = '' } = req.body

    if (!assignment_id) {
      return res.status(400).json(formatErrorResponse('Missing required field: assignment_id'))
    }

    // Use database function to decline assignment
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: 'declined',
        p_notes: reason
      })

    if (error) {
      console.error('Error declining order:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to decline order'))
    }

    res.status(200).json(formatSuccessResponse({ declined: true }, 'Order declined successfully'))
  } catch (error) {
    console.error('Decline order error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
```

### **Task 6.4: Update Delivery Status**

**File**: `grocery-delivery-api/pages/api/delivery/update-status.js`

```javascript
import { withAdminAuth } from '../../../lib/adminMiddleware'
import { supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'PUT') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { assignment_id, status, notes = '', proof_of_delivery = null } = req.body

    const validStatuses = ['in_transit', 'completed']
    
    if (!assignment_id || !status) {
      return res.status(400).json(formatErrorResponse('Missing required fields: assignment_id, status'))
    }

    if (!validStatuses.includes(status)) {
      return res.status(400).json(formatErrorResponse(`Invalid status. Must be one of: ${validStatuses.join(', ')}`))
    }

    // Use database function
    const { data, error } = await supabase
      .rpc('update_delivery_assignment_status', {
        p_assignment_id: assignment_id,
        p_personnel_id: req.user.id,
        p_new_status: status,
        p_notes: notes
      })

    if (error) {
      console.error('Error updating delivery status:', error)
      return res.status(500).json(formatErrorResponse(error.message || 'Failed to update delivery status'))
    }

    res.status(200).json(formatSuccessResponse({ status }, 'Delivery status updated successfully'))
  } catch (error) {
    console.error('Update delivery status error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
```

### **Task 6.5: Location Tracking Endpoint**

**File**: `grocery-delivery-api/pages/api/delivery/update-location.js`

```javascript
import { withAdminAuth } from '../../../lib/adminMiddleware'
import { supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { latitude, longitude, accuracy, speed, heading, order_id = null } = req.body

    if (latitude === undefined || longitude === undefined) {
      return res.status(400).json(formatErrorResponse('Missing required fields: latitude, longitude'))
    }

    // Validate coordinates
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      return res.status(400).json(formatErrorResponse('Invalid coordinates'))
    }

    // Insert location
    const { data, error } = await supabase
      .from('delivery_locations')
      .insert({
        delivery_personnel_id: req.user.id,
        order_id,
        latitude: parseFloat(latitude),
        longitude: parseFloat(longitude),
        accuracy: accuracy ? parseFloat(accuracy) : null,
        speed: speed ? parseFloat(speed) : null,
        heading: heading ? parseFloat(heading) : null
      })
      .select()
      .single()

    if (error) {
      console.error('Error updating location:', error)
      return res.status(500).json(formatErrorResponse('Failed to update location'))
    }

    res.status(201).json(formatSuccessResponse(data, 'Location updated successfully'))
  } catch (error) {
    console.error('Update location error:', error)
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}

export default withAdminAuth(handler, ['delivery_driver'])
```

### **Testing Task 6:**

**Complete Delivery Flow Test**:

```javascript
// Postman Collection: Delivery Personnel Flow

// SETUP: Create test order and assign to delivery person (via admin APIs)

// 1. Delivery Login
POST /api/delivery/auth/login
{
  "email": "driver@grocery.com",
  "password": "DriverPass123"
}
// Save delivery_token

// 2. Get Available Orders (Assigned)
GET /api/delivery/available-orders
Headers: Authorization: Bearer {{delivery_token}}
// Save assignment_id

// 3. Accept Order
POST /api/delivery/accept
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}",
  "notes": "On my way to pick up the order"
}

// 4. Update Status to In Transit
PUT /api/delivery/update-status
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}",
  "status": "in_transit",
  "notes": "Picked up and heading to customer"
}

// 5. Update Location (Simulate GPS tracking)
POST /api/delivery/update-location
Headers: Authorization: Bearer {{delivery_token}}
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "accuracy": 10,
  "order_id": "{{order_id}}"
}

// 6. Update Status to Completed
PUT /api/delivery/update-status
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}",
  "status": "completed",
  "notes": "Order delivered successfully"
}

// 7. Verify Order Status Updated (via customer or admin API)
GET /api/orders/{{order_id}}
// Should show status = 'delivered'
```

**Completion Criteria**: Complete delivery workflow from assignment to completion

---

## 📋 PHASE 7: END-TO-END FLOW TESTING (Week 3, Days 4-5)

### **Priority**: CRITICAL
**Duration**: 8-12 hours

### **Task 7.1: Complete Customer-to-Delivery Flow**

**Scenario**: Customer Order → Admin Assignment → Delivery Completion

```javascript
// FLOW TEST: Complete Order Lifecycle

// === CUSTOMER FLOW ===
// 1. Customer Login
POST /api/auth/login
{
  "email": "abcd@gmail.com",
  "password": "Password123"
}
// Save customer_token

// 2. Browse Products
GET /api/products/list?featured=true

// 3. Add to Cart
POST /api/cart
Headers: Authorization: Bearer {{customer_token}}
{
  "product_id": "{{product_id}}",
  "quantity": 2,
  "price": 4.99
}

// 4. View Cart
GET /api/cart
Headers: Authorization: Bearer {{customer_token}}

// 5. Create Order
POST /api/orders/create
Headers: Authorization: Bearer {{customer_token}}
{
  "items": [
    {
      "productId": "{{product_id}}",
      "productName": "Fresh Apples",
      "quantity": 2,
      "unitPrice": 4.99,
      "totalPrice": 9.98,
      "productImageUrl": "https://..."
    }
  ],
  "subtotal": 9.98,
  "taxAmount": 0.80,
  "deliveryFee": 2.99,
  "totalAmount": 13.77,
  "deliveryAddress": {
    "street": "123 Main St",
    "city": "San Francisco",
    "state": "CA",
    "postal_code": "94102"
  },
  "paymentMethod": "cash"
}
// Save order_id

// === ADMIN FLOW ===
// 6. Admin Login
POST /api/admin/auth/login
{
  "email": "admin@grocery.com",
  "password": "AdminPass123"
}
// Save admin_token

// 7. View Pending Orders
GET /api/admin/orders?status=pending
Headers: Authorization: Bearer {{admin_token}}

// 8. Update Order Status
PUT /api/admin/orders/{{order_id}}/status
Headers: Authorization: Bearer {{admin_token}}
{
  "status": "confirmed"
}

// 9. Get Available Delivery Personnel
GET /api/admin/delivery-personnel?is_active=true
Headers: Authorization: Bearer {{admin_token}}
// Save delivery_personnel_id

// 10. Assign Order
POST /api/admin/orders/assign
Headers: Authorization: Bearer {{admin_token}}
{
  "order_id": "{{order_id}}",
  "delivery_personnel_id": "{{delivery_personnel_id}}",
  "estimated_minutes": 30
}

// === DELIVERY FLOW ===
// 11. Delivery Login
POST /api/delivery/auth/login
{
  "email": "driver@grocery.com",
  "password": "DriverPass123"
}
// Save delivery_token

// 12. View Available Orders
GET /api/delivery/available-orders
Headers: Authorization: Bearer {{delivery_token}}

// 13. Accept Order
POST /api/delivery/accept
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}"
}

// 14. Start Delivery
PUT /api/delivery/update-status
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}",
  "status": "in_transit"
}

// 15. Update Location (Multiple times)
POST /api/delivery/update-location
Headers: Authorization: Bearer {{delivery_token}}
{
  "latitude": 37.7749,
  "longitude": -122.4194,
  "order_id": "{{order_id}}"
}

// 16. Complete Delivery
PUT /api/delivery/update-status
Headers: Authorization: Bearer {{delivery_token}}
{
  "assignment_id": "{{assignment_id}}",
  "status": "completed"
}

// === VERIFICATION ===
// 17. Customer Views Order History
GET /api/orders/history
Headers: Authorization: Bearer {{customer_token}}
// Verify order shows as 'delivered'

// 18. Admin Views Order Details
GET /api/admin/orders/{{order_id}}
Headers: Authorization: Bearer {{admin_token}}
// Verify assignment and delivery completion

// 19. Delivery Views History
GET /api/delivery/history
Headers: Authorization: Bearer {{delivery_token}}
// Verify completed delivery appears
```

### **Task 7.2: Edge Case Testing**

```javascript
// EDGE CASE TESTS

// 1. Customer tries to access admin endpoint (Should fail)
GET /api/admin/orders
Headers: Authorization: Bearer {{customer_token}}
// Expected: 401 Unauthorized

// 2. Delivery driver tries to accept already accepted order (Should fail)
POST /api/delivery/accept
{
  "assignment_id": "{{already_accepted_assignment_id}}"
}
// Expected: 400 or 409 error

// 3. Admin tries to assign order to inactive delivery person (Should fail)
POST /api/admin/orders/assign
{
  "order_id": "{{order_id}}",
  "delivery_personnel_id": "{{inactive_driver_id}}"
}
// Expected: 400 error

// 4. Create order with insufficient inventory (Should fail)
POST /api/orders/create
{
  "items": [
    {
      "productId": "{{product_id}}",
      "quantity": 9999, // More than available
      ...
    }
  ]
}
// Expected: 400 error or inventory warning

// 5. Customer tries to view another customer's order (Should fail via RLS)
GET /api/orders/{{other_customer_order_id}}
Headers: Authorization: Bearer {{customer_token}}
// Expected: 404 or 403

// 6. Invalid token (Should fail)
GET /api/users/profile
Headers: Authorization: Bearer invalid_token_here
// Expected: 401 Unauthorized

// 7. Update order status with invalid status
PUT /api/admin/orders/{{order_id}}/status
{
  "status": "invalid_status"
}
// Expected: 400 Bad Request
```

### **Task 7.3: Performance Testing**

```javascript
// PERFORMANCE TESTS (Use Postman Runner or k6)

// 1. Concurrent Order Creation (10 simultaneous orders)
for (i = 0; i < 10; i++) {
  POST /api/orders/create
  // Measure response time
}

// 2. Admin Dashboard Load Test
for (i = 0; i < 50; i++) {
  GET /api/admin/dashboard/metrics
  // Measure response time
}

// 3. Product List Pagination Performance
GET /api/admin/products?page=1&limit=100
// Should return < 500ms

// 4. Location Update Burst (Simulate GPS updates)
for (i = 0; i < 20; i++) {
  POST /api/delivery/update-location
  // Measure throughput
}
```

**Completion Criteria**: 
- ✅ All flow tests pass end-to-end
- ✅ Edge cases handled correctly
- ✅ Performance meets requirements (<500ms for most endpoints)
- ✅ RLS policies working (users can't access unauthorized data)

---

## 📋 PHASE 8: DOCUMENTATION & HANDOFF (Week 4, Days 1-2)

### **Priority**: HIGH
**Duration**: 8-10 hours

### **Task 8.1: API Documentation**

Create comprehensive API documentation using Swagger/OpenAPI or Postman Documentation

**File**: `grocery-delivery-api/API_DOCUMENTATION.md`

```markdown
# Grocery Delivery API Documentation

## Base URL
- Production: `https://andoid-app-kotlin.vercel.app`
- Staging: `https://andoid-app-kotlin-staging.vercel.app`

## Authentication
All protected endpoints require Bearer token authentication:
```
Authorization: Bearer <access_token>
```

## User Roles
- `customer`: Regular customers
- `admin`: Administrative users
- `delivery_driver`: Delivery personnel

---

## Customer APIs

### Authentication
- `POST /api/auth/login` - Customer login
- `POST /api/auth/register` - Customer registration
- `POST /api/auth/logout` - Logout
- `POST /api/auth/refresh-token` - Refresh access token

### Products
- `GET /api/products/list` - List products
- `GET /api/products/[id]` - Get product details
- `GET /api/products/categories` - List categories

### Cart
- `GET /api/cart` - View cart
- `POST /api/cart` - Add item to cart
- `PUT /api/cart` - Update cart item
- `DELETE /api/cart` - Clear cart
- `PUT /api/cart/[productId]` - Update specific product
- `DELETE /api/cart/[productId]` - Remove specific product

### Orders
- `POST /api/orders/create` - Create order
- `GET /api/orders/history` - Order history
- `GET /api/orders/[id]` - Order details

### User Profile
- `GET /api/users/profile` - Get profile
- `PUT /api/users/profile` - Update profile
- `GET /api/users/addresses` - List addresses
- `POST /api/users/addresses` - Create address
- `PUT /api/users/addresses` - Update address
- `DELETE /api/users/addresses` - Delete address

---

## Admin APIs

### Authentication
- `POST /api/admin/auth/login` - Admin login
- `POST /api/admin/auth/logout` - Admin logout

### Dashboard
- `GET /api/admin/dashboard/metrics` - Dashboard metrics

### Products
- `GET /api/admin/products` - List all products
- `POST /api/admin/products` - Create product
- `GET /api/admin/products/[id]` - Get product
- `PUT /api/admin/products/[id]` - Update product
- `DELETE /api/admin/products/[id]` - Delete product
- `POST /api/admin/products/[id]/images` - Upload images

### Inventory
- `GET /api/admin/inventory` - Get inventory
- `PUT /api/admin/inventory` - Update stock

### Orders
- `GET /api/admin/orders` - List all orders
- `GET /api/admin/orders/[id]` - Get order details
- `PUT /api/admin/orders/[id]/status` - Update status
- `POST /api/admin/orders/assign` - Assign to delivery

### Delivery Personnel
- `GET /api/admin/delivery-personnel` - List drivers
- `POST /api/admin/delivery-personnel` - Register driver
- `PUT /api/admin/delivery-personnel/[id]` - Update driver
- `PUT /api/admin/delivery-personnel/[id]/status` - Activate/deactivate

---

## Delivery APIs

### Authentication
- `POST /api/delivery/auth/login` - Delivery login
- `POST /api/delivery/auth/logout` - Delivery logout

### Orders
- `GET /api/delivery/available-orders` - Available assignments
- `POST /api/delivery/accept` - Accept order
- `POST /api/delivery/decline` - Decline order
- `GET /api/delivery/current-order` - Current delivery
- `PUT /api/delivery/update-status` - Update delivery status

### Tracking
- `POST /api/delivery/update-location` - Update GPS location

### History
- `GET /api/delivery/history` - Delivery history
- `GET /api/delivery/earnings` - Earnings summary
- `GET /api/delivery/profile` - Profile
- `PUT /api/delivery/profile` - Update profile

---

[Include detailed request/response examples for each endpoint]
```

### **Task 8.2: Postman Collection Export**

Export complete Postman collection with:
- All endpoints organized by feature
- Example requests with sample data
- Environment variables
- Test scripts
- Documentation for each request

### **Task 8.3: Setup Guide**

**File**: `grocery-delivery-api/BACKEND_SETUP_GUIDE.md`

```markdown
# Backend Setup & Testing Guide

## Prerequisites
- Node.js 18+
- Supabase account
- Vercel account (for deployment)
- Postman or Insomnia

## Local Development Setup

1. Clone repository
2. Install dependencies: `npm install`
3. Copy `.env.example` to `.env.local`
4. Add Supabase credentials
5. Run dev server: `npm run dev`

## Database Setup

1. Run migration scripts in order:
   - `01_create_product_images.sql`
   - `02_create_delivery_assignments.sql`
   - `03_create_delivery_locations.sql`
   - `04_create_admin_activity_logs.sql`
   - `05_create_database_functions.sql`

2. Verify RLS policies are enabled
3. Create test users (1 admin, 1 customer, 1 delivery driver)
4. Seed sample product data

## Testing

1. Import Postman collection
2. Set environment variables
3. Run test suites in order:
   - Authentication flows
   - Customer workflows
   - Admin workflows
   - Delivery workflows
   - End-to-end scenarios

## Deployment

1. Connect GitHub repo to Vercel
2. Configure environment variables
3. Deploy to staging first
4. Run smoke tests
5. Deploy to production

## Troubleshooting

[Include common issues and solutions]
```

**Completion Criteria**:
- ✅ Complete API documentation
- ✅ Exported Postman collection
- ✅ Setup guide with clear instructions
- ✅ All test scripts documented

---

## 📊 SUMMARY & TIMELINE

### **Complete Backend Implementation Timeline**

| Phase | Focus | Duration | Deliverables |
|-------|-------|----------|--------------|
| **Phase 1** | Database Foundation | 1-2 days | Tables, RLS, Functions |
| **Phase 2** | Admin Auth | 1 day | Admin login/logout |
| **Phase 3** | Dashboard & Metrics | 1 day | Dashboard API |
| **Phase 4** | Product & Inventory | 2-3 days | Product CRUD, Stock management |
| **Phase 5** | Order Management | 2-3 days | Order assignment, Status updates |
| **Phase 6** | Delivery APIs | 2-3 days | Driver auth, Accept/decline, Tracking |
| **Phase 7** | End-to-End Testing | 1-2 days | Complete flow tests |
| **Phase 8** | Documentation | 1-2 days | API docs, Postman collection |

**Total Duration**: 3-4 weeks (15-20 working days)

### **Success Criteria**

✅ **Database**: All tables, RLS policies, and functions implemented  
✅ **Authentication**: Role-based auth for admin and delivery  
✅ **Admin APIs**: Complete product, inventory, and order management  
✅ **Delivery APIs**: Full delivery workflow from assignment to completion  
✅ **Testing**: All flows tested end-to-end with Postman  
✅ **Documentation**: Complete API documentation and test collections  
✅ **Performance**: <500ms response time for 95% of endpoints  
✅ **Security**: RLS working, proper authorization checks

### **Next Steps After Backend Completion**

1. ✅ All APIs verified and documented
2. ✅ Test users created for each role
3. ✅ Sample data seeded in database
4. ➡️ **Begin Admin Mobile App Development** (Week 5+)
5. ➡️ **Begin Delivery Mobile App Development** (Week 7+)

---

**Plan Created**: October 26, 2025, 08:25 UTC  
**Status**: ✅ Ready for implementation  
**Next Action**: Begin Phase 1 - Database Foundation
