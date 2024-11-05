package com.codelry.util;

import com.couchbase.lite.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.List;

public class ReplicateDatabase {
  private static final Logger LOGGER = LogManager.getLogger(ReplicateDatabase.class);
  private static volatile ReplicateDatabase instance;
  private static volatile Replicator replicator;
  private static ReplicationStatus replicationStatus;
  private static String replicationProgress;
  private static ListenerToken listenerToken;
  private static ListenerToken docToken;
  private static boolean selfSignedCertificate = true;
  private static int retryCount = 10;

  private ReplicateDatabase() {}

  public static ReplicateDatabase getInstance() {
    if (instance == null) {
      synchronized (ReplicateDatabase.class) {
        if (instance == null) {
          instance = new ReplicateDatabase();
        }
      }
    }
    return instance;
  }

  public static void init(String url, String username, String password, List<Collection> collections, List<String> channels, boolean selfSigned) throws URISyntaxException {
    CollectionConfiguration config = new CollectionConfiguration()
        .setChannels(channels);

    ReplicatorConfiguration replicatorConfig = new ReplicatorConfiguration(new URLEndpoint(new URI(url)))
        .addCollections(collections, config)
        .setType(ReplicatorType.PULL)
        .setContinuous(false)
        .setAutoPurgeEnabled(false)
        .setAuthenticator(new BasicAuthenticator(username, password.toCharArray()));
    if (selfSigned) {
      replicatorConfig.setAcceptOnlySelfSignedServerCertificate(true);
    }
    replicator = new Replicator(replicatorConfig);


    listenerToken = replicator.addChangeListener(change -> {
      CouchbaseLiteException err = change.getStatus().getError();
      if (err != null) {
        LOGGER.error("Error code :: {}", err.getCode(), err);
      }

      switch (change.getStatus().getActivityLevel()) {
        case OFFLINE:
          replicationStatus = ReplicationStatus.OFFLINE;
          LOGGER.info("Replication Status OFFLINE");
          break;
        case IDLE:
          replicationStatus = ReplicationStatus.IDlE;
          LOGGER.info("Replication Status IDLE");
          break;
        case STOPPED:
          replicationStatus = ReplicationStatus.STOPPED;
          LOGGER.info("Replication Status STOPPED");
          break;
        case BUSY:
          replicationStatus = ReplicationStatus.BUSY;
          LOGGER.info("Replication Status BUSY");
          break;
        case CONNECTING:
          replicationStatus = ReplicationStatus.CONNECTING;
          LOGGER.info("Replication Status CONNECTING");
          break;
      }

      if (change.getStatus().getProgress().getCompleted() == change.getStatus().getProgress().getTotal() || change.getStatus().getProgress().getCompleted() == 0) {
        replicationProgress = "Completed";
        LOGGER.info("Replication Status COMPLETED");
      } else {
        replicationProgress = String.format("%s / %s", change.getStatus().getProgress().getTotal(), change.getStatus().getProgress().getCompleted());
      }
    });

    docToken = replicator.addDocumentReplicationListener(replication -> {
      for (ReplicatedDocument document : replication.getDocuments()) {
        LOGGER.info("Doc ID: {}", document.getID());

        if (document.getFlags().contains(DocumentFlag.DELETED)) {
          LOGGER.info("Successfully replicated a deleted document");
        }
      }
    });
  }

  public static void start() {
    replicator.start(true);
  }

  public static void stop() {
    replicator.stop();
  }

  public static void replicationWait() {
    LOGGER.info("Waiting for replication to complete");
    for (int i = 0; i < retryCount * 10; i++) {
      boolean completed = Boolean.TRUE.equals(Flux.just(replicationProgress)
          .flatMap(r -> Flux.just(r.equals("Completed")))
          .delayElements(Duration.ofMillis(100L))
          .blockLast());
      if (completed) {
        break;
      }
    }
  }

  public static ReplicationStatus getReplicationStatus() {
    return replicationStatus;
  }

  public static Replicator getReplicator() {
    return replicator;
  }
}
