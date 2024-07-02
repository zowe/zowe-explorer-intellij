# Contribution Guidelines

#### If you have something to introduce, but there is no related issue in the project repo, then you are free to either create the issue by yourself, or contact us to help you with it.

This document is a living summary of conventions and best practices for development within For Mainframe Plugin.

  - [General](#general)
  - [Coding standards](#coding-standards)
  - [Testing Guidelines](#testing-guidelines)
  - [Branch Naming Guidelines](#branch-naming-guidelines)
  - [Steps To Contribute](#steps-to-contribute)

## General
The following list describes general conventions for contributing to For Mainframe Plugin:
* Feel free to ask any questions related to the project or its components.
* Before introducing some new functionality, please, discuss it with any of the project team members.
* Reuse logging and error handling patterns already in place.
* To be clear in future after you developed a new functionality, don't forget to leave the @author KDoc tag in your comments.
* Document your changes as descriptive as you can. We use Javadoc with our project.
* Provide adequate logging to diagnose problems that happen at external customer sites.
* Follow the general formatting rules for the code you are working on.
* Use sensible class, method, and variable names.
* Keep core services independent without cross dependencies.
* Keep classes small, maintain Single Responsibility Principle.
* Pull requests should include tests.
* Code coverage for new code should be at least 80%.
* SonarCloud quality gate should be passed and no code smells, security hotspots and bugs should be added in pull requests.

### Acceptable contributions
* Follows our coding standards
* Follows our branch naming standard
* Fully described in terms of changes in the existing code

### Unacceptable contributions
* Non-descriptive commits with name "Fix" or "Feature", etc. (Acceptable only if the whole bunch of changes is described in the last commit / pull request / issue)
* Breaking changes without the workaround for the minor project versions
* With comments different from the English language

## Coding standards
Our project follows the next coding rules:
- [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- [Java Coding Conventions](https://google.github.io/styleguide/javaguide.html)

## Testing Guidelines
To provide stable and reliable code, our project is regularly tested, either manually or through the automated test suite.
There are two types of tests, used in here:
- Unit-tests
- UI regression tests

Unit-tests are written using chain [Kotest](https://kotest.io/) + [MockK](https://mockk.io/) + [Kover](https://github.com/Kotlin/kotlinx-kover).
All the unit-tests are stored under [src/test](src/test/).
We use Kotest's `ShouldSpec` testing style to write our tests.
To provide mocked interfaces and semi-initialized IntelliJ platform's components during the tests execution time, there is a [WithApplicationShouldSpec](src/test/kotlin/eu/ibagroup/formainframe/testutils/WithApplicationShouldSpec.kt) abstract class. This class allows testers to mock out some services without the need to fully initialize them, and provides the IntelliJ application in a lightweight headless mode.
To see how we mock our services, investigate [this folder](src/test/kotlin/eu/ibagroup/formainframe/testutils/testServiceImpl/)
For more info on how to deal with tests in IntelliJ platform, refer to [this guide](https://plugins.jetbrains.com/docs/intellij/testing-plugins.html).

UI regression tests are written using chain [JUnit](https://junit.org/) + [IntellIJ UI test framework](https://github.com/JetBrains/intellij-ui-test-robot).
The UI tests are stored under [srv/uiTest](src/uiTest/).
These tests are too heavy to run after each build and are meant to run separately from the common development flow to make sure that there are no core-breaking changes to the plug-in's functionality.

***Important: These tests are running interactively. If you want to run the UI tests, consider do it on some other device or place, different from the main one where you are working. For these tests to run successfully, it is needed to exclude any other interactions with your device until they are finished to simulate all the actions, the same as user would do working with the plug-in.***

For the guides on how to run tests, see [this README section](README.md/#how-to-run-tests)

## Branch Naming Guidelines
This project follows the branching strategy:
- **main** - the main project branch with the latest stable code changes. It is changed as soon as the new version of the project is released.
- **release** - this branch is used to store the latest changes before the new project version release.
- **bugfix** - this branch is used to introduce some fix for a bug. It is a short-term branch, used to fix the bug and check the changes before it is pushed to some stable branch.
- **feature** - this branch is used to develop some new functionality for the project. It is a short-term branch, used to introduce the changes and check them before it is pushed to some stable branch.

To create a new branch, related to a GitHub issue, you need to follow the template:

`<branch_type>/GHI-<issue_number>[-your-description]`

***Important: you should follow the branch naming guidelines before you publish your changes to the project. In other case, the pull request you create could be denied due to incorrect branch name.***

### Example on branches usage
Consider some new feature for the plugin. The first that you need is to create a new branch from the branch you want to change (it depends on whether you want to introduce it as a bugfix or as a feature). The branch name will be something like **feature/GHI-123-introduce-some-cool-functionality**. ***It is necessary to have the right branch name, that follows the template!***

## Steps To Contribute
1. Create a fork of this repository
2. Create a branch using [Branch Naming Guidelines](#branch-naming-guidelines)
3. Make any changes you want
4. Provide at least 80% of test coverage for all the new methods and their branches
5. When you are ready, create an issue for your changes with "to be defined" label
6. Describe the issue:
   - In case of bug - make a reproduction scheme or short description on how to achieve this;
   - In case of new feature or improvement - describe, what you are trying to implement, how it should work, and (if applicable) why it should be introduced in the plugin;
7. After the changes are made, create a pull request on any of the main branches in the project repo. ***It is not a problem if you specified an incorrect target branch, we will help you with it before the changes are pushed***
8. Assign Uladzislau Kalesnikau as a reviewer to the pull request
9. Attach the issue to the pull request
10. Wait on the approval (thanks in advance)
