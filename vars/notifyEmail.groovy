def call(Map config = [:]) {
    def subject = config.success ? "Build SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}"
                                 : "Build FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}"

    def color = config.success ? "green" : "red"
    def suggestion = config.success ? "Great job! No issues detected." : (config.suggestion ?: "Please check the error summary below and Jenkins console output.")

    def errorSummary = ""
    if (!config.success) {
        def log = currentBuild.rawBuild.getLog() // get full log
        def errors = []

        log.each { line ->
            if (line ==~ /(?i).*error.*/ || line ==~ /(?i).*not recognized as an internal or external command.*/) {
                errors << line.trim()
            }
        }

        if (errors.isEmpty()) {
            errorSummary = "<p>No specific error patterns matched. Please refer to the console output.</p>"
        } else {
            def rows = errors.collect { e ->
                "<tr><td>Error</td><td>${e.replaceAll('<', '&lt;').replaceAll('>', '&gt;')}</td></tr>"
            }.join("\n")

            errorSummary = """
                <table border="1" cellpadding="5" cellspacing="0" style="border-collapse: collapse;">
                    <tr style="background-color: #f2f2f2;">
                        <th>Error</th>
                        <th>Description</th>
                    </tr>
                    ${rows}
                </table>
            """
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
                ${errorSummary}
            </body>
        </html>
        """,
        mimeType: 'text/html',
        to: config.to ?: "aniketbagal12345@gmail.com"
    )
}
