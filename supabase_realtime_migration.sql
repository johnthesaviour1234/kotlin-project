-- ================================================================
-- REALTIME SYNC MIGRATION FOR GROCERY DELIVERY SYSTEM
-- ================================================================
-- This migration sets up Supabase Realtime Broadcast functionality
-- to enable real-time synchronization between Customer, Admin, and Delivery apps
--
-- Date: January 30, 2025
-- Version: 1.0.0
-- ================================================================

-- STEP 1: Enable Realtime on required tables
-- ================================================================

-- Enable realtime on cart table
ALTER PUBLICATION supabase_realtime ADD TABLE cart;

-- Enable realtime on orders table
ALTER PUBLICATION supabase_realtime ADD TABLE orders;

-- Enable realtime on delivery_assignments table
ALTER PUBLICATION supabase_realtime ADD TABLE delivery_assignments;

-- Enable realtime on delivery_locations table
ALTER PUBLICATION supabase_realtime ADD TABLE delivery_locations;

-- ================================================================
-- STEP 2: Create Broadcast Trigger Functions
-- ================================================================

-- Function: Broadcast Cart Changes (Customer → Customer's all devices)
-- ================================================================
CREATE OR REPLACE FUNCTION public.broadcast_cart_changes()
RETURNS trigger
SECURITY DEFINER SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
    user_id_text text;
    payload jsonb;
BEGIN
    user_id_text := COALESCE(NEW.user_id, OLD.user_id)::text;
    
    -- Build payload
    IF TG_OP = 'DELETE' THEN
        payload := jsonb_build_object(
            'event_type', 'CART_ITEM_REMOVED',
            'cart_item_id', OLD.id,
            'user_id', OLD.user_id,
            'product_id', OLD.product_id,
            'timestamp', NOW()
        );
    ELSIF TG_OP = 'INSERT' THEN
        payload := jsonb_build_object(
            'event_type', 'CART_ITEM_ADDED',
            'cart_item', row_to_json(NEW),
            'user_id', NEW.user_id,
            'timestamp', NOW()
        );
    ELSE -- UPDATE
        payload := jsonb_build_object(
            'event_type', 'CART_ITEM_UPDATED',
            'cart_item', row_to_json(NEW),
            'user_id', NEW.user_id,
            'timestamp', NOW()
        );
    END IF;
    
    -- Broadcast to user's all devices via topic: cart:{user_id}
    PERFORM pg_notify(
        'cart:' || user_id_text,
        payload::text
    );
    
    RETURN NULL;
END;
$$;

-- Function: Broadcast Order Placed (Customer → Admin + Customer)
-- ================================================================
CREATE OR REPLACE FUNCTION public.broadcast_order_placed()
RETURNS trigger
SECURITY DEFINER SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
    payload jsonb;
BEGIN
    IF (TG_OP = 'INSERT') THEN
        -- Build payload
        payload := jsonb_build_object(
            'event_type', 'ORDER_PLACED',
            'order', row_to_json(NEW),
            'order_id', NEW.id,
            'customer_id', NEW.customer_id,
            'status', NEW.status,
            'total_amount', NEW.total_amount,
            'timestamp', NOW()
        );
        
        -- Broadcast to admin (all new orders topic)
        PERFORM pg_notify('orders:new', payload::text);
        
        -- Broadcast to customer's devices
        PERFORM pg_notify('orders:' || NEW.customer_id::text, payload::text);
    END IF;
    
    RETURN NULL;
END;
$$;

-- Function: Broadcast Order Status Changed (Any status change)
-- ================================================================
CREATE OR REPLACE FUNCTION public.broadcast_order_status_changed()
RETURNS trigger
SECURITY DEFINER SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
    payload jsonb;
    delivery_person_id_text text;
BEGIN
    -- Check if status changed
    IF (TG_OP = 'UPDATE' AND OLD.status != NEW.status) THEN
        
        -- Build payload
        payload := jsonb_build_object(
            'event_type', 'ORDER_STATUS_CHANGED',
            'order', row_to_json(NEW),
            'order_id', NEW.id,
            'customer_id', NEW.customer_id,
            'old_status', OLD.status,
            'new_status', NEW.status,
            'timestamp', NOW()
        );
        
        -- Broadcast to customer
        PERFORM pg_notify('orders:' || NEW.customer_id::text, payload::text);
        
        -- Broadcast to admin (specific order channel)
        PERFORM pg_notify('orders:' || NEW.id::text, payload::text);
        
        -- Special handling for confirmed status (notify delivery pool)
        IF (NEW.status = 'confirmed') THEN
            payload := jsonb_build_object(
                'event_type', 'ORDER_CONFIRMED',
                'order', row_to_json(NEW),
                'order_id', NEW.id,
                'timestamp', NOW()
            );
            PERFORM pg_notify('orders:confirmed:' || NEW.id::text, payload::text);
        END IF;
        
    END IF;
    
    RETURN NULL;
END;
$$;

-- Function: Broadcast Delivery Assignment (Admin → Delivery + Customer)
-- ================================================================
CREATE OR REPLACE FUNCTION public.broadcast_delivery_assignment()
RETURNS trigger
SECURITY DEFINER SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
    payload jsonb;
    order_data record;
BEGIN
    IF (TG_OP = 'INSERT' OR (TG_OP = 'UPDATE' AND OLD.delivery_personnel_id IS NULL AND NEW.delivery_personnel_id IS NOT NULL)) THEN
        
        -- Fetch order details
        SELECT * INTO order_data FROM orders WHERE id = NEW.order_id;
        
        -- Build payload
        payload := jsonb_build_object(
            'event_type', 'ORDER_ASSIGNED',
            'assignment', row_to_json(NEW),
            'order', row_to_json(order_data),
            'order_id', NEW.order_id,
            'delivery_personnel_id', NEW.delivery_personnel_id,
            'assigned_by', NEW.assigned_by,
            'timestamp', NOW()
        );
        
        -- Broadcast to assigned delivery person
        IF NEW.delivery_personnel_id IS NOT NULL THEN
            PERFORM pg_notify('delivery:' || NEW.delivery_personnel_id::text, payload::text);
        END IF;
        
        -- Broadcast to customer
        IF order_data.customer_id IS NOT NULL THEN
            PERFORM pg_notify('orders:' || order_data.customer_id::text, payload::text);
        END IF;
        
        -- Broadcast to admin
        PERFORM pg_notify('orders:' || NEW.order_id::text, payload::text);
        
    END IF;
    
    RETURN NULL;
END;
$$;

-- Function: Broadcast Delivery Location Updates (Delivery → Customer + Admin)
-- ================================================================
CREATE OR REPLACE FUNCTION public.broadcast_delivery_location()
RETURNS trigger
SECURITY DEFINER SET search_path = public
LANGUAGE plpgsql
AS $$
DECLARE
    payload jsonb;
    order_data record;
BEGIN
    IF (TG_OP = 'INSERT' OR TG_OP = 'UPDATE') THEN
        
        -- Build payload
        payload := jsonb_build_object(
            'event_type', 'DELIVERY_LOCATION_UPDATE',
            'location', row_to_json(NEW),
            'order_id', NEW.order_id,
            'delivery_personnel_id', NEW.delivery_personnel_id,
            'latitude', NEW.latitude,
            'longitude', NEW.longitude,
            'timestamp', NOW()
        );
        
        -- If order_id exists, broadcast to customer
        IF NEW.order_id IS NOT NULL THEN
            SELECT * INTO order_data FROM orders WHERE id = NEW.order_id;
            IF order_data.customer_id IS NOT NULL THEN
                PERFORM pg_notify('orders:' || order_data.customer_id::text, payload::text);
            END IF;
            
            -- Broadcast to admin
            PERFORM pg_notify('orders:' || NEW.order_id::text, payload::text);
        END IF;
        
    END IF;
    
    RETURN NULL;
END;
$$;

-- ================================================================
-- STEP 3: Attach Triggers to Tables
-- ================================================================

-- Cart items trigger
DROP TRIGGER IF EXISTS broadcast_cart_changes_trigger ON public.cart;
CREATE TRIGGER broadcast_cart_changes_trigger
AFTER INSERT OR UPDATE OR DELETE ON public.cart
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_cart_changes();

-- Order placed trigger (on INSERT)
DROP TRIGGER IF EXISTS broadcast_order_placed_trigger ON public.orders;
CREATE TRIGGER broadcast_order_placed_trigger
AFTER INSERT ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_placed();

-- Order status changed trigger (on any status change)
DROP TRIGGER IF EXISTS broadcast_order_status_changed_trigger ON public.orders;
CREATE TRIGGER broadcast_order_status_changed_trigger
AFTER UPDATE ON public.orders
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_order_status_changed();

-- Delivery assignment trigger
DROP TRIGGER IF EXISTS broadcast_delivery_assignment_trigger ON public.delivery_assignments;
CREATE TRIGGER broadcast_delivery_assignment_trigger
AFTER INSERT OR UPDATE ON public.delivery_assignments
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_delivery_assignment();

-- Delivery location update trigger
DROP TRIGGER IF EXISTS broadcast_delivery_location_trigger ON public.delivery_locations;
CREATE TRIGGER broadcast_delivery_location_trigger
AFTER INSERT OR UPDATE ON public.delivery_locations
FOR EACH ROW
EXECUTE FUNCTION public.broadcast_delivery_location();

-- ================================================================
-- STEP 4: Grant Necessary Permissions
-- ================================================================

-- Grant execute on trigger functions to authenticated users
GRANT EXECUTE ON FUNCTION public.broadcast_cart_changes() TO authenticated;
GRANT EXECUTE ON FUNCTION public.broadcast_order_placed() TO authenticated;
GRANT EXECUTE ON FUNCTION public.broadcast_order_status_changed() TO authenticated;
GRANT EXECUTE ON FUNCTION public.broadcast_delivery_assignment() TO authenticated;
GRANT EXECUTE ON FUNCTION public.broadcast_delivery_location() TO authenticated;

-- ================================================================
-- STEP 5: Test Triggers (Comment out in production)
-- ================================================================

-- Test 1: Cart update (should trigger broadcast_cart_changes)
-- UPDATE cart SET quantity = quantity + 1 WHERE id = (SELECT id FROM cart LIMIT 1);

-- Test 2: Order status change (should trigger broadcast_order_status_changed)
-- UPDATE orders SET status = 'confirmed' WHERE id = (SELECT id FROM orders LIMIT 1);

-- Test 3: Delivery assignment (should trigger broadcast_delivery_assignment)
-- UPDATE delivery_assignments SET status = 'accepted' WHERE id = (SELECT id FROM delivery_assignments LIMIT 1);

-- ================================================================
-- MIGRATION COMPLETE
-- ================================================================
-- Next Steps:
-- 1. Run this migration using Supabase CLI or Dashboard
-- 2. Test triggers by making database changes
-- 3. Implement client-side realtime listeners in Android apps
-- 4. Monitor pg_notify() messages in Supabase logs
-- ================================================================
