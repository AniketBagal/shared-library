def call(Map config = [:]) {
    def subject = config.success ? "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" 
                                 : "Build FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

    def color = config.success ? "green" : "red"
    def suggestion = config.success ? "Great job! No issues detected." 
                                    : (config.suggestion ?: "Please check the Jenkins console output for more information.")

    def errorTable = ""
    if (!config.success) {
        def errorLines = extractErrorsFromConsole()
        if (errorLines) {
            errorTable = """
                <h3 style="color:red;">Error Summary:</h3>
                <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse; font-family: Arial, sans-serif;">
                    <tr style="background-color: #f2f2f2;">
                        <th>#</th>
                        <th>Error Message</th>
                    </tr>
                    ${errorLines.collectWithIndex { line, i -> "<tr><td>${i + 1}</td><td>${line}</td></tr>" }.join("\n")}
                </table>
            """
        } else {
            errorTable = "<p><b>No error messages found in the console log.</b></p>"
        }
    }

    emailext (
        subject: subject,
        body: """
        <html>
            <body style="font-family: Arial, sans-serif;">
                <h2 style="color:${color}">${subject}</h2>
                <p><b>Project:</b> ${env.JOB_NAME}</p>
                <p><b>Build Number:</b> ${env.BUILD_NUMBER}</p>
                <p><b>Status:</b> ${config.success ? 'Success' : 'Failure'}</p>
                <p><b>Suggestions:</b> ${suggestion}</p>
                <hr/>
                ${errorTable}
                <hr/>
                <p><a href="${env.BUILD_URL}" style="color:blue;">Click here to view the full build log</a></p>
            </body>
        </html>
        """,
        mimeType: 'text/html',
        to: config.to ?: "aniketbagal12345@gmail.com"
    )
}

// Utility method for extracting filtered error lines from the Jenkins log
def extractErrorsFromConsole() {
    def errorKeywords = ["error", "failed", "exception", "not recognized", "not found"]
    def ignorePatterns = [
        ~/0 error\(s\)/i,
        ~/0 errors/i,
        ~/^Note:/i,
        ~/^\[INFO\]/i,
        ~/^\s*$/ // Empty lines
    ]

    def errorLines = []

    try {
        def logLines = currentBuild.rawBuild.getLog(100000)
        logLines.each { line ->
            def lowerLine = line.toLowerCase()
            def isKeywordMatch = errorKeywords.any { keyword -> lowerLine.contains(keyword) }
            def isIgnored = ignorePatterns.any { pattern -> line ==~ pattern }

            if (isKeywordMatch && !isIgnored) {
                errorLines << line.trim()
            }
        }
    } catch (e) {
        errorLines << "Could not parse console output: ${e.message}"
    }

    return errorLines
}
