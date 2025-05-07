def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    // Prepare prompt to send to Ollama
    def prompt = """
    You are an expert DevOps assistant.

    Please analyze the following Jenkins build log and:
    1. Identify the most important and meaningful build errors or faults.
    2. Provide suggestions or fixes for each error in a clean and structured HTML table format with two columns: "Error / Fault" and "Suggested Fix".

    Jenkins Log:
    ${buildLog.take(5000)}  // limit to first 5000 characters for performance, adjust as needed
    """

    // Send to Ollama and get response
    def response = bat(
        script: """ollama run deepseek-coder:6.7b "${prompt.replace('"', '\\"')}" """,
        returnStdout: true
    ).trim()

    // Email content
    def emailBody = """
    <html>
    <body>
        <h2>🔧 Jenkins Build Analysis Report</h2>
        <p>Below is the analysis provided by the AI model (<b>deepseek-coder:6.7b</b>) based on your Jenkins build log:</p>
        <div style="font-family: monospace; background-color: #f9f9f9; padding: 10px; border: 1px solid #ccc;">
            ${response.encodeAsHTML()}
        </div>
    </body>
    </html>
    """

    // Send the email
    emailext (
        to: toEmail,
        subject: "Jenkins Build Analysis & Fixes - ${env.JOB_NAME} #${env.BUILD_NUMBER}",
        mimeType: 'text/html',
        body: emailBody
    )
}
