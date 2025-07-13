package com.sakila.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import redis.clients.jedis.Jedis;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class App {

    // PostgreSQL connection details
    private static final String PG_URL = "jdbc:postgresql://localhost:5432/sakila_project";
    private static final String PG_USER = "postgres";
    private static final String PG_PASSWORD = "ilham@25";

    // Redis connection details
    private static final String REDIS_HOST = "127.0.0.1";
    private static final int REDIS_PORT = 6379;

    // MongoDB connection details
    private static final String MONGO_URI = "mongodb://localhost:27017";
    private static final String MONGO_DATABASE_NAME = "sakila_nosql";

    public static void main(String[] args) {
        System.out.println("Starting Sakila Data Migration...");

        // Establish PostgreSQL Connection
        try (Connection pgConnection = DriverManager.getConnection(PG_URL, PG_USER, PG_PASSWORD)) {
            System.out.println("Connected to PostgreSQL database successfully!");

            // Establish Redis Connection
            try (Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT)) {
                System.out.println("Connected to Redis successfully!");
                System.out.println("Redis PING: " + jedis.ping());

                // Optional: Clear existing data for clean runs
                // jedis.del("city:*");
                // jedis.del("country:*");
                // System.out.println("Cleared existing city and country data from Redis (if any).");

                // Read and Migrate City data from PostgreSQL to Redis 
                readAndMigrateCitiesToRedis(pgConnection, jedis);

                // Read and Migrate Country data from PostgreSQL to Redis 
                readAndMigrateCountriesToRedis(pgConnection, jedis);

            } catch (Exception e) {
                System.err.println("Failed to connect to Redis or perform Redis operations!");
                System.err.println("Error details: " + e.getMessage());
                e.printStackTrace();
            }

            // Establish MongoDB Connection
            try (MongoClient mongoClient = MongoClients.create(MONGO_URI)) {
                MongoDatabase database = mongoClient.getDatabase(MONGO_DATABASE_NAME);
                System.out.println("Connected to MongoDB database: " + MONGO_DATABASE_NAME + " successfully!");

                // Optional: Clear existing data for clean runs (for Film, Actor, Category, Language collections)
                // database.getCollection("films").drop();
                // database.getCollection("actors").drop();
                // database.getCollection("categories").drop();
                // database.getCollection("languages").drop();
                // System.out.println("Cleared existing MongoDB collections (if any).");

                // Read and Migrate Film data from PostgreSQL to MongoDB 
                readAndMigrateFilmsToMongoDB(pgConnection, database);

                // Read and Migrate Actor data from PostgreSQL to MongoDB 
                readAndMigrateActorsToMongoDB(pgConnection, database);

                // Read and Migrate Category data from PostgreSQL to MongoDB 
                readAndMigrateCategoriesToMongoDB(pgConnection, database);

                // Read and Migrate Language data from PostgreSQL to MongoDB 
                readAndMigrateLanguagesToMongoDB(pgConnection, database);

            } catch (Exception e) {
                System.err.println("Failed to connect to MongoDB or perform MongoDB operations!");
                System.err.println("Error details: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.err.println("Failed to connect to PostgreSQL database!");
            e.printStackTrace();
        }

        System.out.println("Migration process completed (or failed).");
    }

    /**
     * Reads all cities from the PostgreSQL database and migrates them to Redis.
     * @param connection The active PostgreSQL JDBC connection.
     * @param jedis The active Jedis (Redis) client instance.
     */
    private static void readAndMigrateCitiesToRedis(Connection connection, Jedis jedis) {
        System.out.println("Reading cities from PostgreSQL and migrating to Redis...");
        String sql = "SELECT city_id, city, country_id FROM city";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int cityCount = 0;
            while (resultSet.next()) {
                int cityId = resultSet.getInt("city_id");
                String cityName = resultSet.getString("city");
                int countryId = resultSet.getInt("country_id");

                String redisKey = "city:" + cityId;
                // Storing country_id as a number, not a string inside JSON
                String redisValue = String.format("{\"id\":%d, \"name\":\"%s\", \"country_id\":%d}",
                                                   cityId, cityName, countryId);

                jedis.set(redisKey, redisValue);
                cityCount++;
            }
            System.out.println("Finished migrating " + cityCount + " cities from PostgreSQL to Redis.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating cities from PostgreSQL:");
            e.printStackTrace();
        }
    }

    /**
     * Reads all countries from the PostgreSQL database and migrates them to Redis. 
     * @param connection The active PostgreSQL JDBC connection.
     * @param jedis The active Jedis (Redis) client instance.
     */
    private static void readAndMigrateCountriesToRedis(Connection connection, Jedis jedis) {
        System.out.println("Reading countries from PostgreSQL and migrating to Redis...");
        String sql = "SELECT country_id, country FROM country";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int countryCount = 0;
            while (resultSet.next()) {
                int countryId = resultSet.getInt("country_id");
                String countryName = resultSet.getString("country");

                String redisKey = "country:" + countryId;
                String redisValue = String.format("{\"id\":%d, \"name\":\"%s\"}",
                                                   countryId, countryName);

                jedis.set(redisKey, redisValue);
                countryCount++;
            }
            System.out.println("Finished migrating " + countryCount + " countries from PostgreSQL to Redis.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating countries from PostgreSQL:");
            e.printStackTrace();
        }
    }

    /**
     * Reads all films from the PostgreSQL database and migrates them to MongoDB. 
     * @param connection The active PostgreSQL JDBC connection.
     * @param database The active MongoDB database instance.
     */
    private static void readAndMigrateFilmsToMongoDB(Connection connection, MongoDatabase database) {
        System.out.println("Reading films from PostgreSQL and migrating to MongoDB...");
        // Assuming 'film' table has film_id, title, description, release_year, language_id
        String sql = "SELECT film_id, title, description, release_year, language_id FROM film";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int filmCount = 0;
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("films");

            while (resultSet.next()) {
                int filmId = resultSet.getInt("film_id");
                String title = resultSet.getString("title");
                String description = resultSet.getString("description");
                int releaseYear = resultSet.getInt("release_year");
                int languageId = resultSet.getInt("language_id");

                // Create a MongoDB document
                Document filmDocument = new Document("_id", filmId)
                                            .append("title", title)
                                            .append("description", description)
                                            .append("release_year", releaseYear)
                                            .append("language_id", languageId);

                collection.insertOne(filmDocument);
                filmCount++;
            }
            System.out.println("Finished migrating " + filmCount + " films from PostgreSQL to MongoDB.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating films from PostgreSQL:");
            e.printStackTrace();
        }
    }

    /**
     * Reads all actors from the PostgreSQL database and migrates them to MongoDB. 
     * @param connection The active PostgreSQL JDBC connection.
     * @param database The active MongoDB database instance.
     */
    private static void readAndMigrateActorsToMongoDB(Connection connection, MongoDatabase database) {
        System.out.println("Reading actors from PostgreSQL and migrating to MongoDB...");
        // Assuming 'actor' table has actor_id, first_name, last_name
        String sql = "SELECT actor_id, first_name, last_name FROM actor";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int actorCount = 0;
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("actors");

            while (resultSet.next()) {
                int actorId = resultSet.getInt("actor_id");
                String firstName = resultSet.getString("first_name");
                String lastName = resultSet.getString("last_name");

                // Create a MongoDB document
                Document actorDocument = new Document("_id", actorId)
                                            .append("first_name", firstName)
                                            .append("last_name", lastName);

                collection.insertOne(actorDocument);
                actorCount++;
            }
            System.out.println("Finished migrating " + actorCount + " actors from PostgreSQL to MongoDB.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating actors from PostgreSQL:");
            e.printStackTrace();
        }
    }

    /**
     * Reads all categories from the PostgreSQL database and migrates them to MongoDB. 
     * @param connection The active PostgreSQL JDBC connection.
     * @param database The active MongoDB database instance.
     */
    private static void readAndMigrateCategoriesToMongoDB(Connection connection, MongoDatabase database) {
        System.out.println("Reading categories from PostgreSQL and migrating to MongoDB...");
        String sql = "SELECT category_id, name FROM category";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int categoryCount = 0;
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("categories");

            while (resultSet.next()) {
                int categoryId = resultSet.getInt("category_id");
                String categoryName = resultSet.getString("name");

                Document categoryDocument = new Document("_id", categoryId)
                                                .append("name", categoryName);

                collection.insertOne(categoryDocument);
                categoryCount++;
            }
            System.out.println("Finished migrating " + categoryCount + " categories from PostgreSQL to MongoDB.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating categories from PostgreSQL:");
            e.printStackTrace();
        }
    }

    /**
     * Reads all languages from the PostgreSQL database and migrates them to MongoDB. 
     * @param connection The active PostgreSQL JDBC connection.
     * @param database The active MongoDB database instance.
     */
    private static void readAndMigrateLanguagesToMongoDB(Connection connection, MongoDatabase database) {
        System.out.println("Reading languages from PostgreSQL and migrating to MongoDB...");
        String sql = "SELECT language_id, name FROM language";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {

            int languageCount = 0;
            com.mongodb.client.MongoCollection<Document> collection = database.getCollection("languages");

            while (resultSet.next()) {
                int languageId = resultSet.getInt("language_id");
                String languageName = resultSet.getString("name");

                Document languageDocument = new Document("_id", languageId)
                                                .append("name", languageName);

                collection.insertOne(languageDocument);
                languageCount++;
            }
            System.out.println("Finished migrating " + languageCount + " languages from PostgreSQL to MongoDB.");

        } catch (SQLException e) {
            System.err.println("Error reading or migrating languages from PostgreSQL:");
            e.printStackTrace();
        }
    }
}