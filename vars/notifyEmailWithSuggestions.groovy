def call(Map config = [:]) {
    def buildStatus = config.buildStatus ?: 'SUCCESS'
    def buildNumber = config.buildNumber ?: 'N/A'
    def jobName = config.jobName ?: 'N/A'
    def buildUrl = config.buildUrl ?: ''
    def recipient = config.recipient ?: 'your-email@example.com'

    def log = currentBuild.rawBuild.getLog(1000).join("\n")
    def errors = []

    def errorSuggestions = [
        (~/(?i).*cpp-buil\.exe' is not recognized as an internal or external command.*/): "Fix the typo: it should be 'cpp-build.exe', not 'cpp-buil.exe'.",
        (~/(?i).*error C\d{4}.*/): "C++ compilation error. Check for syntax issues or missing headers.",
        (~/(?i).*fatal error LNK\d+.*/): "Linker error. Check project linker settings and library paths.",
        (~/(?i).*MSB\d{4}.*/): "MSBuild error. Verify the .sln or .vcxproj file path and configuration.",
        (~/(?i).*BUILD FAILED.*/): "General build failure. Check preceding logs for specific errors.",
        (~/(?i).*Access is denied.*/): "Permission issue. Run Jenkins agent with appropriate access rights.",
        (~/(?i).*No such file or directory.*/): "Missing file. Verify all paths and dependencies are correct.",
        (~/(?i).*cannot open include file:.*No such file or directory.*/): "Missing header. Ensure all include paths are properly set.",
        (~/(?i).*Undefined reference to .*/): "Linking issue. Ensure all required object files and libraries are included.",
        (~/(?i).*java.lang.OutOfMemoryError.*/): "Java memory issue. Try increasing heap size.",
        (~/(?i).*Timeout exceeded.*/): "Execution timeout. Optimize the script or increase the timeout limit.",
        (~/(?i).*not a git repository.*/): "Repository issue. Check the Git configuration and URL.",
        (~/(?i).*could not resolve host.*/): "DNS issue. Ensure the hostname is correct and reachable.",
        (~/(?i).*Failed to connect to .* port .*/): "Network issue. Ensure the service is running and accessible."
    ]

    // Collect matched errors
    errorSuggestions.eachWithIndex { pattern, suggestion, i ->
        if (log =~ pattern) {
            errors << [id: "Error ${errors.size() + 1}", pattern: pattern.pattern(), suggestion: suggestion]
        }
    }

    // Format HTML tables
    def errorTable = errors.collect { "<tr><td>${it.id}</td><td>${it.pattern}</td></tr>" }.join("\n")
    def suggestionTable = errors.collect { "<tr><td>${it.id}</td><td>${it.suggestion}</td></tr>" }.join("\n")

    def subject = "Build ${buildStatus}: ${jobName} #${buildNumber}"
    def body = """
        <h2>Build ${buildStatus}</h2>
        <p><b>Project:</b> ${jobName}<br>
        <b>Build #:</b> ${buildNumber}<br>
        <b>URL:</b> <a href="${buildUrl}">${buildUrl}</a></p>
        <h3>Error Summary:</h3>
        <table border="1" cellpadding="5" cellspacing="0">
            <tr><th>Error</th><th>Description</th></tr>
            ${errorTable}
        </table>
        <h3>Suggestions:</h3>
        <table border="1" cellpadding="5" cellspacing="0">
            <tr><th>Error</th><th>Suggestion</th></tr>
            ${suggestionTable}
        </table>
    """

    emailext(
        subject: subject,
        body: body,
        to: recipient,
        mimeType: 'text/html'
    )
}
