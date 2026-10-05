package com.uberfreight.search.scripts;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

/**
 * Generates synthetic shipment listings and loads them into PostgreSQL.
 * Run with: mvn exec:java -Dexec.mainClass=com.uberfreight.search.scripts.SeedData
 *
 * Re-running this script truncates and reloads the table, so it is safe to run repeatedly.
 */
public final class SeedData {

    private static final int SHIPMENT_COUNT = 300;

    private record CityState(String city, String state) {
    }

    private static final List<CityState> CITIES = List.of(
            new CityState("Chicago", "IL"),
            new CityState("Dallas", "TX"),
            new CityState("Atlanta", "GA"),
            new CityState("Columbus", "OH"),
            new CityState("Phoenix", "AZ"),
            new CityState("Denver", "CO"),
            new CityState("Memphis", "TN"),
            new CityState("Charlotte", "NC"),
            new CityState("Indianapolis", "IN"),
            new CityState("Kansas City", "MO"),
            new CityState("Sacramento", "CA"),
            new CityState("Portland", "OR"),
            new CityState("Newark", "NJ"),
            new CityState("Louisville", "KY"),
            new CityState("Salt Lake City", "UT"),
            new CityState("Tampa", "FL"),
            new CityState("Omaha", "NE"),
            new CityState("Richmond", "VA"));

    private static final List<String> EQUIPMENT_TYPES =
            List.of("Dry Van", "Reefer", "Flatbed", "Step Deck", "Tanker");

    private static final List<String> STATUSES =
            List.of("available", "booked", "in_transit", "delivered", "cancelled");

    public static void main(String[] args) throws Exception {
        String url = env("DB_URL", "jdbc:postgresql://localhost:5433/freight");
        String user = env("DB_USER", "freight");
        String password = env("DB_PASSWORD", "freight");
        Random random = new Random();

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            createTable(connection);
            truncateTable(connection);
            insertShipments(connection, random);
        }

        System.out.println("Seeded " + SHIPMENT_COUNT + " synthetic shipments into PostgreSQL.");
    }

    private static void createTable(Connection connection) throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS shipments (
                    id BIGSERIAL PRIMARY KEY,
                    origin_city VARCHAR(100) NOT NULL,
                    origin_state VARCHAR(2) NOT NULL,
                    destination_city VARCHAR(100) NOT NULL,
                    destination_state VARCHAR(2) NOT NULL,
                    pickup_date DATE NOT NULL,
                    equipment_type VARCHAR(50) NOT NULL,
                    rate_usd NUMERIC(10,2) NOT NULL,
                    weight_lbs INTEGER NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    updated_at TIMESTAMP NOT NULL DEFAULT now()
                )
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    private static void truncateTable(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "TRUNCATE TABLE shipments RESTART IDENTITY")) {
            statement.execute();
        }
    }

    private static void insertShipments(Connection connection, Random random) throws Exception {
        String sql = """
                INSERT INTO shipments
                    (origin_city, origin_state, destination_city, destination_state,
                     pickup_date, equipment_type, rate_usd, weight_lbs, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < SHIPMENT_COUNT; i++) {
                CityState origin = CITIES.get(random.nextInt(CITIES.size()));
                CityState destination = CITIES.get(random.nextInt(CITIES.size()));

                statement.setString(1, origin.city());
                statement.setString(2, origin.state());
                statement.setString(3, destination.city());
                statement.setString(4, destination.state());
                statement.setObject(5, LocalDate.now().plusDays(random.nextInt(35) - 5));
                statement.setString(6, EQUIPMENT_TYPES.get(random.nextInt(EQUIPMENT_TYPES.size())));
                statement.setDouble(7, 500 + random.nextDouble() * 4500);
                statement.setInt(8, 5000 + random.nextInt(40000));
                statement.setString(9, STATUSES.get(random.nextInt(STATUSES.size())));
                statement.setTimestamp(10, Timestamp.valueOf(
                        java.time.LocalDateTime.now().minusMinutes(random.nextInt(10000))));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
