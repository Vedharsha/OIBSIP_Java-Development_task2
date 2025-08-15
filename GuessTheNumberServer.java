import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.*;

public class GuessTheNumberServer {
    private static final int MIN = 1;
    private static final int MAX = 100;
    private static final int MAX_ATTEMPTS = 7;

    private static int secretNumber;
    private static int attempts;

    public static void main(String[] args) throws IOException {
        resetGame();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new GameHandler());
        server.setExecutor(null);
        System.out.println("Server running at http://localhost:8080");
        server.start();
    }

    private static void resetGame() {
        secretNumber = new Random().nextInt(MAX - MIN + 1) + MIN;
        attempts = 0;
        System.out.println("🔹 Secret number is: " + secretNumber);
    }

    static class GameHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("POST")) {
                Map<String, String> params = parsePostData(exchange);
                String guessStr = params.get("guess");

                if (guessStr != null) {
                    try {
                        int guess = Integer.parseInt(guessStr);
                        attempts++;
                        int diff = Math.abs(guess - secretNumber);

                        if (guess == secretNumber) {
                            response = "✅ Correct! You guessed it in " + attempts + " attempts."+".<br><br><br>"
                                    + "<a href='/' style='text-decoration:none; color:white; background-color:blue; padding:8px 16px; border-radius:5px;'>Play Again</a>";;
                            resetGame();
                        } else if (attempts >= MAX_ATTEMPTS) {
                            response = "❌ Out of attempts! The number was " + secretNumber + ".<br><br><br>"
                                    + "<a href='/' style='text-decoration:none; color:white; background-color:blue; padding:8px 16px; border-radius:5px;'>Play Again</a>";
                            resetGame();
                        } else if (guess < secretNumber) {
                            if (diff <= 5) {
                                response = "📉 Low, but 🔥 Very Close! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            } else if (diff <= 10) {
                                response = "📉 Low, but ✨ Close! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            } else {
                                response = "📉 Too Low! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            }
                        } else { // guess > secretNumber
                            if (diff <= 5) {
                                response = "📈 High, but 🔥 Very Close! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            } else if (diff <= 10) {
                                response = "📈 High, but ✨ Close! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            } else {
                                response = "📈 Too High! Attempts left: " + (MAX_ATTEMPTS - attempts);
                            }
                        }

                    } catch (NumberFormatException e) {
                        response = "❌ Invalid number!";
                    }
                }
            }

            // Form for user input
            String form = "<form method='POST'>" +
                          "<label>Enter your guess (" + MIN + "-" + MAX + "): </label>" +
                          "<input type='number' name='guess' min='" + MIN + "' max='" + MAX + "' required>" +
                          "<input type='submit' value='Guess'>" +
                          "</form>";

            // HTML with CSS for attractive output
            String html = "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<title>🎯 Guess the Number</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; text-align: center; background: linear-gradient(to right, #f8cdda, #1c92d2); padding: 50px; }" +
            "h1 { color: #fff; text-shadow: 2px 2px 4px #000; }" +
            "form { margin: 20px auto; }" +
            "input[type=number] { padding: 10px; width: 100px; border-radius: 5px; border: 1px solid #ccc; }" +
            "input[type=submit] { padding: 10px 20px; border-radius: 5px; border: none; background-color: #4CAF50; color: white; cursor: pointer; font-weight: bold; }" +
            "input[type=submit]:hover { background-color: #45a049; }" +
            ".response { font-size: 18px; margin-top: 20px; padding: 15px; border-radius: 8px; display: inline-block; color: #fff; }" +
            ".correct { background-color: #28a745; }" +
            ".wrong { background-color: #dc3545; }" +
            ".close { background-color: #ffc107; color: #000; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<h1>🎯 Guess the Number</h1>" +
            form +
            "<div class='response " + getResponseClass(response) + "'>" + response + "</div>" +
            "</body>" +
            "</html>";

            byte[] bytes = html.getBytes();
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }

        private Map<String, String> parsePostData(HttpExchange exchange) throws IOException {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            StringBuilder buf = new StringBuilder();
            int b;
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }
            String[] pairs = buf.toString().split("&");
            Map<String, String> params = new HashMap<>();
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length > 1) {
                    params.put(kv[0], java.net.URLDecoder.decode(kv[1], "UTF-8"));
                }
            }
            return params;
        }

        private String getResponseClass(String response) {
            if (response.contains("Correct")) return "correct";
            if (response.contains("Very Close") || response.contains("Close")) return "close";
            if (response.contains("Too Low") || response.contains("Too High") || response.contains("Out of attempts")) return "wrong";
            return "";
        }
    }
}
