def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    if (!buildLog?.trim()) {
        echo "Build log is empty or null."
        currentBuild.result = 'FAILURE'
        return
    }

    // Improved regex: avoid false positives like 'not recognized'
    def errorLines = buildLog.readLines().findAll { line -> 
        line =~ /(?i)(error:|exception|fatal|build failed|undefined reference|compilation terminated)/
    }

    def selectedErrors = errorLines.take(10)
    def buildStatus = selectedErrors.isEmpty() ? 'SUCCESS' : 'FAILURE'  // Build is SUCCESS only if no errors are found
    echo "Detected build status from log: ${buildStatus}"

    String formattedResponse = ""
    String emailBody = ""

    if (selectedErrors.isEmpty()) {
        echo "No errors detected in the build log."

        formattedResponse = """
            <p style="color:#006600;"><strong>Congratulations!</strong> The build completed successfully with 0 critical errors.</p>
        """

        emailBody = """
        <html>
          <body style="font-family: Arial, sans-serif;">
            <h2 style="color:#003366;">Jenkins Build Report</h2>
            <p><b>Build Status:</b> ${buildStatus}</p>
            <p><b>Model Used:</b> deepseek-coder:6.7b</p>
            ${formattedResponse}
          </body>
        </html>
        """
    } else {
        echo "Extracted Errors:\n" + selectedErrors.join("\n")

        def prompt = """
        You are an expert DevOps assistant.

        Analyze the following Jenkins errors and provide a short, clear suggestion or fix for each.

        Format:
        Error 1: <actual error line>
        Suggestion: <one-line fix>

        Errors:
        ${selectedErrors.join("\n")}
        """

        try {
            writeFile file: 'prompt.txt', text: prompt
            echo "Prompt successfully written to prompt.txt"
        } catch (Exception e) {
            echo "Failed to write prompt to file: ${e.message}"
            currentBuild.result = 'FAILURE'
            return
        }

        def ollamaPath = 'C:\\Users\\aniketb\\AppData\\Local\\Programs\\Ollama\\ollama.exe'

        if (!fileExists(ollamaPath)) {
            echo "Ollama is not found at: ${ollamaPath}"
            currentBuild.result = 'FAILURE'
            return
        }

        def response = ''
        try {
            response = bat(
                script: "\"${ollamaPath}\" run deepseek-coder:6.7b < prompt.txt",
                returnStdout: true
            ).trim()
        } catch (Exception e) {
            echo "Failed to run Ollama: ${e.message}"
            currentBuild.result = 'FAILURE'
            return
        }

        if (!response) {
            echo "Ollama did not return a valid response."
            currentBuild.result = 'FAILURE'
            return
        }

        def blocks = response.split(/(?i)(?=Error \d+:)/)
        blocks.each { block -> 
            if (block.trim()) {
                def parts = block.split(/(?i)Suggestion:/)
                def errorPart = parts[0]?.trim()
                def suggestionPart = parts.size() > 1 ? parts[1]?.trim() : ""

                formattedResponse += """
                    <div style="margin-bottom: 20px;">
                        <p style="color:#b30000;"><strong>${errorPart}</strong></p>
                        <p style="color:#006600;"><strong>Suggestion:</strong> ${suggestionPart}</p>
                    </div>
                """
            }
        }

        emailBody = """
        <html>
          <body style="font-family: Arial, sans-serif;">
            <h2 style="color:#003366;">Jenkins Build Analysis Report</h2>
            <p><b>Build Status:</b> ${buildStatus}</p>
            <p><b>Model Used:</b> deepseek-coder:6.7b</p>
            <p><b>Detected Errors and Suggested Fixes:</b></p>
            ${formattedResponse}
          </body>
        </html>
        """
    }

    // Send the email with the appropriate build status
    emailext(
        to: toEmail,
        subject: "Jenkins Build ${buildStatus} - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        body: emailBody
    )
}
