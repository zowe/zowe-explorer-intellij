/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright IBA Group 2020
 */
def jiraSite = 'jira-iba'
def gitCredentialsId = 'e92a3d13-efc3-47d7-955f-a78ad9d7faac'
//def gitUrl = 'https://code.iby.icdc.io/ijmp/for-mainframe.git'
def gitUrl = 'git@code.ycz.icdc.io:ijmp/for-mainframe.git'
def apacheInternalUrl = 'http://jenks2.iba6d.cmp.ycz.icdc.io'
def jenkinsServerUrl = 'http://jenks2.iba6d.cmp.ycz.icdc.io:8080'
def resultFileName = ''
String jiraTicket = ''
def gitlabBranch = env.BRANCH_NAME
properties([gitLabConnection('code.ycz.icdc.io-connection')])

// @NonCPS
// def changeVersion(String xmlFile) {

//     def xml = new XmlSlurper().parseText(xmlFile)
//     println xml.'idea-version'.'@since-build'
//     xml.'idea-version'.'@since-build' =  '203.7148.72'
//     def w = new StringWriter()
//     XmlUtil.serialize(xml, w)
//     return w.toString()
// }

pipeline {
    agent any
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, skipWorkInProgressMergeRequest: true,
                noteRegex: "Jenkins please retry a build")
    }
    options {
        disableConcurrentBuilds()
    }
    tools {
        gradle 'Default'
        jdk 'Java 11'
    }
    stages {
        stage('Initial checkup') {
            steps {
                sh 'java -version'
            }
        }
        stage('Get Jira Ticket') {
            steps {
                echo gitlabBranch
                script {
                    if (gitlabBranch.equals("development")) {
                        jiraTicket = 'development'
                    } else if (gitlabBranch.equals("zowe-development")) {
                        jiraTicket = 'zowe-development'
                    } else if (gitlabBranch.startsWith("release/")) {
                        jiraTicket = gitlabBranch
                    } else {
                        def pattern = ~/(?i)ijmp-\d+/
                        def matcher = gitlabBranch =~ pattern
                        if (matcher.find()) {
                            jiraTicket = matcher[0].toUpperCase()
                        } else {
                            jiraTicket = "null"
                            echo "Jira ticket name wasn't found!"
                        }
                    }
                }
                echo "Jira ticket: $jiraTicket"
            }
        }
        stage('Clone Branch') {
            steps {
                cleanWs()
                sh "ls -la"
                git branch: "$gitlabBranch", credentialsId: "$gitCredentialsId", url: "$gitUrl"
            }
        }
        stage('Build Plugin') {
            steps {
                // sh 'sudo chmod +x /etc/profile.d/gradle.sh'
                // sh 'sudo -s source /etc/profile.d/gradle.sh'
                sh './gradlew -v'
                sh './gradlew test'
                sh './gradlew buildPlugin'
            }
        }
        stage('Verify Plugin') {
            steps {
                withGradle {
                    script {
                        if (gitlabBranch.contains("release")) {
                            sh './gradlew runPluginVerifier'
                        } else {
                            echo 'Plugin verification is skipped as the branch to verify is not a release branch'
                        }
                    }
                }
            }
        }
        stage('Form and post Jira message') {
            steps {
                script {
                    resultFileName = sh(returnStdout: true, script: "cd build/distributions/ && ls").trim()
                }
                sh """
                if [ "$jiraTicket" = "null" ]
                then
                    echo "jira ticket is not determined"
                else
                    if [ -d "/var/www/ijmp-plugin/$jiraTicket" ]
                    then
                        sudo rm -r /var/www/ijmp-plugin/$jiraTicket
                    fi
                    sudo mkdir -p /var/www/ijmp-plugin/$jiraTicket
                    sudo mkdir /var/www/ijmp-plugin/$jiraTicket/idea

                    sudo mv build/distributions/$resultFileName /var/www/ijmp-plugin/$jiraTicket/idea
                fi
                """
            }
        }

        // stage('Change Plugin Version'){
        //     steps{
        //         script{
        //             def xmlFileData = readFile(file: "src/main/resources/META-INF/plugin.xml")
        //             def res = changeVersion(xmlFileData)
        //             writeFile file: "src/main/resources/META-INF/plugin.xml", text: res
        //         }
    }
    post {
        success {
            script {
                if (!jiraTicket.startsWith('release') && !'development'.equals(jiraTicket) && !'zowe-development'.equals(jiraTicket) && !"null".equals(jiraTicket)) {
                    jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch was successfully built. You can download your build from the following link $apacheInternalUrl/ijmp-plugin/$jiraTicket/idea/$resultFileName.", site: "$jiraSite"
                }
            }
        }
        failure {
            script {
                if (!jiraTicket.startsWith('release') && !'development'.equals(jiraTicket) && !'zowe-development'.equals(jiraTicket) && !"null".equals(jiraTicket)) {
                    def gitlabBranchUrlEncoded = java.net.URLEncoder.encode(gitlabBranch, "UTF-8")
                    def fullUrl = "$jenkinsServerUrl/job/BuildPluginPipelineMultibranch/job/$gitlabBranchUrlEncoded/${env.BUILD_NUMBER}/console"
                    jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch failed to build for Intellij IDEA. You can get console output by the following link $fullUrl", site: "$jiraSite"
                }
            }
        }
    }
}
