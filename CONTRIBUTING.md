# Contribution Guidelines

#### If you have something to introduce, but there is no related issue in the project repo, then you are free to either create the issue by yourself, or contact us to help you with it.

This document is a living summary of conventions and best practices for development within For Mainframe Plugin.

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
4. When you are ready, create an issue for your changes with "to be defined" label
5. Describe the issue:
   - In case of bug - make a reproduction scheme or short description on how to achieve this;
   - In case of new feature or improvement - describe, what you are trying to implement, how it should work, and (if applicable) why it should be introduced in the plugin;
6. After the changes are made, create a pull request on any of the main branches in the project repo. ***It is not a problem if you specified an incorrect target branch, we will help you with it before the changes are pushed***
7. Assign Uladzislau Kalesnikau as a reviewer to the pull request
8. Attach the issue to the pull request
9. Wait on the approval (thanks in advance)
