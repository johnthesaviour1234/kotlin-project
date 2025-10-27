# Testing Setup Guide

## Test Users Configuration

For complete end-to-end testing, you need three types of users:

### 1. Admin User
- **Email**: testadmin@gmail.com
- **Password**: Password123
- **User Type**: admin
- **Status**: Go to Supabase Dashboard → Authentication → Users → Find testadmin@gmail.com → Reset password to "Password123"

### 2. Delivery Driver  
- **Email**: testdriver99@gmail.com
- **Password**: Password123
- **User Type**: delivery_driver
- **Status**: Go to Supabase Dashboard → Authentication → Users → Find testdriver99@gmail.com → Reset password to "Password123"

### 3. Customer (Already Working)
- **Email**: abcd@gmail.com
- **Password**: Password123 ✅
- **User Type**: customer

## Quick Password Reset via Supabase Dashboard

1. Go to: https://supabase.com/dashboard/project/hfxdxxpmcemdjsvhsdcf/auth/users
2. Search for the user email
3. Click on the user
4. Click "Reset Password"
5. Set password to: Password123
6. Confirm

## Local Server Testing

Server is running at: http://localhost:3000

### Test Endpoints:

#### Customer Endpoints (✅ Working)
```powershell
# Login
$body = @{ email = "abcd@gmail.com"; password = "Password123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:3000/api/auth/login" -Method Post -Body $body -ContentType "application/json"
```

#### Admin Endpoints (Needs password reset)
```powershell
# Login
$body = @{ email = "testadmin@gmail.com"; password = "Password123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:3000/api/admin/auth/login" -Method Post -Body $body -ContentType "application/json"

# Dashboard Metrics (After login, use the access_token)
$token = "YOUR_ACCESS_TOKEN_HERE"
Invoke-RestMethod -Uri "http://localhost:3000/api/admin/dashboard/metrics" -Method Get -Headers @{Authorization = "Bearer $token"}

# List Products
Invoke-RestMethod -Uri "http://localhost:3000/api/admin/products" -Method Get -Headers @{Authorization = "Bearer $token"}

# List Orders
Invoke-RestMethod -Uri "http://localhost:3000/api/admin/orders" -Method Get -Headers @{Authorization = "Bearer $token"}

# Get Inventory
Invoke-RestMethod -Uri "http://localhost:3000/api/admin/inventory" -Method Get -Headers @{Authorization = "Bearer $token"}
```

#### Delivery Driver Endpoints (Needs password reset)
```powershell
# Login
$body = @{ email = "testdriver99@gmail.com"; password = "Password123" } | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:3000/api/delivery/auth/login" -Method Post -Body $body -ContentType "application/json"

# Get Available Orders
$token = "YOUR_ACCESS_TOKEN_HERE"
Invoke-RestMethod -Uri "http://localhost:3000/api/delivery/available-orders" -Method Get -Headers @{Authorization = "Bearer $token"}
```

## Testing Flow

### Phase 1: Setup ✅
- [x] Database tables created
- [x] Database functions created
- [x] API endpoints implemented
- [x] Local server running

### Phase 2: User Access
- [ ] Reset admin password in Supabase Dashboard
- [ ] Reset delivery driver password in Supabase Dashboard
- [x] Customer login working

### Phase 3: API Testing
1. Admin authentication
2. Admin dashboard metrics
3. Product management (CRUD)
4. Inventory management
5. Order management
6. Order assignment to delivery driver
7. Delivery driver acceptance/decline
8. Delivery status updates
9. Location tracking

## Test Data Available

### Categories
- Fruits (id: e84c6ea4-0418-4705-ac10-032ad6e60e88)
- Vegetables (id: 89d59a0c-14fd-40bb-8cc6-23fac0a1dc81)
- Dairy (id: a54f83b2-be7d-41ab-95bf-c9c8df689e4c)
- Snacks (id: 7550cf60-5149-40ad-8018-2ac055b36ec4)
- Beverages (id: 80cd1c4c-67d1-4eae-8d73-0f1c7069964e)

### Existing Products
- 8 products already exist in database

### Existing Orders
- 23 orders exist for testing
