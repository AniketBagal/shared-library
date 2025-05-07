def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    // Debug: Check if the build log is null or empty
    if (!buildLog?.trim()) {
        echo "Build log is empty or null."
        currentBuild.result = 'FAILURE'
        return
    }

    // Log content (for debugging purposes)
    echo "Build Log Length: ${buildLog.length()}"
    echo "Build Log Preview: ${buildLog.take(200)}"  // Preview the first 200 characters

    // Generate the AI prompt
    def prompt = """
    You are a professional DevOps AI assistant.

    Task:
    1. From the following Jenkins build log, extract the key ERROR lines or failure messages (maximum 6–8 entries).
    2. For each ERROR line, give a brief 2–3 line suggestion for fixing the issue.
    3. Output format:
       Error: <actual error line>
       Suggestion: <concise fix or action>

    Format your response clearly, with no extra text, no table, no explanations.

    Jenkins Log:
    ${buildLog.take(5000)}
    """

    // Debug: Log the generated prompt (only a preview)
    echo "Generated prompt: ${prompt.take(500)}"

    // Save prompt to a file (in the current workspace)
    try {
        writeFile file: 'prompt.txt', text: prompt
        echo "Prompt successfully written to prompt.txt"
    } catch (Exception e) {
        echo "Failed to write prompt to file: ${e.message}"
        currentBuild.result = 'FAILURE'
        return
    }

    // Define full path to Ollama
    def ollamaPath = 'C:\\Users\\aniketb\\AppData\\Local\\Programs\\Ollama\\ollama.exe'

    // Check if Ollama is available at the given path
    if (!fileExists(ollamaPath)) {
        echo "Ollama is not found at: ${ollamaPath}"
        currentBuild.result = 'FAILURE'
        return
    }

    // Run Ollama and capture response
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

    // Check if response is valid
    if (!response) {
        echo "Ollama did not return a valid response."
        currentBuild.result = 'FAILURE'
        return
    }

    // Escape HTML characters for safe display
    def safeResponse = response
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')

    // Build email content
    def emailBody = """
    <html>
      <body>
        <h2>Jenkins Build Analysis Report</h2>
        <p><b>Model Used:</b> deepseek-coder:6.7b</p>
        <pre style="background-color:#f4f4f4; padding:10px; border:1px solid #ccc; font-family: monospace; white-space: pre-wrap;">
${safeResponse}
        </pre>
      </body>
    </html>
    """

    // Send email
    emailext(
        to: toEmail,
        subject: "Jenkins Build Analysis - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        body: emailBody
    )
}
