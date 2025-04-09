import java.util.regex.Pattern

def call(Map config = [:]) {
    def subject = config.success ? "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" 
                                 : "Build FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

    def color = config.success ? "green" : "red"
    def suggestion = config.success ? "Great job! No issues detected." : (config.suggestion ?: "Please check the Jenkins console output for more information.")

    def errorTable = ""
    if (!config.success) {
        def errorLines = extractErrorsFromConsole()
        if (errorLines) {
            errorTable = """
                <h3>Error Summary:</h3>
                <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                    <tr style="background-color: #f2f2f2;">
                        <th>Error</th>
                        <th>Error Description</th>
                    </tr>
                    ${errorLines.collectWithIndex { line, i -> "<tr><td>Error ${i + 1}</td><td>${line}</td></tr>" }.join("\n")}
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
            <body>
                <h2 style="color:${color}">${subject}</h2>
                <p><b>Project:</b> ${env.JOB_NAME}</p>
                <p><b>Build #:</b> ${env.BUILD_NUMBER}</p>
                <p><b>Status:</b> ${config.success ? 'Success' : 'Failure'}</p>
                <p><b>Suggestions:</b> ${suggestion}</p>
                ${errorTable}
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
        Pattern.compile("0 error\\(s\\)", Pattern.CASE_INSENSITIVE),
        Pattern.compile("0 errors", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^Note:", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\[INFO\\]", Pattern.CASE_INSENSITIVE),
        Pattern.compile("^\\\\s*\$") // Empty lines (escaped properly)
    ]

    def errorLines = []

    try {
        def logLines = currentBuild.rawBuild.getLog(100000)
        logLines.each { line ->
            def lowerLine = line.toLowerCase()
            def isKeywordMatch = errorKeywords.any { keyword -> lowerLine.contains(keyword) }
            def isIgnored = ignorePatterns.any { pattern -> pattern.matcher(line).matches() }

            if (isKeywordMatch && !isIgnored) {
                errorLines << line.trim()
            }
        }
    } catch (e) {
        errorLines << "Could not parse console output: ${e.message}"
    }

    return errorLines
}
