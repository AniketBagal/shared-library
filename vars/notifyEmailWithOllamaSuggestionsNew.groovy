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
    def buildStatus = selectedErrors.isEmpty() ? 'SUCCESS' : 'FAILURE'  // Set to FAILURE when errors are detected
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

        // Continue with error handling
    }

    // Set the build status based on the detected errors
    currentBuild.result = buildStatus
}
