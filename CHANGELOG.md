# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.6] - 2025-06-19

### Changed
* Added support for spring boot 3.5
* Removed support for spring boot 3.3
* Updated support for spring boot 3.4.0 to 3.4.6

## [1.6.5] - 2024-12-06

### Changed
* Added support for spring boot 3.4
* Removed support for spring boot 3.2

## [1.6.4] - 2024-07-16

### Changed
* - Added support for Spring Boot 3.3.
  - Updated dependencies.

## [1.6.3] - 2024-04-05

### Changed
* Use static methods to create BeanPostProcessors.

## [1.6.2] - 2024-02-22

### Changed
* - Added support for Spring Boot 3.2.
    - Updated dependencies.

## [1.6.1] - 2023-08-03

### Added

* Support for Spring Boot 3.1

### Bumped

* Build against Spring Boot 3.0.6 --> 3.0.9
* Build against Spring Boot 2.7.11 --> 2.7.14
* Build against Spring Boot 2.6.14 --> 2.6.15

## [1.6.0] - 2023-05-09

### Added

* Support for Spring Boot 3.0.

### Removed

* Support for Spring Boot 2.5.

## [1.5.0] - 2023-02-14

### Removed

* `tw-spyql-rx` module.
  It is not in use in Wise, and in the light of virtual threads will highly likely never needed again.
* `tw-spyql-test` module.
  Was testing the `tw-spyql-rx` module.

### Changed

* Spyql datasources and connections are now implementing new proxy interfaces.

* Upgraded all dependencies.

### Added

* Spring Boot matrix tests.

* Spring Boot starter module `tw-spyql-starter`.

## [1.4.1] - 2022-02-01

### Changed
*
* Stop enforcing springboot platform

## [1.4.0] - 2021-05-28
### Changed
* SpyqlConnection is implementing ProxyConnection interface. This would allow to get a delegate connection
in places where unwrap does not work properly.

## [1.3.0] - 2021-05-28
### Changed
* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.
