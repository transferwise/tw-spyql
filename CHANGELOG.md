# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.1] - 2022-02-01
### Changed
* Stop enforcing springboot platform

## [1.4.0] - 2021-05-28
### Changed
* SpyqlConnection is implementing ProxyConnection interface. This would allow to get a delegate connection
in places where unwrap does not work properly.

## [1.3.0] - 2021-05-28
### Changed
* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.
