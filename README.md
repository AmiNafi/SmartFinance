# SmartFinance

A modern Android finance management application with intelligent transaction processing built with Clean Architecture, SOLID principles, and Jetpack Compose.

## Our Vision

**In a world where financial apps demand constant connectivity and drain precious resources, SmartFinance stands apart as a beacon of thoughtful design and user empowerment.**

We believe that **true financial freedom begins with privacy, accessibility, and independence**. That's why SmartFinance is crafted to be **100% offline**, running entirely on your device without requiring internet access or cloud services. No data leaves your phone, ensuring your financial information remains completely private and secure.

**Designed for everyone, regardless of their device's capabilities**, SmartFinance is optimized to run smoothly on budget phones with minimal system requirements. We understand that financial management shouldn't be a luxury reserved for flagship devices - it's a fundamental need that should be accessible to all.

**Completely free to use**, with no hidden costs, subscriptions, or premium features locked behind paywalls. We believe that essential financial tools should be as free as breathing.

**Our intelligent processing uses sophisticated rule-based algorithms** that analyze your transaction patterns without relying on machine learning models. However, if we discover lightweight ML solutions in the future that enhance the user experience without compromising our core principles of privacy and accessibility, we remain open to thoughtful integration.

**Looking ahead, we envision SmartFinance evolving into a comprehensive financial companion**, with planned features including savings tracking, asset management, and gentle financial coaching - all while maintaining our commitment to offline-first, privacy-focused, and universally accessible design.

**This is more than an app - it's a statement that financial management should be personal, private, and accessible to everyone.**

## Features

### Intelligent Transaction Processing
- **Rule-Based Analysis**: Uses sophisticated pattern-matching algorithms (no machine learning)
- **Natural Language Processing**: Add transactions using conversational language
- **Smart Amount Extraction**: Automatically detects monetary values from text
- **Intelligent Categorization**: Automatically classifies income vs expenses using rule-based logic
- **Smart Title Generation**: Creates meaningful transaction descriptions through pattern analysis

### Financial Management
- **Real-time Balance Tracking**: Live calculation of income, expenses, and balance
- **Monthly Overview**: Navigate through different months to view historical data
- **Transaction History**: Complete list of all financial transactions
- **Visual Analytics**: Clean, intuitive UI for financial insights

### Modern UI/UX
- **Jetpack Compose**: Built with modern Android UI toolkit
- **Material Design 3**: Latest Material Design components and theming
- **Responsive Design**: Optimized for various screen sizes
- **Dark/Light Theme**: Automatic theme adaptation

### Technical Excellence
- **Clean Architecture**: Domain-Driven Design with clear separation of concerns
- **SOLID Principles**: Perfect adherence to software design principles
- **Dependency Injection**: Koin-powered DI for maintainable code
- **MVVM Pattern**: Modern Android architecture pattern
- **Room Database**: Local data persistence with SQLite
- **Kotlin Coroutines**: Asynchronous programming for smooth UX

## Tech Stack

### Core Technologies
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room (SQLite)
- **Dependency Injection**: Koin
- **Async Programming**: Kotlin Coroutines + Flow

### Libraries & Tools
- **Compose BOM**: Latest stable Compose components
- **Material 3**: Modern Material Design implementation
- **Lifecycle**: ViewModel and LiveData management
- **Navigation**: Compose navigation components
- **KAPT**: Kotlin annotation processing

## Project Structure

```
SmartFinance/
├── app/                          # Main application module
│   ├── src/main/
│   │   ├── java/com/aminafi/smartfinance/
│   │   │   ├── ai/               # AI processing components
│   │   │   │   ├── analyzers/       # AI analysis interfaces & implementations
│   │   │   │   └── SimpleTransactionAIService.kt
│   │   │   ├── data/             # Data layer
│   │   │   │   └── repository/      # Repository pattern implementation
│   │   │   ├── di/               # Dependency injection
│   │   │   │   └── AppModule.kt     # Koin module definitions
│   │   │   ├── domain/           # Domain layer (business logic)
│   │   │   │   └── usecase/         # Use cases for business operations
│   │   │   ├── ui/               # Presentation layer
│   │   │   │   ├── App.kt            # Main app composable (refactored)
│   │   │   │   ├── components/       # Reusable UI components
│   │   │   │   │   ├── FinanceComponents.kt     # Financial UI components
│   │   │   │   │   ├── MessengerInput.kt        # Message input component
│   │   │   │   │   ├── MonthYearSelector.kt     # Month/year navigation
│   │   │   │   │   └── TransactionComponents.kt # Transaction UI components
│   │   │   │   ├── dialogs/          # Dialog components
│   │   │   │   │   └── TransactionDialogs.kt    # Transaction dialogs
│   │   │   │   ├── navigation/       # Navigation management
│   │   │   │   ├── screens/          # Screen components
│   │   │   │   │   ├── HomeScreen.kt            # Home screen
│   │   │   │   │   └── TransactionListScreen.kt # Transaction list screen
│   │   │   │   └── state/            # UI state management
│   │   │   ├── FinanceViewModel.kt   # Main ViewModel
│   │   │   ├── MainActivity.kt       # Main activity (25 lines - refactored)
│   │   │   └── SmartFinanceApplication.kt
│   │   └── res/                      # Android resources
│   └── build.gradle.kts              # App-level build configuration
├── gradle/                        # Gradle wrapper and libs
│   └── libs.versions.toml            # Version catalog
├── build.gradle.kts               # Project-level build configuration
├── README.md                      # Project documentation
└── gradlew                        # Gradle wrapper scripts
```

## Recent Refactoring (September 2025)

### Code Quality Improvements
**MainActivity Refactoring**: Transformed from a monolithic 600+ line file into a clean, focused 25-line activity that follows SOLID principles perfectly.

### Before vs After
- **Before**: MainActivity contained all UI components, business logic, and state management
- **After**: MainActivity only handles Android lifecycle; all UI logic properly separated

### New File Structure
- **`ui/App.kt`**: Main app composable with navigation and state management
- **`ui/components/`**: Properly organized reusable components:
  - `FinanceComponents.kt`: Financial UI components (cards, summaries, balance)
  - `MessengerInput.kt`: Message input with AI processing
  - `MonthYearSelector.kt`: Month/year navigation component
  - `TransactionComponents.kt`: Transaction list and item components
- **`ui/dialogs/TransactionDialogs.kt`**: All transaction-related dialogs
- **`ui/screens/`**: Screen-level components properly separated

### Benefits Achieved
- ✅ **Single Responsibility**: Each file has one clear purpose
- ✅ **Maintainability**: Easy to modify individual components
- ✅ **Testability**: Components can be tested in isolation
- ✅ **Reusability**: UI components can be reused across screens
- ✅ **Clean Architecture**: Perfect separation of concerns
- ✅ **SOLID Compliance**: All design principles properly implemented

## Architecture Overview

### Clean Architecture Layers

#### Domain Layer (Business Logic)
- **Use Cases**: `ProcessAIMessageUseCase`, `ManageTransactionUseCase`
- **Entities**: `Transaction`, `MonthlySummary`
- **Interfaces**: Repository contracts, AI analyzer contracts

#### Data Layer (Data Access)
- **Repository Pattern**: `TransactionRepository` interface and implementation
- **Local Storage**: Room database with DAOs
- **Data Models**: Database entities and DTOs

#### Presentation Layer (UI)
- **MVVM Pattern**: ViewModels with LiveData/StateFlow
- **Compose UI**: Declarative UI components
- **State Management**: UiStateManager for complex state
- **Navigation**: Compose Navigation for screen transitions

#### Dependency Injection
- **Koin Modules**: Centralized dependency configuration
- **Interface Binding**: Runtime implementation selection
- **Scope Management**: Singleton, factory, and viewModel scopes

## Getting Started

### Prerequisites
- **Android Studio**: Arctic Fox or later
- **JDK**: 11 or higher
- **Android SDK**: API level 24+ (Android 7.0)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/SmartFinance.git
   cd SmartFinance
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing Android Studio project"
   - Navigate to the cloned directory and select it

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click the "Run" button in Android Studio

## Usage

### Adding Transactions

#### Method 1: Intelligent Processing (Recommended)
1. Tap the message input field
2. Type naturally: *"Spent 50 dollars on lunch"* or *"Received 1000 from salary"*
3. The system automatically detects amount, type, and creates a description using rule-based analysis
4. Confirm and save the transaction

#### Method 2: Manual Entry
1. Tap the "+" button
2. Fill in description, amount, and select type (Income/Expense)
3. The date is automatically set to current date/time
4. Save the transaction

### Viewing Financial Data
- **Home Screen**: Current month overview with balance, income, and expenses
- **Month Navigation**: Use arrow buttons to navigate between months
- **Transaction Lists**: Tap income/expense cards to see detailed lists
- **Edit Transactions**: Long press any transaction to edit or delete

## Testing

### Running Tests
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew testDebugUnitTest --tests="com.aminafi.smartfinance.IncomeCategorizationTest"
```

### Test Coverage
- **Unit Tests**: Business logic and data processing
- **Integration Tests**: Repository and use case testing
- **UI Tests**: Compose component testing

## Development

### Code Style
- **Kotlin Coding Conventions**: Follows official Kotlin guidelines
- **Android Best Practices**: Adheres to Android development standards
- **SOLID Principles**: All code follows SOLID design principles

### Building for Production
```bash
# Build release APK
./gradlew assembleRelease

# Build and install debug version
./gradlew installDebug
```

### Debugging
- **Android Studio Profiler**: Performance monitoring
- **Logcat**: Runtime logging and debugging
- **Compose Preview**: UI component preview and testing

## Contributing

We welcome contributions from everyone! Whether you're fixing bugs, adding features, improving documentation, or suggesting ideas - your input is valuable and appreciated.

### Ways to Contribute
- **🐛 Bug Reports**: Found an issue? Let us know!
- **💡 Feature Suggestions**: Have an idea for improvement? Share it!
- **📝 Documentation**: Help improve our docs and guides
- **🔧 Code Contributions**: Submit pull requests for fixes and features
- **🧪 Testing**: Help us improve test coverage and quality

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow existing code style and architecture patterns
- Write tests for new features
- Update documentation for API changes
- Ensure all tests pass before submitting PR

### Suggestions Welcome!
Have ideas for new features, improvements, or just want to share your thoughts? We're all ears! Feel free to:
- Open a GitHub issue with your suggestions
- Start a discussion in our community
- Reach out via email with your ideas

**No suggestion is too small or too big - we value all input that helps make SmartFinance better for everyone!**

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **Android Jetpack**: For providing excellent development tools
- **Kotlin**: For the amazing programming language
- **Material Design**: For beautiful design system
- **Open Source Community**: For inspiration and tools
- **Large Language Models**: For assistance with debugging and syntactical guidelines during development

---

**Built with love using Clean Architecture, SOLID principles, and modern Android development practices.**
