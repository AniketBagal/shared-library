def call(Map config = [:]) {
    def subject = config.success ? "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" 
                                 : "Build FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

    def color = config.success ? "green" : "red"
    def suggestion = config.success ? "Great job! No issues detected." : ""

    def errorTable = ""
    if (!config.success) {
        def errorLines = extractErrorsFromConsole()
        if (errorLines && errorLines.size() > 0) {
            errorTable = """
                <h3>Error Summary:</h3>
                <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                    <tr style="background-color: #f2f2f2;">
                        <th>#</th>
                        <th>Error Message</th>
                        <th>Suggestion</th>
                    </tr>
                    ${generateErrorTableRows(errorLines)}
                </table>
            """
        } else {
            errorTable = "<p><b>No relevant error messages found in the console log.</b></p>"
            suggestion = "Please check the Jenkins console output for more information."
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
                ${errorTable}
                <p><a href="${env.BUILD_URL}">View Build Details</a></p>
            </body>
        </html>
        """,
        mimeType: 'text/html',
        to: config.to ?: "aniketbagal12345@gmail.com"
    )
}

def extractErrorsFromConsole() {
    def errorKeywords = ["error", "failed", "exception", "not recognized", "not found"]
    def ignorePatterns = [
        ~/(?i).*0 error\(s\).*/,
        ~/(?i).*0 errors.*/,
        ~/(?i).*0 warnings.*/,
        ~/^Note:.*/,
        ~/^\[INFO\].*/,
        ~/^\s*$/ // Empty lines
    ]

    def errorSet = [] as LinkedHashSet  // Preserve order, remove duplicates

    try {
        def logLines = currentBuild.rawBuild.getLog(100000)
        logLines.each { line ->
            def lowerLine = line.toLowerCase()
            def isKeywordMatch = errorKeywords.any { keyword -> lowerLine.contains(keyword) }
            def isIgnored = ignorePatterns.any { pattern -> (line ==~ pattern) }

            if (isKeywordMatch && !isIgnored) {
                errorSet << line.trim()
            }
        }
    } catch (e) {
        errorSet << "Could not parse console output: ${e.message}"
    }

    return errorSet.toList()
}

def getSuggestionForError(String errorLine) {
    def suggestionsMap = [
        (~/.*not recognized as an internal or external command.*/) : "Check if the tool is installed and added to PATH.",
        (~/.*No such file or directory.*/)                         : "Ensure the file path is correct and the file exists.",
        (~/.*Permission denied.*/)                                 : "Check file/directory permissions.",
        (~/.*Connection timed out.*/)                              : "Verify network connectivity and target availability.",
        (~/.*Compilation failed.*/)                                : "Check for syntax errors or missing dependencies.",
        (~/.*java.lang.NullPointerException.*/)                    : "Make sure all objects are initialized.",
        (~/.*Build step.*failed.*/)                                : "Review the step’s logs for more info.",
        (~/.*error: cannot find symbol.*/)                         : "Check for missing imports or undefined variables.",
        (~/.*error MSB.*: The command exited with code [1-9].*/)   : "Check .vcxproj or related scripts for issues.",
        (~/.*fatal error LNK\d+:.*/)                               : "Verify linker settings and dependencies.",
        (~/.*C\d{4}: .*/)                                          : "Lookup compiler error code (e.g., C1004) for details.",
        (~/.*Authentication failed.*/)                             : "Check your Git credentials or token validity.",
        (~/.*Failed to fetch from.*/)                              : "Verify Git URL and credentials. Ensure remote repo is accessible.",
        (~/.*Maximum checkout retry attempts reached.*/)           : "Verify SCM settings or increase retry attempts.",
        (~/.*Logon failed, use ctrl\+c to cancel.*/)               : "Make sure Git credentials are set up correctly in Jenkins."
    ]

    for (pattern in suggestionsMap.keySet()) {
        if (errorLine ==~ pattern) {
            return suggestionsMap[pattern]
        }
    }

    return "Refer to the console log or documentation for more information."
}

def generateErrorTableRows(List errorLines) {
    def rows = ""
    for (int i = 0; i < errorLines.size(); i++) {
        def error = errorLines[i]
        def suggestion = getSuggestionForError(error)
        rows += "<tr><td>${i + 1}</td><td>${error}</td><td>${suggestion}</td></tr>\n"
    }
    return rows
}
