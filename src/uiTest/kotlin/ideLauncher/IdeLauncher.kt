/*
 * Copyright (c) 2020-2024 IBA Group.
 *
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   IBA Group
 *   Zowe Community
 */

package ideLauncher

import workingset.ideLauncher.Ide
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

object IdeLauncher {

    fun launchIde(
        pathToIde: Path,
        additionalProperties: Map<String, Any>,
        additionalVmOptions: List<String>,
        requiredPluginsArchives: List<Path>,
        ideSandboxDir: Path,
        beforeLaunchAction: (Map<String, Any>) -> Unit = {}
    ): Process {
        val configDir = Files.createTempDirectory(ideSandboxDir, "config")
        val systemDir = Files.createTempDirectory(ideSandboxDir, "system")
        val pluginsDir = Files.createTempDirectory(ideSandboxDir, "plugins")
        for (pluginPath in requiredPluginsArchives) {
            if (pluginPath.fileName.toString().endsWith(".jar")) {
                Files.copy(pluginPath, pluginsDir.resolve(pluginPath.fileName.toString()))
            } else {
                extractZip(pluginPath, pluginsDir)
            }
        }
        val logDir = Files.createTempDirectory(ideSandboxDir, "log")

        val ideProperties =
            buildIdeProperties(pathToIde, configDir, systemDir, pluginsDir, logDir) + additionalProperties
        val vmOptions = readIdeDefaultVmOptions(pathToIde) + additionalVmOptions

        beforeLaunchAction(ideProperties)
        return runIde(pathToIde, ideSandboxDir, ideProperties.mapValues { it.value.toString() }, vmOptions)
    }

    private fun buildIdeProperties(
        pathToIde: Path,
        configDir: Path,
        systemDir: Path,
        pluginsDir: Path,
        logDir: Path
    ): Map<String, Any> {
        val defaultIdeProperties = Properties().apply {
            val defaultPropertiesFileRelativePath = when (Os.hostOS()) {
                Os.MAC -> "Contents/bin/idea.properties"
                else -> "bin${File.separator}idea.properties"
            }
            Files.newBufferedReader(pathToIde.resolve(defaultPropertiesFileRelativePath)).use {
                load(it)
            }
        }.mapKeys { it.key.toString() }

        return mapOf(
            "idea.config.path" to configDir.toAbsolutePath(),
            "idea.system.path" to systemDir.toAbsolutePath(),
            "idea.plugins.path" to pluginsDir.toAbsolutePath(),
            "idea.log.path" to logDir.toAbsolutePath(),
            "jb.privacy.policy.text" to "<!--999.999-->",
            "jb.consents.confirmation.enabled" to "false",
            "native.mac.file.chooser.enabled" to "false",
            "ide.mac.file.chooser.native" to "false",
            "idea.is.internal" to "true",
            "idea.trust.all.projects" to "true",
            "eap.require.license" to "false",
            "Dcom.intellij.certs.ignore" to "true",
//            "Dcom.sun.jndi.ldap.object.disableEndpointIdentification" to true,
        ) + defaultIdeProperties
    }

    private fun readIdeDefaultVmOptions(pathToIde: Path): List<String> {
        val ideBinDir = pathToIde.resolve(
            when (Os.hostOS()) {
                Os.MAC -> "Contents/bin"
                else -> "bin"
            }
        )
        val ideDefaultVmOptionsFile = Files.list(ideBinDir).filter {
            val fileName = it.fileName.toString()
            fileName.endsWith(".vmoptions") && !fileName.contains("(client|cwm)".toRegex())
        }.collect(Collectors.toList()).let { vmOptionsFiles ->
            if (vmOptionsFiles.size == 1) {
                vmOptionsFiles[0]
            } else {
                vmOptionsFiles.singleOrNull {
                    it.fileName.toString().contains("64")
                } ?: throw FileNotFoundException("failed to find default vmoptions file")
            }
        }

        return ideDefaultVmOptionsFile.toFile().readLines()
    }

    private fun runIde(
        pathToIde: Path,
        ideSandboxDir: Path,
        ideProperties: Map<String, String>,
        vmOptions: List<String>
    ): Process {
        val idePropertiesFilePath = ideSandboxDir.resolve("idea.properties").also { file ->
            Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
                Properties().apply {
                    putAll(ideProperties)
                    store(it, null)
                }
            }
        }
        val vmOptionsFilePath = ideSandboxDir.resolve("idea.vmoptions").also { file ->
            Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING).use {
                it.write(vmOptions.joinToString(System.lineSeparator()))
            }
        }

        val buildTxtFilePath =
            if (Os.MAC === Os.hostOS()) pathToIde.resolve("Contents/Resources/build.txt") else pathToIde.resolve("build.txt")
        val ide = Ide.withCode(Files.readAllLines(buildTxtFilePath).single().substringBefore("-"))

        val processBuilder = ProcessBuilder().apply {
            environment().putAll(
                mapOf(
                    ide.getIdePropertiesEnvVarName() to idePropertiesFilePath.toAbsolutePath().toString(),
                    ide.getVmOptionsEnvVarName() to vmOptionsFilePath.toAbsolutePath().toString()
                )
            )
        }

        val startupScriptName = when (ide) {
            Ide.IDEA_COMMUNITY, Ide.IDEA_ULTIMATE -> "idea"
            Ide.CLION -> "clion"
            Ide.WEBSTORM -> "webstorm"
            Ide.RUBY_MINE -> "rubymine"
            Ide.PYCHARM, Ide.PYCHARM_COMMUNITY -> "pycharm"
            Ide.RIDER -> "rider"
        }
        val startupScriptPath = when (Os.hostOS()) {
            Os.LINUX -> "bin/${startupScriptName}.sh"
            Os.WINDOWS -> "bin/${startupScriptName}64.exe"
            Os.MAC -> "Contents/MacOS/${findIdeExecutableNameInInfoPlist(pathToIde)}"
        }
        return processBuilder.command(pathToIde.resolve(startupScriptPath).toAbsolutePath().toString()).inheritIO()
            .start()
    }

    private fun findIdeExecutableNameInInfoPlist(pathToIde: Path): String {
        return ProcessBuilder().command(
            "defaults",
            "read",
            "${pathToIde.toAbsolutePath()}/Contents/Info",
            "CFBundleExecutable"
        ).start().let {
            it.waitFor(10, TimeUnit.SECONDS)
            it.inputStream.bufferedReader().readText().trim()
        }
    }
}
