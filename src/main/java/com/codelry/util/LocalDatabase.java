package com.codelry.util;

import com.couchbase.lite.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//import couchbase.lite;

public class LocalDatabase {
  private static final Logger LOGGER = LogManager.getLogger(LocalDatabase.class);
  private static volatile LocalDatabase instance;
  public static Database database;
  public static Collection collection;
  public static ObjectMapper mapper = new ObjectMapper();

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

  public static void init(String dbPath, String scopeName, String collectionName, boolean reset) throws CouchbaseLiteException {
    CouchbaseLite.init();
    Database.log.getConsole().setLevel(LogLevel.ERROR);
    DatabaseConfiguration config = new DatabaseConfiguration();
    config.setDirectory(dbPath);

    database = new Database(collectionName, config);
    if (reset) {
      LOGGER.info("Resetting database");
      database.delete();
      database = new Database(collectionName, config);
    }
    collection = database.createCollection(collectionName, scopeName);
  }

  public static void clear() throws CouchbaseLiteException {
    if (database != null) {
      database.delete();
    }
  }

  public static List<JsonNode> getCollectionDocs() throws CouchbaseLiteException {
    List<JsonNode> docs = new ArrayList<>();

    final Query listQuery = QueryBuilder
        .select(SelectResult.expression(Meta.id).as("id"))
        .from(DataSource.collection(collection));

    try (ResultSet results = listQuery.execute()) {
      for (Result row: results) {
        String id = row.getString("id");
        if (id == null) {
          LOGGER.warn("Null id in collection");
          continue;
        }
        String json = Objects.requireNonNull(collection.getDocument(id)).toJSON();
        JsonNode doc = mapper.readTree(json);
        docs.add(doc);
      }
    } catch (JsonProcessingException e) {
      LOGGER.error("Error parsing document JSON: {}", e.getMessage());
    }

    return docs;
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
