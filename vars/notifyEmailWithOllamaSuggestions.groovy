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

    // Check if ollama is available
    def ollamaInstalled = bat(script: 'where ollama', returnStatus: true) == 0
    if (!ollamaInstalled) {
        echo "Ollama is not installed or not available in PATH."
        currentBuild.result = 'FAILURE'
        return
    }

    // Run ollama and capture the response
    def response = bat(
        script: 'ollama run deepseek-coder:6.7b < prompt.txt',
        returnStdout: true
    ).trim()

    // Escape HTML-sensitive characters
    def safeResponse = response
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')

    // Prepare the HTML email content
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

    // Send the analysis email
    emailext(
        to: toEmail,
        subject: "Jenkins Build Analysis - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        body: emailBody
    )
}
