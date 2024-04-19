package FYP;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class IMT {

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 6000; // 6 seconds

    public static String sendOpenAIRequest(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";
        String apiKey = "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";
        String model = "gpt-3.5-turbo";

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                URL obj = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                // Build request body
                String requestBody = "{\"model\": \"" + model + "\", \"messages\": [{\"role\": \"user\", \"content\": \"" + prompt + "\"}]}";

                // Write request body to connection
                try (OutputStream outputStream = connection.getOutputStream()) {
                    byte[] input = requestBody.getBytes("utf-8");
                    outputStream.write(input, 0, input.length);
                }

                // Get response
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        return extractMessageFromJSONResponse(response.toString());
                    }
                } else if (responseCode == 429) {
                    // Rate limit exceeded, retry after a delay
                    System.out.println("Rate limit exceeded, retrying after delay...");
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    throw new IOException("HTTP error code: " + responseCode);
                }

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return "Error: " + e.getMessage();
            }
        }
        return "Error: Maximum retries exceeded";
    }

    public static String extractMessageFromJSONResponse(String response) {
        int start = response.indexOf("content") + 11;
        int end = response.indexOf("\"", start);
        String content = response.substring(start, end);

        // Remove special characters like \n and \"
        content = content.replaceAll("\\\\n", "").replaceAll("\\\\\"", "");

        // Replace \\ with \
        content = content.replace("\\\\", "\\");

        return content;
    }

    public static void main(String[] args) {
        final String RSS_URL = "https://www.cshub.com/rss/categories/attacks";

        try {
            URL rssUrl = new URL(RSS_URL);
            HttpURLConnection httpcon = (HttpURLConnection) rssUrl.openConnection();
            httpcon.addRequestProperty("User-Agent", "Mozilla/4.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(httpcon.getInputStream()));
            String inputLine;
            StringBuilder rssResponse = new StringBuilder();

            while ((inputLine = in.readLine()) != null) 
            {
                rssResponse.append(inputLine);
            }

            in.close();

            String rssContent = rssResponse.toString();
            String[] reports = rssContent.split("<link>");

            // Goes through each report and extracts the mitigation technique
            for (String report : reports) 
            {
                if (report.contains("</link>")) 
                {
                    String link = report.substring(0, report.indexOf("</link>"));
                    System.out.println("Report Link: " + link);
                    String mitigationTechniques = sendOpenAIRequest("Using the link " + link + ",  list the following in a concise RSS feed format. <Kind_of_attack>, <One_Mitigation_Technique> ,<MITRE_TTID>,<Severity_Level>");
                    System.out.println("Mitigation Techniques: " + mitigationTechniques);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
