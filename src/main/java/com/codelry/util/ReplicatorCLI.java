package com.codelry.util;

import com.couchbase.lite.Collection;
import com.couchbase.lite.CouchbaseLiteException;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ReplicatorCLI {
  private static final Logger LOGGER = LogManager.getLogger(ReplicatorCLI.class);

  public static void main(String[] args) {
    Options options = new Options();
    CommandLine cmd = null;
    String url;
    String username;
    String password;
    String directory;
    String scope;
    String collection;
    List<String> channels;

    Option urlOpt = new Option("U", "url", true, "Gateway URL");
    Option usernameOpt = new Option("u", "username", true, "Username");
    Option passwordOpt = new Option("p", "password", true, "Password");
    Option directoryOpt = new Option("d", "directory", true, "Directory");
    Option scopeOpt = new Option("s", "scope", true, "Scope");
    Option collectionOpt = new Option("c", "collection", true, "Collection");

    Option channelOpt = new Option("C", "channel", true, "Channel");
    Option dumpOpt = new Option("D", "dump", false, "Dump");
    Option resetOpt = new Option("R", "reset", false, "Reset");
    Option selfSignedOpt = new Option("S", "self", false, "Self Signed Certificate");
    Option tailOpt = new Option("T", "tail", false, "Tail");

    urlOpt.setRequired(true);
    usernameOpt.setRequired(true);
    passwordOpt.setRequired(true);
    directoryOpt.setRequired(true);
    scopeOpt.setRequired(true);
    collectionOpt.setRequired(true);

    channelOpt.setRequired(false);
    dumpOpt.setRequired(false);
    resetOpt.setRequired(false);
    selfSignedOpt.setRequired(false);
    tailOpt.setRequired(false);

    options.addOption(urlOpt);
    options.addOption(usernameOpt);
    options.addOption(passwordOpt);
    options.addOption(directoryOpt);
    options.addOption(scopeOpt);
    options.addOption(collectionOpt);

    options.addOption(channelOpt);
    options.addOption(dumpOpt);
    options.addOption(resetOpt);
    options.addOption(selfSignedOpt);
    options.addOption(tailOpt);

    CommandLineParser parser = new DefaultParser();
    HelpFormatter formatter = new HelpFormatter();

    try {
      cmd = parser.parse(options, args);
    } catch (ParseException e) {
      System.out.println(e.getMessage());
      formatter.printHelp("simulator", options);
      System.exit(1);
    }

    url = cmd.getOptionValue("url");
    username = cmd.getOptionValue("username");
    password = cmd.getOptionValue("password");
    directory = cmd.getOptionValue("directory");
    scope = cmd.getOptionValue("scope");
    collection = cmd.getOptionValue("collection");
    if (cmd.hasOption("channel")) {
      channels = List.of(cmd.getOptionValue("channel"));
    } else {
      channels = new java.util.ArrayList<>();
    }
    boolean dump = cmd.hasOption("dump");
    boolean reset = cmd.hasOption("reset");
    boolean selfSigned = cmd.hasOption("self");
    boolean tail = cmd.hasOption("tail");

    try {
      LocalDatabase.init(directory, scope, collection, reset);
      Collection liteCollection = LocalDatabase.getCollection();
      ReplicateDatabase.init(url, username, password, List.of(liteCollection), channels, selfSigned, tail);
      ReplicateDatabase.start();
      if (tail) {
        InterruptHandler handler = new InterruptHandler();
        handler.run();
      } else {
        ReplicateDatabase.replicationWait();
        ReplicateDatabase.stop();
        if (dump) {
          printCollectionDocs();
        }
        LocalDatabase.close();
      }
      System.exit(0);
    } catch (Exception e) {
      System.err.println("Error: " + e);
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  public static void printCollectionDocs() throws CouchbaseLiteException {
    for (JsonNode doc : LocalDatabase.getCollectionDocs()) {
      System.out.println(doc.toString());
    }
  }

  private ReplicatorCLI() {
    super();
  }
}
