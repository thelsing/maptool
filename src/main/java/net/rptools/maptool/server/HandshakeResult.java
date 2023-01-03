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
package net.rptools.maptool.server;

import net.rptools.clientserver.simple.client.ClientConnection;

public class HandshakeResult {
  private boolean successful;
  private String errorMessage;

  private ClientConnection connection;

  public HandshakeResult(boolean successful, String errorMessage, ClientConnection connection) {
    this.successful = successful;
    this.errorMessage = errorMessage;
    this.connection = connection;
  }

  /**
   * Returns if the handshake has been successful or not.
   *
   * @return {@code true} if the handshake has been successful, {code false} if it has failed.
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Returns the message for the error -- if any -- that occurred during the handshake.
   *
   * @return the message for the error that occurred during handshake.
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Returns the connection for this {@code ServerHandshake}.
   *
   * @return the connection for this {@code ServerHandshake}.
   */
  public ClientConnection getConnection() {
    return connection;
  }
}
