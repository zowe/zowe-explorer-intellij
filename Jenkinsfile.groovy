import groovy.xml.XmlUtil

def jiraSite = 'jira-iba'
def gitCredentialsId = 'c688480b-ff31-4c6f-8edd-8b38088ab5ec'
//def gitUrl = 'https://git.icdc.io/ijmp/for-mainframe.git'
def gitUrl = 'git@git.icdc.io:ijmp/for-mainframe.git'
def resultFileName = ''
String jiraTicket = ''
def gitlabBranch = env.BRANCH_NAME
properties([gitLabConnection('git.icdc.io-connection')])


// @NonCPS
// def changeVersion(String xmlFile) {
    
    
//     def xml = new XmlSlurper().parseText(xmlFile)
//     println xml.'idea-version'.'@since-build'
//     xml.'idea-version'.'@since-build' =  '203.7148.72'

//     def w = new StringWriter()
//     XmlUtil.serialize(xml, w)
    
    
//     return w.toString()

// }

pipeline{
    agent any
    triggers {
        gitlab(triggerOnPush: true, triggerOnMergeRequest: true, skipWorkInProgressMergeRequest: true,
                noteRegex: "Jenkins please retry a build")
    }
    options { 
        disableConcurrentBuilds() 
    }
    tools{
        gradle 'Default'
    }
    stages{
        stage('Initial checkup'){
            steps{
                sh 'java -version'
            }
        }
        stage('Get Jira Ticket'){
            steps{
                echo gitlabBranch
                script{
                    if(gitlabBranch.equals("development")){
                        jiraTicket = 'development'
                    } else if(gitlabBranch.equals("zowe-development")) {
                        jiraTicket = 'zowe-development'
                    } else if(gitlabBranch.contains("release")){
                        version = gitlabBranch.split("/")[1]
                        jiraTicket = 'release/' + version
                    } else {
                        def branchName = gitlabBranch.split("/")[1]
                        def tokens = branchName.split("-")
                        jiraTicket = tokens[0] + "-" + tokens[1]
                    }
                }
                echo "$jiraTicket"
            }
        }
        stage('Clone Branch'){
            steps{
                cleanWs()
                sh "ls -la"
                git branch: "$gitlabBranch", credentialsId: "$gitCredentialsId", url: "$gitUrl"
            }
        }
        stage('Build Plugin IDEA'){
            steps{
                //sh 'sudo chmod +x /etc/profile.d/gradle.sh'
                //sh 'sudo source /etc/profile.d/gradle.sh'
                withGradle {
                    //sh 'gradle -v'
                    sh 'gradle wrapper'
                    sh './gradlew buildPlugin'
                }
            }
        }
        stage('Move to the AWS - IDEA'){
            steps{
                script{
                    resultFileName = sh(returnStdout: true, script: "cd build/distributions/ && ls").trim()
                }
                sh """
                    if [ -d "/var/www/ijmp-plugin/$jiraTicket" ] 
                    then
                        sudo rm -r /var/www/ijmp-plugin/$jiraTicket
                    fi
                    
                    sudo mkdir /var/www/ijmp-plugin/$jiraTicket
                    sudo mkdir /var/www/ijmp-plugin/$jiraTicket/idea
                    sudo mkdir /var/www/ijmp-plugin/$jiraTicket/pycharm
                """
                sh "sudo mv build/distributions/$resultFileName /var/www/ijmp-plugin/$jiraTicket/idea"
            }
            post{
                success {
                    script{
                        if(!jiraTicket.contains('release') && !'development'.equals(jiraTicket) && !'zowe-development'.equals(jiraTicket)){
                            jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch was successfully built. You can download your build from the following link http://10.221.23.186/ijmp-plugin/$jiraTicket/idea/$resultFileName.", site:"$jiraSite" 
                        }
                        
                    }
                }
                failure {
                    script{
                        if(!jiraTicket.contains('release') && !'development'.equals(jiraTicket) && !'zowe-development'.equals(jiraTicket)){
                            jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch failed to build for Intellij IDEA. You can get console output by the following link http://10.221.23.186:8080/job/BuildPluginPipeline/", site:"$jiraSite" 
                        }
                    }
                }
            }
        }

        // stage('Change Plugin Version'){
        //     steps{
        //         script{
        //             def xmlFileData = readFile(file: "src/main/resources/META-INF/plugin.xml")
        //             def res = changeVersion(xmlFileData)
        //             writeFile file: "src/main/resources/META-INF/plugin.xml", text: res
        //         }
                
        //     }
        // }
        // stage('Build Plugin PyCharm'){
        //     steps{
        //         //sh 'sudo chmod +x /etc/profile.d/gradle.sh'
        //         //sh 'sudo source /etc/profile.d/gradle.sh'
        //         withGradle {
        //             //sh 'gradle -v'
        //             sh 'gradle wrapper'
        //             sh './gradlew buildPlugin'
        //         }
        //     }
        // }
        // stage('Move to the AWS - PyCharm'){
        //     steps{
        //         script{
        //             resultFileName = sh(returnStdout: true, script: "cd build/distributions/ && ls").trim()
        //         }
        //         sh "sudo mv build/distributions/$resultFileName /var/www/ijmp-plugin/$jiraTicket/pycharm"
        //     }
        //     post{
        //         success {
        //             script{
        //                 if(!jiraTicket.contains('release') && !'development'.equals(jiraTicket)){
        //                     jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch was successfully built for PyCharm version. You can download your build from the following link http://10.221.23.186/ijmp-plugin/$jiraTicket/pycharm/$resultFileName.", site:"$jiraSite" 
        //                 }
                        
        //             }
        //         }
        //         failure {
        //             script{
        //                 if(!jiraTicket.contains('release') && !'development'.equals(jiraTicket)){
        //                     jiraAddComment idOrKey: "$jiraTicket", comment: "Hello! It's jenkins. Your push in branch failed to build for PyCharm. You can get console output by the following link http://10.221.23.186:8080/job/BuildPluginPipeline/", site:"$jiraSite" 
        //                 }
        //             }
        //         }
        //     }
        // }

        
    }
    
    
    
}