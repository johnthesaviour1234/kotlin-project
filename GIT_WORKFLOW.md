# Git Workflow - Grocery Delivery System

**Document Version**: 1.0  
**Last Updated**: October 17, 2025, 12:28 UTC  
**Author**: Development Team  
**Repository**: https://github.com/johnthesaviour1234/kotlin-project

---

## 🌟 Overview

This document outlines the Git workflow and branching strategy for the Grocery Delivery System project with three mobile applications (Customer, Admin, Delivery) and supporting infrastructure.

## 🌿 GitFlow Branching Strategy

### **Branch Types**

#### **1. Main Branches**

**`main` Branch**
- **Purpose**: Production-ready code only
- **Protection**: Branch protection rules enabled
- **Merging**: Only from `develop` via pull requests
- **Releases**: All production releases tagged from main
- **Direct Commits**: ❌ Forbidden (enforced by branch protection)

**`develop` Branch**
- **Purpose**: Integration branch for feature development
- **Source**: All feature branches merge here
- **Testing**: Staging deployments and integration testing
- **Merging**: Via pull requests with code review required
- **Stability**: Should always be in a deployable state

#### **2. Supporting Branches**

**`feature/*` Branches**
- **Naming**: `feature/description-of-feature`
- **Source**: Branched from `develop`
- **Merge Target**: Back to `develop`
- **Lifetime**: Temporary - deleted after merge
- **Examples**: 
  - `feature/customer-authentication`
  - `feature/admin-dashboard`
  - `feature/delivery-gps-integration`

**`hotfix/*` Branches**
- **Naming**: `hotfix/urgent-bug-description`
- **Source**: Branched from `main`
- **Merge Target**: Both `main` and `develop`
- **Purpose**: Critical production bug fixes
- **Priority**: Immediate deployment to production

**`release/*` Branches**
- **Naming**: `release/v1.0.0`
- **Source**: Branched from `develop`
- **Merge Target**: Both `main` and `develop`
- **Purpose**: Release preparation and final testing
- **Content**: Version bumps, documentation, final bug fixes

### **Multi-App Workflow Considerations**

Since we have three mobile applications, feature branches should indicate the target app:

- `feature/customer-app/authentication`
- `feature/admin-app/product-management`
- `feature/delivery-app/gps-navigation`
- `feature/backend/api-enhancement`
- `feature/shared/documentation-update`

---

## 🔒 Branch Protection Rules

### **`main` Branch Protection**
```yaml
Protection Rules:
- Require pull request reviews before merging: ✅ (minimum 1 reviewer)
- Dismiss stale PR reviews when new commits are pushed: ✅
- Require status checks to pass before merging: ✅
- Require branches to be up to date before merging: ✅
- Include administrators in protection rules: ✅
- Allow force pushes: ❌
- Allow deletions: ❌

Required Status Checks:
- Android Build (Customer App)
- Android Build (Admin App)  
- Android Build (Delivery App)
- Code Quality (ktlint)
- Static Analysis (detekt)
- Security Scan
```

### **`develop` Branch Protection**
```yaml
Protection Rules:
- Require pull request reviews before merging: ✅ (minimum 1 reviewer)
- Require status checks to pass before merging: ✅
- Require branches to be up to date before merging: ✅
- Include administrators in protection rules: ❌
- Allow force pushes: ❌
- Allow deletions: ❌

Required Status Checks:
- Android Build (All Apps)
- Code Quality Checks
- Integration Tests
```

---

## 📋 Pull Request Process

### **Pull Request Template** (Already Configured)

Our pull request template includes:
- Description of changes
- Type of change (bugfix, feature, docs, etc.)
- Testing checklist
- Review checklist
- Documentation updates

### **Review Requirements**

**Code Review Criteria:**
- [ ] Code follows team coding standards (KOTLIN_CODING_STANDARDS.md)
- [ ] All tests pass (unit, integration, UI)
- [ ] Code quality tools pass (ktlint, detekt)
- [ ] Documentation updated as needed
- [ ] No security vulnerabilities introduced
- [ ] Performance impact assessed
- [ ] Mobile app specific considerations addressed

**Required Reviewers by Component:**
- **Mobile Apps**: Senior Android Developer + 1 peer
- **Backend APIs**: Backend Developer + DevOps Engineer
- **Documentation**: Technical Writer or Team Lead
- **CI/CD Changes**: DevOps Engineer + Senior Developer

---

## 💬 Commit Message Standards

### **Conventional Commits Format**

```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### **Commit Types**

- **feat**: New feature for users
- **fix**: Bug fix for users  
- **docs**: Documentation changes
- **style**: Code formatting (no logic changes)
- **refactor**: Code refactoring (no feature changes)
- **perf**: Performance improvements
- **test**: Adding or updating tests
- **build**: Build system or dependency changes
- **ci**: CI/CD configuration changes
- **chore**: Maintenance tasks

### **Scope Examples**

- **customer-app**: Customer mobile application
- **admin-app**: Admin mobile application
- **delivery-app**: Delivery mobile application
- **backend**: API or backend services
- **ci**: CI/CD pipeline changes
- **docs**: Documentation updates

### **Example Commit Messages**

```bash
# Good examples
feat(customer-app): implement user authentication with Supabase
fix(delivery-app): resolve GPS location accuracy issues
docs: update API documentation for product endpoints
ci: add automated testing for all three mobile apps
refactor(admin-app): improve product management UI components

# Bad examples  
added new stuff
fixing bugs
update
```

---

## 🔄 Workflow Procedures

### **Starting New Feature**

```bash
# 1. Switch to develop and pull latest changes
git checkout develop
git pull origin develop

# 2. Create feature branch
git checkout -b feature/customer-app/shopping-cart

# 3. Work on feature with regular commits
git add .
git commit -m "feat(customer-app): implement cart item management"

# 4. Push feature branch
git push origin feature/customer-app/shopping-cart

# 5. Create pull request to develop via GitHub UI
```

### **Code Review Process**

```bash
# 1. Reviewer checks out PR branch locally
git fetch origin
git checkout feature/customer-app/shopping-cart

# 2. Test the changes locally
./gradlew assembleDebug  # Build all apps
./gradlew test           # Run tests
./gradlew ktlintCheck   # Check formatting
./gradlew detekt        # Static analysis

# 3. Review code and provide feedback via GitHub
# 4. Approve and merge via GitHub UI
```

### **Release Process**

```bash
# 1. Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/v1.0.0

# 2. Version bump and final preparations
# Update version numbers in build.gradle.kts files
# Update CHANGELOG.md
# Final testing

# 3. Merge to main and tag
git checkout main
git merge --no-ff release/v1.0.0
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main --tags

# 4. Merge back to develop
git checkout develop  
git merge --no-ff release/v1.0.0
git push origin develop

# 5. Delete release branch
git branch -d release/v1.0.0
git push origin --delete release/v1.0.0
```

### **Hotfix Process**

```bash
# 1. Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/critical-security-fix

# 2. Fix the issue
git add .
git commit -m "fix: resolve critical security vulnerability in auth"

# 3. Merge to main
git checkout main
git merge --no-ff hotfix/critical-security-fix
git tag -a v1.0.1 -m "Hotfix version 1.0.1"
git push origin main --tags

# 4. Merge to develop
git checkout develop
git merge --no-ff hotfix/critical-security-fix
git push origin develop

# 5. Delete hotfix branch
git branch -d hotfix/critical-security-fix
git push origin --delete hotfix/critical-security-fix
```

---

## 🛡️ Git Hooks

### **Pre-commit Hook**

```bash
#!/bin/sh
# .git/hooks/pre-commit

# Run code formatting
echo "Running ktlint check..."
./gradlew ktlintCheck
if [ $? -ne 0 ]; then
    echo "❌ Code formatting issues found. Please run ./gradlew ktlintFormat"
    exit 1
fi

# Run static analysis  
echo "Running detekt analysis..."
./gradlew detekt
if [ $? -ne 0 ]; then
    echo "❌ Static analysis issues found. Please fix detekt warnings"
    exit 1
fi

echo "✅ Pre-commit checks passed"
exit 0
```

### **Commit Message Hook**

```bash
#!/bin/sh
# .git/hooks/commit-msg

commit_regex='^(feat|fix|docs|style|refactor|perf|test|build|ci|chore)(\(.+\))?: .{1,50}'

error_msg="❌ Invalid commit message format. Please use conventional commits:
<type>[optional scope]: <description>

Examples:
feat(customer-app): add shopping cart functionality  
fix(delivery-app): resolve GPS accuracy issues
docs: update API documentation"

if ! grep -qE "$commit_regex" "$1"; then
    echo "$error_msg" >&2
    exit 1
fi
```

---

## 📊 Workflow Metrics

### **Quality Gates**

- **Code Coverage**: Minimum 70% for new code
- **Build Success Rate**: >95% for all branches
- **PR Review Time**: <24 hours average
- **Hotfix Resolution**: <4 hours for critical issues

### **Branch Lifecycle**

- **Feature Branches**: Maximum 1 week lifetime
- **Release Branches**: 3-5 days for final testing
- **Hotfix Branches**: <4 hours from creation to deployment

---

## 🎯 Mobile App Specific Considerations

### **Android Build Requirements**

Each mobile app must pass:
- **Compilation**: All variants (debug/release)
- **Unit Tests**: >70% coverage
- **UI Tests**: Critical user flows
- **Code Quality**: ktlint + detekt compliance
- **Security**: Dependency vulnerability scan

### **App-Specific Workflows**

**Customer App:**
- Focus on UI/UX testing
- Performance testing for shopping flows
- Payment integration security review

**Admin App:**  
- Security review for admin privileges
- Data integrity testing
- Analytics accuracy validation

**Delivery App:**
- Location services testing
- Battery usage optimization
- Background service stability

---

## 🔧 Tools Integration

### **Required Tools**

- **Git**: Version control
- **GitHub**: Repository hosting and collaboration
- **GitHub Actions**: CI/CD automation
- **Android Studio**: IDE with Git integration
- **Gradle**: Build system with quality plugins

### **Optional Tools**

- **SourceTree**: Git GUI client
- **GitKraken**: Advanced Git workflow visualization
- **GitHub Desktop**: Simple Git interface
- **Android Studio Git Integration**: Built-in Git tools

---

## 📚 Resources

### **Documentation References**

- [TEAM_DEVELOPMENT_GUIDELINES.md](TEAM_DEVELOPMENT_GUIDELINES.md) - Team collaboration standards
- [KOTLIN_CODING_STANDARDS.md](KOTLIN_CODING_STANDARDS.md) - Code quality requirements
- [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) - Complete project context
- [Sprint_1_Task_Breakdown.md](Sprint_1_Task_Breakdown.md) - Current development status

### **External Resources**

- [Conventional Commits Specification](https://www.conventionalcommits.org/)
- [GitFlow Workflow](https://www.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow)
- [GitHub Flow](https://guides.github.com/introduction/flow/)
- [Git Hooks Documentation](https://git-scm.com/docs/githooks)

---

## ✅ Implementation Checklist

### **Repository Setup**
- [x] Branch protection rules configured
- [ ] Git hooks installed locally
- [ ] Team members added as collaborators
- [ ] Repository settings optimized

### **Process Implementation**
- [ ] Team trained on GitFlow process
- [ ] Commit message standards enforced
- [ ] PR review process documented
- [ ] Release procedures tested

### **Quality Automation**
- [ ] Pre-commit hooks active
- [ ] CI/CD pipeline validates all branches
- [ ] Automated code quality reporting
- [ ] Security scanning integrated

---

**Git Workflow Status**: 📝 **DOCUMENTED & READY FOR IMPLEMENTATION**  
**Next Steps**: Configure branch protection rules and implement Git hooks  
**Estimated Setup Time**: 2 hours for complete implementation  
**Team Impact**: Professional Git workflow supporting parallel development of three mobile applications