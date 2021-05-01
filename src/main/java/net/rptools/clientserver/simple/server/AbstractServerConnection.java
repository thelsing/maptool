package net.rptools.clientserver.simple.server;

import net.rptools.clientserver.simple.AbstractConnection;
import net.rptools.clientserver.simple.DisconnectHandler;
import net.rptools.clientserver.simple.MessageHandler;
import net.rptools.clientserver.simple.client.IClientConnection;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.*;

public abstract class AbstractServerConnection extends AbstractConnection
    implements MessageHandler, DisconnectHandler, IServerConnection {

  private static final Logger log = Logger.getLogger(AbstractServerConnection.class);
  //    private final ReaperThread reaperThread;

  private final Map<String, IClientConnection> clients =
      Collections.synchronizedMap(new HashMap<String, IClientConnection>());
  private final List<ServerObserver> observerList =
      Collections.synchronizedList(new ArrayList<ServerObserver>());

  private final IHandshake handshake;

  public AbstractServerConnection(IHandshake handshake) {
    this.handshake = handshake;
  }


  public void addObserver(ServerObserver observer) {
    observerList.add(observer);
  }

  public void removeObserver(ServerObserver observer) {
    observerList.remove(observer);
  }

  public void handleMessage(String id, byte[] message) {
    dispatchMessage(id, message);
  }

  public void broadcastMessage(byte[] message) {
    synchronized (clients) {
      for (IClientConnection conn : clients.values()) {
        conn.sendMessage(message);
      }
    }
  }

  public void broadcastMessage(String[] exclude, byte[] message) {
    Set<String> excludeSet = new HashSet<String>();
    for (String e : exclude) {
      excludeSet.add(e);
    }
    synchronized (clients) {
      for (Map.Entry<String, IClientConnection> entry : clients.entrySet()) {
        if (!excludeSet.contains(entry.getKey())) {
          entry.getValue().sendMessage(message);
        }
      }
    }
  }

  public void sendMessage(String id, byte[] message) {
    sendMessage(id, null, message);
  }

  public void sendMessage(String id, Object channel, byte[] message) {
    IClientConnection client = clients.get(id);
    client.sendMessage(channel, message);
  }

  public void close() throws IOException {
    synchronized (clients) {
      for (IClientConnection conn : clients.values()) {
        conn.close();
      }
    }
  }

  protected void reapClients() {
    log.debug("About to reap clients");
    synchronized (clients) {
      log.debug("Reaping clients");

      for (Iterator<Map.Entry<String, IClientConnection>> i = clients.entrySet().iterator();
           i.hasNext(); ) {
        Map.Entry<String, IClientConnection> entry = i.next();
        IClientConnection conn = entry.getValue();
        if (!conn.isAlive()) {
          log.debug("\tReaping: " + conn.getId());
          try {
            i.remove();
            fireClientDisconnect(conn);
            conn.close();
          } catch (IOException e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    }
  }

  protected void fireClientConnect(IClientConnection conn) {
    log.debug("Firing: clientConnect: " + conn.getId());
    for (ServerObserver observer : observerList) {
      observer.connectionAdded(conn);
    }
  }

  protected void fireClientDisconnect(IClientConnection conn) {
    log.debug("Firing: clientDisconnect: " + conn.getId());
    for (ServerObserver observer : observerList) {
      observer.connectionRemoved(conn);
    }
  }

  ////
  // DISCONNECT HANDLER
  public void handleDisconnect(AbstractConnection conn) {
    if (conn instanceof IClientConnection) {
      log.debug("HandleDisconnect: " + ((IClientConnection) conn).getId());
      fireClientDisconnect((IClientConnection) conn);
    }
  }

  protected void handleConnection(IClientConnection conn) throws IOException {
    // Make sure the client is allowed
    if (!handshake.handleConnectionHandshake(conn)) {
      log.debug("Client closing: bad handshake");
      close();
      return;
    }

    conn.addMessageHandler(this);
    conn.addDisconnectHandler(this);
    conn.start();

    log.debug("About to add new client");
    synchronized (clients) {
      reapClients();

      log.debug("Adding new client");
      clients.put(conn.getId(), conn);
      fireClientConnect(conn);
      // System.out.println("new client " + conn.getId() + " added, " + server.clients.size()
      // + " total");
    }
  }
}