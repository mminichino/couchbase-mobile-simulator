package com.codelry.util;

import com.couchbase.lite.Collection;
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
    String channel;

    Option urlOpt = new Option("U", "url", true, "Gateway URL");
    Option usernameOpt = new Option("u", "username", true, "Username");
    Option passwordOpt = new Option("p", "password", true, "Password");
    Option directoryOpt = new Option("d", "directory", true, "Directory");
    Option scopeOpt = new Option("s", "scope", true, "Scope");
    Option collectionOpt = new Option("c", "collection", true, "Collection");
    Option channelOpt = new Option("C", "channel", true, "Channel");
    urlOpt.setRequired(true);
    usernameOpt.setRequired(true);
    passwordOpt.setRequired(true);
    directoryOpt.setRequired(true);
    scopeOpt.setRequired(true);
    collectionOpt.setRequired(true);
    channelOpt.setRequired(true);
    options.addOption(urlOpt);
    options.addOption(usernameOpt);
    options.addOption(passwordOpt);
    options.addOption(directoryOpt);
    options.addOption(scopeOpt);
    options.addOption(collectionOpt);
    options.addOption(channelOpt);

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
    channel = cmd.getOptionValue("channel");

    try {
      LocalDatabase.init(directory, scope, collection);
      Collection liteCollection = LocalDatabase.getCollection();
      ReplicateDatabase.init(url, username, password, List.of(liteCollection), List.of(channel), false);
      ReplicateDatabase.start();
    } catch (Exception e) {
      System.err.println("Error: " + e);
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private ReplicatorCLI() {
    super();
  }
}
