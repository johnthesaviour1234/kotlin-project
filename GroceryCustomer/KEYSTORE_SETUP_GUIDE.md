# 🔐 Release Keystore Setup Guide

## Step 1: Install Java Development Kit (JDK)
If you don't have Java/JDK installed:
1. Download OpenJDK 17+ from https://adoptium.net/
2. Install and add to PATH
3. Verify: `java -version` and `keytool` commands work

## Step 2: Generate Release Keystore

**Important**: Keep this keystore file and passwords EXTREMELY SECURE. If lost, you cannot update your app on Play Store.

### Command to generate keystore:
```bash
keytool -genkey -v -keystore grocery-customer-release.keystore -alias grocery_customer -keyalg RSA -keysize 2048 -validity 10000
```

### You'll be prompted for:
- **Keystore password**: Choose strong password (save this!)
- **Key password**: Choose strong password (save this!)  
- **Your details**:
  - First name: Your name
  - Organizational unit: Your company/team
  - Organization: Your company name
  - City: Your city
  - State: Your state
  - Country code: Your country (e.g., US, IN, UK)

### Example responses:
```
Enter keystore password: [CREATE_STRONG_PASSWORD]
Re-enter new password: [SAME_PASSWORD]
What is your first and last name? John Doe
What is the name of your organizational unit? Development Team
What is the name of your organization? Grocery App Co
What is the name of your City or Locality? New York
What is the name of your State or Province? NY
What is the two-letter country code? US
Is CN=John Doe, OU=Development Team, O=Grocery App Co, L=New York, ST=NY, C=US correct? yes

Enter key password for <grocery_customer>: [SAME_OR_DIFFERENT_PASSWORD]
```

## Step 3: Secure Storage
1. **Backup keystore file** to multiple secure locations:
   - Cloud storage (encrypted)
   - External drive
   - Company secure server
2. **Document passwords** securely (password manager)
3. **Never commit keystore** to version control

## Step 4: Configure Build

Create `keystore.properties` file in project root:
```properties
storePassword=YOUR_KEYSTORE_PASSWORD
keyPassword=YOUR_KEY_PASSWORD
keyAlias=grocery_customer
storeFile=grocery-customer-release.keystore
```

Add to `.gitignore`:
```
keystore.properties
*.keystore
*.jks
```

Update `build.gradle.kts`:
```kotlin
// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## Step 5: Test Build
```bash
./gradlew assembleRelease
```

The signed APK will be in: `app/build/outputs/apk/release/app-release.apk`

## 🚨 CRITICAL WARNINGS
- **NEVER lose the keystore file or passwords**
- **NEVER share keystore credentials**  
- **NEVER commit keystore to version control**
- **ALWAYS backup keystore in multiple secure locations**

Losing your keystore means you cannot update your app on Google Play Store!