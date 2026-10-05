package com.fitcoach.app.backend.service;

import com.fitcoach.app.backend.entity.StravaAccount;
import com.fitcoach.app.backend.entity.StravaActivity;
import com.fitcoach.app.backend.entity.User;
import com.fitcoach.app.backend.repository.StravaAccountRepository;
import com.fitcoach.app.backend.repository.StravaActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StravaService {

    private final UserService userService;
    private final StravaAccountRepository stravaAccountRepository;
    private final StravaActivityRepository stravaActivityRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${strava.client-id:}")
    private String clientId;

    @Value("${strava.client-secret:}")
    private String clientSecret;

    @Value("${strava.redirect-uri:http://localhost:8080/api/strava/callback}")
    private String redirectUri;

    @Value("${strava.auth-url:https://www.strava.com/oauth/authorize}")
    private String authUrl;

    @Value("${strava.token-url:https://www.strava.com/oauth/token}")
    private String tokenUrl;

    public String getConnectUrl() {
        if (clientId == null || clientId.isBlank()) {
            throw new IllegalStateException("Strava client ID is not configured");
        }
        return authUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri + "&response_type=code&scope=read,activity:read_all";
    }

    @Transactional
    public String handleCallback(String code) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalStateException("Strava credentials are not configured");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String body = "client_id=" + clientId + "&client_secret=" + clientSecret + "&code=" + code + "&grant_type=authorization_code";
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("Could not exchange Strava code");
        }

        Map<String, Object> data = response.getBody();
        String accessToken = (String) data.get("access_token");
        String refreshToken = (String) data.get("refresh_token");
        Number expiresAt = (Number) data.get("expires_at");
        Map<String, Object> athlete = (Map<String, Object>) data.get("athlete");
        String athleteId = athlete != null ? String.valueOf(athlete.get("id")) : null;

        User user = userService.getCurrentUser();
        StravaAccount account = stravaAccountRepository.findByUserId(user.getId()).orElse(new StravaAccount());
        account.setUser(user);
        account.setAccessToken(accessToken);
        account.setRefreshToken(refreshToken);
        account.setExpiresAt(expiresAt.longValue());
        account.setAthleteId(athleteId);
        account.setConnected(true);
        stravaAccountRepository.save(account);
        syncActivities();
        return "Connected";
    }

    @Transactional
    public void syncActivities() {
        User user = userService.getCurrentUser();
        StravaAccount account = stravaAccountRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("No Strava account connected"));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(account.getAccessToken());
        HttpEntity<Void> request = new HttpEntity<>(headers);

        ResponseEntity<List> response = restTemplate.exchange(
                "https://www.strava.com/api/v3/athlete/activities?per_page=20",
                org.springframework.http.HttpMethod.GET,
                request,
                List.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            return;
        }

        for (Object item : response.getBody()) {
            Map<String, Object> activity = (Map<String, Object>) item;
            String externalId = String.valueOf(activity.get("id"));
            if (stravaActivityRepository.findByUserIdAndExternalId(user.getId(), externalId).isPresent()) {
                continue;
            }

            StravaActivity entity = new StravaActivity();
            entity.setUser(user);
            entity.setExternalId(externalId);
            entity.setType(activity.get("type") != null ? activity.get("type").toString() : "Other");
            entity.setName(activity.get("name") != null ? activity.get("name").toString() : "Activity");
            Long startEpoch = activity.get("start_date") != null ? Instant.parse(activity.get("start_date").toString()).getEpochSecond() : System.currentTimeMillis() / 1000;
            entity.setStartDate(LocalDateTime.ofInstant(Instant.ofEpochSecond(startEpoch), ZoneId.systemDefault()));
            entity.setDurationSeconds(activity.get("moving_time") != null ? Long.valueOf(activity.get("moving_time").toString()) : 0L);
            entity.setDistanceMeters(activity.get("distance") != null ? Double.valueOf(activity.get("distance").toString()) / 1000.0 : 0.0);
            entity.setAverageSpeed(activity.get("average_speed") != null ? Double.valueOf(activity.get("average_speed").toString()) : null);
            entity.setElevationGain(activity.get("total_elevation_gain") != null ? Double.valueOf(activity.get("total_elevation_gain").toString()) : null);
            entity.setCalories(activity.get("calories") != null ? Double.valueOf(activity.get("calories").toString()) : null);
            entity.setAverageHeartRate(activity.get("average_heartrate") != null ? Double.valueOf(activity.get("average_heartrate").toString()) : null);
            stravaActivityRepository.save(entity);
        }
    }

    public void disconnect() {
        User user = userService.getCurrentUser();
        stravaAccountRepository.findByUserId(user.getId()).ifPresent(stravaAccountRepository::delete);
    }
}
