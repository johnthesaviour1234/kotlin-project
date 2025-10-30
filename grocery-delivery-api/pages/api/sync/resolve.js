import { supabaseClient, getAuthenticatedClient } from '../../../lib/supabase';
import { formatSuccessResponse, formatErrorResponse } from '../../../lib/validation';

/**
 * Helper function to get Bearer token from request
 */
function getBearer(req) {
  const h = req.headers['authorization'] || req.headers['Authorization'];
  if (!h || typeof h !== 'string') return null;
  if (!h.toLowerCase().startsWith('bearer ')) return null;
  return h.slice(7).trim();
}

/**
 * POST /api/sync/resolve
 * Resolves conflicts between local and server state using timestamp comparison
 * 
 * Request body:
 * {
 *   "entity": "cart" | "orders" | "profile",
 *   "local_state": { ...local data },
 *   "local_timestamp": "2025-01-30T10:00:05.000Z"
 * }
 * 
 * Response:
 * {
 *   "success": true,
 *   "data": {
 *     "resolved_state": { ...winner state },
 *     "action": "local_wins" | "server_wins" | "no_conflict",
 *     "timestamp": "2025-01-30T10:00:10.000Z"
 *   }
 * }
 * 
 * Logic:
 * - Compare local_timestamp with server updated_at timestamp
 * - If local > server: Update server with local state (local wins)
 * - If server > local: Return server state (server wins)
 * - If equal: No action needed (no conflict)
 */
export default async function handler(req, res) {
  // Only allow POST requests
  if (req.method !== 'POST') {
    return res.status(405).json(formatErrorResponse('Method not allowed'));
  }

  const token = getBearer(req);
  if (!token) {
    return res.status(401).json(formatErrorResponse('Authorization: Bearer token required'));
  }

  try {
    // Authenticate user
    const { data: userData, error: authError } = await supabaseClient.auth.getUser(token);
    if (authError || !userData?.user) {
      return res.status(401).json(formatErrorResponse('Invalid or expired token'));
    }
    const userId = userData.user.id;
    const client = getAuthenticatedClient(token);

    // Parse request body
    const { entity, local_state, local_timestamp } = req.body || {};

    // Validate request
    if (!entity || !['cart', 'orders', 'profile'].includes(entity)) {
      return res.status(400).json(formatErrorResponse('Invalid entity type. Must be: cart, orders, or profile'));
    }

    if (!local_timestamp) {
      return res.status(400).json(formatErrorResponse('local_timestamp is required'));
    }

    // Parse timestamps
    const localTime = new Date(local_timestamp);
    if (isNaN(localTime.getTime())) {
      return res.status(400).json(formatErrorResponse('Invalid local_timestamp format'));
    }

    // Handle different entity types
    let serverTimestamp;
    let serverState;
    let resolvedState;
    let action;

    if (entity === 'cart') {
      // Fetch server cart state
      const { data: cartData, error: cartError } = await client
        .from('cart')
        .select('id, product_id, quantity, price, updated_at')
        .eq('user_id', userId)
        .order('updated_at', { ascending: false })
        .limit(1);

      if (cartError) {
        return res.status(500).json(formatErrorResponse('Failed to fetch server cart state'));
      }

      // Get most recent server update
      serverTimestamp = cartData && cartData.length > 0
        ? new Date(cartData[0].updated_at)
        : new Date(0); // Unix epoch if no items

      // Compare timestamps
      if (localTime > serverTimestamp) {
        // Local is newer - update server with local state
        if (local_state && Array.isArray(local_state.items)) {
          // Clear existing cart
          const { error: deleteError } = await client
            .from('cart')
            .delete()
            .eq('user_id', userId);

          if (deleteError) {
            return res.status(500).json(formatErrorResponse('Failed to clear server cart'));
          }

          // Insert new items from local state
          if (local_state.items.length > 0) {
            const itemsToInsert = local_state.items.map(item => ({
              user_id: userId,
              product_id: item.product_id,
              quantity: item.quantity,
              price: item.price,
              updated_at: new Date().toISOString()
            }));

            const { error: insertError } = await client
              .from('cart')
              .insert(itemsToInsert);

            if (insertError) {
              return res.status(500).json(formatErrorResponse('Failed to update server cart'));
            }
          }
        }

        resolvedState = local_state;
        action = 'local_wins';
      } else if (serverTimestamp > localTime) {
        // Server is newer - return server state
        resolvedState = {
          items: cartData || [],
          updated_at: serverTimestamp.toISOString()
        };
        action = 'server_wins';
      } else {
        // Timestamps are equal - no conflict
        resolvedState = {
          items: cartData || [],
          updated_at: serverTimestamp.toISOString()
        };
        action = 'no_conflict';
      }
    } else if (entity === 'orders') {
      // For orders, we generally don't allow client-side updates
      // Orders are created and updated by the server
      // Just return the latest server state
      const { data: ordersData, error: ordersError } = await client
        .from('orders')
        .select('id, order_number, status, total_amount, updated_at')
        .eq('customer_id', userId)
        .order('updated_at', { ascending: false })
        .limit(20);

      if (ordersError) {
        return res.status(500).json(formatErrorResponse('Failed to fetch server orders state'));
      }

      serverTimestamp = ordersData && ordersData.length > 0
        ? new Date(ordersData[0].updated_at)
        : new Date(0);

      resolvedState = {
        items: ordersData || [],
        updated_at: serverTimestamp.toISOString()
      };
      
      // Orders are always server-authoritative
      action = 'server_wins';
    } else if (entity === 'profile') {
      // Fetch server profile
      const { data: profileData, error: profileError } = await client
        .from('user_profiles')
        .select('*')
        .eq('id', userId)
        .single();

      if (profileError && profileError.code !== 'PGRST116') { // PGRST116 = not found
        return res.status(500).json(formatErrorResponse('Failed to fetch server profile'));
      }

      serverTimestamp = profileData?.updated_at
        ? new Date(profileData.updated_at)
        : new Date(0);

      // Compare timestamps
      if (localTime > serverTimestamp && local_state) {
        // Local is newer - update server
        const { data: updatedProfile, error: updateError } = await client
          .from('user_profiles')
          .update({
            ...local_state,
            updated_at: new Date().toISOString()
          })
          .eq('id', userId)
          .select()
          .single();

        if (updateError) {
          return res.status(500).json(formatErrorResponse('Failed to update server profile'));
        }

        resolvedState = updatedProfile;
        action = 'local_wins';
      } else if (serverTimestamp > localTime) {
        resolvedState = profileData;
        action = 'server_wins';
      } else {
        resolvedState = profileData;
        action = 'no_conflict';
      }
    }

    // Return resolved state
    return res.status(200).json(formatSuccessResponse({
      resolved_state: resolvedState,
      action,
      timestamp: new Date().toISOString()
    }));

  } catch (error) {
    console.error('Conflict resolution error:', error);
    return res.status(500).json(formatErrorResponse('Internal server error'));
  }
}
