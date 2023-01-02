/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.client;

import static net.rptools.maptool.server.proto.Message.MessageTypeCase.HEARTBEAT_MSG;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import net.rptools.clientserver.ConnectionFactory;
import net.rptools.clientserver.simple.client.ClientConnection;
import net.rptools.maptool.client.ui.ActivityMonitorPanel;
import net.rptools.maptool.model.player.LocalPlayer;
import net.rptools.maptool.server.ClientHandshake;
import net.rptools.maptool.server.HandshakeResult;
import net.rptools.maptool.server.ServerConfig;
import net.rptools.maptool.server.proto.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** @author trevor */
public class MapToolConnection {

  /** Instance used for log messages. */
  private static final Logger log = LogManager.getLogger(MapToolConnection.class);

  private final LocalPlayer player;
  private ClientConnection connection;
  private ServerConfig config;

  public MapToolConnection(ServerConfig config, LocalPlayer player) {
    this.config = config;
    this.player = player;
  }

  public CompletableFuture start() {
    var future = new CompletableFuture();
    Executors.newSingleThreadExecutor()
        .submit(
            () -> {
              try {
                this.connection =
                    ConnectionFactory.getInstance()
                        .createClientConnection(player.getName(), config);
                new ClientHandshake(connection, player)
                    .execute()
                    .exceptionally(
                        t -> {
                          log.warn(t);
                          return new HandshakeResult(false, t.toString());
                        })
                    .thenApply(
                        (result) -> {
                          if (!result.successful()) {
                            MapTool.showError(result.errorMessage());
                            connection.close();
                            AppActions.disconnectFromServer();
                          }
                          future.complete(null);
                          return result;
                        });
              } catch (Throwable t) {
                future.completeExceptionally(t);
              }
            });
    return future;
  }

  public void addMessageHandler(ClientMessageHandler handler) {
    connection.addMessageHandler(handler);
  }

  public void addActivityListener(ActivityMonitorPanel activityMonitor) {
    connection.addActivityListener(activityMonitor);
  }

  public void addDisconnectHandler(ServerDisconnectHandler serverDisconnectHandler) {
    connection.addDisconnectHandler(serverDisconnectHandler);
  }

  public boolean isAlive() {
    return connection.isAlive();
  }

  public void close() throws IOException {
    connection.close();
  }

  public void sendMessage(Message msg) {
    var msgType = msg.getMessageTypeCase();
    var logText = player.getName() + " sent " + msg.getMessageTypeCase();
    if (msgType == HEARTBEAT_MSG) {
      log.debug(logText);
    } else {
      log.info(logText);
    }
    connection.sendMessage(msg.toByteArray());
  }
}
