# Zowe IntelliJ Plug-in Conformance Evaluation Criteria

The Zowe Conformance Evaluation Criteria is a set of self-certifying and self-service tests to help the development
community integrate and extend specific technology into the Zowe framework.

This document describes the requirements of the three available conformance programs. Items marked **(required)** are
required for an application to be conformant. Items marked **(best practice)** are considered best practices for
conformant applications.

Throughout this document you will find the following terminology being used:

- _Zowe IntelliJ Plug-in_: An installable piece of software that provides new functionality to IntelliJ Platform or
  uses/calls services provided by related Zowe IntelliJ Plug-ins. Also simply referred to here as an "plug-ins", this
  can be a IntelliJ Plug-in or an independent piece of software. The conformance criteria below call out conformance
  requirements for Zowe IntelliJ Plug-ins, but it is possible that more kinds of plug-ins can be created. If such new
  plug-in kinds surface, then Zowe IntelliJ Plug-ins APIs, Zowe SDKs and this document can be expanded to support them
  in the future.
- _Zowe SDKs_ are [SDKs published by the Zowe project](https://docs.zowe.org/stable/user-guide/sdks-using) that provides
  various APIs for writing Zowe-based capabilities in general.

## Core

| #   | Ver | Required | Best Practise | Conformant | Criteria                                                                                                                                                                                                               | Additional Information                                |
|:----|:---:|:--------:|:-------------:|:-----------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------------------------------------------------|
| 1   | v1  |    x     |               |            | Plug-in must have the unique name                                                                                                                                                                                      |                                                       |
| 2   | v1  |    x     |               |            | If the plug-in uses the word "Zowe" in its name, it abides by The Linux Foundation Trademark Usage Guidelines and Branding Guidelines to ensure the word Zowe is used in a way intended by the Zowe community          | https://www.linuxfoundation.org/legal/trademark-usage |
| 3   | v1  |          |       x       |            | Normally the plug-in must not depend on other Zowe-related products until the real need                                                                                                                                |                                                       |
| 4   | v1  |    x     |               |            | Plug-in must be compatible with at least one of the latest major versions of IntelliJ                                                                                                                                  |                                                       |
| 5   | v1  |          |       x       |            | Plug-in should be compatible with the latest stable version of IntelliJ                                                                                                                                                |                                                       |
| 6   | v1  |          |       x       |            | Plug-in should support EAP IntelliJ versions when possible                                                                                                                                                             |                                                       |
| 7   | v1  |          |       x       |            | Plug-in should not use internal IntelliJ API when possible                                                                                                                                                             |                                                       |
| 8   | v1  |          |       x       |            | Plug-in should comply with the IntelliJ Platform UI Guidelines                                                                                                                                                         | https://jetbrains.github.io/ui/                       |
| 9   | v1  |    x     |               |            | If the plug-in accesses the same mainframe service as a Zowe CLI plug-in, the connection information should be shared via Zowe Team Config (zowe.config.json)                                                          |                                                       |
| 10  | v1  |    x     |               |            | If the plug-in uses and stores sensitive information, it must ensure the safety of such kind of information, normally - with the help of native IntelliJ Platform functions (credentials storage, virtual files, etc.) |                                                       |

## Distribution

| #   | Ver | Required | Best Practise | Conformant | Criteria                                                                                                                                                        | Additional Information |
|:----|:---:|:--------:|:-------------:|:-----------|:----------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------|
| 1   | v1  |    x     |               |            | Plug-in is being delivered either through the JetBrains marketplace or through the standalone zip/tar package                                                   |                        |
| 2   | v1  |    x     |               |            | If the plug-in is being delivered through the standalone package, it must describe the reason, why it is not being delivered through the JetBrains marketplace  |                        |

## Documentation

| #   | Ver | Required | Best Practise | Conformant | Criteria                                                                                                                                                                                                                                       | Additional Information |
|:----|:---:|:--------:|:-------------:|:-----------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:-----------------------|
| 1   | v1  |    x     |               |            | Plug-in must have comprehensive README.md with the information or the links to the other pieces of information on how to use, contribute, troubleshoot and support the plug-in                                                                 |                        |
| 2   | v1  |    x     |               |            | If the plug-in relates to the other Zowe IntelliJ plug-in, it must describe the way how they are related (which components are used, if the target plug-in extends the source one; what part of the source plug-in is being enhanced/reworked) |                        |
| 3   | v1  |          |       x       |            | If the plug-in is intended to be extended, it should provide the information on how to extend it (external API of the plug-in, best-practices on how to extend the plug-in)                                                                    |                        |
| 4   | v1  |          |       x       |            | If the plug-in uses Zowe SDK, it should mention the usage of the SDK at least in README.md with the information on what SDK is used, why and how it is used                                                                                    |                        |
