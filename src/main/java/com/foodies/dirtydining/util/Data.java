package com.foodies.dirtydining.util;

import com.foodies.dirtydining.dao.IRestaurantRepository;
import com.foodies.dirtydining.model.Restaurant;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class Data {

    private final Logger logger = LoggerFactory.getLogger(Data.class);

    @Autowired
    private IRestaurantRepository restaurantRepository;

    @Value("${restaurant.data.url}")
    private String restaurantDataUrl;

    private static final String ZIP_FILE_PATH = "src/main/resources/data/restaurants.zip";
    private static final String DESTINATION_PATH = "src/main/resources/data/";
    private static final String RESTAURANTS_FILE_PATH = "src/main/resources/data/restaurant_establishments.csv";
    private static final String DELIMITER = ";";

    private void downloadData() throws IOException {
        logger.info("Downloading zipped restaurant data...");
        FileUtils.copyURLToFile(new URL(restaurantDataUrl), new File(ZIP_FILE_PATH), 30000, 30000);
        logger.info("Download complete.");
    }

    private void extractZippedFile() throws ZipException {
        logger.info("Extracting zipped files...");
        new ZipFile(ZIP_FILE_PATH).extractAll(DESTINATION_PATH);
        logger.info("Extracted files successfully.");
    }

    private void removeFiles() {
        logger.info("Removing files...");
        final File[] files = new File(DESTINATION_PATH).listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        logger.info("Files removed successfully.");
    }

    public void initializeDB() {
        try {
            final boolean collectionExists = restaurantRepository.doesCollectionExist();

            if (collectionExists) {
                logger.info("Restaurant collection already exists.");
            } else {
                logger.info("Initializing Restaurant collection...");

                downloadData();
                extractZippedFile();

                List<Restaurant> restaurants = new ArrayList<>();
                String line = "";
                BufferedReader br = new BufferedReader(new FileReader(RESTAURANTS_FILE_PATH));
                final String[] headers = br.readLine().split(DELIMITER);
                while ((line = br.readLine()) != null) {
                    try {
                        final Restaurant restaurant = createRestaurant(createDataMap(headers, line.split(DELIMITER)));
                        restaurants.add(restaurant);
                        if (restaurants.size() >= 1000) {
                            restaurantRepository.insertRestaurants(restaurants);
                            restaurants = new ArrayList<>();
                        }
                    } catch (Exception ex) {
                        logger.error("Error while inserting record.", ex);
                    }
                }

                if (restaurants.size() >= 1) {
                    restaurantRepository.insertRestaurants(restaurants);
                }
                br.close();

                removeFiles();
                logger.info("Restaurant collection initialized successfully.");
            }
        } catch (Exception ex) {
            logger.error("Error when initializing Restaurant collection.", ex);
        }
    }

    public void updateDB() {
        try{
            downloadData();
            extractZippedFile();

            logger.info("Updating Restaurant collection...");

            String line = "";
            BufferedReader br = new BufferedReader(new FileReader(RESTAURANTS_FILE_PATH));
            final String[] headers = br.readLine().split(DELIMITER);
            while ((line = br.readLine()) != null) {
                try {
                    final Restaurant restaurant = createRestaurant(createDataMap(headers, line.split(DELIMITER)));
                    restaurantRepository.updateOrInsertRestaurant(restaurant);
                } catch (Exception ex) {
                    logger.error("Error while updating/inserting record.", ex);
                }
            }
            br.close();
            removeFiles();
            logger.info("Updated Restaurant collection successfully.");
        } catch (Exception e) {
            logger.error("Error while populating database.", e);
        }
    }

    private Restaurant createRestaurant(Map<String, String> restaurantData) {

        // permit_number;facility_id;owner_id;PE;restaurant_name;location_name;address;latitude;
        // longitude;city_id;city_name;zip_code;nciaa;plan_review;record_status;current_grade;
        // current_demerits;date_current;previous_grade;date_previous;search_text;
        final String permitNumber = restaurantData.get("permit_number").trim();
        final String restaurantName = restaurantData.get("restaurant_name").trim();
        final String address = restaurantData.get("address").trim();
        final double latitude = Double.parseDouble(restaurantData.get("latitude") != null ? restaurantData.get("latitude") : "0");
        final double longitude = Double.parseDouble(restaurantData.get("longitude") != null ? restaurantData.get("latitude") : "0");
        final int cityId = Integer.parseInt(restaurantData.get("city_id") != null ? restaurantData.get("city_id") : "0");
        final String cityName = restaurantData.get("city_name");
        final String zipCode = restaurantData.get("zip_code");
        final String currentGrade = restaurantData.get("current_grade");
        final String currentDemerits = restaurantData.get("current_demerits");
        final String dateCurrent = restaurantData.get("date_current");
        final String previousGrade = restaurantData.get("previous_grade");
        final String datePrevious = restaurantData.get("date_previous");
        final String searchText = restaurantData.get("search_text");

        return new Restaurant(
                permitNumber,
                restaurantName,
                address,
                latitude,
                longitude,
                cityId,
                cityName,
                zipCode,
                searchText,
                currentGrade,
                currentDemerits,
                dateCurrent,
                previousGrade,
                datePrevious
        );
    }

    private Map<String, String> createDataMap(String[] headers, String[] data) {
        final Map<String, String> dataMap = new HashMap<>();

        int i = 0;
        for (String header : headers) {
            dataMap.put(header, data[i].trim());
            i++;
        }

        return dataMap;
    }

}
