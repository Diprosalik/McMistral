package net.diprosalik.mcmistral.mistral;

public class ApiResponseParser {

    public static String parseContent(String responseBody) {
        try {
            String target = "\"content\":\"";
            int textIndex = responseBody.indexOf(target);

            if (textIndex == -1) {
                target = "\"content\": \"";
                textIndex = responseBody.indexOf(target);
            }

            if (textIndex == -1) {
                return "Unexpected response structure. Raw API output: " + responseBody;
            }

            int start = textIndex + target.length();
            int end = start;

            while (end < responseBody.length()) {
                end = responseBody.indexOf("\"", end);
                if (end == -1) break;
                if (responseBody.charAt(end - 1) != '\\') break;
                end++;
            }

            if (end == -1) {
                return "Unexpected response structure. Raw API output: " + responseBody;
            }

            return cleanMarkdown(resultMapping(responseBody.substring(start, end)));
        } catch (Exception e) {
            return "Error reading response: " + e.getMessage();
        }
    }

    private static String resultMapping(String raw) {
        return raw.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String cleanMarkdown(String input) {
        return input.replace("**", "").replace("*", "").replace("`", "").replace("#", "");
    }
}