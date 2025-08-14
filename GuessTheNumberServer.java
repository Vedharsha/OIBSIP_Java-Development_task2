import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
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

                        if (guess == secretNumber) {
                            response = "<h1>✅ Correct! You guessed it in " + attempts + " attempts.</h1>"
                                     + "<a href='/'>Play Again</a>";
                            resetGame();
                        } else if (attempts >= MAX_ATTEMPTS) {
                            response = "<h1>❌ Out of attempts! The number was " + secretNumber + ".</h1>"
                                     + "<a href='/'>Play Again</a>";
                            resetGame();
                        } else if (guess < secretNumber) {
                            response = "<p>📉 Too low! Attempts left: " + (MAX_ATTEMPTS - attempts) + "</p>";
                        } else {
                            response = "<p>📈 Too high! Attempts left: " + (MAX_ATTEMPTS - attempts) + "</p>";
                        }
                    } catch (NumberFormatException e) {
                        response = "<p>❌ Invalid number!</p>";
                    }
                }
            }

            // Always return the form
            String form = "<form method='POST'>" +
                          "<label>Enter your guess (" + MIN + "-" + MAX + "): </label>" +
                          "<input type='number' name='guess' min='" + MIN + "' max='" + MAX + "' required>" +
                          "<input type='submit' value='Guess'>" +
                          "</form>";

            String html = "<html><body style='font-family:Arial;text-align:center;'>" +
                          "<h1>🎯 Guess the Number</h1>" +
                          form +
                          "<div>" + response + "</div>" +
                          "</body></html>";

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
    }
}
