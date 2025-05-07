def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    // Generate the AI prompt (limit log to 5000 chars for performance)
    def prompt = """
You are an expert DevOps assistant.

Please analyze the following Jenkins build log and:
1. Identify the most important and meaningful build errors or faults.
2. Provide suggestions or fixes for each error in a clean and structured HTML table format with two columns: "Error / Fault" and "Suggested Fix".

Jenkins Log:
${buildLog.take(5000)}
"""

    // Save prompt to a file
    writeFile file: 'prompt.txt', text: prompt

    // Define full path to Ollama
    def ollamaPath = 'C:\\Users\\aniketb\\AppData\\Local\\Programs\\Ollama\\ollama.exe'

    // Check if Ollama is available at the given path
    if (!fileExists(ollamaPath)) {
        echo "Ollama is not found at: ${ollamaPath}"
        currentBuild.result = 'FAILURE'
        return
    }

    // Run Ollama and capture response
    def response = bat(
        script: "\"${ollamaPath}\" run deepseek-coder:6.7b < prompt.txt",
        returnStdout: true
    ).trim()

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
    <h2>🔧 Jenkins Build Analysis Report</h2>
    <p>Below is the analysis provided by <b>deepseek-coder:6.7b</b> based on your Jenkins build log:</p>
    <pre style="background-color:#f4f4f4; padding:10px; border:1px solid #ccc; white-space: pre-wrap;">
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
