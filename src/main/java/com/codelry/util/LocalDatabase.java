package com.codelry.util;

import com.couchbase.lite.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

//import couchbase.lite;

public class LocalDatabase {
  private static final Logger LOGGER = LogManager.getLogger(LocalDatabase.class);
  private static volatile LocalDatabase instance;
  public static Database database;
  public static Collection collection;

  private LocalDatabase() {}

  public static LocalDatabase getInstance() {
    if (instance == null) {
      synchronized (LocalDatabase.class) {
        if (instance == null) {
          instance = new LocalDatabase();
        }
      }
    }
    return instance;
  }

  public static void init(String dbPath, String scopeName, String collectionName) throws CouchbaseLiteException {
    CouchbaseLite.init();
    Database.log.getConsole().setLevel(LogLevel.ERROR);
    DatabaseConfiguration config = new DatabaseConfiguration();
    config.setDirectory(dbPath);

    database = new Database(collectionName, config);
    collection = database.createCollection(collectionName, scopeName);
  }

  public static void close() {
    if (database != null) {
      try {
        database.close();
      } catch (CouchbaseLiteException e) {
        LOGGER.error(e);
      }
    }
  }

  public static Collection getCollection() {
    return collection;
  }

  public static Database getDatabase() {
    return database;
  }
}
