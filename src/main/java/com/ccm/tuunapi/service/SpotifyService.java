package com.ccm.tuunapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.cache.annotation.Cacheable;
import java.util.Base64;

@Service
public class SpotifyService {
    
    @Value("${SPOTIFY_CLIENT_ID}")
    private String clientId;

    @Value("${SPOTIFY_CLIENT_SECRET}")
    private String clientSecret;
    
    private final RestTemplate restTemplate;
    private String token;
    private long tokenExpirationTime;

    private static final String SPOTIFY_BASE_URL = "https://api.spotify.com/v1";
    private static final String SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";

    public SpotifyService(RestTemplate restTemplate){
        this.restTemplate = restTemplate;
    }

    private String getAccessToken(){
        if(token != null && System.currentTimeMillis() < tokenExpirationTime){
            return token;
        }
        try {
            String auth = clientId + ":" + clientSecret;
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("Authorization", "Basic " + encodedAuth);

            String body = "grant_type=client_credentials";
            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(
                    SPOTIFY_TOKEN_URL,
                    request,
                    JsonNode.class);

            if (response.getBody() != null) {
                token = response.getBody().get("access_token").asText();
                int expiresIn = response.getBody().get("expires_in").asInt();
                tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000L);
                return token;
            }
        }
        catch (Exception e){
            throw new RuntimeException("Failed to get access token", e);
        }

        throw new RuntimeException("Failed to get access token");
    }
    
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public JsonNode searchTrack(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty");
        }

        try {
            String url = String.format(
                    "%s/search?q=%s&type=track&limit=20",
                    SPOTIFY_BASE_URL,
                    query.replace(" ", "%20")
            );
            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );
            if (response.getBody() != null) {
                return response.getBody();
            }
        }
        catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to search track", e);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to search tracks", e);
        }

        throw new RuntimeException("Failed to search track");
    }

    @Cacheable(value = "trackDetails", key="id")
    public JsonNode getTrackDetails(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Track id cannot be empty");
        }

        try {
            String url = String.format("%s/tracks/%s", SPOTIFY_BASE_URL, id);

            HttpEntity<String> entity = new HttpEntity<>(createAuthHeaders());
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getBody() != null) {
                return response.getBody();
            }
        }
        catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Track not found with ID: " + id, e);
        }
        catch (HttpClientErrorException e) {
            throw new RuntimeException("Spotify API error: " + e.getStatusCode(), e);
        }
        catch (Exception e) {
            throw new RuntimeException("Spotify API error: " + e.getMessage(), e);
        }

        throw new RuntimeException("Spotify API error: " + id);
    }

}