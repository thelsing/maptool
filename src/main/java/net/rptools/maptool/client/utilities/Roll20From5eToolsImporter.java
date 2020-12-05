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
package net.rptools.maptool.client.utilities;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jidesoft.utils.Base64;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

import net.rptools.maptool.client.AppStyle;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.functions.json.JsonArrayFunctions;
import net.rptools.maptool.client.ui.MapPropertiesDialog;
import net.rptools.maptool.model.Asset;
import net.rptools.maptool.model.AssetManager;
import net.rptools.maptool.model.GridFactory;
import net.rptools.maptool.model.Token;
import net.rptools.maptool.model.Zone;
import net.rptools.maptool.model.Zone.Layer;
import net.rptools.maptool.model.Zone.TopologyMode;
import net.rptools.maptool.model.ZoneFactory;

import javax.imageio.ImageIO;

/** Class for importing Roll20 Module json format. */
public class Roll20From5eToolsImporter {

    /** The file containing the dungeondraft VTT export. */
    private final File jsonFile;

    private String moduleDir;

    /**
     * Creates a new {@code DungeonDraftImporter} object.
     *
     * @param file the file to import.
     */
    public Roll20From5eToolsImporter(File file) {
        jsonFile = file;
    }

    /**
     * Import the json file and craate a new {@link Zone} which is added to the campaign.
     *
     * @throws IOException if an error occurs during the import.
     */
    public void importJson() throws Exception {
        JsonObject json;
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFile))) {
            json = JsonParser.parseReader(reader).getAsJsonObject();
        }
        moduleDir = jsonFile.getParent();

        JsonArray maps = json.getAsJsonArray("maps");
        for (JsonElement m : maps) {
            Zone zone = ZoneFactory.createZone();
            JsonObject map = m.getAsJsonObject();

            JsonObject mapAttributes = map.getAsJsonObject("attributes");

            JsonArray graphics = map.getAsJsonArray("graphics");

            JsonArray paths = map.getAsJsonArray("paths");

            String mapName = mapAttributes.get("name").getAsString()
                    .replaceAll("’","").replaceAll(":", "")
                    .replaceAll("'","").trim();

            JsonObject background = findMapGraphics(graphics);

            if(background == null) {
                MapTool.showInformation("No map graphic for " + mapName);
                continue;
            }
            String backgroundId = background.get("id").getAsString();

            double backgroundWidth = background.get("width").getAsDouble();
            double gridWidth = mapAttributes.get("width").getAsDouble();
            int pixelsPerCell = (int) (backgroundWidth / gridWidth);

            String backGroundImageFile = getImageFromGraphics(background);

            Path imagePath = Paths.get(moduleDir, mapName, backGroundImageFile);
            BufferedImage backGroundImage = null;
            try {
                backGroundImage = ImageIO.read(imagePath.toFile());
            }
            catch (Exception e)
            {
                MapTool.showError(mapName + " " +e.toString());
                continue;
            }


            Asset asset = new Asset(mapName, backGroundImage);
            AssetManager.putAsset(asset);
            MapPropertiesDialog dialog =
                    MapPropertiesDialog.createMapPropertiesImportDialog(MapTool.getFrame());
            dialog.setZone(zone);
            dialog.forcePixelsPerCell(pixelsPerCell);
            dialog.forceGridType(GridFactory.SQUARE);
            dialog.forceMap(asset);
            dialog.setVisible(true);
            if (dialog.getStatus() == MapPropertiesDialog.Status.OK) {
                MapTool.addZone(zone);
            }
            else
                return;

            placeTokens(zone, backgroundId, moduleDir, mapName, graphics);

            // Handle Walls

            for (JsonElement p : paths)
                RenderPath(p.getAsJsonObject(), zone);
        }

        MapTool.showInformation("Done.");
    }

    private void RenderPath(JsonObject path, Zone zone) {
        String color = path.get("stroke").getAsString();
        float strokeWidth = path.get("stroke_width").getAsFloat();
        BasicStroke stroke = new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
        Area vblArea =
                new Area(
                        stroke.createStrokedShape(
                                getVBLPath(path)));
        zone.addTopology(vblArea, TopologyMode.VBL);
        zone.addTopology(vblArea, TopologyMode.MBL);
    }

    private JsonObject findMapGraphics(JsonArray graphics) throws IOException {
        JsonObject map = null;
        double mapWidth = 0;

        for(JsonElement e : graphics)
        {
            JsonObject g  = e.getAsJsonObject();
            String layer = g.get("layer").getAsString();
            double width = g.get("width").getAsDouble();
            if(layer.equals("map") && width > mapWidth) {
                map = g;
                mapWidth = width;
            }
        }
        return map;
    }

    private String getImageFromGraphics(JsonObject graphic) throws URISyntaxException, MalformedURLException {
        String file = graphic.get("name").getAsString();
        if(file.isEmpty())
            file = graphic.get("id").getAsString();

        file = file.replace("\"", "");

        URI uri = new URI(graphic.get("imgsrc").getAsString());
        String ext = getFileExtension(uri.toURL()).get();
        return file + "." + ext;
    }

    public static Optional<String> getFileExtension(final URL url) {
        Objects.requireNonNull(url, "url is null");

        final String file = url.getFile();

        if (file.contains(".")) {

            final String sub = file.substring(file.lastIndexOf('.') + 1);

            if (sub.length() == 0) {
                return Optional.empty();
            }

            if (sub.contains("?")) {
                return Optional.of(sub.substring(0, sub.indexOf('?')));
            }

            return Optional.of(sub);
        }

        return Optional.empty();
    }

    private void placeTokens(Zone zone, String backgroundId, String moduleDir, String mapDir, JsonArray graphics) throws Exception {
        for (JsonElement ele : graphics) {
            JsonObject graphic = ele.getAsJsonObject();

            String layer = graphic.get("layer").getAsString();


            String img = getImageFromGraphics(graphic);
            String name = graphic.get("name").getAsString();
            String id = graphic.get("id").getAsString();

            if(layer.equals("map") && id.equals(backgroundId))
                continue;

            double width = graphic.get("width").getAsDouble();
            double height = graphic.get("height").getAsDouble();
            double left = graphic.get("left").getAsDouble();
            double top = graphic.get("top").getAsDouble();

            if(name.isEmpty())
                name = id;

            Path imagePath = Paths.get(moduleDir, mapDir, img);
            System.out.println(imagePath.toString());
            BufferedImage image = null;
            try {
                image = ImageIO.read(imagePath.toFile());
            }
            catch (Exception e)
            {
                continue;
            }

            Asset asset = new Asset(name, image);
            if(!AssetManager.hasAsset(asset))
                AssetManager.putAsset(asset);

            Token token = new Token(name, asset.getId());
            token.setSnapToGrid(false);
            token.setSnapToScale(false);

            if(layer.equals("gmlayer"))
                token.setLayer(Layer.GM);
            else if(layer.equals("objects") && graphic.get("represents").getAsString().isEmpty())
                token.setLayer(Layer.OBJECT);
            else if(layer.equals("map"))
                token.setLayer(Layer.BACKGROUND);
            else {
                token.setLayer(Layer.TOKEN);
                token.setSnapToGrid(true);
                token.setSnapToScale(true);
            }

            token.setVisible(true);
            token.setSnapToGrid(false);
            token.setSnapToScale(false);
            token.setWidth((int)width);
            token.setHeight((int)height);
            token.setX((int) (left - width / 2));
            token.setY((int) (top - height / 2));
            zone.putToken(token);
        }
    }

    private Path2D getVBLPath(JsonObject pathObject) {
        String jsonPath = pathObject.get("path").getAsString();
        JsonArray points = (JsonArray)JsonParser.parseString(jsonPath);

        boolean first = true;
        Path2D path = new GeneralPath();
        double top = pathObject.get("top").getAsDouble();
        double left = pathObject.get("left").getAsDouble();
        double width = pathObject.get("width").getAsDouble();
        double height = pathObject.get("height").getAsDouble();

        double diffY = top - height/2;
        double diffX = left - width/2;

        for (JsonElement element : points) {
            JsonArray point = element.getAsJsonArray();

            double x = point.get(1).getAsDouble() + diffX;
            double y = point.get(2).getAsDouble() + diffY;
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        return path;
    }
}