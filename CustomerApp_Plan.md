# Customer App — Plan & Status (Updated)

Updated: 2025-10-19 11:29 UTC
Owner: Mobile (Android Kotlin) + Backend (Supabase + Vercel)

## 1) Overview & Launcher
- Launcher Activity: SplashActivity
- Routing from Splash:
  - First run → OnboardingActivity (persist seen_onboarding via DataStore)
  - Else if authenticated → MainActivity
  - Else → LoginActivity

## 2) Scope (2–3 weeks)
- Customer onboarding
- Main navigation (Home, Categories, Cart, Profile)
- Home (featured products), Categories, Cart (scaffold), Profile (view/update, logout)
- Backend on Supabase + Vercel (Pages Router)
- QA, observability, and security to production standards

## 3) Backend Architecture (Supabase + Vercel)
### 3.1 Supabase Data Model (DONE)
- Tables:
  - product_categories(id uuid pk, name, description, parent_id, is_active bool, created_at)
  - products(id uuid pk, name, category_id fk, price numeric(10,2), description, image_url, featured bool, is_active bool, created_at, updated_at)
  - inventory(product_id pk/fk, stock int, updated_at)
- Indexes:
  - product_categories(name)
  - products(category_id, featured, is_active, created_at desc)
  - products(lower(name))
  - inventory(product_id) via PK
- Triggers:
  - set_updated_at() on products, inventory
- Storage:
  - Bucket product-images (public read)
- RLS:
  - Public/Auth: SELECT only active rows on product_categories, products
  - Admin writes via service_role; no anon/auth INSERT/UPDATE/DELETE
- Migration applied in Supabase: customer_products_schema_v1
- Status: ✅ completed and verified

### 3.2 Vercel Project Status (Current)
- Project: kotlin-project (Next.js, Node 22.x)
- Project ID: prj_qvF21NdJG0FC1UeAVUKrywJZ4ne7; Team: project3-f5839d18
- Domains: kotlin-project.vercel.app, kotlin-project-project3-f5839d18.vercel.app, kotlin-project-git-main-project3-f5839d18.vercel.app
- Latest production deployment: READY (commit e28a86c on main, region iad1)
- Base URL: https://kotlin-project.vercel.app/
- Build: Next.js 15.5.6 (Turbopack), no build errors (only ESLint warnings)

### 3.3 Current API Implementation (grocery-delivery-api)
- Repo path: grocery-delivery-api (Pages Router)
- Implemented endpoints:
  - POST /api/auth/login
  - POST /api/auth/register
  - POST /api/auth/resend-verification
  - POST /api/auth/verify
  - GET  /api/health
  - GET  /api/products/categories
  - GET  /api/products/list
  - GET  /api/products/[id]
  - GET/PUT /api/users/profile
- Middleware: CORS enabled (Android-friendly)
- Response shape utilities: formatSuccessResponse/formatErrorResponse
- Supabase clients: service role (admin), anon, getAuthenticatedClient(accessToken)
- Tests: auth end-to-end test runner (normal, bypass, errors)
- Not implemented yet: POST /api/images/sign (optional); extend tests for products/profile; Postman/OpenAPI

### 3.4 Remaining API Work (Pages Router, grocery-delivery-api)
- Endpoints:
  - POST /api/images/sign (optional signed upload)
- Testing/Docs:
  - Extend tests for products/profile; publish Postman/OpenAPI collection
- Consistent response: reuse formatSuccessResponse/formatErrorResponse
- ENV per env: SUPABASE_URL, SUPABASE_ANON_KEY, SUPABASE_SERVICE_ROLE_KEY, JWT_SECRET
- Flags: ALLOW_UNVERIFIED_LOGIN=true only in development (never in production)

## 4) Android App Implementation
### 4.1 Navigation & Structure
- MainActivity hosts NavHostFragment + BottomNavigationView (Home, Categories, Cart, Profile)
- nav_graph.xml with HomeFragment, CategoriesFragment, CartFragment, ProfileFragment

### 4.2 Onboarding
- OnboardingActivity using ViewPager2 (2–3 slides + dots)
- Persist seen_onboarding via DataStore
- Splash routes: first-run → Onboarding → Login/Main depending on token

### 4.3 Profile
- ProfileFragment shows email/full name/role from /api/users/profile
- Edit name/phone (PUT)
- Logout clears TokenStore → Login

### 4.4 Home & Categories
- HomeFragment: RecyclerView grid of featured products with loading/empty/error states
- CategoriesFragment: list of categories; tap filters products or navigates to ProductsByCategoryFragment

### 4.5 Data Layer
- Retrofit services: ProductsApi, UsersApi
- Repositories: ProductRepository, UserRepository
- DI via Hilt; ViewModel state with Flow<Resource<…>>
- Caching: in-memory + optional Room short TTL cache

### 4.6 Cart (Scaffold)
- Local Room CartItem(productId, name, price, qty, imageUrl, updatedAt)
- CartRepository add/update/remove, totals
- CartFragment UI with qty steppers & subtotal; checkout disabled (placeholder)

## 5) QA / Observability / Security
- Postman/OpenAPI collection for /products and /users/profile
- Unit tests for repositories/ViewModels; basic instrumented smoke (launch, bottom-nav, lists)
- Logging: Timber; HTTP logging debug-only
- API request logs; basic rate limiting on search/auth
- Vercel test runner present for auth; extend for products/profile

## 6) Timeline (parallelizable)
- Day 1–2: Supabase DDL/RLS ✅; Products/profile endpoints + CORS ✅ (branch feature/backend/products-api, PR open); Nav + Onboarding scaffolding (Splash as launcher)
- Day 2–3: Wire Android repositories/DI; Home (featured) + Categories using new endpoints
- Day 3–4: Profile GET/PUT + Logout in app; Cart local scaffold
- Day 4–5: Tests, Postman, polish, docs

## 7) Acceptance Criteria Mapping
- CUST-001 Onboarding: ViewPager2 flow; DataStore flag; Splash routing
- CUST-002 Navigation: Bottom nav + 4 fragments; correct back-stack
- CUST-003 Auth: Login/register done (API present); Profile logout works; Splash → Main when authenticated
- CUST-004 Home: Featured list loads from API with states; tap → detail (stub ok)
- CUST-005 Profile: Shows/updates user info via API; logout

## 8) Status Summary (Today)
- ✅ Supabase schema created: product_categories, products, inventory (indexes, triggers, RLS)
- ✅ Vercel project live (Next.js Pages) with auth endpoints and health; auth E2E tests exist
- ✅ Products endpoints implemented (/api/products/*) — branch feature/backend/products-api (PR pending)
- ✅ Users profile endpoint implemented (/api/users/profile) — PR pending
- ✅ CORS middleware enabled
- ⏳ Seed sample data (categories/products/inventory)
- ⏳ Postman/OpenAPI collection for products/profile
- ⏳ Android app scaffolding (Navigation, Onboarding, Main)

## 9) Next Actions
1) Open/review PR feature/backend/products-api, verify Vercel preview and logs; merge to develop.
2) Seed Supabase with sample categories/products/inventory.
3) Add tests for products/profile and publish Postman/OpenAPI collection.
4) Android: add Navigation Component, fragments, Splash/Onboarding routing, Hilt setup.
5) Implement Retrofit services + repositories; wire Home/Categories/Profile.
6) Configure/confirm Vercel env vars; ensure ALLOW_UNVERIFIED_LOGIN only in dev.

## 10) Risks & Mitigations
- Auth/RLS alignment: use authenticated client for user-owned reads; service role for profile writes
- CORS: ensure Android clients can call endpoints (preflight handled)
- Pagination/search performance: server-side pagination + client caching
- Image delivery: public bucket now; add signed uploads for admin later
