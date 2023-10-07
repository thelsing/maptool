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
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.Bezier;
import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.FloatArray;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import net.rptools.lib.gdx.Joiner;
import space.earlygrey.shapedrawer.DefaultSideEstimator;
import space.earlygrey.shapedrawer.ShapeDrawer;
import space.earlygrey.shapedrawer.ShapeUtils;
import space.earlygrey.shapedrawer.SideEstimator;

public class AreaRenderer {

  public AreaRenderer(
      EarClippingTriangulator triangulator, TextureRegion whitePixelRegion, ShapeDrawer drawer) {
    this.triangulator = triangulator;
    this.whitePixel = whitePixelRegion;
    this.drawer = drawer;
  }

  private ShapeDrawer drawer;
  private TextureRegion whitePixel;

  private FloatArray tmpFloat = new FloatArray();

  private Color color;

  public void setColor(Color value) {
    color = value;
    textureRegion = whitePixel;
  }

  public EarClippingTriangulator triangulator;

  private float[] floatsFromArea = new float[6];
  private Vector2 tmpVector = new Vector2();
  private Vector2 tmpVector0 = new Vector2();
  private Vector2 tmpVector1 = new Vector2();
  private Vector2 tmpVector2 = new Vector2();
  private Vector2 tmpVector3 = new Vector2();
  private Vector2 tmpVectorOut = new Vector2();

  private TextureRegion textureRegion = null;

  public TextureRegion getTextureRegion() {
    return textureRegion;
  }

  public void setTextureRegion(TextureRegion textureRegion) {
    this.textureRegion = textureRegion;
  }

  public void fillArea(PolygonSpriteBatch batch, Area area) {
    if (area == null || area.isEmpty()) return;

    pathToFloatArray(area.getPathIterator(null));
    paintVertices(batch, tmpFloat.toArray());
  }

  private boolean debug = false;

  public void drawArea(PolygonSpriteBatch batch, Area area, boolean rounded, float thickness) {
    if (area == null || area.isEmpty()) return;

    pathToFloatArray(area.getPathIterator(null));
    var vertices = path(tmpFloat, thickness, rounded ? JoinType.Round : JoinType.Pointy, false);
    // drawDebug(vertices);
    // debug = true;
    paintVertices(batch, vertices);
    // debug = false;
  }

  private void drawDebug(float[] vertices) {
    var oldColor = drawer.getPackedColor();
    drawer.setColor(Color.ORANGE);
    for (int j = 0; j < vertices.length; j += 2) {
      float x1 = vertices[j];
      float y1 = vertices[j + 1];

      if (j + 2 >= vertices.length) break;
      float x2 = vertices[j + 2];
      float y2 = vertices[j + 3];
      drawer.line(x1, y1, x2, y2);
    }
    drawer.setColor(oldColor);
  }

  private void drawDebug(float[] vertices, short[] indicies) {
    var oldColor = drawer.getPackedColor();
    drawer.setColor(Color.CYAN);
    for (int j = 0; j < indicies.length; j += 3) {
      float x1 = vertices[2 * indicies[j]];
      float y1 = vertices[2 * indicies[j] + 1];
      float x2 = vertices[2 * indicies[j + 1]];
      float y2 = vertices[2 * indicies[j + 1] + 1];
      float x3 = vertices[2 * indicies[j + 2]];
      float y3 = vertices[2 * indicies[j + 2] + 1];
      drawer.triangle(x1, y1, x2, y2, x3, y3);
    }
    drawer.setColor(oldColor);
  }

  protected void paintVertices(PolygonSpriteBatch batch, float[] vertices) {
    var indicies = triangulator.computeTriangles(vertices).toArray();
    var polyreg = new PolygonRegion(textureRegion, vertices, indicies);
    var poly = new PolygonSprite(polyreg);
    if (color != null) {
      poly.setColor(color);
    }
    if (debug) drawDebug(vertices, indicies);
    else poly.draw(batch);
    color = null;
  }

  public FloatArray pathToFloatArray(PathIterator it) {
    tmpFloat.clear();

    float moveToX = 0;
    float moveToY = 0;
    for (; !it.isDone(); it.next()) {
      int type = it.currentSegment(floatsFromArea);

      switch (type) {
        case PathIterator.SEG_MOVETO:
          //                   System.out.println("Move to: ( " + floatsFromArea[0] + ", " +
          // floatsFromArea[1] + ")");
          moveToX = floatsFromArea[0];
          moveToY = -floatsFromArea[1];
          tmpFloat.add(floatsFromArea[0], -floatsFromArea[1]);

          break;
        case PathIterator.SEG_CLOSE:
          //                   System.out.println("Close");
          tmpFloat.add(moveToX, moveToY);
          break;
          // return tmpFloat;
        case PathIterator.SEG_LINETO:
          //                  System.out.println("Line to: ( " + floatsFromArea[0] + ", " +
          // floatsFromArea[1] + ")");
          tmpFloat.add(floatsFromArea[0], -floatsFromArea[1]);
          break;
        case PathIterator.SEG_QUADTO:
          //                  System.out.println("quadratic bezier with: ( " + floatsFromArea[0] +
          // ", " + floatsFromArea[1] +
          //                          "), (" + floatsFromArea[2] + ", " + floatsFromArea[3] + ")");

          tmpVector0.set(tmpFloat.get(tmpFloat.size - 2), tmpFloat.get(tmpFloat.size - 1));
          tmpVector1.set(floatsFromArea[0], -floatsFromArea[1]);
          tmpVector2.set(floatsFromArea[2], -floatsFromArea[3]);
          for (var i = 1; i <= GdxRenderer.POINTS_PER_BEZIER; i++) {
            Bezier.quadratic(
                tmpVectorOut,
                i / GdxRenderer.POINTS_PER_BEZIER,
                tmpVector0,
                tmpVector1,
                tmpVector2,
                tmpVector);
            tmpFloat.add(tmpVectorOut.x, tmpVectorOut.y);
          }
          break;
        case PathIterator.SEG_CUBICTO:
          //                    System.out.println("cubic bezier with: ( " + floatsFromArea[0] + ",
          // " + floatsFromArea[1] +
          //                            "), (" + floatsFromArea[2] + ", " + floatsFromArea[3] +
          //                            "), (" + floatsFromArea[4] + ", " + floatsFromArea[5] +
          // ")");

          tmpVector0.set(tmpFloat.get(tmpFloat.size - 2), tmpFloat.get(tmpFloat.size - 1));
          tmpVector1.set(floatsFromArea[0], -floatsFromArea[1]);
          tmpVector2.set(floatsFromArea[2], -floatsFromArea[3]);
          tmpVector3.set(floatsFromArea[4], -floatsFromArea[5]);
          for (var i = 1; i <= GdxRenderer.POINTS_PER_BEZIER; i++) {
            Bezier.cubic(
                tmpVectorOut,
                i / GdxRenderer.POINTS_PER_BEZIER,
                tmpVector0,
                tmpVector1,
                tmpVector2,
                tmpVector3,
                tmpVector);
            tmpFloat.add(tmpVectorOut.x, tmpVectorOut.y);
          }
          break;
        default:
          System.out.println("Type: " + type);
      }
    }
    if (tmpFloat.get(0) == tmpFloat.get(tmpFloat.size - 2)
        && tmpFloat.get(1) == tmpFloat.get(tmpFloat.size - 1)) {
      // make sure we don't have last and first point the same
      tmpFloat.pop();
      tmpFloat.pop();
    }
    return tmpFloat;
  }

  private Vector2 A = new Vector2();
  private Vector2 B = new Vector2();
  private Vector2 C = new Vector2();
  private Vector2 D = new Vector2();
  private Vector2 E = new Vector2();
  private Vector2 E0 = new Vector2();
  private Vector2 D0 = new Vector2();
  private Vector2 AB = new Vector2();
  private Vector2 BC = new Vector2();
  private Vector2 vec1 = new Vector2();

  enum JoinType {
    Pointy,
    Smooth,
    Round
  }

  public float[] path(FloatArray path, float lineWidth, JoinType joinType, boolean open) {
    var outer = new ArrayList<Float>();
    var inner = new ArrayList<Float>();

    float halfWidth = lineWidth / 2f;

    if (path.size == 2) {
      var x = path.get(0);
      var y = path.get(1);
      if (joinType == JoinType.Round) {
        addArc(outer, x, y, halfWidth, 0, MathUtils.PI2 - 0.1f, false);
      } else {
        outer.add(x - halfWidth);
        outer.add(y - halfWidth);
        outer.add(x - halfWidth);
        outer.add(y + halfWidth);
        outer.add(x + halfWidth);
        outer.add(y + halfWidth);
        outer.add(x + halfWidth);
        outer.add(y - halfWidth);
      }

    } else if (path.size == 4) {
      A.set(path.get(0), path.get(1));
      B.set(path.get(2), path.get(3));
      if (joinType == JoinType.Round) {
        Joiner.prepareFlatEndpoint(B, A, D, E, halfWidth);
        E0.set(D);
        outer.add(D.x);
        outer.add(D.y);
        vec1.set(D).add(-A.x, -A.y);
        var angle = vec1.angleRad();
        addArc(outer, A.x, A.y, halfWidth, angle, angle + MathUtils.PI, false);
        outer.add(E.x);
        outer.add(E.y);

        Joiner.prepareFlatEndpoint(A, B, D, E, halfWidth);
        outer.add(D.x);
        outer.add(D.y);
        vec1.set(D).add(-B.x, -B.y);
        angle = vec1.angleRad();
        addArc(outer, B.x, B.y, halfWidth, angle, angle + MathUtils.PI, false);
        outer.add(E.x);
        outer.add(E.y);
        outer.add(E0.x);
        outer.add(E0.y);
      } else {
        Joiner.prepareSquareEndpoint(B, A, D, E, halfWidth);
        E0.set(D);
        outer.add(D.x);
        outer.add(D.y);
        outer.add(E.x);
        outer.add(E.y);
        Joiner.prepareSquareEndpoint(A, B, D, E, halfWidth);
        outer.add(D.x);
        outer.add(D.y);
        outer.add(E.x);
        outer.add(E.y);
        outer.add(E0.x);
        outer.add(E0.y);
      }

    } else {
      for (int i = 2; i < path.size - 2; i += 2) {
        A.set(path.get(i - 2), path.get(i - 1));
        B.set(path.get(i), path.get(i + 1));
        C.set(path.get(i + 2), path.get(i + 3));
        if (i == 2) {
          if (open) {
            if (joinType == JoinType.Round) {
              Joiner.prepareFlatEndpoint(B, A, D, E, halfWidth);
              outer.add(D.x);
              outer.add(D.y);
              vec1.set(D).add(-A.x, -A.y);
              var angle = vec1.angleRad();
              addArc(inner, A.x, A.y, halfWidth, angle, angle + MathUtils.PI, false);
              inner.add(E.x);
              inner.add(E.y);
            } else {
              Joiner.prepareSquareEndpoint(B, A, D, E, halfWidth);
              outer.add(D.x);
              outer.add(D.y);

              // add link at start
              //	inner.add(D.x);
              //	inner.add(D.y);

              inner.add(E.x);
              inner.add(E.y);
            }
          } else {
            vec1.set(path.get(path.size - 2), path.get(path.size - 1));
            if (joinType == JoinType.Pointy) {
              Joiner.preparePointyJoin(vec1, A, B, D0, E0, halfWidth);
            } else {
              Joiner.prepareSmoothJoin(vec1, A, B, D0, E0, halfWidth, true);
            }
            outer.add(D0.x);
            outer.add(D0.y);
            inner.add(E0.x);
            inner.add(E0.y);
          }
        }
        if (joinType == JoinType.Pointy) {
          Joiner.preparePointyJoin(A, B, C, D, E, halfWidth);
          outer.add(D.x);
          outer.add(D.y);
          inner.add(E.x);
          inner.add(E.y);
        } else {
          var bendsLeft = Joiner.prepareSmoothJoin(A, B, C, D, E, halfWidth, false);
          if (bendsLeft) {
            vec1.set(E);
          } else {
            vec1.set(D);
          }
          outer.add(D.x);
          outer.add(D.y);
          inner.add(E.x);
          inner.add(E.y);
          // shapeRenderer.circle(B.x, B.y, halfWidth);

          Joiner.prepareSmoothJoin(A, B, C, D, E, halfWidth, true);
          if (bendsLeft) {
            if (joinType == JoinType.Round) {
              AB.set(B).sub(A);
              BC.set(C).sub(B);
              vec1.add(-B.x, -B.y);
              var angle = vec1.angleRad();
              var angleDiff = MathUtils.PI2 - ShapeUtils.angleRad(AB, BC);
              addArc(inner, B.x, B.y, halfWidth, angle, angle + angleDiff, false);
            }
            inner.add(E.x);
            inner.add(E.y);
          } else {
            if (joinType == JoinType.Round) {
              AB.set(B).sub(A);
              BC.set(C).sub(B);
              vec1.add(-B.x, -B.y);
              var angle = vec1.angleRad();
              var angleDiff = MathUtils.PI2 - ShapeUtils.angleRad(AB, BC);
              addArc(outer, B.x, B.y, halfWidth, angle, angle + angleDiff, true);
            }
            outer.add(D.x);
            outer.add(D.y);
          }
        }
      }
      if (open) {
        if (joinType == JoinType.Round) {
          Joiner.prepareFlatEndpoint(B, C, D, E, halfWidth);
          outer.add(E.x);
          outer.add(E.y);
          inner.add(D.x);
          inner.add(D.y);
          vec1.set(D).add(-C.x, -C.y);
          var angle = vec1.angleRad();
          addArc(inner, C.x, C.y, halfWidth, angle, angle + MathUtils.PI, false);
        } else {
          Joiner.prepareSquareEndpoint(B, C, D, E, halfWidth);
          outer.add(E.x);
          outer.add(E.y);

          inner.add(D.x);
          inner.add(D.y);
        }
      } else {
        if (joinType == JoinType.Pointy) {
          // draw last link on path
          A.set(path.get(0), path.get(1));
          Joiner.preparePointyJoin(B, C, A, D, E, halfWidth);
          outer.add(D.x);
          outer.add(D.y);
          inner.add(E.x);
          inner.add(E.y);

          // draw connection back to first vertex
          outer.add(D0.x);
          outer.add(D0.y);
          inner.add(E0.x);
          inner.add(E0.y);

        } else {
          // draw last link on path
          A.set(B);
          B.set(C);
          C.set(path.get(0), path.get(1));
          var bendsLeft = Joiner.prepareSmoothJoin(A, B, C, D, E, halfWidth, false);
          if (bendsLeft) {
            vec1.set(E);
          } else {
            vec1.set(D);
          }
          outer.add(D.x);
          outer.add(D.y);
          inner.add(E.x);
          inner.add(E.y);

          // draw connection back to first vertex
          Joiner.prepareSmoothJoin(A, B, C, D, E, halfWidth, true);
          if (bendsLeft) {
            if (joinType == JoinType.Round) {
              AB.set(B).sub(A);
              BC.set(C).sub(B);
              vec1.add(-B.x, -B.y);
              var angle = vec1.angleRad();
              var angleDiff = MathUtils.PI2 - ShapeUtils.angleRad(AB, BC);
              addArc(inner, B.x, B.y, halfWidth, angle, angle + angleDiff, false);
            }
            inner.add(E.x);
            inner.add(E.y);
          } else {
            if (joinType == JoinType.Round) {
              AB.set(B).sub(A);
              BC.set(C).sub(B);
              vec1.add(-B.x, -B.y);
              var angle = vec1.angleRad();
              var angleDiff = MathUtils.PI2 - ShapeUtils.angleRad(AB, BC);
              addArc(outer, B.x, B.y, halfWidth, angle, angle + angleDiff, true);
            }
            outer.add(D.x);
            outer.add(D.y);
          }

          A.set(B);
          B.set(C);
          C.set(path.get(2), path.get(3));
          bendsLeft = Joiner.prepareSmoothJoin(A, B, C, D, E, halfWidth, false);
          if (bendsLeft) {
            vec1.set(E);
          } else {
            vec1.set(D);
          }
          outer.add(D.x);
          outer.add(D.y);
          inner.add(E.x);
          inner.add(E.y);

          if (joinType == JoinType.Round) {
            AB.set(B).sub(A);
            BC.set(C).sub(B);
            vec1.add(-B.x, -B.y);
            var angle = vec1.angleRad();
            var angleDiff = MathUtils.PI2 - ShapeUtils.angleRad(AB, BC);
            if (bendsLeft) {
              addArc(inner, B.x, B.y, halfWidth, angle, angle + angleDiff, false);
              inner.add(E0.x);
              inner.add(E0.y);
            } else {
              addArc(outer, B.x, B.y, halfWidth, angle, angle + angleDiff, true);
              outer.add(D0.x);
              outer.add(D0.y);
            }
          } else {
            if (bendsLeft) {
              inner.add(E0.x);
              inner.add(E0.y);
            } else {
              outer.add(D0.x);
              outer.add(D0.y);
            }
          }
        }
      }
    }
    float[] floatArray = new float[outer.size() + inner.size()];
    int i = 0;

    for (Float f : outer) {
      floatArray[i++] = f;
    }

    for (int j = 1; j <= inner.size(); j += 2) {
      floatArray[floatArray.length - j] = inner.get(j);
      floatArray[floatArray.length - j - 1] = inner.get(j - 1);
    }
    return floatArray;
  }

  private Vector2 A1 = new Vector2();
  private Vector2 B1 = new Vector2();
  private Vector2 dir = new Vector2();

  private void addArc(
      List<Float> list,
      float centreX,
      float centreY,
      float radius,
      float startAngle,
      float endAngle,
      boolean clockwise) {
    if (startAngle < 0) {
      startAngle += MathUtils.PI2;
    }

    if (endAngle < 0) {
      endAngle += MathUtils.PI2;
    }

    var deltaAngle = (endAngle + MathUtils.PI2 - startAngle) % MathUtils.PI2;
    if (clockwise) {
      deltaAngle = MathUtils.PI2 - deltaAngle;
    }
    var sides = estimateSidesRequired(radius, radius);
    sides *= deltaAngle / MathUtils.PI2;

    var dAnglePerSide = deltaAngle / sides;
    var angle = startAngle;
    angle += dAnglePerSide;
    sides -= 1;
    if (clockwise) {
      dAnglePerSide *= -1;
      angle += 2 * dAnglePerSide;
    }
    var oldColor = drawer.getPackedColor();
    drawer.setColor(Color.WHITE_FLOAT_BITS);
    for (var i = 1; i <= sides; i++) {
      if (i > 1 && debug) {
        drawer.setColor(Color.RED);
      }
      var cos = MathUtils.cos(angle);
      var sin = MathUtils.sin(angle);
      angle += dAnglePerSide;
      var x = centreX + cos * radius;
      var y = centreY + sin * radius;

      list.add(x);
      list.add(y);
      if (debug) {
        drawer.circle(x, y, 2);
      }
    }
    drawer.setColor(oldColor);
  }

  private SideEstimator sideEstimator = new DefaultSideEstimator();

  protected int estimateSidesRequired(float radiusX, float radiusY) {
    return sideEstimator.estimateSidesRequired(1, radiusX, radiusY);
  }
}
