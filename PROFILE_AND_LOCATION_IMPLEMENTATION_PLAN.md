# Profile and Location Tracking - Complete Implementation Plan

**Date**: October 28, 2025  
**Estimated Time**: 3-4 hours  
**Complexity**: Medium-High

---

## Part 1: Profile Feature (Complete)

### Backend Status
✅ **Already Deployed and Working**
- Endpoint: `GET /api/delivery/profile` - Fetch profile with stats
- Endpoint: `PUT /api/delivery/profile` - Update profile
- Returns: Full profile data + delivery statistics

### Android Components to Build

#### 1. Data Layer
**Files to create/modify:**
- `data/dto/ProfileResponse.kt` - Response models
- `data/api/DeliveryApiService.kt` - Add profile endpoints
- `data/repository/DeliveryRepository.kt` - Add profile methods

**Implementation:**
```kotlin
// Complete DTOs with all fields from backend
data class ProfileResponse(
    val profile: DriverProfile,
    val stats: DeliveryStats
)

data class DriverProfile(
    val id: String,
    val email: String,
    val full_name: String,
    val phone: String,
    val user_type: String,
    val is_active: Boolean,
    val avatar_url: String?,
    val preferences: Map<String, Any>?,
    val created_at: String,
    val updated_at: String
)

data class DeliveryStats(
    val total_deliveries: Int,
    val completed_deliveries: Int,
    val active_deliveries: Int
)

// Repository with proper error handling
suspend fun getProfile(): Flow<Resource<ProfileResponse>>
suspend fun updateProfile(updates: ProfileUpdateRequest): Flow<Resource<DriverProfile>>
```

#### 2. ViewModel
**File:** `ui/viewmodels/ProfileViewModel.kt`

**Features:**
- Load profile data on init
- Validate all inputs before submission
- Handle update with loading/success/error states
- Support for photo upload (if implemented)
- Refresh functionality

#### 3. UI Layer
**File:** `ui/activities/ProfileActivity.kt`  
**Layout:** `res/layout/activity_profile.xml`

**Features:**
- Display current profile information
- Editable fields: Name, Phone
- Non-editable: Email, Member since
- Statistics card showing delivery counts
- Save button with validation
- Loading states during updates
- Error messages with retry
- Success confirmation
- Back navigation

**UI Components:**
- Profile header with avatar (placeholder or real photo)
- Editable text fields with input validation
- Statistics dashboard
- Save/Cancel buttons
- Progress indicators

#### 4. Navigation Integration
- Add menu item in MainActivity drawer/toolbar
- Add intent from main screen to profile

---

## Part 2: Location Tracking Feature (Complete)

### Backend Status
✅ **Already Deployed and Working**
- Endpoint: `POST /api/delivery/update-location`
- Accepts: latitude, longitude, accuracy, speed, heading, order_id
- Stores location history in database

### Critical Requirements

**Real GPS Tracking:**
- Use Google Play Services FusedLocationProviderClient
- Continuous location updates during active delivery
- Foreground service with persistent notification
- Handle battery optimization
- Proper permission flow (runtime + background)

**Production-Ready Features:**
- Start tracking when delivery accepted
- Stop tracking when delivery completed
- Survive app kills with foreground service
- Network failure handling with retry queue
- Location accuracy monitoring
- Battery-efficient update intervals

### Android Components to Build

#### 1. Permissions Layer
**File:** `utils/LocationPermissionManager.kt`

**Features:**
- Check all location permissions (FINE, COARSE, BACKGROUND)
- Request permissions with proper rationale
- Handle permission results
- Guide user to settings if denied permanently
- Android 10+ background permission handling

#### 2. Data Layer
**Files:**
- `data/dto/LocationUpdate.kt` - Location data models
- `data/api/DeliveryApiService.kt` - Add location endpoint
- `data/repository/LocationRepository.kt` - Location updates to backend

**Implementation:**
```kotlin
data class LocationUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Double,
    val speed: Double?,
    val heading: Double?,
    val order_id: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Repository with queue for offline updates
class LocationRepository @Inject constructor(
    private val apiService: DeliveryApiService,
    private val localDb: AppDatabase
) {
    // Send location with retry logic
    suspend fun sendLocationUpdate(update: LocationUpdateRequest)
    
    // Queue for offline mode
    suspend fun queueLocationUpdate(update: LocationUpdateRequest)
    
    // Retry queued updates
    suspend fun retryQueuedUpdates()
}
```

#### 3. Location Service
**File:** `services/LocationTrackingService.kt`

**Features:**
- Foreground service with notification
- FusedLocationProviderClient integration
- Configurable update intervals (15-30 seconds)
- Location accuracy filtering (only high accuracy)
- Battery optimization handling
- Automatic retry on network failure
- Queue updates when offline
- Service lifecycle management

**Notification:**
- Shows "Tracking delivery location"
- Display current location or "Searching for GPS..."
- Action buttons: Stop tracking
- Updates in real-time

#### 4. Location Manager
**File:** `utils/LocationManager.kt`

**Features:**
- Start/Stop tracking wrapper
- Check location services enabled
- Handle permission states
- Coordinate with service
- Expose tracking state as Flow

#### 5. UI Integration

**ActiveDeliveryActivity Updates:**
- Show location tracking status badge
- Display "GPS Active" indicator
- Show last location update time
- Request permissions before starting delivery
- Auto-start tracking on delivery accept
- Auto-stop tracking on delivery complete

**UI Indicators:**
- Green badge: "Location Tracking Active"
- Yellow badge: "Searching for GPS..."
- Red badge: "Location Services Disabled"
- Gray badge: "Permission Denied"

#### 6. Database Layer (Optional Queue)
**File:** `data/local/entity/QueuedLocationUpdate.kt`  
**DAO:** `data/local/dao/LocationUpdateDao.kt`

**Purpose:**
- Store failed location updates
- Retry when network returns
- Prevent data loss

---

## Implementation Order

### Phase 1: Profile Feature (60-90 min)
1. ✅ Create DTOs for profile responses
2. ✅ Add repository methods
3. ✅ Create ProfileViewModel
4. ✅ Design and implement Profile UI
5. ✅ Add navigation from MainActivity
6. ✅ Test complete flow: View → Edit → Save → Verify

### Phase 2: Location Tracking Setup (30 min)
1. ✅ Add Google Play Services dependency
2. ✅ Add location permissions to manifest
3. ✅ Create LocationPermissionManager
4. ✅ Create location DTOs
5. ✅ Add location repository methods

### Phase 3: Location Service (60-90 min)
1. ✅ Create LocationTrackingService (foreground service)
2. ✅ Implement FusedLocationProviderClient integration
3. ✅ Add notification for tracking status
4. ✅ Implement location update sending
5. ✅ Add retry logic for failures
6. ✅ Test service lifecycle

### Phase 4: UI Integration (30 min)
1. ✅ Update ActiveDeliveryActivity
2. ✅ Add location status indicators
3. ✅ Integrate permission flow
4. ✅ Auto-start/stop with delivery state
5. ✅ Test complete delivery flow with tracking

### Phase 5: Testing & Polish (30 min)
1. ✅ Test profile CRUD operations
2. ✅ Test location tracking with real GPS
3. ✅ Test offline mode and retry
4. ✅ Test permission flows (grant, deny, revoke)
5. ✅ Test service survives app kill
6. ✅ Verify backend receives location updates

---

## Quality Checklist

### Profile Feature
- [ ] Can view current profile data
- [ ] Can edit name and phone
- [ ] Validation works (phone format, name length)
- [ ] Save button shows loading state
- [ ] Success message shown after update
- [ ] Error handling with retry option
- [ ] Statistics display correctly
- [ ] Back navigation preserves state
- [ ] Works offline (cached data)

### Location Tracking
- [ ] Permissions requested at appropriate time
- [ ] Background permission explained clearly
- [ ] Foreground service shows notification
- [ ] GPS location acquired and accurate
- [ ] Updates sent every 15-30 seconds
- [ ] Works in background/app killed
- [ ] Stops when delivery completed
- [ ] Handles network failures gracefully
- [ ] Queues updates when offline
- [ ] Retries failed updates
- [ ] Battery efficient (not draining)
- [ ] Location accuracy adequate (< 50m)
- [ ] UI reflects tracking status accurately

---

## Technical Specifications

### Location Tracking Configuration
```kotlin
// Update interval: 15 seconds (good balance)
private const val UPDATE_INTERVAL_MS = 15_000L

// Fastest interval: 10 seconds (don't update faster)
private const val FASTEST_INTERVAL_MS = 10_000L

// Minimum accuracy: 50 meters
private const val MIN_ACCURACY_METERS = 50f

// Priority: High accuracy (GPS)
private const val PRIORITY = Priority.PRIORITY_HIGH_ACCURACY
```

### Notification Configuration
```kotlin
// Notification ID for foreground service
private const val NOTIFICATION_ID = 1001

// Channel ID
private const val CHANNEL_ID = "location_tracking"

// Channel Name
private const val CHANNEL_NAME = "Delivery Location Tracking"
```

### Profile Validation Rules
```kotlin
// Name: 2-100 characters
private const val MIN_NAME_LENGTH = 2
private const val MAX_NAME_LENGTH = 100

// Phone: 10 digits (US format)
private val PHONE_REGEX = Regex("^[0-9]{10}$")
```

---

## Error Handling

### Profile
- Network errors: Show retry button
- Validation errors: Show field-specific messages
- Server errors: Show generic error + support contact
- Timeout: Show "Taking longer than usual" + retry

### Location Tracking
- Permission denied: Guide to settings
- Location services disabled: Prompt to enable
- GPS not available: Show "Searching..." message
- Network failure: Queue updates locally
- Service killed: Auto-restart with foreground service
- Low accuracy: Wait for better signal
- Battery saver mode: Explain reduced accuracy

---

## Database Schema (Local Queue)

```sql
CREATE TABLE queued_location_updates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    accuracy REAL NOT NULL,
    speed REAL,
    heading REAL,
    order_id TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    retry_count INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL
)
```

---

## Testing Plan

### Manual Testing
1. **Profile:**
   - Open profile from menu
   - Edit name and phone
   - Save with valid data → Success
   - Save with invalid data → Error shown
   - Go offline, try to save → Error handled

2. **Location Tracking:**
   - Accept delivery
   - Grant permissions
   - See notification appear
   - Check backend receives updates
   - Go offline → Updates queued
   - Come online → Updates sent
   - Complete delivery → Tracking stops
   - Kill app → Service continues

### Device Testing
- Android 10 (background permission)
- Android 11 (location restrictions)
- Android 12+ (precise location)
- Different GPS conditions (indoor, outdoor)
- Different network conditions (WiFi, LTE, offline)

---

## Documentation Updates

### Files to Update
1. `API_INTEGRATION_GUIDE.md`
   - Add profile endpoints documentation
   - Update location tracking section

2. `DELIVERY_HISTORY_FIX_SUMMARY.md`
   - Add new features summary

3. Create `PROFILE_AND_LOCATION_FEATURES_SUMMARY.md`
   - Complete feature documentation
   - Architecture overview
   - Usage guide

---

## Success Criteria

✅ **Profile Feature Complete When:**
- Driver can view their profile information
- Driver can update name and phone successfully
- Changes persist and reflect in backend
- UI is polished and responsive
- Error handling is comprehensive
- Works as expected in production

✅ **Location Tracking Complete When:**
- GPS tracking starts automatically with delivery
- Location updates sent every 15 seconds
- Foreground service keeps tracking alive
- Backend receives and stores locations
- Tracking stops when delivery completes
- Works offline with queue + retry
- Battery drain is acceptable
- Permissions handled properly
- Production-ready and reliable

---

**Next Step:** Begin implementation with Phase 1 (Profile Feature)
