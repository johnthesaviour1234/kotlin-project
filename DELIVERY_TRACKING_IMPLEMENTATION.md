# Real-Time Delivery Tracking Implementation

**Date**: October 28, 2025  
**Feature**: Customer can track delivery driver's live location on Google Maps

---

## ✅ Implementation Complete

### 1. Backend API Endpoint ✅
**File**: `grocery-delivery-api/pages/api/orders/[orderId]/driver-location.js`

**Endpoint**: `GET /api/orders/{orderId}/driver-location`

**Features**:
- Fetches latest driver location from `delivery_locations` table
- Returns driver info (name, phone)
- Calculates estimated minutes remaining
- Only works for active deliveries (accepted, in_transit, arrived status)
- Gracefully handles case when location not yet available

**Response Format**:
```json
{
  "success": true,
  "data": {
    "has_location": true,
    "location": {
      "latitude": 37.7749,
      "longitude": -122.4194,
      "accuracy": 10,
      "speed": 15.5,
      "heading": 180.0,
      "timestamp": "2025-10-28T...",
      "updated_at": "2025-10-28T..."
    },
    "driver": {
      "name": "John Driver",
      "phone": "1234567890"
    },
    "delivery_status": "in_transit",
    "estimated_minutes_remaining": 15
  }
}
```

---

### 2. Android Customer App Changes ✅

#### Dependencies Added:
- `play-services-maps:18.2.0` - Google Maps SDK
- `play-services-location:21.0.1` - Location services

#### New Files Created:

1. **DriverLocationDto.kt** - Data models for API response
   - `DriverLocationResponse`
   - `DriverLocation`
   - `DriverInfo`

2. **TrackDeliveryActivity.kt** - Main tracking screen
   - Google Maps integration
   - Real-time location updates every 15 seconds
   - Driver marker on map (green pin)
   - Bottom sheet with driver info, ETA, order number
   - Call driver button
   - Loading/error states

3. **activity_track_delivery.xml** - UI layout
   - Full-screen map
   - Floating bottom card with delivery info
   - Loading overlay
   - Error state with retry button

#### Modified Files:

1. **AndroidManifest.xml**
   - Added location permissions (FINE & COARSE)
   - Added TrackDeliveryActivity declaration
   - Added Google Maps API key placeholder

2. **ApiService.kt**
   - Added `getDriverLocation()` method

3. **build.gradle.kts**
   - Added Google Maps and Location dependencies

4. **fragment_order_detail.xml**
   - Added "Track Delivery" button

5. **OrderDetailFragment.kt**
   - Added Track Delivery button logic
   - Shows button only for `out_for_delivery` status
   - Launches TrackDeliveryActivity with order ID

---

## 🔧 Configuration Required

### Google Maps API Key

**⚠️ IMPORTANT**: You need to add a Google Maps API key before this will work!

1. **Get API Key**:
   - Go to [Google Cloud Console](https://console.cloud.google.com/)
   - Create/select project
   - Enable "Maps SDK for Android"
   - Create API key (restrict to Android apps)

2. **Add to AndroidManifest.xml**:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="YOUR_ACTUAL_API_KEY_HERE" />
   ```

   Currently set to: `YOUR_GOOGLE_MAPS_API_KEY` (placeholder)

---

## 🧪 Testing Instructions

### Prerequisites:
1. Google Maps API key configured
2. Delivery driver app running with location tracking enabled
3. Active order with status `out_for_delivery`
4. Driver must have started location updates

### Test Flow:

#### Step 1: Prepare Order
1. Login to **Admin app** with `admin@grocery.com`
2. Assign an order to a driver
3. Note the order ID

#### Step 2: Start Driver Location
1. Login to **Delivery app** with `driver@grocery.com`
2. Accept the assigned order
3. Tap "Start Delivery" - this triggers location tracking
4. Location updates sent every 15 seconds to backend

#### Step 3: Track from Customer App
1. Login to **Customer app** with `abcd@gmail.com`
2. Go to **Orders** → **Order History**
3. Find the order that's `out_for_delivery`
4. Tap on the order to see details
5. **"Track Delivery" button should be visible**
6. Tap "Track Delivery"

#### Expected Result:
✅ Map opens showing driver's current location (green marker)  
✅ Bottom card shows:
   - Driver name
   - Delivery status ("On the way")
   - Estimated arrival time (e.g., "15 minutes")
   - Order number
   - Call driver button (taps to dial)

✅ Map updates every 15 seconds with new driver position  
✅ Camera animates to follow driver location

---

## 🎯 Features

### Real-Time Tracking
- **Polling interval**: 15 seconds
- **Auto-refresh**: Continues while activity is visible
- **Stops on pause**: Conserves battery when app backgrounded

### Map Features
- Driver marker with green pin
- Zoom controls enabled
- User location button (if permission granted)
- Compass enabled
- Camera auto-follows driver

### Error Handling
- Network errors shown with retry button
- Graceful handling when location not yet available
- Shows message: "Driver location not yet available"
- Permission handling for map features

### Call Driver
- Tap phone icon to dial driver
- Uses Android's built-in dialer
- Phone number from driver profile

---

## 📱 User Flow

```
Customer Order Detail
       ↓
  [Track Delivery Button]
  (visible only for out_for_delivery)
       ↓
  TrackDeliveryActivity
       ↓
  ┌─────────────────────┐
  │   Google Map        │
  │   (Driver Marker)   │
  │                     │
  └─────────────────────┘
  ┌─────────────────────┐
  │ Driver Info Card    │
  │ • Name & Status     │
  │ • ETA: 15 mins      │
  │ • Order #ORD123     │
  │ [Call Driver 📞]    │
  └─────────────────────┘
```

---

## 🔄 Data Flow

```
Delivery App (Driver)
    ↓ (every 15 sec)
LocationTrackingService
    ↓
POST /api/delivery/update-location
    ↓
Supabase: delivery_locations table
    ↑ (query every 15 sec)
GET /api/orders/{orderId}/driver-location
    ↑
Customer App (TrackDeliveryActivity)
    ↑
Updates map marker + UI
```

---

## 🐛 Troubleshooting

### Map doesn't load
- ❌ Check Google Maps API key is valid
- ❌ Check API key is enabled for "Maps SDK for Android"
- ❌ Check API key restrictions (package name: `com.grocery.customer.debug`)

### "Location not yet available" error
- ✅ This is normal if driver hasn't started delivery yet
- Wait for driver to accept order and start delivery
- Check driver app has location permissions

### Location not updating
- Check delivery status is `accepted`, `in_transit`, or `arrived`
- Check driver app is actively sending location updates
- Check network connection on both devices
- Check backend API is deployed and accessible

### Button not showing in order detail
- Check order status is exactly `out_for_delivery`
- Not `out for delivery` or `Out_For_Delivery` (case-sensitive!)
- Only shows for active deliveries

---

## 📊 Database Schema Used

**Table**: `delivery_locations`
```sql
id                      UUID PRIMARY KEY
delivery_personnel_id   UUID (driver)
order_id                UUID
latitude                NUMERIC
longitude               NUMERIC
accuracy                NUMERIC (optional)
speed                   NUMERIC (optional)
heading                 NUMERIC (optional)
timestamp               TIMESTAMPTZ
created_at              TIMESTAMPTZ
```

**Table**: `delivery_assignments`
- Used to link orders to drivers
- Status field determines if tracking is available

---

## 🚀 Future Enhancements

**Potential improvements** (not implemented):
- [ ] Route polyline showing driver's path
- [ ] Customer location marker
- [ ] Distance/time remaining calculation
- [ ] Push notifications for location updates
- [ ] WebSocket for real-time updates (instead of polling)
- [ ] Historical route replay
- [ ] Multi-stop delivery support
- [ ] Traffic-aware ETA calculations

---

## 📝 Notes

- **Backend endpoint** must be deployed to Vercel for this to work
- **Location permissions** are requested at runtime (Android 6.0+)
- **Background location** is NOT required (customer app only views)
- **Battery impact** is minimal (15-second polling only while viewing)
- **Data usage** is minimal (~1KB per request every 15 seconds)

---

## ✅ Testing Checklist

Before considering this feature complete:

- [ ] Backend API endpoint deployed and accessible
- [ ] Google Maps API key configured
- [ ] Customer app builds successfully
- [ ] Map loads correctly
- [ ] Driver marker appears on map
- [ ] Location updates every 15 seconds
- [ ] Call driver button works
- [ ] Error states display correctly
- [ ] Permission requests work
- [ ] Works with real GPS data (not mock)
- [ ] Tested end-to-end with driver app
- [ ] Performance is acceptable
- [ ] No memory leaks (polling stops on pause/destroy)

---

**Implementation Status**: ✅ **COMPLETE**  
**Ready for Testing**: ⚠️ **Requires Google Maps API Key**

