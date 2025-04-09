def call(String buildStatus = 'SUCCESS', String buildNumber, String jobName, String buildUrl, String recipient) {
    def subject = "Build ${buildStatus}: ${jobName} #${buildNumber}"
    def log = currentBuild.rawBuild.getLog(10000).join('\n') // Increased to capture more errors
    def errorLines = log.readLines().findAll { it.toLowerCase().contains('error') }

    // Error to Suggestion Mapping
    def suggestionMap = [
        ~/.*error.*not recognized as an internal or external command.*/i:
            "Check if the executable name is correct and exists in the expected path.",
        ~/.*fatal error: '.*' file not found.*/i:
            "Verify if the required file exists and the include paths are correct.",
        ~/.*linker command failed with exit code.*/i:
            "Check for missing libraries or incorrect linker flags.",
        ~/.*undefined reference to.*/i:
            "Ensure all functions are implemented and the correct libraries are linked.",
        ~/.*compilation terminated.*/i:
            "Check for missing headers or syntax errors earlier in the file.",
        ~/.*expected.*before.*/i:
            "There might be a syntax issue; double-check your code structure.",
        ~/.*no such file or directory.*/i:
            "Ensure all referenced files exist and paths are correctly set.",
        ~/.*0 error\(s\).*/i:
            "There are no compilation errors. Check runtime logs for more info.",
        ~/.*permission denied.*/i:
            "Check file permissions or whether the user has access to required paths.",
        ~/.*cannot find -l.*/i:
            "A library is missing. Install the required dev packages.",
        ~/.*invalid use of incomplete type.*/i:
            "Forward declaration used without including full definition. Add proper header file.",
        ~/.*assignment makes integer from pointer without a cast.*/i:
            "Mismatched types. Ensure the pointer and target variable types are compatible."
    ]

    // Error Table
    def errorTable = new StringBuilder()
    def suggestionTable = new StringBuilder()
    int errorCount = 1

    errorTable << "<h3>Error Summary:</h3><table border='1' cellpadding='5' cellspacing='0'><tr><th>Error</th><th>Description</th></tr>"
    suggestionTable << "<h3>Suggestions:</h3><table border='1' cellpadding='5' cellspacing='0'><tr><th>Error</th><th>Suggestion</th></tr>"

    errorLines.each { line ->
        def errorKey = "Error ${errorCount}"
        errorTable << "<tr><td>${errorKey}</td><td>${line}</td></tr>"

        def matched = false
        suggestionMap.each { pattern, suggestion ->
            if (line ==~ pattern) {
                suggestionTable << "<tr><td>${errorKey}</td><td>${suggestion}</td></tr>"
                matched = true
            }
        }
        if (!matched) {
            suggestionTable << "<tr><td>${errorKey}</td><td>No suggestion available. Please check logs.</td></tr>"
        }
        errorCount++
    }

    errorTable << "</table>"
    suggestionTable << "</table>"

    def body = """
    <html>
    <body>
        <h2>Build ${buildStatus}: ${jobName} #${buildNumber}</h2>
        <p>View the build: <a href="${buildUrl}">${buildUrl}</a></p>
        ${errorTable}
        <br/>
        ${suggestionTable}
    </body>
    </html>
    """

    emailext(
        subject: subject,
        body: body,
        recipientProviders: [[$class: 'DevelopersRecipientProvider']],
        to: recipient,
        mimeType: 'text/html'
    )
}
