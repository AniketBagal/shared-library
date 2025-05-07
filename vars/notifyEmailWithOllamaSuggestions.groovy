def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    // Validate build log
    if (!buildLog?.trim()) {
        echo "Build log is empty. Please provide valid Jenkins build log."
        currentBuild.result = 'FAILURE'
        return
    }

    // Generate prompt for Ollama
    def prompt = """
You are an expert DevOps assistant.

Please analyze the following Jenkins build log and:
1. Identify the most important and meaningful build errors or faults.
2. Provide suggestions or fixes for each error in clear and concise sentences.
3. Format response in plain lines (not a table), like:
Error 1: short title
Description: short explanation
Suggestion: short fix

Jenkins Log:
${buildLog.take(5000)}
"""

    // Save prompt to file
    writeFile file: 'prompt.txt', text: prompt

    // Define path to Ollama executable
    def ollamaPath = 'C:\\Users\\aniketb\\AppData\\Local\\Programs\\Ollama\\ollama.exe'

    // Verify Ollama exists
    if (!fileExists(ollamaPath)) {
        echo "Ollama is not found at: ${ollamaPath}"
        currentBuild.result = 'FAILURE'
        return
    }

    // Run Ollama with the prompt
    def response = bat(
        script: "\"${ollamaPath}\" run deepseek-coder:6.7b < prompt.txt",
        returnStdout: true
    ).trim()

    // Build email body without escaping HTML characters, just wrap in <pre>
    def emailBody = """
<html>
  <body>
    <h2>Jenkins Build Analysis Report</h2>
    <p><b>Model Used:</b> deepseek-coder:6.7b</p>
    <p><b>Analysis:</b></p>
    <pre style="background-color:#f4f4f4; padding:10px; border:1px solid #ccc; font-family: monospace; white-space: pre-wrap;">
${response}
    </pre>
  </body>
</html>
"""

    // Send email with UTF-8 HTML content
    emailext(
        to: toEmail,
        subject: "Jenkins Build Analysis - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html; charset=UTF-8',
        body: emailBody
    )
}
