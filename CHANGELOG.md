## [1.16] - 2024-03-07
### Added
- Added support for Eyes SCM integration [Trello 3273](https://trello.com/c/GFvXy9jP)

## [1.15] - 2024-01-28
### Updated
- Check if using a custom batch id before archiving it. [Trello 2201](https://trello.com/c/B8KrpLpF)
- Update `jenkins.version` to version `2.361.4` as [recommended here](https://www.jenkins.io/doc/developer/plugin-development/choosing-jenkins-baseline/).
- Update parent pom to `4.51`.
- Remove deprecated `java.level` property.
- Bump JobDSL to `1.72` to rely on non-vulnerable version.
- Rely on Jenkins core BOM for workflow plugins versions.

## [1.14] - 2023-02-27
### Fixed
- Fixed crash on newer Jenkins versions.

## [1.12] - 2019-11-13
### Added
- Allow setting Batch ID explicitly. 

## [1.11] - 2019-11-13
### Added
- This CHANGELOG file.
- Batch close & notification support.
