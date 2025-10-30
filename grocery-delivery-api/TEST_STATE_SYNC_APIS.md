# Test State Sync APIs

**Date**: January 30, 2025  
**APIs Created**:
- `GET /api/sync/state` - Get current user state with timestamps and checksums
- `POST /api/sync/resolve` - Resolve conflicts with timestamp-based logic

---

## Prerequisites

1. **Start local development server** (if testing locally):
   ```bash
   cd "E:\warp projects\kotlin mobile application\grocery-delivery-api"
   npm run dev
   ```

2. **Or use deployed Vercel URL**:
   ```
   https://andoid-app-kotlin.vercel.app
   ```

3. **Test Credentials**:
   - Customer: `abcd@gmail.com` / `Password123`
   - Admin: `admin@grocery.com` / `AdminPass123`
   - Driver: `driver@grocery.com` / `DriverPass123`

---

## Test 1: Login and Get Access Token

### Customer Login
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "abcd@gmail.com",
    "password": "Password123"
  }'
```

**Expected Response**:
```json
{
  "success": true,
  "data": {
    "user": { "id": "...", "email": "abcd@gmail.com" },
    "tokens": {
      "access_token": "eyJhbGc...",
      "refresh_token": "...",
      "expires_at": 1738238400,
      "expires_in": 3600
    }
  }
}
```

**Save the `access_token` for subsequent requests**

---

## Test 2: Get Current State

### Request
```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "cart": {
      "items": [],
      "total_items": 0,
      "total_price": 0,
      "updated_at": "1970-01-01T00:00:00.000Z",
      "checksum": "d41d8cd98f00b204e9800998ecf8427e"
    },
    "orders": {
      "items": [],
      "count": 0,
      "updated_at": "1970-01-01T00:00:00.000Z",
      "checksum": "4f53cda18c2baa0c0354bb5f9a3ecbe5"
    },
    "profile": {
      "data": {
        "id": "...",
        "email": "abcd@gmail.com",
        "full_name": "...",
        "user_type": "customer",
        "is_active": true,
        "created_at": "...",
        "updated_at": "..."
      },
      "updated_at": "..."
    },
    "timestamp": "2025-01-30T10:30:00.000Z"
  }
}
```

### Verification Points
✅ Cart has `updated_at` timestamp (Unix epoch if empty)  
✅ Cart has `checksum` for change detection  
✅ Orders included with timestamps  
✅ Profile included with updated_at  
✅ Response has server `timestamp`  

---

## Test 3: Add Item to Cart

Before testing sync conflict resolution, let's add an item to the cart:

```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/cart \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "product_id": "PRODUCT_UUID_HERE",
    "quantity": 2,
    "price": 4.99
  }'
```

Then fetch state again to see updated cart with new timestamp:

```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
```

Now cart should have items with recent `updated_at` timestamp.

---

## Test 4: Resolve Conflict - Local Wins

Simulate scenario where local state is newer than server:

### Request
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/sync/resolve \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "cart",
    "local_state": {
      "items": [
        {
          "product_id": "PRODUCT_UUID_HERE",
          "quantity": 3,
          "price": 4.99
        }
      ]
    },
    "local_timestamp": "2025-01-30T12:00:00.000Z"
  }'
```

### Expected Response (Local Wins)
```json
{
  "success": true,
  "data": {
    "resolved_state": {
      "items": [
        {
          "product_id": "PRODUCT_UUID_HERE",
          "quantity": 3,
          "price": 4.99
        }
      ]
    },
    "action": "local_wins",
    "timestamp": "2025-01-30T12:00:05.000Z"
  }
}
```

### Verification
✅ Response `action` is `"local_wins"`  
✅ Server cart should now have the local state  
✅ Fetch state again to confirm server was updated  

---

## Test 5: Resolve Conflict - Server Wins

Simulate scenario where server state is newer:

### Request
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/sync/resolve \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "cart",
    "local_state": {
      "items": []
    },
    "local_timestamp": "2025-01-30T10:00:00.000Z"
  }'
```

### Expected Response (Server Wins)
```json
{
  "success": true,
  "data": {
    "resolved_state": {
      "items": [ /* current server cart */ ],
      "updated_at": "2025-01-30T12:00:00.000Z"
    },
    "action": "server_wins",
    "timestamp": "2025-01-30T12:00:10.000Z"
  }
}
```

### Verification
✅ Response `action` is `"server_wins"`  
✅ Server state returned (not local)  
✅ Server cart not modified  

---

## Test 6: Resolve Conflict - Orders (Always Server Wins)

Orders are server-authoritative, so they always win:

### Request
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/sync/resolve \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "orders",
    "local_state": {},
    "local_timestamp": "2025-01-30T12:00:00.000Z"
  }'
```

### Expected Response
```json
{
  "success": true,
  "data": {
    "resolved_state": {
      "items": [ /* server orders */ ],
      "updated_at": "..."
    },
    "action": "server_wins",
    "timestamp": "2025-01-30T12:00:15.000Z"
  }
}
```

### Verification
✅ Response `action` is always `"server_wins"` for orders  
✅ Server orders returned  
✅ Local state ignored for orders  

---

## Test 7: Error Handling

### Test 7.1: Missing Authorization Header
```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state
```

**Expected**: 401 Unauthorized

### Test 7.2: Invalid Token
```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
  -H "Authorization: Bearer invalid_token"
```

**Expected**: 401 Unauthorized

### Test 7.3: Invalid Entity Type
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/sync/resolve \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "invalid_entity",
    "local_timestamp": "2025-01-30T10:00:00.000Z"
  }'
```

**Expected**: 400 Bad Request - "Invalid entity type. Must be: cart, orders, or profile"

### Test 7.4: Missing Timestamp
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/sync/resolve \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "cart"
  }'
```

**Expected**: 400 Bad Request - "local_timestamp is required"

---

## Test 8: Admin and Driver Users

### Admin Login
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/admin/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@grocery.com",
    "password": "AdminPass123"
  }'
```

### Admin Get State
```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
  -H "Authorization: Bearer ADMIN_ACCESS_TOKEN"
```

**Expected**: Admin state (likely empty cart, no customer orders)

### Driver Login
```bash
curl -X POST https://andoid-app-kotlin.vercel.app/api/delivery/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "driver@grocery.com",
    "password": "DriverPass123"
  }'
```

### Driver Get State
```bash
curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
  -H "Authorization: Bearer DRIVER_ACCESS_TOKEN"
```

**Expected**: Driver state (empty cart, no customer orders)

---

## Test 9: Checksum Optimization

### Scenario: Unchanged State

1. Fetch state and save checksum:
   ```bash
   curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
   ```

2. Fetch state again (no changes made):
   ```bash
   curl -X GET https://andoid-app-kotlin.vercel.app/api/sync/state \
     -H "Authorization: Bearer YOUR_ACCESS_TOKEN_HERE"
   ```

### Verification
✅ Both responses have same `cart.checksum`  
✅ Client can skip sync if checksums match  
✅ Reduces unnecessary data transfer  

---

## Success Criteria

✅ **Authentication**: All endpoints require valid Bearer token  
✅ **State Endpoint**: Returns cart, orders, profile with timestamps and checksums  
✅ **Resolve Endpoint**: Correctly handles local_wins, server_wins, no_conflict  
✅ **Cart Sync**: Local changes pushed to server when local > server  
✅ **Orders**: Always server-authoritative (server_wins)  
✅ **Profile Sync**: Bidirectional with timestamp comparison  
✅ **Error Handling**: Proper 400/401/500 responses  
✅ **Checksum**: MD5 hash for change detection  
✅ **Timestamps**: ISO 8601 format (UTC)  
✅ **All User Types**: Works for customer, admin, delivery driver  

---

## Next Steps After Testing

1. ✅ Verify both endpoints work with all test scenarios
2. 🔄 Deploy to Vercel production
3. 🔄 Create Android StateManager.kt (Phase 2 Day 8)
4. 🔄 Create Android SyncRepository.kt (Phase 2 Day 9)
5. 🔄 Create Android BackgroundSyncWorker.kt (Phase 2 Days 11-12)

---

**Document Status**: Ready for testing  
**APIs Status**: Created and ready for deployment  
**Next Task**: Test APIs then deploy to Vercel
