# Kotlin Coding Standards - Online Grocery System

**Version**: 1.0  
**Created**: October 17, 2025  
**Applies to**: All three Android applications (Customer, Admin, Delivery)

---

## 📝 **General Principles**

1. **Readability First**: Code should be self-documenting and easy to understand
2. **Consistency**: Follow the same patterns throughout the codebase
3. **Kotlin Idioms**: Embrace Kotlin's language features and best practices
4. **Performance Awareness**: Write efficient code without premature optimization
5. **Maintainability**: Code should be easy to modify and extend

---

## 🏗️ **Project Structure & Package Naming**

### **Package Organization**
```kotlin
com.grocerysystem.customer/admin/delivery
├── ui/                     // UI layer (Activities, Fragments, Composables)
│   ├── activities/         // Activity classes
│   ├── fragments/          // Fragment classes  
│   ├── adapters/           // RecyclerView adapters
│   ├── viewmodels/         // ViewModels
│   └── theme/              // UI theming and styles
├── data/                   // Data layer
│   ├── repositories/       // Repository implementations
│   ├── datasources/        // Remote/Local data sources
│   ├── models/             // Data models and DTOs
│   ├── network/            // API interfaces and network configuration
│   └── database/           // Room database entities and DAOs
├── domain/                 // Business logic layer
│   ├── usecases/           // Use case implementations
│   ├── entities/           // Domain entities
│   └── repositories/       // Repository interfaces
├── di/                     // Dependency injection modules
├── utils/                  // Utility classes and extensions
└── constants/              // Application constants
```

### **File Naming**
- **Activities**: `MainActivity.kt`, `ProductDetailsActivity.kt`
- **Fragments**: `HomeFragment.kt`, `CartFragment.kt`
- **ViewModels**: `HomeViewModel.kt`, `ProductDetailsViewModel.kt`
- **Repositories**: `ProductRepository.kt`, `UserRepository.kt`
- **Use Cases**: `GetProductsUseCase.kt`, `PlaceOrderUseCase.kt`
- **Models**: `Product.kt`, `User.kt`, `Order.kt`
- **Interfaces**: Prefix with `I` → `IProductRepository.kt`

---

## 🎯 **Naming Conventions**

### **Classes & Interfaces**
```kotlin
// ✅ Good - PascalCase
class ProductRepository
class OrderManagementService
interface IPaymentProcessor

// ❌ Bad
class productRepository
class orderManagement_service
```

### **Functions & Methods**
```kotlin
// ✅ Good - camelCase, descriptive
fun calculateTotalPrice(items: List<CartItem>): Double
fun isUserLoggedIn(): Boolean
suspend fun fetchProductsByCategory(categoryId: String): List<Product>

// ❌ Bad
fun calc(items: List<CartItem>): Double
fun check(): Boolean
fun get(id: String): List<Product>
```

### **Variables & Properties**
```kotlin
// ✅ Good - camelCase, descriptive
private val userRepository: IUserRepository
private var isLoadingData = false
private val maxRetryAttempts = 3

// ❌ Bad
private val repo: IUserRepository
private var loading = false  
private val MAX_RETRY = 3  // Use const val for compile-time constants
```

### **Constants**
```kotlin
// ✅ Good - SCREAMING_SNAKE_CASE for compile-time constants
object ApiConstants {
    const val BASE_URL = "https://api.grocerysystem.com"
    const val TIMEOUT_SECONDS = 30
    const val MAX_RETRY_ATTEMPTS = 3
}

// ✅ Good - camelCase for runtime constants
class NetworkConfig {
    companion object {
        val defaultTimeout = Duration.ofSeconds(30)
        val supportedLanguages = listOf("en", "es", "fr")
    }
}
```

### **Enums**
```kotlin
// ✅ Good - PascalCase for enum class, SCREAMING_SNAKE_CASE for values
enum class OrderStatus {
    PENDING,
    CONFIRMED, 
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}

// ✅ Alternative - PascalCase for more readable values
enum class UserType {
    Customer,
    Admin,
    DeliveryPerson
}
```

---

## 🛠️ **Kotlin-Specific Guidelines**

### **Data Classes**
```kotlin
// ✅ Good - Use data classes for simple data holders
data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val categoryId: String,
    val isActive: Boolean = true,
    val imageUrl: String? = null
)

// ✅ Good - Use regular classes for complex behavior
class OrderProcessor(
    private val paymentService: IPaymentService,
    private val inventoryService: IInventoryService
) {
    fun processOrder(order: Order): OrderResult {
        // Complex business logic here
    }
}
```

### **Extension Functions**
```kotlin
// ✅ Good - Extensions for existing types
fun String.isValidEmail(): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
}

fun Double.toCurrency(): String {
    return NumberFormat.getCurrencyInstance().format(this)
}

// ✅ Good - Extensions for domain-specific operations
fun List<CartItem>.calculateTotal(): Double {
    return this.sumOf { it.price * it.quantity }
}

// ❌ Bad - Don't extend types you don't own with complex behavior
fun Context.complexDatabaseOperation() // Avoid this
```

### **Null Safety**
```kotlin
// ✅ Good - Use safe calls and Elvis operator
val userName = user?.profile?.name ?: "Unknown User"

// ✅ Good - Use let for null checks with operations
user?.let { currentUser ->
    updateProfile(currentUser.id)
    logActivity(currentUser.name)
}

// ✅ Good - Use requireNotNull for programming errors
fun processOrder(orderId: String?) {
    val id = requireNotNull(orderId) { "Order ID cannot be null" }
    // Process order
}

// ❌ Bad - Avoid unnecessary null checks
if (someNonNullableString != null) {  // Redundant
    // Do something
}
```

### **Sealed Classes & When Expressions**
```kotlin
// ✅ Good - Sealed classes for restricted hierarchies
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error<T>(val message: String, val code: Int? = null) : NetworkResult<T>()
    class Loading<T> : NetworkResult<T>()
}

// ✅ Good - Exhaustive when expressions
fun handleNetworkResult(result: NetworkResult<List<Product>>) {
    when (result) {
        is NetworkResult.Success -> displayProducts(result.data)
        is NetworkResult.Error -> showError(result.message)
        is NetworkResult.Loading -> showLoadingIndicator()
        // No else branch needed - compiler ensures exhaustiveness
    }
}
```

### **Coroutines & Async Patterns**
```kotlin
// ✅ Good - Use suspend functions for async operations
class ProductRepository {
    suspend fun fetchProducts(categoryId: String): List<Product> {
        return withContext(Dispatchers.IO) {
            apiService.getProducts(categoryId)
        }
    }
}

// ✅ Good - Use viewModelScope in ViewModels
class ProductViewModel : ViewModel() {
    fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = productRepository.fetchProducts("electronics")
                _products.value = products
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

// ✅ Good - Handle cancellation properly
suspend fun uploadImage(imageData: ByteArray): String {
    return withContext(Dispatchers.IO) {
        ensureActive() // Check for cancellation
        apiService.uploadImage(imageData)
    }
}
```

---

## 📚 **Documentation Standards**

### **KDoc Format**
```kotlin
/**
 * Calculates the total price including tax and shipping for a list of cart items.
 *
 * @param items The list of items in the shopping cart
 * @param taxRate The tax rate to apply (default: 0.08 for 8%)
 * @param shippingCost The shipping cost to add (default: 5.99)
 * @return The total price including tax and shipping
 * @throws IllegalArgumentException if taxRate is negative
 * 
 * @sample
 * ```kotlin
 * val items = listOf(CartItem("apple", 2.99, 3))
 * val total = calculateTotalWithTax(items, taxRate = 0.08)
 * ```
 */
fun calculateTotalWithTax(
    items: List<CartItem>,
    taxRate: Double = 0.08,
    shippingCost: Double = 5.99
): Double {
    require(taxRate >= 0) { "Tax rate cannot be negative" }
    
    val subtotal = items.sumOf { it.price * it.quantity }
    val tax = subtotal * taxRate
    return subtotal + tax + shippingCost
}
```

### **Inline Comments**
```kotlin
// ✅ Good - Explain why, not what
class OrderProcessor {
    fun processOrder(order: Order) {
        // Apply business rules before payment processing
        // This ensures inventory is reserved before charging the customer
        reserveInventory(order.items)
        
        try {
            processPayment(order.paymentInfo)
        } catch (e: PaymentException) {
            // Release inventory if payment fails to prevent overselling
            releaseInventory(order.items)
            throw e
        }
    }
}

// ❌ Bad - Don't comment obvious code
val total = price * quantity  // Calculate total price (obvious)
```

### **TODO Comments**
```kotlin
// ✅ Good - Structured TODO format
// TODO(username, YYYY-MM-DD): Add caching mechanism for frequently accessed products
// TODO(teamlead, 2025-10-20): Implement retry logic for network failures
// FIXME: Handle edge case where user cancels order during payment processing

// ❌ Bad
// TODO fix this later
// todo - add error handling
```

---

## 📦 **Import Organization**

### **Import Order**
```kotlin
// 1. Android framework imports
import android.content.Context
import android.view.View

// 2. Third-party library imports  
import retrofit2.Response
import kotlinx.coroutines.flow.Flow

// 3. Project imports (grouped by module)
import com.grocerysystem.shared.models.Product
import com.grocerysystem.customer.data.repositories.ProductRepository
import com.grocerysystem.customer.ui.viewmodels.HomeViewModel

// Use blank lines between different groups
```

### **Import Guidelines**
```kotlin
// ✅ Good - Use specific imports
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ❌ Bad - Avoid wildcard imports (except for testing)
import kotlinx.coroutines.*

// ✅ Exception - Wildcard OK for test assertions
import org.junit.Assert.*
```

---

## 🔍 **Code Quality Rules**

### **Function Complexity**
- **Maximum function length**: 50 lines
- **Maximum parameters**: 5 parameters (use data classes for more)
- **Maximum nesting depth**: 3 levels
- **Cyclomatic complexity**: Maximum 10

```kotlin
// ✅ Good - Simple, focused function
fun validateEmail(email: String): ValidationResult {
    return when {
        email.isBlank() -> ValidationResult.Error("Email is required")
        !email.isValidEmail() -> ValidationResult.Error("Invalid email format")
        else -> ValidationResult.Success
    }
}

// ✅ Good - Use data class for multiple parameters
data class OrderRequest(
    val customerId: String,
    val items: List<CartItem>,
    val shippingAddress: Address,
    val paymentMethod: PaymentMethod,
    val deliveryInstructions: String? = null
)

fun createOrder(request: OrderRequest): Order {
    // Implementation
}
```

### **Class Design**
- **Single Responsibility**: Each class should have one reason to change
- **Maximum class length**: 300 lines
- **Prefer composition over inheritance**
- **Use interfaces for abstraction**

```kotlin
// ✅ Good - Single responsibility
class EmailValidator {
    fun isValid(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

class PasswordValidator {
    fun isValid(password: String): Boolean {
        return password.length >= 8 && 
               password.any { it.isUpperCase() } &&
               password.any { it.isLowerCase() } &&
               password.any { it.isDigit() }
    }
}

// ✅ Good - Composition
class UserValidator(
    private val emailValidator: EmailValidator,
    private val passwordValidator: PasswordValidator
) {
    fun validateUser(email: String, password: String): List<String> {
        val errors = mutableListOf<String>()
        
        if (!emailValidator.isValid(email)) {
            errors.add("Invalid email format")
        }
        
        if (!passwordValidator.isValid(password)) {
            errors.add("Password does not meet requirements")
        }
        
        return errors
    }
}
```

---

## 🎨 **Formatting Rules**

### **Line Length & Spacing**
- **Maximum line length**: 120 characters
- **Indentation**: 4 spaces (no tabs)
- **Blank lines**: One blank line between functions, two between classes
- **Trailing spaces**: Remove all trailing whitespace

### **Braces & Brackets**
```kotlin
// ✅ Good - K&R style braces
if (condition) {
    doSomething()
} else {
    doSomethingElse()
}

// ✅ Good - Same line for functions
fun shortFunction() {
    return value
}

// ✅ Good - Chain calls formatting
val result = listOf(1, 2, 3, 4, 5)
    .filter { it % 2 == 0 }
    .map { it * 2 }
    .sortedDescending()
```

---

## 🔒 **Security & Performance Guidelines**

### **Security Best Practices**
```kotlin
// ✅ Good - Never hardcode sensitive data
class ApiConfig {
    private val apiKey = BuildConfig.API_KEY  // From build config
    private val baseUrl = BuildConfig.BASE_URL
}

// ✅ Good - Use encrypted storage for sensitive data
class SecureStorage(context: Context) {
    private val encryptedSharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
}

// ❌ Bad - Never hardcode secrets
const val API_KEY = "sk_live_abc123..."  // Never do this!
```

### **Performance Guidelines**
```kotlin
// ✅ Good - Use lazy initialization for expensive operations
class ExpensiveService {
    private val heavyResource by lazy {
        createExpensiveResource()
    }
}

// ✅ Good - Use appropriate collection types
val frequentLookups: Map<String, Product> = products.associateBy { it.id }
val orderedItems: List<CartItem> = items.sortedBy { it.name }
val uniqueCategories: Set<String> = products.map { it.categoryId }.toSet()

// ✅ Good - Avoid memory leaks in Android
class MyViewModel : ViewModel() {
    private val repository = ProductRepository()
    
    override fun onCleared() {
        super.onCleared()
        // Clean up resources
        repository.cleanup()
    }
}
```

---

## ✅ **Checklist for Code Reviews**

Before submitting code for review, ensure:

### **Code Quality**
- [ ] Functions are focused and do one thing well
- [ ] Classes follow single responsibility principle
- [ ] Proper error handling is implemented
- [ ] No code duplication (DRY principle)
- [ ] Performance considerations addressed

### **Kotlin Best Practices**
- [ ] Appropriate use of data classes vs regular classes
- [ ] Proper null safety handling
- [ ] Idiomatic Kotlin syntax used
- [ ] Extension functions used appropriately
- [ ] Coroutines used correctly for async operations

### **Documentation**
- [ ] Complex functions have KDoc comments
- [ ] Business logic is explained with inline comments
- [ ] TODOs are properly formatted with assignee and date
- [ ] README files updated if needed

### **Testing**
- [ ] Unit tests cover critical business logic
- [ ] Edge cases are tested
- [ ] Integration tests for complex workflows
- [ ] UI tests for user interactions

### **Security**
- [ ] No hardcoded secrets or sensitive data
- [ ] Input validation implemented
- [ ] Proper authentication and authorization
- [ ] Secure data storage practices followed

---

## 📈 **Continuous Improvement**

This coding standards document is a living document and should be updated as the team learns and grows. Regular reviews should be conducted to:

- Update standards based on new Kotlin language features
- Incorporate lessons learned from code reviews
- Adapt to new libraries and frameworks
- Improve based on team feedback

**Last Updated**: October 17, 2025  
**Next Review**: November 17, 2025  
**Maintained by**: Development Team Lead

---

*For questions about these standards or suggestions for improvements, please create an issue in the project repository or discuss with the development team.*