
def call(Map config = [:]) {
    def solution = config.solution ?: 'cpp-build.sln'
    def exePath = config.exePath ?: 'x64\\Release\\cpp-build.exe'
    def aipPath = config.aipPath ?: 'installer\\InstallerProject.aip'
    def aic = config.advInstallerCli ?: '"C:\\Program Files (x86)\\Caphyon\\Advanced Installer 20.8\\bin\\x86\\AdvancedInstaller.com"'
    def productName = config.productName ?: 'My C++ App'


    pipeline {
        agent any

        environment {
            VERSION = "${new Date().format('yyyy')}.1.0.${env.BUILD_NUMBER}"
        }

        stages {
            stage('Build C++ Project') {
                steps {
                    bat "msbuild ${solution} /p:Configuration=Release /p:Platform=x64"
                }
            }

            stage('Prepare Installer Project') {
                steps {
                    bat """
                    if not exist ${aipPath} (
                        ${aic} /newproject ${aipPath} /type "Simple"
                    )

                    ${aic} /edit ${aipPath} -add ${exePath} ${installDir}
                    ${aic} /edit ${aipPath} -setprop ProductVersion %VERSION%
                    ${aic} /edit ${aipPath} -setprop ProductName "${productName}"
                    """
                }
            }

            stage('Build Installer') {
                steps {
                    bat "${aic} /build ${aipPath}"
                }
            }
        }

        post {
            success {
                echo " Installer built successfully. Version: ${env.VERSION}"
            }
            failure {
                echo " Installer build failed."
            }
        }
    }
}
