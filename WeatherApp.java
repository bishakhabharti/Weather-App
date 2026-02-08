import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// Cache class
class CacheEntry {

    String data;
    long time;

    CacheEntry(String data) {
        this.data = data;
        this.time = System.currentTimeMillis();
    }

    boolean isExpired() {

        long now = System.currentTimeMillis();

        return (now - time) > 600000; // 10 min
    }
}

// API Client
class WeatherClient {

    private final String API_KEY = "YOUR_API_KEY_HERE";

    private final HttpClient client;

    private Map<String, CacheEntry> cache = new HashMap<>();

    public WeatherClient() {

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String getWeather(String city)
            throws Exception {

        city = city.toLowerCase();

        // Check cache
        if (cache.containsKey(city)) {

            CacheEntry entry = cache.get(city);

            if (!entry.isExpired()) {

                return entry.data
                        + "\n(Cached)";
            }
        }

        String url = "https://api.openweathermap.org/data/2.5/weather?q="
                + city
                + "&appid="
                + API_KEY
                + "&units=metric";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(15))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new Exception(
                    "City not found or API error");
        }

        String result = parseWeather(response.body());

        cache.put(city,
                new CacheEntry(result));

        return result;
    }

    private String parseWeather(String json) {

        String city = extract(json, "\"name\":\"", "\"");

        String temp = extract(json, "\"temp\":", ",");

        String humidity = extract(json, "\"humidity\":", ",");

        String weather = extract(json, "\"main\":\"", "\"");

        String wind = extract(json, "\"speed\":", ",");

        return "City: " + city
                + "\nTemperature: " + temp + " °C"
                + "\nWeather: " + weather
                + "\nHumidity: " + humidity + "%"
                + "\nWind Speed: " + wind + " m/s";
    }

    private String extract(
            String json,
            String key,
            String end) {

        try {

            int start = json.indexOf(key)
                    + key.length();

            int stop = json.indexOf(end, start);

            return json.substring(start, stop);

        } catch (Exception e) {

            return "N/A";
        }
    }
}

// GUI Class
public class WeatherApp extends JFrame {

    JTextField cityField;

    JTextArea display;

    JButton button;

    JLabel status;

    WeatherClient client;

    public WeatherApp() {

        client = new WeatherClient();

        setTitle("Weather App");

        setSize(500, 400);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        setLocationRelativeTo(null);

        JPanel top = new JPanel();

        top.add(new JLabel("City:"));

        cityField = new JTextField(15);

        top.add(cityField);

        button = new JButton("Get Weather");

        top.add(button);

        display = new JTextArea();

        display.setFont(
                new Font("Arial", Font.PLAIN, 16));

        display.setEditable(false);

        JScrollPane scroll = new JScrollPane(display);

        status = new JLabel("Ready");

        add(top, BorderLayout.NORTH);

        add(scroll, BorderLayout.CENTER);

        add(status, BorderLayout.SOUTH);

        button.addActionListener(e -> fetchWeather());
    }

    private void fetchWeather() {

        String city = cityField.getText();

        if (city.isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Enter city");

            return;
        }

        new Thread(() -> {

            try {

                status.setText(
                        "Loading...");

                String data = client.getWeather(city);

                display.setText(data);

                status.setText(
                        "Success");

            } catch (Exception e) {

                display.setText(
                        "Error: "
                                + e.getMessage());

                status.setText(
                        "Failed");
            }

        }).start();
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            new WeatherApp()
                    .setVisible(true);
        });
    }
}
