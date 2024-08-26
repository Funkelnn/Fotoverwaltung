package de.thm.mni.gruppe8.fotoverwaltung;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
  private static Connection connection;

  public static void initialize() throws SQLException, IOException {
    Properties properties = new Properties();

    // Lade die Eigenschaften aus der Datei
    try (InputStream input = DatabaseManager.class.getClassLoader().getResourceAsStream("database.properties")) {
      if (input == null) {
        throw new IOException("Unable to find database.properties");
      }
      properties.load(input);
    }

    String url = properties.getProperty("db.url");
    String username = properties.getProperty("db.username");
    String password = properties.getProperty("db.password");

    connection = DriverManager.getConnection(url, username, password);

    if (connection.isValid(5)) {
      System.out.println("Connected to the database");
    } else {
      throw new SQLException("Database connection is not valid");
    }
  }

  public static Connection getConnection() throws IllegalStateException {
    if (connection == null) {
      throw new IllegalStateException("Database connection is not initialized");
    }
    return connection;
  }

  public static void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      System.out.println("Error closing database connection -> " + e.getMessage());
    }
  }

}
