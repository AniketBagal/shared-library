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
    You are an expert DevOps assistant.

    Please analyze the following Jenkins build log and:
    1. Identify the most important,Unique and meaningful build errors or faults.
    2. Provide one suggestion or fixe for each error in clear and concise sentences.
    3. Format response in plain lines (not a table), like:
    Error 1: <actual error line in jenkins log>
   
    Suggestion: <short fix> single one

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
    //def ollamaPath = 'C:\\Users\\aniketb\\AppData\\Local\\Programs\\Ollama\\ollama.exe'
    def ollamaPath = 'C:\Users\Admin\AppData\Local\Programs\Ollama\ollama.exe'

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
            script: "\"${ollamaPath}\" run tinyllama:1.1b < prompt.txt",
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
        <p><b>Analysis:</b></p>
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
