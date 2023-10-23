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
package net.rptools.maptool.client.ui.zone.gdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.FloatArray;
import java.awt.geom.Area;
import net.rptools.maptool.model.drawing.*;

public abstract class AbstractDrawingDrawer {
  protected Color tmpColor = new Color();

  protected Float alpha = null;
  protected AreaRenderer areaRenderer;

  public AbstractDrawingDrawer(AreaRenderer areaRenderer) {
    this.areaRenderer = areaRenderer;
  }

  public void draw(PolygonSpriteBatch batch, Drawable element, Pen pen) {
    applyColor(pen.getBackgroundPaint(), true);
    drawBackground(batch, element, pen);

    applyColor(pen.getPaint(), false);
    drawBorder(batch, element, pen);
  }

  protected void applyColor(DrawablePaint paint, boolean applyAlpha) {
    if (paint instanceof DrawableColorPaint colorPaint) {
      Color.argb8888ToColor(tmpColor, colorPaint.getColor());

      if (alpha != null && applyAlpha) {
        tmpColor.set(tmpColor.r, tmpColor.g, tmpColor.b, alpha);
      }
      areaRenderer.setColor(tmpColor);
    } else if (paint instanceof DrawableTexturePaint texturePaint) {
      var image = texturePaint.getAsset().getData();
      var pix = new Pixmap(image, 0, image.length);
      var tex = new Texture(pix);
      tex.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
      // FIXME properly dispose
      var region = new TextureRegion(tex);
      if (alpha != null) {
        tmpColor.set(1, 1, 1, alpha);
        areaRenderer.setColor(tmpColor);
      }
      areaRenderer.setTextureRegion(region);
      pix.dispose();
    }
  }

  protected void line(PolygonSpriteBatch batch, Pen pen, float x1, float y1, float x2, float y2) {
    var floats = new FloatArray();
    // negate y values because we are y-up
    floats.add(x1, -y1, x2, -y2);
    var path =
        areaRenderer.path(
            floats.toArray(),
            pen.getThickness(),
            pen.getSquareCap() ? AreaRenderer.JoinType.Pointy : AreaRenderer.JoinType.Round,
            false);
    applyColor(pen.getPaint(), false);
    areaRenderer.paintVertices(batch, path, null);
  }

  protected void fillArea(PolygonSpriteBatch batch, Area area, Pen pen) {
    applyColor(pen.getBackgroundPaint(), true);
    areaRenderer.fillArea(batch, area);
  }

  protected void drawArea(PolygonSpriteBatch batch, Area area, Pen pen) {
    applyColor(pen.getPaint(), false);
    areaRenderer.drawArea(batch, area, !pen.getSquareCap(), pen.getThickness());
  }

  protected abstract void drawBackground(PolygonSpriteBatch batch, Drawable element, Pen pen);

  protected abstract void drawBorder(PolygonSpriteBatch batch, Drawable element, Pen pen);
}
