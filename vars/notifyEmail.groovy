def call(Map config = [:]) {
    def subject = config.success ? "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}" 
                                 : "Build FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
    def color = config.success ? "green" : "red"
    def suggestion = config.success ? "Great job! No issues detected." : (config.suggestion ?: "Please check Jenkins console output for more info.")

    def errorTable = ""
    if (!config.success) {
        def errorLines = extractErrorsFromConsole()
        if (errorLines) {
            errorTable = """
                <h3>Error Summary:</h3>
                <table border="1" cellpadding="5" cellspacing="0">
                    <tr><th>#</th><th>Error Line</th></tr>
            """
            errorLines.eachWithIndex { line, i ->
                errorTable += "<tr><td>${i + 1}</td><td>${line}</td></tr>"
            }
            errorTable += "</table><br/>"
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
                <p><b>Suggestions:</b> ${suggestion}</p>
                <!-- <p><a href="${env.BUILD_URL}">View Build</a></p> -->
            </body>
        </html>
        """,
        mimeType: 'text/html',
        to: config.to ?: "aniketbagal12345@gmail.com"
    )
}

def extractErrorsFromConsole() {
    def errorKeywords = ["error", "failed", "exception", "not recognized", "not found"]
    def errorLines = []

    try {
        def logLines = currentBuild.rawBuild.getLog(100000) // gets full log up to 100k lines
        logLines.each { line ->
            def lowerLine = line.toLowerCase()
            if (errorKeywords.any { keyword -> lowerLine.contains(keyword) }) {
                errorLines << line.trim()
            }
        }
    } catch (e) {
        errorLines << "Could not parse console output: ${e.message}"
    }

    return errorLines
}
