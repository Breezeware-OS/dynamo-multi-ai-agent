package net.breezeware.dynamo.ai.agent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

/**
 * Service for interacting with the Weather API to fetch current weather information.
 * Documentation: https://www.weatherapi.com/api-explorer.aspx
 */
public class WeatherService implements Function<WeatherService.Request, WeatherService.Response> {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final RestClient restClient;
    private final WeatherConfigProperties weatherProps;

    /**
     * Constructor for initializing the WeatherService with configuration properties.
     *
     * @param props WeatherConfigProperties containing API URL and API key.
     */
    public WeatherService(WeatherConfigProperties props) {
        this.weatherProps = props;
        log.debug("Weather API URL: {}", weatherProps.apiUrl());
        log.debug("Weather API Key: {}", weatherProps.apiKey());
        this.restClient = RestClient.create(weatherProps.apiUrl());
    }

    /**
     * Fetches current weather information for a given city using the Weather API.
     *
     * @param weatherRequest Request object containing the city name.
     * @return Response object containing the current weather details.
     */
    @Override
    public Response apply(Request weatherRequest) {
        try {
            log.info("Weather Request: {}", weatherRequest);

            Response response = restClient.get()
                    .uri("/current.json?key={key}&q={q}", weatherProps.apiKey(), weatherRequest.city())
                    .retrieve()
                    .body(Response.class);

            log.info("Weather API Response: {}", response);
            return response;
        } catch (Exception e) {
            log.error("Failed to fetch weather information for city: {}", weatherRequest.city(), e);
            throw new RuntimeException("Error fetching weather information. Please try again later.", e);
        }
    }

    /**
     * Request object representing the city for which weather information is requested.
     *
     * @param city Name of the city.
     */
    public record Request(String city) {}

    /**
     * Response object representing weather information returned by the Weather API.
     *
     * @param location Location details such as city name, region, and country.
     * @param current Current weather details such as temperature, condition, wind speed, and humidity.
     */
    public record Response(Location location, Current current) {}

    /**
     * Represents location details including city, region, country, latitude, and longitude.
     *
     * @param name City name.
     * @param region Region name.
     * @param country Country name.
     * @param lat Latitude.
     * @param lon Longitude.
     */
    public record Location(String name, String region, String country, Long lat, Long lon) {}

    /**
     * Represents current weather conditions including temperature, wind speed, and humidity.
     *
     * @param temp_f Temperature in Fahrenheit.
     * @param condition Weather condition details.
     * @param wind_mph Wind speed in miles per hour.
     * @param humidity Humidity level as a percentage.
     */
    public record Current(String temp_f, Condition condition, String wind_mph, String humidity) {}

    /**
     * Represents detailed weather condition such as description.
     *
     * @param text Textual description of the weather condition.
     */
    public record Condition(String text) {}
}