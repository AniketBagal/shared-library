def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    if (!buildLog?.trim()) {
        echo "Build log is empty or null."
        currentBuild.result = 'FAILURE'
        return
    }

    // def errorLines = buildLog.readLines().findAll { line ->
    // line =~ /(?i)(error|exception|failed|not found|undefined|unable to|missing|not recognized|cannot find the path specified|command not found)/
    // }
    def errorLines = buildLog.readLines().findAll { line ->
    line =~ /(?i)(error:|exception|failed|undefined|not found|unable to|missing|not recognized|cannot find|command not found)/ &&
    !(line =~ /cl\.exe|ollama\.exe|run deepseek-coder|errorReport:/)
    }

    def selectedErrors = errorLines.take(10)

    if (selectedErrors.isEmpty()) {
        echo "No errors detected in the build log."
        currentBuild.result = 'SUCCESS'
        return
    }

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

    echo "Generated prompt:\n" + prompt.take(500)

    try {
        writeFile file: 'prompt.txt', text: prompt
        echo "Prompt successfully written to prompt.txt"
    } catch (Exception e) {
        echo "Failed to write prompt to file: ${e.message}"
        currentBuild.result = 'FAILURE'
        return
    }

    def ollamaPath = 'C:\\Users\\Admin\\AppData\\Local\\Programs\\Ollama\\ollama.exe'

    if (!fileExists(ollamaPath)) {
        echo "Ollama is not found at: ${ollamaPath}"
        currentBuild.result = 'FAILURE'
        return
    }

    def response = ''
    try {
        response = bat(
            script: "\"${ollamaPath}\" run tinyllama:1.1b < prompt.txt",
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

    // Format response into separate HTML blocks for each Error + Suggestion
    def formattedResponse = ""
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

    def emailBody = """
    <html>
      <body style="font-family: Arial, sans-serif;">
        <h2 style="color:#003366;">Jenkins Build Analysis Report</h2>
        <p><b>Model Used:</b> deepseek-coder:6.7b</p>
        <p><b>Detected Errors and Suggested Fixes:</b></p>
        ${formattedResponse}
      </body>
    </html>
    """

    emailext(
        to: toEmail,
        subject: "Jenkins Build Analysis - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        body: emailBody
    )
}
