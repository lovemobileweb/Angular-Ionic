package com;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.mapping.MappingManager;

/**
 * Created by andrewlynch on 28/02/2016.
 */
public class SessionHolder2 {
  public static Cluster cluster;
  public static Session csession;
  public static MappingManager manager;

  static volatile boolean done = false;

  private static void init() {
    if (!done) {
      cluster = Cluster
          .builder()
          .withClusterName("Test Cluster")
          .addContactPoint("127.0.0.1")
          .withSocketOptions(
              new SocketOptions().setConnectTimeoutMillis(50000)
                  .setReadTimeoutMillis(50000)).build();
      csession = cluster.connect();
      manager = new MappingManager(csession);
      done = true;
    }
  }

  public static Cluster getCluster () {
    init();
    return cluster;
  }

  public static Session getCsession() {
    init();
    return csession;
  }

  public static MappingManager getManager() {
    init();
    return manager;
  }
}
