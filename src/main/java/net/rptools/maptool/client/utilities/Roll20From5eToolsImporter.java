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
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import net.rptools.maptool.client.AppStyle;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.functions.json.JsonArrayFunctions;
import net.rptools.maptool.client.ui.MapPropertiesDialog;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.Zone.Layer;
import net.rptools.maptool.model.Zone.TopologyMode;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;

/** Class for importing Roll20 Module json format. */
public class Roll20From5eToolsImporter {

    /** The file containing the dungeondraft VTT export. */
    private final File jsonFile;

    private String moduleDir;

    private Map<String, JsonObject> monsters = new HashMap<>();
    private Map<String, JsonObject> monstersFluff = new HashMap<>();

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

        var moduleFile = jsonFile.getName();
        var pattern = "roll20-module-([a-z]+)\\.json";
        var moduleShort = moduleFile.replaceAll(pattern, "$1");
        fetchMonsterData(moduleShort);

        moduleDir = jsonFile.getParent();

        JsonArray maps = json.getAsJsonArray("maps");
        for (JsonElement m : maps) {
            Zone zone = ZoneFactory.createZone();
            JsonObject map = m.getAsJsonObject();

            JsonObject mapAttributes = map.getAsJsonObject("attributes");

            JsonArray graphics = map.getAsJsonArray("graphics");

            JsonArray paths = map.getAsJsonArray("paths");

            JsonArray texts = map.getAsJsonArray("text");

            String mapName = mapAttributes.get("name").getAsString()
                    .replaceAll("’","").replaceAll(":", "")
                    .replaceAll("'","").trim();

            JsonObject background = findMapGraphics(graphics);
            String backgroundId = null;
            Asset asset = null;
            int pixelsPerCell = 70;
            if(background != null) {

                backgroundId = background.get("id").getAsString();

                double backgroundWidth = background.get("width").getAsDouble();
                double gridWidth = mapAttributes.get("width").getAsDouble();
                pixelsPerCell = (int) (backgroundWidth / gridWidth);

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


                asset = new Asset(mapName, backGroundImage);
                AssetManager.putAsset(asset);
            }
            MapPropertiesDialog dialog =
                    MapPropertiesDialog.createMapPropertiesImportDialog(MapTool.getFrame());
            dialog.setZone(zone);
            dialog.forcePixelsPerCell(pixelsPerCell);

            //dialog.forceGridType(GridFactory.SQUARE);
            if(asset != null)
                dialog.forceMap(asset);
            else
                dialog.getNameTextField().setText(mapName);

            dialog.setVisible(true);
            if (dialog.getStatus() == MapPropertiesDialog.Status.OK) {
                MapTool.addZone(zone);
            }
            else
                return;

            placeTokens(zone, backgroundId, moduleDir, mapName, graphics);

            placeTexts(zone, texts);

            // Handle Walls

            for (JsonElement p : paths)
                RenderPath(p.getAsJsonObject(), zone);
        }

        MapTool.showInformation("Done.");
    }

    private void fetchMonsterData(String module)
    {
        try {
            var url = "http://immerlund/data/bestiary/bestiary-" + module + ".json";
            var urlFluff = "http://immerlund/data/bestiary/fluff-bestiary-" + module + ".json";
            var dataJson = readJsonFromUrl(url);
            var fluffJson = readJsonFromUrl(urlFluff);
            parseMonsterFluff(fluffJson);
            parseMonsterData(dataJson);
        }
        catch (Exception e)
        {
            MapTool.showInformation(e.toString());
        }
    }

    private void parseMonsterData(JsonObject dataJson) {
        var monsterDatas = dataJson.getAsJsonArray("monster");
        for(var m: monsterDatas) {
            var monster = m.getAsJsonObject();
            var name = monster.get("name").getAsString();
            monsters.put(name, monster);
        }

        var meta = dataJson.getAsJsonObject("_meta");
        if (meta == null)
            return;

        var otherSources = meta.getAsJsonObject("otherSources");
        if(otherSources == null)
            return;

        var otherMonsterSources = otherSources.getAsJsonObject("monster");
        if(otherMonsterSources == null)
            return;

        for(String key: otherMonsterSources.keySet())
            fetchMonsterData(key.toLowerCase());
    }

    private void parseMonsterFluff(JsonObject dataJson) {
        var fluffs = dataJson.getAsJsonArray("monsterFluff");
        for(var f: fluffs) {
            var fluff = f.getAsJsonObject();
            var name = fluff.get("name").getAsString();
            monstersFluff.put(name, fluff);
        }
    }

    public static JsonObject readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            return json;
        } finally {
            is.close();
        }
    }

    private void placeTexts(Zone zone, JsonArray texts) {
        for (JsonElement ele : texts) {
            JsonObject text = ele.getAsJsonObject();

            double width = text.get("width").getAsDouble();
            double height = text.get("height").getAsDouble();
            double left = text.get("left").getAsDouble();
            double top = text.get("top").getAsDouble();
            String textValue = text.get("text").getAsString();


            Label label = new Label();
            label.setX((int) (left - width / 2));
            label.setY((int) (top - height / 2));
            label.setLabel(textValue);

            zone.putLabel(label);
        }
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
            String sidesValue = graphic.get("sides").getAsString();

            if(layer.equals("map") && id.equals(backgroundId))
                continue;

            double width = graphic.get("width").getAsDouble();
            double height = graphic.get("height").getAsDouble();
            double left = graphic.get("left").getAsDouble();
            double top = graphic.get("top").getAsDouble();
            double rotation = graphic.get("rotation").getAsDouble();
            String translatedName = name;
            Asset fullImg = null;

            if(name.isEmpty()) {
                name = id;
                translatedName = id;
            } else {
                translatedName = translateName(name);
                fullImg = getFullImg(name);
            }


            Path imagePath = Paths.get(moduleDir, mapDir, img);

            Asset asset = getAsset(name, imagePath);
            if (asset == null)
                continue;

            Token token = new Token(translatedName, asset.getId());
            if(fullImg != null) {
                token.setPortraitImage(fullImg.getId());
                token.setCharsheetImage(fullImg.getId());
            }


            if(!name.equals(translatedName))
                token.setGMName(name);

            if(name.equals(id)) {
                token.setSnapToGrid(false);
                token.setSnapToScale(false);
            } else {
                token.setSnapToGrid(true);
                token.setSnapToScale(true);
            }


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

            if(rotation != 0) {
                token.setShape(Token.TokenShape.TOP_DOWN);
                token.setFacing(0);
                token.setFacing(((int)Math.round(rotation)+90)*-1);
            }
            zone.putToken(token);

            if(!sidesValue.isEmpty()) {
                var sideList = sidesValue.split("\\|");
                var macro = new MacroButtonProperties(token, 0, "");
                macro.setCommand(
                    "<!--\n" +
                    "[formList=\"\"]\n" +
                    "[tableName=\"" + name + "\"]\n" +
                    "[COUNT(" + sideList.length + "): " +
                        "formList = formList+table(tableName,roll.count)+\" \"+tableImage(tableName,roll.count)+\",\"]\n" +
                    "[input(\"form|\"+formList+\"|Select Form|LIST|ICON=TRUE\")]\n" +
                    "[value = table(tableName, form)]\n" +
                    "[start = lastIndexOf(value, \"[\")]\n" +
                    "[end = lastIndexOf(value, \"]\")]]\n" +
                    "[if(start > 0 && end > 0 && end > start), code:\n" +
                    "{\n" +
	                "    [size = substring(value, start +1, end)]\n" +
	                "    [setSize(size)]\n" +
                    "};\n" +
                    "{\n" +
	                "    [resetSize()]\n" +
                    "}\n" +
                    "]\n" +
                    "[setTokenImage(tableImage(tableName, form))]\n" +
                    "[abort(0)]"
                );
                macro.setLabel("Shapeshift");

                var table = MapTool.getCampaign().getLookupTableMap().get(name);
                if(table != null)
                    continue;

                table = new LookupTable();
                table.setName(name);


                for(int i = 0; i< sideList.length; i++)
                {
                    var decodedUrl = URLDecoder.decode(sideList[i]);
                    var uri = new URI(decodedUrl);
                    asset = getAsset(name+ "_" + i, uri.toURL());
                    table.addEntry(i,i, String.valueOf(i), asset.getId());
                }
                MapTool.getCampaign().getLookupTableMap().put(name, table);
            }
        }
    }

    @Nullable
    private Asset getFullImg(String name) throws MalformedURLException {
        Asset fullImg = null;
        var fluffJson = monstersFluff.get(name);
        if(fluffJson == null)
            return null;

        var images = fluffJson.getAsJsonArray("images");
        if(images == null)
            return null;

        if(images != null && images.size() == 1)
        {
            var href = images.get(0).getAsJsonObject().get("href").getAsJsonObject();
            if(href == null)
                return null;

            var path = href.get("path").getAsString();
            if(path == null | path.isEmpty())
                return null;

            return getAsset(name + "_img", new URL( "http://immerlund/img/" +
                                path.replaceAll(" ", "%20")));

        } else {
            System.out.println();
        }

        return null;
    }

    private String translateName(String name) {
        String translatedName = name;
        String transURl = "https://www.dnddeutsch.de/tools/json.php?s="+ URLEncoder.encode(name) +
                "&o=dict&mi=off&mo=on&sp=off&it=off&misc=off";
        try {
            var resp = readJsonFromUrl(transURl);
            var resultList = resp.getAsJsonArray("result");
            if(resultList == null)
                return name;

            for(JsonElement r : resultList) {
                var result = r.getAsJsonObject();

                var name_en = result.get("name_en").getAsString();

                if(name_en.equals(name))
                {
                    translatedName = result.get("name_de").getAsString();
                    break;
                }
            }
        } catch (Exception e)
        { /* TODO handle HTTP response codes */}
        return translatedName;
    }

    @Nullable
    private Asset getAsset(String name, URL imagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(imagePath);
        }
        catch (Exception e)
        {
            return null;
        }

        Asset asset = new Asset(name, image);
        if(!AssetManager.hasAsset(asset))
            AssetManager.putAsset(asset);
        return asset;
    }

    @Nullable
    private Asset getAsset(String name, Path imagePath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(imagePath.toFile());
        }
        catch (Exception e)
        {
            return null;
        }

        Asset asset = new Asset(name, image);
        if(!AssetManager.hasAsset(asset))
            AssetManager.putAsset(asset);
        return asset;
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