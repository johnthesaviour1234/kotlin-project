# Sprint 2: Core Database Schema & Authentication - Detailed Task Breakdown

**Sprint Goal**: Implement core database structure and authentication system  
**Duration**: 1 week (Starting October 26, 2025)  
**Team Size**: 6-8 developers (recommended allocation)  
**Epic**: Backend Services & API Development  
**Last Updated**: October 26, 2025, 04:50 UTC  
**Current Status**: 0% complete - Ready to start

---

## 📊 Sprint 2 Overview

### Sprint Objectives
- Expand database schema beyond basic foundation (users, products, orders, inventory)
- Complete authentication API endpoints with proper security
- Implement comprehensive Row Level Security (RLS) policies
- Prepare backend for Sprint 3 mobile app UI development

### Success Criteria
- [ ] Database schema supports all Sprint 3-5 use cases
- [ ] Authentication endpoints fully functional and tested
- [ ] RLS policies prevent unauthorized data access
- [ ] API documentation updated for all endpoints
- [ ] Sample data loaded for development/testing

---

## 🎯 Current Project State (Sprint 1 Complete)

### ✅ What We Have (Sprint 1 Achievements)
**Infrastructure:**
- ✅ Supabase project operational (hfxdxxpmcemdjsvhsdcf)
- ✅ 3 basic tables: user_profiles, product_categories, products
- ✅ 9 RLS policies implemented
- ✅ Vercel API deployed (https://andoid-app-kotlin.vercel.app)
- ✅ 3 mobile apps with Clean Architecture foundation

**API Endpoints (Implemented):**
- ✅ `/api/health` - Health check
- ✅ `/api/auth/login` - User authentication
- ✅ `/api/auth/register` - User registration
- ✅ `/api/auth/verify` - Email verification
- ✅ `/api/auth/resend-verification` - Resend verification email
- ✅ `/api/auth/change-password` - Password change
- ✅ `/api/products/list` - Product listing with pagination
- ✅ `/api/products/categories` - Category listing
- ✅ `/api/products/[id]` - Product details
- ✅ `/api/cart/index` - View cart
- ✅ `/api/cart/[productId]` - Add to cart
- ✅ `/api/orders/create` - Create order
- ✅ `/api/orders/history` - Order history
- ✅ `/api/orders/[id]` - Order details
- ✅ `/api/users/profile` - User profile
- ✅ `/api/users/addresses` - Manage addresses

**Mobile Apps (Foundation Complete):**
- ✅ GroceryCustomer - Full Clean Architecture with MVVM
- ✅ GroceryAdmin - Template replicated with admin features
- ✅ GroceryDelivery - Template replicated with GPS features

### 🎯 What We Need (Sprint 2 Goals)

**Database Expansion:**
- [ ] Orders table with complete order lifecycle
- [ ] Order items (line items) table
- [ ] Cart items (persistent cart) table
- [ ] User addresses table
- [ ] Product inventory tracking
- [ ] Delivery assignments table
- [ ] Payment transactions table (foundation)
- [ ] Additional RLS policies for new tables

**API Enhancements:**
- [ ] Complete order management endpoints
- [ ] Cart persistence with user sync
- [ ] Address management CRUD
- [ ] Product inventory updates
- [ ] Admin role validation
- [ ] Enhanced error handling

---

## 📋 User Stories & Tasks

## User Story: BACK-001
**As a system, I need a user management database schema for customers, admins, and delivery personnel**

### Tasks:

#### BACK-001-T1: Expand User Profiles Table (2 hours)
**Priority**: High  
**Dependencies**: DEV-002-T1 (Complete)

**Implementation:**
```sql
-- Add missing fields to user_profiles
ALTER TABLE user_profiles 
ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
ADD COLUMN IF NOT EXISTS user_type VARCHAR(20) DEFAULT 'customer' CHECK (user_type IN ('customer', 'admin', 'delivery_person')),
ADD COLUMN IF NOT EXISTS profile_image_url TEXT,
ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ;

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_user_profiles_user_type ON user_profiles(user_type);
CREATE INDEX IF NOT EXISTS idx_user_profiles_email ON user_profiles(email);
```

**Acceptance Criteria:**
- [ ] Phone number field added with validation
- [ ] User type field distinguishes customer/admin/delivery
- [ ] Profile image URL field available
- [ ] Email verification status tracked
- [ ] Last login timestamp tracked
- [ ] Performance indexes created
- [ ] Migration script created and documented

---

#### BACK-001-T2: Create User Addresses Table (2 hours)
**Priority**: High  
**Dependencies**: BACK-001-T1

**Implementation:**
```sql
-- User addresses for delivery
CREATE TABLE IF NOT EXISTS user_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    address_type VARCHAR(20) DEFAULT 'home' CHECK (address_type IN ('home', 'work', 'other')),
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'India',
    latitude DECIMAL(10, 8),
    longitude DECIMAL(11, 8),
    is_default BOOLEAN DEFAULT false,
    delivery_instructions TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_user_addresses_is_default ON user_addresses(user_id, is_default);

-- Update timestamp trigger
CREATE TRIGGER update_user_addresses_updated_at 
    BEFORE UPDATE ON user_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS policies
ALTER TABLE user_addresses ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own addresses"
ON user_addresses FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own addresses"
ON user_addresses FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own addresses"
ON user_addresses FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete own addresses"
ON user_addresses FOR DELETE
USING (auth.uid() = user_id);

CREATE POLICY "Admins can view all addresses"
ON user_addresses FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM user_profiles
        WHERE id = auth.uid() AND user_type = 'admin'
    )
);
```

**Acceptance Criteria:**
- [ ] user_addresses table created with proper schema
- [ ] Supports home, work, other address types
- [ ] Lat/long fields for GPS coordinates
- [ ] Default address flag implemented
- [ ] Delivery instructions field available
- [ ] RLS policies restrict to own addresses
- [ ] Admins can view all addresses
- [ ] Automatic timestamp updates configured

---

## User Story: BACK-002
**As a system, I need a product catalog database schema with categories and inventory**

### Tasks:

#### BACK-002-T1: Expand Products Table (2 hours)
**Priority**: High  
**Dependencies**: DEV-002-T1 (Complete)

**Implementation:**
```sql
-- Add missing fields to products table
ALTER TABLE products
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS stock_quantity INTEGER DEFAULT 0 CHECK (stock_quantity >= 0),
ADD COLUMN IF NOT EXISTS unit VARCHAR(20) DEFAULT 'unit' CHECK (unit IN ('unit', 'kg', 'g', 'l', 'ml', 'dozen', 'pack')),
ADD COLUMN IF NOT EXISTS brand VARCHAR(100),
ADD COLUMN IF NOT EXISTS sku VARCHAR(50) UNIQUE,
ADD COLUMN IF NOT EXISTS featured BOOLEAN DEFAULT false,
ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2) DEFAULT 0 CHECK (discount_percentage >= 0 AND discount_percentage <= 100),
ADD COLUMN IF NOT EXISTS min_order_quantity INTEGER DEFAULT 1,
ADD COLUMN IF NOT EXISTS max_order_quantity INTEGER,
ADD COLUMN IF NOT EXISTS tags TEXT[];

-- Create indexes for search and filtering
CREATE INDEX IF NOT EXISTS idx_products_category_id ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_featured ON products(featured) WHERE featured = true;
CREATE INDEX IF NOT EXISTS idx_products_brand ON products(brand);
CREATE INDEX IF NOT EXISTS idx_products_sku ON products(sku);
CREATE INDEX IF NOT EXISTS idx_products_tags ON products USING GIN(tags);

-- Full-text search index
CREATE INDEX IF NOT EXISTS idx_products_search ON products USING GIN(to_tsvector('english', name || ' ' || COALESCE(description, '') || ' ' || COALESCE(brand, '')));
```

**Acceptance Criteria:**
- [ ] Description field for detailed product info
- [ ] Stock quantity with non-negative constraint
- [ ] Unit of measurement (kg, g, l, ml, unit, etc.)
- [ ] Brand field for filtering
- [ ] SKU (Stock Keeping Unit) for inventory
- [ ] Featured products flag
- [ ] Discount percentage field
- [ ] Min/max order quantity constraints
- [ ] Tags array for flexible categorization
- [ ] Search indexes for performance
- [ ] Full-text search capability

---

#### BACK-002-T2: Create Product Images Table (1.5 hours)
**Priority**: Medium  
**Dependencies**: BACK-002-T1

**Implementation:**
```sql
-- Product images (multiple per product)
CREATE TABLE IF NOT EXISTS product_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    image_url TEXT NOT NULL,
    display_order INTEGER DEFAULT 0,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_product_images_product_id ON product_images(product_id);
CREATE INDEX idx_product_images_display_order ON product_images(product_id, display_order);

-- RLS policies (public read for active products)
ALTER TABLE product_images ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can view product images"
ON product_images FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM products
        WHERE id = product_images.product_id AND is_active = true
    )
);

CREATE POLICY "Admins can manage product images"
ON product_images FOR ALL
USING (
    EXISTS (
        SELECT 1 FROM user_profiles
        WHERE id = auth.uid() AND user_type = 'admin'
    )
);
```

**Acceptance Criteria:**
- [ ] product_images table supports multiple images per product
- [ ] Display order for image carousel
- [ ] Primary image flag
- [ ] Public read access for active products
- [ ] Admin-only management

---

## User Story: BACK-003
**As a user, I want to register and authenticate securely using Supabase Auth**

### Tasks:

#### BACK-003-T1: Enhance Authentication API (3 hours)
**Priority**: Critical  
**Dependencies**: DEV-003-T1 (Complete)

**Files to Update:**
- `grocery-delivery-api/pages/api/auth/login.js` ✅ (Already complete)
- `grocery-delivery-api/pages/api/auth/register.js` ✅ (Already complete)
- `grocery-delivery-api/pages/api/auth/verify.js` ✅ (Already complete)

**New API Endpoints to Create:**

**1. `/api/auth/refresh-token.js`** (Token refresh)
```javascript
import { supabaseClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const { refresh_token } = req.body

    if (!refresh_token) {
      return res.status(400).json(formatErrorResponse('Refresh token required'))
    }

    const { data, error } = await supabaseClient.auth.refreshSession({
      refresh_token
    })

    if (error) {
      return res.status(401).json(formatErrorResponse('Invalid refresh token'))
    }

    res.status(200).json(formatSuccessResponse({
      access_token: data.session.access_token,
      refresh_token: data.session.refresh_token,
      expires_at: data.session.expires_at,
      expires_in: data.session.expires_in
    }))
  } catch (error) {
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**2. `/api/auth/logout.js`** (User logout)
```javascript
import { getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'))
  }

  try {
    const authHeader = req.headers.authorization
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json(formatErrorResponse('No authentication token provided'))
    }

    const token = authHeader.substring(7)
    const authenticatedClient = getAuthenticatedClient(token)

    const { error } = await authenticatedClient.auth.signOut()

    if (error) {
      return res.status(500).json(formatErrorResponse('Logout failed'))
    }

    res.status(200).json(formatSuccessResponse({ message: 'Logged out successfully' }))
  } catch (error) {
    res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**Acceptance Criteria:**
- [ ] Token refresh endpoint implemented
- [ ] Logout endpoint implemented
- [ ] Proper authentication header validation
- [ ] Error handling for expired/invalid tokens
- [ ] API documentation updated

---

## User Story: BACK-004
**As a developer, I want to implement Row Level Security (RLS) for data protection**

### Tasks:

#### BACK-004-T1: Create Orders and Cart Tables with RLS (4 hours)
**Priority**: Critical  
**Dependencies**: BACK-001-T2, BACK-002-T1

**Implementation:**

**1. Shopping Cart Table**
```sql
-- Shopping cart items (persistent cart)
CREATE TABLE IF NOT EXISTS cart_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, product_id)
);

-- Indexes
CREATE INDEX idx_cart_items_user_id ON cart_items(user_id);
CREATE INDEX idx_cart_items_product_id ON cart_items(product_id);

-- Update timestamp trigger
CREATE TRIGGER update_cart_items_updated_at 
    BEFORE UPDATE ON cart_items
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS policies
ALTER TABLE cart_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own cart"
ON cart_items FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can insert into own cart"
ON cart_items FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can update own cart"
ON cart_items FOR UPDATE
USING (auth.uid() = user_id);

CREATE POLICY "Users can delete from own cart"
ON cart_items FOR DELETE
USING (auth.uid() = user_id);
```

**2. Orders Table**
```sql
-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id),
    address_id UUID REFERENCES user_addresses(id),
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'confirmed', 'processing', 'out_for_delivery', 'delivered', 'cancelled')),
    subtotal DECIMAL(10,2) NOT NULL CHECK (subtotal >= 0),
    tax DECIMAL(10,2) DEFAULT 0 CHECK (tax >= 0),
    delivery_fee DECIMAL(10,2) DEFAULT 0 CHECK (delivery_fee >= 0),
    discount DECIMAL(10,2) DEFAULT 0 CHECK (discount >= 0),
    total DECIMAL(10,2) NOT NULL CHECK (total >= 0),
    payment_method VARCHAR(50),
    payment_status VARCHAR(20) DEFAULT 'pending' CHECK (payment_status IN ('pending', 'processing', 'completed', 'failed', 'refunded')),
    delivery_instructions TEXT,
    scheduled_delivery_time TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    cancelled_at TIMESTAMPTZ,
    cancellation_reason TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- Update timestamp trigger
CREATE TRIGGER update_orders_updated_at 
    BEFORE UPDATE ON orders
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS policies
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own orders"
ON orders FOR SELECT
USING (auth.uid() = user_id);

CREATE POLICY "Users can create own orders"
ON orders FOR INSERT
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Admins can view all orders"
ON orders FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM user_profiles
        WHERE id = auth.uid() AND user_type = 'admin'
    )
);

CREATE POLICY "Admins can update orders"
ON orders FOR UPDATE
USING (
    EXISTS (
        SELECT 1 FROM user_profiles
        WHERE id = auth.uid() AND user_type = 'admin'
    )
);
```

**3. Order Items Table**
```sql
-- Order items (line items)
CREATE TABLE IF NOT EXISTS order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price DECIMAL(10,2) NOT NULL CHECK (unit_price >= 0),
    subtotal DECIMAL(10,2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

-- RLS policies
ALTER TABLE order_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can view own order items"
ON order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM orders
        WHERE id = order_items.order_id AND user_id = auth.uid()
    )
);

CREATE POLICY "Admins can view all order items"
ON order_items FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM user_profiles
        WHERE id = auth.uid() AND user_type = 'admin'
    )
);
```

**Acceptance Criteria:**
- [ ] cart_items table created with RLS
- [ ] orders table created with status workflow
- [ ] order_items table created with RLS
- [ ] Users can only access their own data
- [ ] Admins can access all data
- [ ] Proper foreign key constraints
- [ ] Performance indexes created
- [ ] Automatic timestamp updates

---

## User Story: BACK-005
**As a system, I need API endpoints for user authentication and profile management**

### Tasks:

#### BACK-005-T1: Implement User Profile APIs (3 hours)
**Priority**: High  
**Dependencies**: BACK-001-T1

**Files to Create/Update:**

**1. Update `/api/users/profile.js`** (Add update capability)
```javascript
import { getAuthenticatedClient, supabase } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json(formatErrorResponse('Authentication required'))
  }

  const token = authHeader.substring(7)
  const authenticatedClient = getAuthenticatedClient(token)

  try {
    // Get current user
    const { data: { user }, error: authError } = await authenticatedClient.auth.getUser()
    if (authError || !user) {
      return res.status(401).json(formatErrorResponse('Invalid authentication token'))
    }

    if (req.method === 'GET') {
      // Fetch user profile
      const { data: profile, error: profileError } = await authenticatedClient
        .from('user_profiles')
        .select('*')
        .eq('id', user.id)
        .single()

      if (profileError) {
        return res.status(500).json(formatErrorResponse('Failed to fetch profile'))
      }

      return res.status(200).json(formatSuccessResponse({ profile }))

    } else if (req.method === 'PUT') {
      // Update user profile
      const { full_name, phone_number, profile_image_url } = req.body

      const updateData = {}
      if (full_name !== undefined) updateData.full_name = full_name
      if (phone_number !== undefined) updateData.phone_number = phone_number
      if (profile_image_url !== undefined) updateData.profile_image_url = profile_image_url

      const { data: updatedProfile, error: updateError } = await authenticatedClient
        .from('user_profiles')
        .update(updateData)
        .eq('id', user.id)
        .select()
        .single()

      if (updateError) {
        return res.status(500).json(formatErrorResponse('Failed to update profile'))
      }

      return res.status(200).json(formatSuccessResponse({ profile: updatedProfile }))

    } else {
      return res.status(405).json(formatErrorResponse('Method not allowed'))
    }

  } catch (error) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**2. Update `/api/users/addresses.js`** (Full CRUD)
```javascript
import { getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json(formatErrorResponse('Authentication required'))
  }

  const token = authHeader.substring(7)
  const authenticatedClient = getAuthenticatedClient(token)

  try {
    const { data: { user }, error: authError } = await authenticatedClient.auth.getUser()
    if (authError || !user) {
      return res.status(401).json(formatErrorResponse('Invalid authentication token'))
    }

    if (req.method === 'GET') {
      // Fetch all addresses
      const { data: addresses, error } = await authenticatedClient
        .from('user_addresses')
        .select('*')
        .eq('user_id', user.id)
        .order('is_default', { ascending: false })
        .order('created_at', { ascending: false })

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to fetch addresses'))
      }

      return res.status(200).json(formatSuccessResponse({ addresses: addresses || [] }))

    } else if (req.method === 'POST') {
      // Create new address
      const addressData = {
        user_id: user.id,
        ...req.body
      }

      // If setting as default, unset others
      if (addressData.is_default) {
        await authenticatedClient
          .from('user_addresses')
          .update({ is_default: false })
          .eq('user_id', user.id)
      }

      const { data: newAddress, error } = await authenticatedClient
        .from('user_addresses')
        .insert(addressData)
        .select()
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to create address'))
      }

      return res.status(201).json(formatSuccessResponse({ address: newAddress }))

    } else if (req.method === 'PUT') {
      // Update address
      const { id, ...updateData } = req.body

      if (!id) {
        return res.status(400).json(formatErrorResponse('Address ID required'))
      }

      // If setting as default, unset others
      if (updateData.is_default) {
        await authenticatedClient
          .from('user_addresses')
          .update({ is_default: false })
          .eq('user_id', user.id)
          .neq('id', id)
      }

      const { data: updatedAddress, error } = await authenticatedClient
        .from('user_addresses')
        .update(updateData)
        .eq('id', id)
        .eq('user_id', user.id)
        .select()
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to update address'))
      }

      return res.status(200).json(formatSuccessResponse({ address: updatedAddress }))

    } else if (req.method === 'DELETE') {
      // Delete address
      const { id } = req.query

      if (!id) {
        return res.status(400).json(formatErrorResponse('Address ID required'))
      }

      const { error } = await authenticatedClient
        .from('user_addresses')
        .delete()
        .eq('id', id)
        .eq('user_id', user.id)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to delete address'))
      }

      return res.status(200).json(formatSuccessResponse({ message: 'Address deleted successfully' }))

    } else {
      return res.status(405).json(formatErrorResponse('Method not allowed'))
    }

  } catch (error) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**Acceptance Criteria:**
- [ ] GET /api/users/profile - Fetch user profile
- [ ] PUT /api/users/profile - Update profile (name, phone, image)
- [ ] GET /api/users/addresses - List all addresses
- [ ] POST /api/users/addresses - Create address
- [ ] PUT /api/users/addresses - Update address
- [ ] DELETE /api/users/addresses - Delete address
- [ ] Default address management works correctly
- [ ] Proper authentication validation
- [ ] Error handling for all scenarios

---

#### BACK-005-T2: Implement Cart Management APIs (3 hours)
**Priority**: High  
**Dependencies**: BACK-004-T1

**Files to Update:**

**Update `/api/cart/index.js`** (Complete cart management)
```javascript
import { getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json(formatErrorResponse('Authentication required'))
  }

  const token = authHeader.substring(7)
  const authenticatedClient = getAuthenticatedClient(token)

  try {
    const { data: { user }, error: authError } = await authenticatedClient.auth.getUser()
    if (authError || !user) {
      return res.status(401).json(formatErrorResponse('Invalid authentication token'))
    }

    if (req.method === 'GET') {
      // Fetch cart with product details
      const { data: cartItems, error } = await authenticatedClient
        .from('cart_items')
        .select(`
          id,
          quantity,
          created_at,
          products:product_id (
            id,
            name,
            price,
            image_url,
            stock_quantity,
            unit,
            discount_percentage
          )
        `)
        .eq('user_id', user.id)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to fetch cart'))
      }

      // Calculate totals
      const items = cartItems || []
      const subtotal = items.reduce((sum, item) => {
        const price = item.products.price
        const discount = (price * item.products.discount_percentage) / 100
        return sum + ((price - discount) * item.quantity)
      }, 0)

      return res.status(200).json(formatSuccessResponse({
        items,
        summary: {
          itemCount: items.length,
          totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
          subtotal,
          tax: subtotal * 0.05, // 5% tax
          total: subtotal + (subtotal * 0.05)
        }
      }))

    } else if (req.method === 'DELETE') {
      // Clear entire cart
      const { error } = await authenticatedClient
        .from('cart_items')
        .delete()
        .eq('user_id', user.id)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to clear cart'))
      }

      return res.status(200).json(formatSuccessResponse({ message: 'Cart cleared successfully' }))

    } else {
      return res.status(405).json(formatErrorResponse('Method not allowed'))
    }

  } catch (error) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**Update `/api/cart/[productId].js`** (Add/Update/Remove item)
```javascript
import { getAuthenticatedClient } from '../../../lib/supabase'
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation'

export default async function handler(req, res) {
  const { productId } = req.query

  const authHeader = req.headers.authorization
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json(formatErrorResponse('Authentication required'))
  }

  const token = authHeader.substring(7)
  const authenticatedClient = getAuthenticatedClient(token)

  try {
    const { data: { user }, error: authError } = await authenticatedClient.auth.getUser()
    if (authError || !user) {
      return res.status(401).json(formatErrorResponse('Invalid authentication token'))
    }

    if (req.method === 'POST' || req.method === 'PUT') {
      // Add or update cart item
      const { quantity } = req.body

      if (!quantity || quantity < 1) {
        return res.status(400).json(formatErrorResponse('Invalid quantity'))
      }

      // Check product exists and has stock
      const { data: product, error: productError } = await authenticatedClient
        .from('products')
        .select('id, stock_quantity, max_order_quantity')
        .eq('id', productId)
        .single()

      if (productError || !product) {
        return res.status(404).json(formatErrorResponse('Product not found'))
      }

      if (product.stock_quantity < quantity) {
        return res.status(400).json(formatErrorResponse('Insufficient stock'))
      }

      if (product.max_order_quantity && quantity > product.max_order_quantity) {
        return res.status(400).json(formatErrorResponse(`Maximum order quantity is ${product.max_order_quantity}`))
      }

      // Upsert cart item
      const { data: cartItem, error } = await authenticatedClient
        .from('cart_items')
        .upsert({
          user_id: user.id,
          product_id: productId,
          quantity
        }, {
          onConflict: 'user_id,product_id'
        })
        .select()
        .single()

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to update cart'))
      }

      return res.status(200).json(formatSuccessResponse({ cartItem }))

    } else if (req.method === 'DELETE') {
      // Remove item from cart
      const { error } = await authenticatedClient
        .from('cart_items')
        .delete()
        .eq('user_id', user.id)
        .eq('product_id', productId)

      if (error) {
        return res.status(500).json(formatErrorResponse('Failed to remove item from cart'))
      }

      return res.status(200).json(formatSuccessResponse({ message: 'Item removed from cart' }))

    } else {
      return res.status(405).json(formatErrorResponse('Method not allowed'))
    }

  } catch (error) {
    return res.status(500).json(formatErrorResponse('Internal server error'))
  }
}
```

**Acceptance Criteria:**
- [ ] GET /api/cart - Fetch cart with product details and totals
- [ ] POST /api/cart/[productId] - Add product to cart
- [ ] PUT /api/cart/[productId] - Update quantity
- [ ] DELETE /api/cart/[productId] - Remove item
- [ ] DELETE /api/cart - Clear entire cart
- [ ] Stock validation before adding to cart
- [ ] Max order quantity validation
- [ ] Subtotal and tax calculation
- [ ] Proper error messages

---

#### BACK-005-T3: Enhance Order APIs (3 hours)
**Priority**: High  
**Dependencies**: BACK-004-T1

**Update `/api/orders/create.js`** (Complete order creation)
**Update `/api/orders/history.js`** (Add pagination and filtering)
**Update `/api/orders/[id].js`** (Add order details with items)

**Acceptance Criteria:**
- [ ] POST /api/orders/create validates cart, address, payment
- [ ] Order creation atomically creates order + order_items
- [ ] Cart is cleared after successful order
- [ ] Stock quantities are decreased
- [ ] GET /api/orders/history supports pagination and status filtering
- [ ] GET /api/orders/[id] returns complete order with items
- [ ] Proper authorization checks

---

## 📊 Sprint 2 Metrics & Tracking

### Time Estimates
| Task | Estimated Hours | Priority |
|------|----------------|----------|
| BACK-001-T1: Expand User Profiles | 2 | High |
| BACK-001-T2: User Addresses Table | 2 | High |
| BACK-002-T1: Expand Products Table | 2 | High |
| BACK-002-T2: Product Images Table | 1.5 | Medium |
| BACK-003-T1: Enhance Authentication | 3 | Critical |
| BACK-004-T1: Orders/Cart Tables RLS | 4 | Critical |
| BACK-005-T1: User Profile APIs | 3 | High |
| BACK-005-T2: Cart Management APIs | 3 | High |
| BACK-005-T3: Enhanced Order APIs | 3 | High |
| **Total** | **23.5 hours** | - |

### Success Criteria
- [ ] All database tables created and tested
- [ ] All API endpoints implemented and documented
- [ ] RLS policies prevent unauthorized access
- [ ] Sample data loaded for testing
- [ ] No critical security vulnerabilities
- [ ] API documentation updated
- [ ] Postman/Thunder Client collection created

---

## 🚀 Sprint 2 Deliverables

### Database Deliverables
1. ✅ Expanded user_profiles table
2. ✅ user_addresses table with RLS
3. ✅ Enhanced products table with search
4. ✅ product_images table
5. ✅ cart_items table with RLS
6. ✅ orders table with status workflow
7. ✅ order_items table with RLS

### API Deliverables
1. ✅ Authentication refresh/logout endpoints
2. ✅ User profile CRUD endpoints
3. ✅ Address management endpoints
4. ✅ Complete cart management
5. ✅ Enhanced order management

### Documentation Deliverables
1. ✅ Database schema documentation
2. ✅ API endpoint documentation
3. ✅ Postman collection for testing
4. ✅ RLS policy documentation

---

## 🔄 Next Sprint Preview (Sprint 3)

**Sprint 3: Customer App - Basic UI Framework**
- Implement UI screens for Customer app
- Connect mobile app to API endpoints
- Implement navigation and auth flow
- Product browsing UI
- Cart and checkout screens

**Prerequisites from Sprint 2:**
- ✅ All authentication endpoints working
- ✅ Product and cart APIs functional
- ✅ User profile and address management ready
- ✅ Order creation API operational

---

**Sprint 2 Status**: Ready to begin  
**Team Coordination**: Backend and database team priority  
**Mobile Team**: Can begin Sprint 3 planning while Sprint 2 executes  
**Estimated Completion**: October 31, 2025 (5 days from now)
