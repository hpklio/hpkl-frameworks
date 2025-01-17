# HPKL Frameworks

## Overview

The `hpkl-frameworks` repository provides scripts for generating configurations for various frameworks, packaging them into `.pkl` files, and publishing them as consumable packages. This simplifies integration and dependency management for your projects.

### Key Features
- **Configuration Generation:** Automates the generation of configuration files for supported frameworks.
- **Package Publishing:** Publishes configurations as reusable packages.
- **Framework Support:** Includes a variety of frameworks and versions.

---

## Supported Frameworks

### Spring Boot
- **3.3.0**
- **2.7.0**

### Spring gRPC
- **Client**: 2.13.1, 3.1.0
- **Server**: 2.13.1, 3.1.0

### Sentry
- **6.28.0**

---

## Installation and Usage

To use the configurations provided by `hpkl-frameworks`, include the desired dependency in your PklProject. Below is an example for adding Spring dependencies:

### Example: Adding Dependencies
```pkl
dependencies {
  ["spring"] { uri = "package://pkg.hpkl.io/hpkl-frameworks/spring-3.3.0@0.2.0" }
}
```

Replace `spring-3.3.0@0.2.0` with the desired package version and framework.

---

## How It Works

1. **Configuration Scripts:** Scripts generate framework-specific configuration files.
2. **Packaging:** The `.pkl` format is used for packaging the configurations.
3. **Publishing:** Packages are published to a repository for seamless consumption.

---

## Contributing
We welcome contributions! Please follow these steps:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Commit your changes with clear commit messages.
4. Submit a pull request for review.

---

## License
This project is licensed under the Apache2 License. See the [LICENSE](LICENSE) file for details.
