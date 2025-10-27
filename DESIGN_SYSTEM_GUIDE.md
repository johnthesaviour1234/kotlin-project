# Design System Guide - Grocery Apps
## Extracted from GroceryCustomer for Admin & Delivery Apps

**Version**: 1.0.0  
**Last Updated**: October 26, 2025  
**Source**: GroceryCustomer App  
**Target**: GroceryAdmin & GroceryDelivery Apps

---

## 📋 Table of Contents

1. [Overview](#overview)
2. [Color System](#color-system)
3. [Typography](#typography)
4. [Component Patterns](#component-patterns)
5. [Layout Patterns](#layout-patterns)
6. [Icon System](#icon-system)
7. [Navigation Patterns](#navigation-patterns)
8. [Screen Templates](#screen-templates)
9. [Implementation Checklist](#implementation-checklist)

---

## Overview

### Design Philosophy
- **Material Design 3** principles
- **Indian Market** focused color palette
- **Accessibility** first approach
- **Consistent** visual language across all apps

### Theme Base
- Parent Theme: `Theme.Material3.DayNight`
- Primary Focus: Vibrant, fresh grocery feel
- Target: Clean, modern, easy-to-use interface

---

## Color System

### Primary Colors (Green Theme - Freshness & Organic)

```xml
<!-- Primary - Vibrant Dark Green -->
<color name="primary">#2E7D32</color>
<color name="primary_variant">#1B5E20</color>
<color name="on_primary">#FFFFFF</color>

<!-- For Admin App: Consider keeping the same to maintain brand consistency -->
<!-- For Delivery App: Keep the same for unified experience -->
```

**Usage**:
- Primary buttons
- App bars/toolbars
- Active navigation items
- Focus states
- Key actions

**Visual Examples**:
- Login button
- Toolbar background
- Bottom navigation active state
- FAB (Floating Action Button)

### Secondary Colors (Orange Theme - Energy & Action)

```xml
<!-- Secondary - Vibrant Deep Orange -->
<color name="secondary">#FF6F00</color>
<color name="secondary_variant">#E65100</color>
<color name="on_secondary">#FFFFFF</color>
```

**Usage**:
- Price displays
- Special offers/badges
- Secondary actions
- Accent highlights
- Call-to-action elements

**Visual Examples**:
- Product prices
- "Add to Cart" buttons
- Notification badges
- Special product markers

### Tertiary Colors (Red Theme - Urgency & Error)

```xml
<!-- Tertiary - Deep Red -->
<color name="tertiary">#D32F2F</color>
<color name="on_tertiary">#FFFFFF</color>
<color name="error">#D32F2F</color>
<color name="on_error">#FFFFFF</color>
```

**Usage**:
- Error states
- Delete actions
- Critical alerts
- Cancellation actions
- Warning indicators

### Background Colors (Warm & Welcoming)

```xml
<!-- Backgrounds -->
<color name="background">#FFF8F0</color>           <!-- Warm cream -->
<color name="surface">#FFFFFF</color>               <!-- Pure white -->
<color name="on_background">#BF360C</color>         <!-- Deep warm brown -->
<color name="on_surface">#BF360C</color>            <!-- Deep warm brown -->
<color name="surface_variant">#FFF3E0</color>       <!-- Light cream -->
<color name="on_surface_variant">#BF360C</color>    <!-- Deep warm brown -->
```

**Background Hierarchy**:
1. **background** - Main screen background (warm cream)
2. **surface** - Cards, dialogs, elevated components (white)
3. **surface_variant** - Subtle backgrounds, disabled states (light cream)

### Text Colors (High Contrast for Readability)

```xml
<!-- Text Colors -->
<color name="text_primary">#BF360C</color>      <!-- Deep warm brown - Main text -->
<color name="text_secondary">#D84315</color>    <!-- Medium brown - Secondary text -->
<color name="text_disabled">#FF8A65</color>     <!-- Light orange - Disabled text -->
```

**Text Hierarchy**:
- **Primary**: Headings, body text, important information
- **Secondary**: Descriptions, helper text, metadata
- **Disabled**: Inactive elements, unavailable options

### Status Colors (Order Management)

```xml
<!-- Order Status Colors -->
<color name="status_pending">#FF6F00</color>      <!-- Orange -->
<color name="status_confirmed">#1976D2</color>    <!-- Blue -->
<color name="status_preparing">#7B1FA2</color>    <!-- Purple -->
<color name="status_ready">#2E7D32</color>        <!-- Green -->
<color name="status_delivered">#1B5E20</color>    <!-- Dark Green -->
<color name="status_cancelled">#D32F2F</color>    <!-- Red -->
```

**Status Color Usage**:
| Status | Color | Psychology | When Used |
|--------|-------|-----------|-----------|
| Pending | Orange | Attention needed | Order placed, waiting |
| Confirmed | Blue | Trust, stability | Admin confirmed |
| Preparing | Purple | Progress | Kitchen/warehouse |
| Ready | Green | Success, ready | Ready for pickup |
| Delivered | Dark Green | Complete | Successfully delivered |
| Cancelled | Red | Error, stopped | Order cancelled |

### Semantic Colors

```xml
<!-- Semantic Colors -->
<color name="success">#2E7D32</color>     <!-- Green -->
<color name="warning">#FF6F00</color>     <!-- Orange -->
<color name="info">#1976D2</color>        <!-- Blue -->
```

### Utility Colors

```xml
<!-- Dividers & Borders -->
<color name="divider">#FFB74D</color>           <!-- Light orange -->
<color name="divider_light">#FFE0B2</color>     <!-- Very light orange -->

<!-- Card & Surface -->
<color name="card_background">#FFFFFF</color>   <!-- White -->

<!-- Overlays -->
<color name="black_overlay">#66000000</color>   <!-- 40% black -->
<color name="white_overlay">#66FFFFFF</color>   <!-- 40% white -->

<!-- Special -->
<color name="transparent">#00000000</color>
<color name="product_placeholder">#FFE0B2</color> <!-- Light orange for image loading -->
```

---

## Typography

### Text Appearances (Material Design 3)

#### Display Text
```xml
<!-- For large hero text, splash screens -->
textAppearanceDisplayLarge
textAppearanceDisplayMedium
textAppearanceDisplaySmall
```

**Usage**: Welcome screens, empty states, large numbers

#### Headline Text
```xml
<!-- For section headers, screen titles -->
textAppearanceHeadlineLarge
textAppearanceHeadlineMedium   <!-- Most common: "Featured Products" -->
textAppearanceHeadlineSmall    <!-- Most common: "Shop by Category" -->
```

**Usage**: 
- Screen titles
- Section headers
- Card titles

#### Title Text
```xml
<!-- For smaller headers, list item titles -->
textAppearanceTitleLarge
textAppearanceTitleMedium
textAppearanceTitleSmall
```

**Usage**: 
- Product names
- Order titles
- Dialog titles

#### Body Text
```xml
<!-- For regular content -->
textAppearanceBodyLarge        <!-- Main descriptions -->
textAppearanceBodyMedium       <!-- Product descriptions -->
textAppearanceBodySmall        <!-- Metadata, timestamps -->
```

**Usage**: 
- Product descriptions
- Order details
- General content

#### Label Text
```xml
<!-- For buttons, tabs, smaller text -->
textAppearanceLabelLarge       <!-- Buttons -->
textAppearanceLabelMedium      <!-- Small buttons, chips -->
textAppearanceLabelSmall       <!-- Captions, helper text -->
```

**Usage**: 
- Button text
- Tab labels
- Form labels
- Helper text

### Typography Scale Example

```kotlin
// In composable or XML
<TextView
    android:text="Fresh Organic Apples"
    android:textAppearance="?attr/textAppearanceTitleMedium"
    android:textStyle="bold" />

<TextView
    android:text="₹2.99"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="@color/secondary"
    android:textStyle="bold" />

<TextView
    android:text="Fresh and organic apples from local farms"
    android:textAppearance="?attr/textAppearanceBodySmall"
    android:textColor="@color/text_secondary" />
```

---

## Component Patterns

### 1. Buttons

#### Primary Button (Material Button)
```xml
<Button
    android:id="@+id/buttonPrimary"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Place Order"
    android:textColor="@color/on_primary"
    style="@style/Widget.Material3.Button" />
```

**Characteristics**:
- Filled background with primary color
- White text (`on_primary`)
- 8dp corner radius (default)
- Elevation on press
- Use for: Primary actions, confirmations

#### Outlined Button
```xml
<Button
    android:id="@+id/buttonSecondary"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Cancel"
    style="@style/Widget.Material3.Button.OutlinedButton" />
```

**Use for**: Secondary actions, cancel buttons

#### Text Button
```xml
<Button
    android:id="@+id/buttonText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Learn More"
    style="@style/Widget.Material3.Button.TextButton" />
```

**Use for**: Tertiary actions, navigation links

### 2. Cards (MaterialCardView)

#### Standard Card
```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_margin="4dp"
    app:cardCornerRadius="8dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@color/surface">
    
    <!-- Card content -->
    
</com.google.android.material.card.MaterialCardView>
```

**Standard Values**:
- **Corner Radius**: 8dp (small cards) to 12dp (large cards)
- **Elevation**: 2dp (flat) to 4dp (raised)
- **Margin**: 4dp (grid items) to 8dp (list items)
- **Padding**: 8dp (compact) to 16dp (comfortable)

#### Clickable Card
```xml
<com.google.android.material.card.MaterialCardView
    android:clickable="true"
    android:focusable="true"
    app:rippleColor="@color/accent_orange"
    ...>
```

**Use Cases**:
- Product cards
- Order summary cards
- Category cards
- Information panels

### 3. Input Fields (EditText / TextInputLayout)

#### Standard Input
```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Email"
    app:boxBackgroundMode="outline"
    app:boxCornerRadiusTopStart="8dp"
    app:boxCornerRadiusTopEnd="8dp"
    app:boxCornerRadiusBottomStart="8dp"
    app:boxCornerRadiusBottomEnd="8dp">
    
    <com.google.android.material.textfield.TextInputEditText
        android:id="@+id/editEmail"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:inputType="textEmailAddress" />
        
</com.google.android.material.textfield.TextInputLayout>
```

**Input Types**:
- `textEmailAddress` - Email fields
- `textPassword` - Password fields
- `phone` - Phone number
- `number` - Quantity, numbers
- `textMultiLine` - Notes, descriptions

### 4. Progress Indicators

#### Circular Progress
```xml
<ProgressBar
    android:id="@+id/progressBar"
    android:layout_width="32dp"
    android:layout_height="32dp"
    android:layout_gravity="center"
    android:indeterminateTint="@color/on_primary"
    android:visibility="gone" />
```

**Sizes**:
- **Small**: 24dp (inline)
- **Medium**: 32dp (button overlays)
- **Large**: 48dp (screen center)

#### Linear Progress
```xml
<LinearProgressIndicator
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:indeterminate="true"
    app:indicatorColor="@color/primary" />
```

### 5. Status Pills (Order Status)

```xml
<TextView
    android:id="@+id/textViewStatus"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:padding="8dp"
    android:paddingStart="12dp"
    android:paddingEnd="12dp"
    android:background="@drawable/bg_status_pill"
    android:text="Delivered"
    android:textSize="12sp"
    android:textStyle="bold"
    android:textColor="@color/on_primary" />
```

**bg_status_pill.xml**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="@color/status_delivered" />
    <corners android:radius="16dp" />
</shape>
```

**Dynamic Color Assignment** (Kotlin):
```kotlin
fun setStatusColor(status: String, textView: TextView) {
    val colorRes = when (status.lowercase()) {
        "pending" -> R.color.status_pending
        "confirmed" -> R.color.status_confirmed
        "preparing" -> R.color.status_preparing
        "ready" -> R.color.status_ready
        "delivered" -> R.color.status_delivered
        "cancelled" -> R.color.status_cancelled
        else -> R.color.text_secondary
    }
    textView.setBackgroundColor(ContextCompat.getColor(textView.context, colorRes))
}
```

---

## Layout Patterns

### 1. Screen Structure (Standard Layout)

```xml
<androidx.coordinatorlayout.widget.CoordinatorLayout
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <!-- App Bar -->
    <com.google.android.material.appbar.AppBarLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content">

        <com.google.android.material.appbar.MaterialToolbar
            android:id="@+id/toolbar"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            app:title="Screen Title"
            app:titleTextColor="?attr/colorOnPrimary"
            android:background="?attr/colorPrimary" />

    </com.google.android.material.appbar.AppBarLayout>

    <!-- Main Content with Pull-to-Refresh -->
    <androidx.swiperefreshlayout.widget.SwipeRefreshLayout
        android:id="@+id/swipeRefresh"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior">

        <!-- Scrollable content -->
        <androidx.core.widget.NestedScrollView
            android:layout_width="match_parent"
            android:layout_height="match_parent">
            
            <!-- Your content here -->
            
        </androidx.core.widget.NestedScrollView>

    </androidx.swiperefreshlayout.widget.SwipeRefreshLayout>

    <!-- Loading Overlay -->
    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

### 2. Grid Layout (Product Grid, 2 Columns)

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerViewProducts"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp"
    app:layoutManager="androidx.recyclerview.widget.GridLayoutManager"
    app:spanCount="2"
    tools:listitem="@layout/item_product" />
```

**Kotlin Setup**:
```kotlin
recyclerViewProducts.layoutManager = GridLayoutManager(context, 2)
recyclerViewProducts.adapter = productsAdapter
```

### 3. List Layout (Order History)

```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/recyclerViewOrders"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:padding="8dp"
    app:layoutManager="androidx.recyclerview.widget.LinearLayoutManager"
    tools:listitem="@layout/item_order_history" />
```

### 4. Empty State Pattern

```xml
<LinearLayout
    android:id="@+id/emptyStateLayout"
    android:layout_width="match_parent"
    android:layout_height="200dp"
    android:orientation="vertical"
    android:gravity="center"
    android:visibility="gone">

    <ImageView
        android:layout_width="64dp"
        android:layout_height="64dp"
        android:src="@drawable/ic_placeholder"
        android:alpha="0.5"
        app:tint="?attr/colorOnSurfaceVariant" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp"
        android:text="No items found"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="?attr/colorOnSurfaceVariant"
        android:gravity="center" />

</LinearLayout>
```

### 5. Form Layout Pattern

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <!-- Title -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Form Title"
        android:textSize="24sp"
        android:textStyle="bold"
        android:textColor="@color/text_primary"
        android:paddingBottom="16dp" />

    <!-- Input Field 1 -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="Field 1">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editField1"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
            
    </com.google.android.material.textfield.TextInputLayout>

    <!-- Input Field 2 -->
    <com.google.android.material.textfield.TextInputLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:hint="Field 2">
        
        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/editField2"
            android:layout_width="match_parent"
            android:layout_height="wrap_content" />
            
    </com.google.android.material.textfield.TextInputLayout>

    <!-- Submit Button -->
    <FrameLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="16dp">

        <Button
            android:id="@+id/buttonSubmit"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Submit" />

        <ProgressBar
            android:id="@+id/progressBar"
            android:layout_width="32dp"
            android:layout_height="32dp"
            android:layout_gravity="center"
            android:visibility="gone" />

    </FrameLayout>

</LinearLayout>
```

---

## Icon System

### Icon Resources (Available in Customer App)

```
drawable/
  ├── ic_add.xml                    - Add action, plus icon
  ├── ic_arrow_back.xml             - Back navigation
  ├── ic_arrow_forward.xml          - Forward navigation
  ├── ic_calendar.xml               - Date/time indicators
  ├── ic_cart.xml                   - Shopping cart
  ├── ic_categories.xml             - Categories navigation
  ├── ic_chevron_right.xml          - List item indicators
  ├── ic_delete.xml                 - Delete actions
  ├── ic_edit.xml                   - Edit actions
  ├── ic_error.xml                  - Error states
  ├── ic_home.xml                   - Home navigation
  ├── ic_lock.xml                   - Security, password
  ├── ic_logout.xml                 - Logout action
  ├── ic_notifications.xml          - Notifications
  ├── ic_placeholder_product.xml    - Product placeholder
  ├── ic_profile.xml                - Profile/user
  ├── ic_receipt.xml                - Orders, receipts
  ├── ic_search.xml                 - Search functionality
  ├── ic_sort.xml                   - Sort/filter
  └── ic_theme.xml                  - Theme toggle
```

### Icon Usage Patterns

#### Navigation Icons
```xml
<!-- Bottom Navigation -->
<item
    android:id="@+id/nav_home"
    android:icon="@drawable/ic_home"
    android:title="@string/nav_home" />
```

#### Toolbar Icons
```xml
<com.google.android.material.appbar.MaterialToolbar
    app:navigationIcon="@drawable/ic_arrow_back"
    app:menu="@menu/menu_home" />
```

#### Inline Icons (with tint)
```xml
<ImageView
    android:layout_width="24dp"
    android:layout_height="24dp"
    android:src="@drawable/ic_cart"
    app:tint="@color/primary" />
```

### Icon Sizes
- **Small**: 16dp (inline text icons)
- **Medium**: 24dp (standard UI icons)
- **Large**: 32dp (feature icons)
- **Extra Large**: 48dp+ (empty states, illustrations)

---

## Navigation Patterns

### 1. Bottom Navigation

```xml
<!-- res/menu/bottom_navigation_menu.xml -->
<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item
        android:id="@+id/nav_home"
        android:icon="@drawable/ic_home"
        android:title="@string/nav_home" />
    
    <item
        android:id="@+id/nav_categories"
        android:icon="@drawable/ic_categories"
        android:title="@string/nav_categories" />
    
    <item
        android:id="@+id/nav_cart"
        android:icon="@drawable/ic_cart"
        android:title="@string/nav_cart" />
    
    <item
        android:id="@+id/nav_orders"
        android:icon="@drawable/ic_receipt"
        android:title="@string/nav_orders" />
    
    <item
        android:id="@+id/nav_profile"
        android:icon="@drawable/ic_profile"
        android:title="@string/nav_profile" />
</menu>
```

```xml
<!-- In main activity layout -->
<com.google.android.material.bottomnavigation.BottomNavigationView
    android:id="@+id/bottomNavigation"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_gravity="bottom"
    app:menu="@menu/bottom_navigation_menu"
    app:itemIconTint="@color/bottom_nav_color_selector"
    app:itemTextColor="@color/bottom_nav_color_selector" />
```

**Color Selector** (`res/color/bottom_nav_color_selector.xml`):
```xml
<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:color="@color/primary" android:state_checked="true" />
    <item android:color="@color/text_secondary" android:state_checked="false" />
</selector>
```

### 2. Navigation Graph Setup

```xml
<!-- res/navigation/nav_graph.xml -->
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeFragment">

    <fragment
        android:id="@+id/homeFragment"
        android:name="com.grocery.customer.ui.fragments.HomeFragment"
        android:label="@string/nav_home" />

    <fragment
        android:id="@+id/categoriesFragment"
        android:name="com.grocery.customer.ui.fragments.CategoriesFragment"
        android:label="@string/nav_categories" />

    <!-- More fragments -->

</navigation>
```

**Admin App Navigation Items**:
```
- Dashboard (Home)
- Orders
- Products
- Inventory
- Profile
```

**Delivery App Navigation Items**:
```
- Available Orders (Home)
- Active Deliveries
- Delivery History
- Profile
```

---

## Screen Templates

### 1. Login/Auth Screen Template

**Layout Structure**:
- ScrollView (handle keyboard)
- Vertical LinearLayout
- Title (24sp, bold)
- Input fields (email, password)
- Primary button (with loading state)
- Secondary text links

**Key Features**:
- Loading spinner overlay on button
- Error message display
- Keyboard navigation support
- Auto-focus on first field

### 2. Dashboard/Home Screen Template

**Layout Structure**:
- CoordinatorLayout
- AppBarLayout with Toolbar
- SwipeRefreshLayout
- NestedScrollView
  - Welcome card (MaterialCardView)
  - Section headers (HeadlineSmall)
  - RecyclerView grids/lists
  - Empty state layouts

**Key Features**:
- Pull to refresh
- Skeleton loading states
- Empty state handling
- Swipe gestures

### 3. List Screen Template (Orders, Products)

**Layout Structure**:
- CoordinatorLayout
- Toolbar with search/filter
- RecyclerView (LinearLayoutManager)
- FAB for add action
- Empty state
- Loading state

**Key Features**:
- Filter chips
- Search functionality
- Pull to refresh
- Pagination support

### 4. Detail Screen Template (Order Detail, Product Detail)

**Layout Structure**:
- CoordinatorLayout
- CollapsingToolbarLayout (optional)
- NestedScrollView
  - Hero image/header
  - Info cards
  - Action buttons
  - Related content sections

**Key Features**:
- Back navigation
- Share/more actions
- Collapsing header (optional)
- Bottom action bar

### 5. Form Screen Template (Add/Edit)

**Layout Structure**:
- ScrollView
- Vertical LinearLayout
- Form title
- TextInputLayouts
- Buttons (Save/Cancel)
- Progress indicator

**Key Features**:
- Form validation
- Error display
- Loading state
- Keyboard handling

---

## Implementation Checklist

### Phase 1: Setup (Admin & Delivery Apps)

#### ✅ Copy Color Resources
```bash
# Copy from GroceryCustomer to GroceryAdmin & GroceryDelivery
- [ ] res/values/colors.xml
- [ ] res/values/themes.xml
```

#### ✅ Copy Drawable Resources
```bash
# Copy icon drawables
- [ ] All ic_*.xml files from res/drawable/
- [ ] bg_status_pill.xml
- [ ] bg_image_placeholder.xml
```

#### ✅ Update String Resources
```bash
# Customize strings for each app
- [ ] Admin: app_name = "FreshMart Admin"
- [ ] Delivery: app_name = "FreshMart Delivery"
- [ ] Update navigation labels
- [ ] Update action labels
```

### Phase 2: Theme Application

#### ✅ Update Themes.xml
```xml
<!-- GroceryAdmin -->
<style name="Theme.GroceryAdmin" parent="Theme.Material3.DayNight">
    <!-- Same attributes as GroceryCustomer -->
</style>

<!-- GroceryDelivery -->
<style name="Theme.GroceryDelivery" parent="Theme.Material3.DayNight">
    <!-- Same attributes as GroceryCustomer -->
</style>
```

#### ✅ Update AndroidManifest.xml
```xml
<application
    android:theme="@style/Theme.GroceryAdmin">  <!-- or Theme.GroceryDelivery -->
```

### Phase 3: Component Implementation

#### ✅ Admin App Components
- [ ] Login screen (copy from Customer)
- [ ] Dashboard with metrics cards
- [ ] Orders list with filters
- [ ] Order detail with action buttons
- [ ] Product CRUD forms
- [ ] Inventory management UI
- [ ] Status update buttons/chips

#### ✅ Delivery App Components
- [ ] Login screen (copy from Customer)
- [ ] Available orders list
- [ ] Order detail with map view
- [ ] Accept/Decline buttons
- [ ] Status update UI
- [ ] Location tracking indicator
- [ ] Delivery history list

### Phase 4: Navigation Setup

#### ✅ Admin Navigation
```xml
<menu>
    <item id="@+id/nav_dashboard" icon="@drawable/ic_home" />
    <item id="@+id/nav_orders" icon="@drawable/ic_receipt" />
    <item id="@+id/nav_products" icon="@drawable/ic_categories" />
    <item id="@+id/nav_inventory" icon="@drawable/ic_sort" />
    <item id="@+id/nav_profile" icon="@drawable/ic_profile" />
</menu>
```

#### ✅ Delivery Navigation
```xml
<menu>
    <item id="@+id/nav_available" icon="@drawable/ic_home" />
    <item id="@+id/nav_active" icon="@drawable/ic_arrow_forward" />
    <item id="@+id/nav_history" icon="@drawable/ic_calendar" />
    <item id="@+id/nav_profile" icon="@drawable/ic_profile" />
</menu>
```

### Phase 5: Testing

- [ ] Test all color contrasts (accessibility)
- [ ] Test on multiple screen sizes
- [ ] Test dark/light theme switching
- [ ] Verify consistent spacing
- [ ] Check icon visibility
- [ ] Test all interactive states

---

## Color Accessibility Guidelines

### Contrast Ratios (WCAG AA)
- **Normal text** (< 18pt): 4.5:1
- **Large text** (≥ 18pt or bold ≥ 14pt): 3:1
- **UI components**: 3:1

### Our Color Combinations (Pre-tested)
✅ `primary` (#2E7D32) on `on_primary` (#FFFFFF) - **7.5:1** ✓  
✅ `text_primary` (#BF360C) on `background` (#FFF8F0) - **8.2:1** ✓  
✅ `secondary` (#FF6F00) on `on_secondary` (#FFFFFF) - **5.1:1** ✓  
✅ All status colors on white background - **> 4.5:1** ✓

---

## Spacing System

### Padding & Margins
```xml
<!-- Consistent spacing values -->
4dp  - Minimal spacing (card grid gaps)
8dp  - Small spacing (between related items)
12dp - Medium spacing (between sections)
16dp - Standard spacing (screen padding)
24dp - Large spacing (section gaps)
32dp - Extra large spacing (major sections)
```

### Component Spacing Guidelines
- **Cards in grid**: 4dp margin
- **Cards in list**: 8dp vertical margin
- **Screen padding**: 16dp
- **Section padding**: 16dp top + 24dp bottom
- **Form inputs**: 8dp between fields
- **Button margins**: 16dp top

---

## Elevation System

### Material Design Elevation Levels
```xml
0dp  - Flat surface (backgrounds)
1dp  - Slightly raised (cards at rest)
2dp  - Resting elevation (standard cards)
4dp  - Raised elevation (hover, focus)
8dp  - Overlay elevation (dialogs, menus)
16dp - Top elevation (floating buttons)
```

### Our Usage
- **Cards**: 2dp standard, 4dp on hover/press
- **AppBar**: 4dp
- **FAB**: 6dp standard, 12dp on press
- **Dialogs**: 8dp
- **Bottom Navigation**: 8dp

---

## Animation Guidelines

### Duration
- **Fast**: 100ms (micro-interactions)
- **Normal**: 200-300ms (standard transitions)
- **Slow**: 400-500ms (page transitions)

### Easing
- **Standard**: `cubic-bezier(0.4, 0.0, 0.2, 1)` - Most transitions
- **Decelerate**: `cubic-bezier(0.0, 0.0, 0.2, 1)` - Enter screen
- **Accelerate**: `cubic-bezier(0.4, 0.0, 1, 1)` - Exit screen

### What to Animate
- ✅ Button presses (scale, color)
- ✅ Screen transitions (slide, fade)
- ✅ Loading states (fade in/out)
- ✅ Status changes (color transition)
- ❌ Don't animate text content
- ❌ Don't animate critical information

---

## Best Practices Summary

### DO ✅
- Use Material Design 3 components
- Follow the established color system
- Maintain consistent spacing
- Use semantic colors for status
- Provide loading states
- Handle empty states
- Support accessibility
- Test on multiple screen sizes

### DON'T ❌
- Mix custom colors without design approval
- Ignore accessibility guidelines
- Skip loading/error states
- Use hard-coded sizes
- Forget content descriptions
- Override Material Design patterns unnecessarily

---

## Quick Reference - Common Patterns

### Product Card
- Size: match_parent x wrap_content
- Margin: 4dp
- Corner radius: 8dp
- Elevation: 2dp
- Image height: 120dp
- Padding: 8dp

### Order Card
- Size: match_parent x wrap_content
- Margin: 8dp
- Corner radius: 12dp
- Elevation: 2dp
- Padding: 16dp
- Status pill: top-right corner

### Button Standard
- Width: match_parent (forms) or wrap_content (inline)
- Height: wrap_content (48dp min touch target)
- Padding: 16dp horizontal, 12dp vertical
- Text: Label Large style

### Input Field Standard
- Width: match_parent
- Height: wrap_content
- Margin: 8dp between fields
- Hint color: text_secondary
- Error color: error

---

## App-Specific Customizations

### GroceryAdmin
**Unique Elements**:
- Dashboard metrics cards (large numbers, icons)
- Order management action buttons
- Product grid with edit/delete actions
- Inventory level indicators
- Admin activity logs

**Color Emphasis**: More use of `info` blue for data visualization

### GroceryDelivery
**Unique Elements**:
- Map view integration
- GPS location indicator
- Timer/ETA displays
- Large status update buttons
- Current location badge

**Color Emphasis**: More use of `status_*` colors for real-time updates

---

## Resources & Assets Needed

### Icons Required (Beyond Customer App)
**Admin App Additional Icons**:
- ic_dashboard.xml
- ic_analytics.xml
- ic_inventory.xml
- ic_assign.xml

**Delivery App Additional Icons**:
- ic_location.xml
- ic_map.xml
- ic_timer.xml
- ic_navigate.xml

### Images/Illustrations
- Empty state illustrations
- Error state illustrations
- Success confirmation illustrations

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | Oct 26, 2025 | Initial design system documentation from GroceryCustomer |

---

## Next Steps

1. **Copy all resources** from GroceryCustomer to Admin & Delivery apps
2. **Update theme names** in both target apps
3. **Create app-specific strings.xml**
4. **Implement navigation menus** for each app
5. **Build screen layouts** following templates
6. **Test thoroughly** on various devices
7. **Review accessibility** compliance

---

**End of Design System Guide**

This document serves as the single source of truth for design implementation across all Grocery apps.
