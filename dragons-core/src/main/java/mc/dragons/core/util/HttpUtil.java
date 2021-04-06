package mc.dragons.core.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.logging.Logger;

import mc.dragons.core.Dragons;

public class HttpUtil {
	private static final Logger LOGGER = Dragons.getInstance().getLogger();
	
	public static String get(String url) {
		LOGGER.finer("GET " + url);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.GET()
				.build();
		HttpResponse<String> response;
		try {
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String post(String url, Map<String, String> data) {
		try {
			String payload = data.entrySet().stream().map(e -> {
				try {
					return e.getKey() + "=" + URLEncoder.encode(e.getValue(), "UTF-8");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
				return "";
			}).reduce((a, b) -> a + "," + b).get();
			LOGGER.finer("POST " + url + " (" + payload + ")");
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.POST(HttpRequest.BodyPublishers.ofString(payload))
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.build();
			HttpResponse<String> response;
			response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return response.body();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return null;
	}
}
