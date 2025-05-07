def call(String buildLog, String toEmail = 'aniketbagal12345@gmail.com') {
    // Generate the AI prompt (limit log to 5000 chars for performance)
    def prompt = """
Analyze the following Jenkins log
 Give the important Errors and its suggestion in formate as
 Error 1 :
 Suggestion :

  Error 2 :
 Suggestion :
 likewise
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
        script: "\"${ollamaPath}\" run phi3:latest  < prompt.txt",
        returnStdout: true
    ).trim()

    // Parse AI response into HTML table
    def tableRows = ''
    def lines = response.split('\n')
    def error = ''
    def suggestions = []

    lines.each { line ->
        if (line.toLowerCase().contains("error") || line.toLowerCase().contains("failed")) {
            if (error) {
                // Add previous error-suggestion pair to table
                tableRows += "<tr><td>${error}</td><td><ul>${suggestions.collect { "<li>${it}</li>" }.join('')}</ul></td></tr>\n"
                suggestions = []
            }
            error = line.trim()
        } else if (line.trim()) {
            suggestions << line.trim()
        }
    }

    // Add last error-suggestion pair
    if (error && suggestions) {
        tableRows += "<tr><td>${error}</td><td><ul>${suggestions.collect { "<li>${it}</li>" }.join('')}</ul></td></tr>\n"
    }

    // Default fallback if parsing fails
    if (!tableRows) {
        tableRows = "<tr><td colspan='2'>No structured output found. Raw response:</td></tr>" +
                    "<tr><td colspan='2'><pre>${response.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;')}</pre></td></tr>"
    }

    // Build email content
    def emailBody = """
<html>
  <body>
    <h2>Jenkins Build Analysis Report</h2>
    <p>Below is the analysis provided by <b>deepseek-coder:6.7b</b> based on your Jenkins build log:</p>
    <table border='1' cellpadding='10' cellspacing='0' style='border-collapse: collapse;'>
      <tr style='background-color:#f2f2f2;'>
        <th>Error / Fault</th>
        <th>Suggested Fix</th>
      </tr>
      ${tableRows}
    </table>
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
