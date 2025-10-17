# Android Studio IDE Setup Guide - Online Grocery System

**Version**: 1.0  
**Created**: October 17, 2025  
**Compatible with**: Android Studio Giraffe (2023.2.1) and later

---

## 🎯 **Overview**

This guide helps team members configure Android Studio for optimal development experience with consistent settings, code formatting, and productivity features for the Online Grocery System project.

---

## 📋 **Required Plugins**

### **Essential Plugins (Install via File → Settings → Plugins)**

1. **Kotlin** (Built-in) - Kotlin language support
2. **Android** (Built-in) - Android development support
3. **Ktlint** - Kotlin linting and formatting
4. **Detekt** - Static code analysis for Kotlin
5. **SonarLint** - Code quality and security analysis
6. **GitToolBox** - Enhanced Git integration
7. **Rainbow Brackets** - Color-coded brackets for better readability
8. **String Manipulation** - Text transformation utilities
9. **ADB Idea** - ADB commands integration
10. **Key Promoter X** - Keyboard shortcut learning assistant

### **Optional but Recommended Plugins**

1. **Database Tools and SQL** (Built-in) - Database management
2. **Markdown** (Built-in) - Markdown file editing
3. **Material Theme UI** - Better visual themes
4. **Grep Console** - Enhanced console output with colors
5. **Presentation Assistant** - Show keyboard shortcuts during presentations
6. **Docker** - If using containerized services
7. **CSV Plugin** - CSV file editing and viewing

---

## ⚙️ **Code Style Configuration**

### **Kotlin Code Style Setup**

1. **Navigate to**: File → Settings → Editor → Code Style → Kotlin

2. **General Settings**:
   ```
   Right margin (columns): 120
   Wrap on typing: Yes
   Use tab character: No
   Tab size: 4
   Indent: 4
   Continuation indent: 8
   ```

3. **Imports Configuration**:
   ```
   Import Layout:
   - import all other imports
   - <blank line>
   - import java.*
   - import javax.*
   - <blank line>
   - import kotlin.*
   - import kotlinx.*
   - <blank line>
   - import android.*
   - import androidx.*
   - <blank line>
   - import com.grocerysystem.*
   - <blank line>
   - import all other imports
   ```

4. **Blank Lines Settings**:
   ```
   Keep Maximum Blank Lines:
   - In declarations: 2
   - In code: 2
   - Before '}'.closing brace: 1
   
   Minimum Blank Lines:
   - Before package statement: 0
   - After package statement: 1
   - Before imports: 1
   - Around class: 1
   - After class header: 0
   - Before class end: 0
   - Around function: 1
   - Around function in interface: 1
   ```

### **XML Code Style Setup**

1. **Navigate to**: File → Settings → Editor → Code Style → XML

2. **Configure**:
   ```
   Tab size: 4
   Indent: 4
   Continuation indent: 8
   Right margin: 120
   Wrap attributes: Do not wrap
   Align attributes: No
   Keep line breaks in text: Yes
   ```

---

## 🔧 **Editor Configuration**

### **General Editor Settings**

1. **File → Settings → Editor → General**:
   ```
   ✓ Change font size with Ctrl+Mouse Wheel
   ✓ Show quick documentation on mouse over (500 ms delay)
   ✓ Enable drag-n-drop functionality in editor
   ✓ Honor "CamelHumps" words settings when selecting on double click
   ```

2. **Auto Import Settings** (File → Settings → Editor → General → Auto Import):
   ```
   ✓ Add unambiguous imports on the fly
   ✓ Optimize imports on the fly (for current project)
   Insert imports for inner classes: All
   ```

3. **Appearance** (File → Settings → Editor → General → Appearance):
   ```
   ✓ Show line numbers
   ✓ Show method separators
   ✓ Show whitespaces: Leading and Trailing
   ✓ Show hard wrap guide (visual line at margin)
   ```

### **Font & Color Scheme**

1. **Recommended Font Settings**:
   ```
   Font: JetBrains Mono (or Fira Code for ligatures)
   Size: 14
   Line spacing: 1.2
   ✓ Enable font ligatures (if using Fira Code)
   ```

2. **Color Scheme**: Use "Darcula" or "IntelliJ Light" based on preference

---

## 🧪 **Testing Configuration**

### **JUnit Configuration**

1. **File → Settings → Build → Build Tools → Gradle**:
   ```
   Build and run using: Gradle
   Run tests using: Gradle
   ```

2. **Test Runner Template** (File → Settings → Editor → File and Code Templates → Other → JUnit Test Class):
   ```kotlin
   #parse("File Header.java")
   
   import org.junit.Test
   import org.junit.Assert.*
   import org.junit.Before
   import org.mockito.Mock
   import org.mockito.MockitoAnnotations
   
   class ${NAME} {
       
       @Mock
       private lateinit var mockDependency: SomeDependency
       
       private lateinit var systemUnderTest: ${TESTED_CLASS}
       
       @Before
       fun setup() {
           MockitoAnnotations.openMocks(this)
           systemUnderTest = ${TESTED_CLASS}(mockDependency)
       }
       
       @Test
       fun \`methodName_should_expectedBehavior_when_condition\`() {
           // Arrange
           
           // Act
           
           // Assert
       }
   }
   ```

---

## 🎨 **Live Templates**

### **Custom Live Templates for Kotlin**

1. **Navigate to**: File → Settings → Editor → Live Templates

2. **Create new template group**: "Kotlin Grocery System"

3. **Add these templates**:

#### **Kotlin Class Template** (`kclass`)
```kotlin
class $NAME$ {
    $END$
}
```

#### **Data Class Template** (`kdata`)
```kotlin
data class $NAME$(
    $END$
)
```

#### **ViewModel Template** (`kvm`)
```kotlin
class $NAME$ViewModel(
    private val $USECASE$: $USECASE_TYPE$
) : ViewModel() {
    
    private val _$STATE$ = MutableLiveData<$STATE_TYPE$>()
    val $STATE$: LiveData<$STATE_TYPE$> = _$STATE$
    
    $END$
}
```

#### **Repository Template** (`krepo`)
```kotlin
class $NAME$Repository(
    private val apiService: $API_SERVICE$,
    private val dao: $DAO$
) : I$NAME$Repository {
    
    override suspend fun $METHOD$(): $RETURN_TYPE$ {
        return try {
            val result = apiService.$API_METHOD$()
            dao.insert(result)
            result
        } catch (e: Exception) {
            dao.getAll()
        }
    }
    
    $END$
}
```

#### **Use Case Template** (`kusecase`)
```kotlin
class $NAME$UseCase(
    private val repository: I$REPOSITORY$Repository
) {
    suspend operator fun invoke($PARAMS$): $RETURN_TYPE$ {
        return repository.$METHOD$($PARAMS$)
    }
}
```

#### **Test Method Template** (`ktest`)
```kotlin
@Test
fun \`$METHOD$_should_$EXPECTED$_when_$CONDITION$\`() {
    // Arrange
    $ARRANGE$
    
    // Act
    val result = $ACT$
    
    // Assert
    assertThat(result).$ASSERT$
    $END$
}
```

---

## 🔍 **Inspection Configuration**

### **Enable Important Inspections**

1. **Navigate to**: File → Settings → Editor → Inspections

2. **Kotlin Inspections to Enable**:
   ```
   ✓ Kotlin → Probable bugs → All items
   ✓ Kotlin → Style issues → All items  
   ✓ Kotlin → Redundant constructs → All items
   ✓ Kotlin → Migration → All items
   ✓ Kotlin → Other problems → All items
   ```

3. **Android Inspections to Enable**:
   ```
   ✓ Android → Performance → All items
   ✓ Android → Security → All items
   ✓ Android → Usability → All items
   ✓ Android → Correctness → All items
   ```

4. **General Inspections**:
   ```
   ✓ General → Error handling → All items
   ✓ General → Probable bugs → All items
   ✓ General → Code style issues → All items
   ```

---

## 🚀 **Performance Optimization**

### **Memory Settings**

1. **Help → Edit Custom VM Options** (create if doesn't exist):
   ```
   -Xms2g
   -Xmx8g
   -XX:ReservedCodeCacheSize=1024m
   -XX:+UseConcMarkSweepGC
   -XX:SoftRefLRUPolicyMSPerMB=50
   -XX:CICompilerCount=2
   -XX:+HeapDumpOnOutOfMemoryError
   -XX:+UseCompressedOops
   -Dfile.encoding=UTF-8
   ```

2. **Appearance & Behavior → System Settings**:
   ```
   ✓ Save files on frame deactivation
   ✓ Save files automatically if application is idle for 15 sec
   Open last project on startup: No (for security)
   ```

### **Build Performance**

1. **File → Settings → Build → Compiler**:
   ```
   Build process heap size: 2048 MB
   ✓ Compile independent modules in parallel
   ✓ Use in-process build
   Command-line Options: --parallel --daemon --configure-on-demand
   ```

2. **gradle.properties** (project level):
   ```properties
   org.gradle.jvmargs=-Xmx4g -XX:MaxPermSize=512m
   org.gradle.parallel=true
   org.gradle.daemon=true
   org.gradle.configureondemand=true
   org.gradle.caching=true
   android.enableJetifier=true
   android.useAndroidX=true
   android.enableR8.fullMode=true
   ```

---

## 🔌 **Version Control Integration**

### **Git Configuration**

1. **File → Settings → Version Control → Git**:
   ```
   ✓ Add the 'cherry-pick' action to the log context menu
   ✓ Warn if CRLF line separators are about to be committed  
   ✓ Use credential helper
   ```

2. **Commit Configuration**:
   ```
   ✓ Use non-modal commit interface
   ✓ Show unversioned files in the commit tool window
   ✓ Clear initial commit message
   ✓ Analyze code (run inspections)
   ✓ Check TODO (Show TODO items)  
   ✓ Optimize imports
   ✓ Reformat code
   ✓ Rearrange code
   ```

### **GitHub Integration**

1. **File → Settings → Version Control → GitHub**:
   - Add your GitHub account
   - Configure authentication (token-based recommended)

---

## 🎯 **Debugging Configuration**

### **Debugger Settings**

1. **File → Settings → Build → Debugger**:
   ```
   ✓ Show alternative source switcher
   ✓ Show library frames in threads view
   ✓ Show synthetic methods
   ✓ Show toString() object view for all classes
   ```

2. **Data Views**:
   ```
   ✓ Show hex values for primitives
   ✓ Show values inline in editor while debugging
   ✓ Show method return values
   ```

---

## 📱 **Android-Specific Configuration**

### **SDK Configuration**

1. **File → Settings → Appearance & Behavior → System Settings → Android SDK**:
   ```
   SDK Platforms to install:
   ✓ Android 14.0 (API 34) - Latest
   ✓ Android 13.0 (API 33) - Target  
   ✓ Android 7.0 (API 24) - Minimum support
   
   SDK Tools to install:
   ✓ Android SDK Build-Tools (latest)
   ✓ Android Emulator
   ✓ Android SDK Platform-Tools
   ✓ Intel x86 Emulator Accelerator (HAXM installer)
   ✓ Google Play services
   ```

### **AVD Configuration**

**Recommended AVD Setup for Testing**:

1. **Phone AVDs**:
   - **Pixel 7 API 34** (Google Play Store)
   - **Medium Phone API 33** (Google Play Store)  
   - **Small Phone API 24** (No Google Play - for minimum SDK testing)

2. **Tablet AVD**:
   - **Pixel Tablet API 34** (Google Play Store)

3. **AVD Settings**:
   ```
   RAM: 4096 MB (or maximum available)
   VM Heap: 512 MB
   Internal Storage: 6 GB
   SD Card: 1 GB
   ✓ Use Host GPU
   ✓ Enable Device Frame
   ```

---

## 🔧 **Keyboard Shortcuts**

### **Essential Shortcuts to Learn**

```
Code Navigation:
Ctrl+B / Cmd+B          - Go to declaration
Ctrl+Alt+B              - Go to implementation  
Ctrl+H                  - Type hierarchy
Ctrl+Shift+H            - Method hierarchy
Ctrl+U                  - Go to super method/class
Alt+F7                  - Find usages

Code Generation:
Alt+Insert              - Generate (constructor, getter, setter, etc.)
Ctrl+Alt+T              - Surround with (try/catch, if, etc.)
Ctrl+/                  - Comment/uncomment line
Ctrl+Shift+/            - Comment/uncomment block

Refactoring:
Shift+F6                - Rename
F6                      - Move
Ctrl+Alt+M              - Extract method
Ctrl+Alt+V              - Extract variable
Ctrl+Alt+C              - Extract constant
Ctrl+Alt+L              - Reformat code
Ctrl+Alt+O              - Optimize imports

Testing & Debugging:
Ctrl+Shift+F10          - Run context configuration
Ctrl+Shift+F9           - Debug context configuration
F8                      - Step over
F7                      - Step into
Shift+F8                - Step out

Search & Replace:
Ctrl+Shift+F            - Find in path
Ctrl+Shift+R            - Replace in path
Double Shift            - Search everywhere
Ctrl+Shift+A            - Find action
```

---

## 📋 **Team Setup Checklist**

### **Initial Setup (First Time)**

- [ ] Install Android Studio (latest stable version)
- [ ] Install required plugins
- [ ] Import code style settings
- [ ] Configure live templates
- [ ] Set up inspections profile
- [ ] Configure Git integration
- [ ] Install and configure AVDs
- [ ] Set up performance optimization settings
- [ ] Test build and run for all three projects

### **Project-Specific Setup**

- [ ] Clone the repository
- [ ] Import the project in Android Studio
- [ ] Sync Gradle dependencies
- [ ] Run ktlint and detekt checks
- [ ] Build all three applications successfully
- [ ] Run unit tests to ensure setup is working
- [ ] Configure signing for release builds (when needed)

### **Team Onboarding Verification**

- [ ] Can build Customer app successfully
- [ ] Can build Admin app successfully  
- [ ] Can build Delivery app successfully
- [ ] Can run apps on emulator
- [ ] Ktlint formatting works correctly
- [ ] Detekt analysis runs without issues
- [ ] Git integration is working
- [ ] Code completion and navigation work
- [ ] Debugging capabilities verified

---

## 🆘 **Troubleshooting**

### **Common Issues and Solutions**

#### **Gradle Sync Issues**
```bash
# Clean and rebuild
./gradlew clean
./gradlew build

# Invalidate caches and restart
File → Invalidate Caches and Restart → Invalidate and Restart
```

#### **Memory Issues**
- Increase heap size in VM options
- Close unused projects
- Disable unnecessary plugins
- Use "Power Save Mode" for basic editing

#### **Slow Performance**
- Exclude build directories from antivirus scanning
- Use SSD for Android Studio and projects
- Increase RAM allocation
- Disable unnecessary inspections

#### **AVD Issues**
```bash
# Reset AVD if it's not working
Tools → AVD Manager → Actions → Cold Boot Now

# Enable hardware acceleration
SDK Manager → SDK Tools → Intel x86 Emulator Accelerator
```

#### **Plugin Issues**
- Disable conflicting plugins
- Update plugins to latest versions
- Reset plugin configuration in Settings

---

## 📞 **Support Resources**

### **Documentation**
- [Android Studio User Guide](https://developer.android.com/studio)
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Development Best Practices](https://developer.android.com/guide)

### **Team Support**
- Create GitHub issue for setup problems
- Ask in team chat for quick help
- Pair with team member for complex setup issues
- Document new issues and solutions in this guide

---

## 🔄 **Maintenance**

### **Regular Updates**
- Update Android Studio monthly
- Update plugins quarterly
- Review and update code style settings as project evolves
- Update this guide with new best practices

### **Settings Backup**
```bash
# Export settings for team sharing
File → Manage IDE Settings → Export Settings

# Import settings for new team members  
File → Manage IDE Settings → Import Settings
```

---

**Document Version**: 1.0  
**Last Updated**: October 17, 2025  
**Next Review**: November 17, 2025  
**Maintainer**: Development Team Lead

*For questions or suggestions about IDE setup, please create an issue in the project repository or contact the development team.*