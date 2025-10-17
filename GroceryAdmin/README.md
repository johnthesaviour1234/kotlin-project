# Grocery Admin Android App

**Part of the Online Grocery System** - A modern Android application for administrators built with Kotlin and Clean Architecture.

## 🏗️ Architecture

This app implements **Clean Architecture** with **MVVM pattern** to ensure maintainability, testability, and scalability.

### Layer Structure
- **UI Layer** (`ui/`): Activities, Fragments, ViewModels, and Adapters
- **Domain Layer** (`domain/`): Business logic, Use Cases, and Repository interfaces
- **Data Layer** (`data/`): Repository implementations, API services, and local database

### Key Components
- **Hilt**: Dependency injection framework
- **Retrofit + OkHttp**: Network communication with Vercel API
- **Room**: Local database for offline-first functionality
- **ViewBinding**: Type-safe view references
- **Kotlin Coroutines**: Asynchronous programming

## 🔧 Technical Stack

- **Language**: Kotlin 100%
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Architecture**: Clean Architecture + MVVM
- **DI**: Hilt (Dagger)
- **Network**: Retrofit + OkHttp
- **Database**: Room
- **UI**: Material Design 3

## 🌐 Backend Integration

### API Endpoints
- **Base URL**: `https://kotlin-project-31l3qkwl4-project3-f5839d18.vercel.app/api/`
- **Health Check**: `GET /health`
- **Authentication**: `POST /auth/login`, `POST /auth/register`
- **Products**: `GET /products/categories`, `GET /products/list`

### Database Integration
- **Supabase URL**: `https://sjujrmvfzzzfskknvgjx.supabase.co`
- **Real-time Features**: Live product updates, order tracking
- **Authentication**: Supabase Auth integration

## 📱 Features (Planned)

### Admin Features
- [x] **Project Foundation**: Clean architecture with all dependencies (replicated from Customer app)
- [ ] **Admin Authentication**: Secure admin login with role verification
- [ ] **Product Management**: Add, edit, delete products and categories
- [ ] **Order Processing**: View, update, and manage customer orders
- [ ] **Analytics Dashboard**: Sales reports, charts, and business insights
- [ ] **User Management**: Manage customers and delivery personnel
- [ ] **Inventory Control**: Stock management and low-stock alerts
- [ ] **System Administration**: App configuration and settings

### Technical Features
- [x] **Offline-First**: Local database with sync capability
- [x] **Error Handling**: Comprehensive error states and retry mechanisms
- [x] **Loading States**: User-friendly loading indicators
- [x] **Type Safety**: Full Kotlin null safety and type checking
- [ ] **Push Notifications**: Order updates and promotions
- [ ] **Caching**: Intelligent data caching for performance

## 🛠️ Development Setup

### Prerequisites
- Android Studio Arctic Fox or newer
- JDK 17+
- Git

### Getting Started
1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Run the app on emulator or device

### Code Quality Tools
- **ktlint**: Kotlin style enforcement
- **detekt**: Static code analysis
- **EditorConfig**: Consistent formatting across IDEs

### Build Variants
- **Debug**: Development build with logging enabled
- **Release**: Production build with ProGuard/R8 optimization

## 📁 Project Structure

```
app/src/main/java/com/grocery/admin/
├── di/                     # Dependency injection modules
├── data/                   # Data layer
│   ├── local/              # Room database, DAOs, entities
│   ├── remote/             # Retrofit API, DTOs
│   └── repository/         # Repository implementations
├── domain/                 # Domain layer
│   ├── model/              # Domain models
│   ├── repository/         # Repository interfaces
│   └── usecase/            # Business logic use cases
├── ui/                     # UI layer
│   ├── activities/         # Admin activities
│   ├── fragments/          # Admin fragments
│   ├── viewmodels/         # Admin ViewModels
│   └── adapters/           # Admin RecyclerView adapters
└── util/                   # Utility classes
```

## 🔄 Development Workflow

### Git Workflow
- **Feature branches**: `feature/feature-name`
- **Commit messages**: Conventional commits format
- **Code review**: Required for all pull requests

### Testing Strategy
- **Unit Tests**: Business logic and ViewModels
- **Integration Tests**: Repository and API interactions
- **UI Tests**: Critical user flows and navigation

## 🚀 Deployment

### Build Process
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run code quality checks
./gradlew ktlintCheck detekt
```

### Distribution
- **Internal Testing**: Google Play Console internal track
- **Production**: Google Play Store public release

## 📊 Performance Targets

- **App Launch Time**: < 2 seconds cold start
- **API Response Handling**: < 200ms UI response
- **Memory Usage**: < 150MB peak memory
- **APK Size**: < 20MB compressed

## 🤝 Contributing

1. Follow established coding standards (ktlint, detekt)
2. Maintain Clean Architecture principles
3. Add comprehensive tests for new features
4. Update documentation as needed

## 📝 License

This project is part of the Online Grocery System and follows the established project licensing.

---

**Status**: ✅ **Template Replication Complete** - Ready for admin feature development  
**Next Steps**: Implement admin authentication and product management features  
**Team**: Admin Mobile Development Team  
**Template Source**: Replicated from Customer app Clean Architecture foundation
