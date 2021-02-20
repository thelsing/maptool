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
import java.util.*;

import jdk.nashorn.api.tree.Tree;
import net.rptools.maptool.client.AppStyle;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.functions.json.JsonArrayFunctions;
import net.rptools.maptool.client.ui.MapPropertiesDialog;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.Zone.Layer;
import net.rptools.maptool.model.Zone.TopologyMode;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;

/** Class for importing Roll20 Module json format. */
public class Roll20From5eToolsImporter {

    /** The file containing the dungeondraft VTT export. */
    private final File jsonFile;

    private String etools = "https://5e.tools/";
    private String moduleDir;

    private Map<String, JsonObject> monsters = new HashMap<>();
    private Map<String, JsonObject> monstersFluff = new HashMap<>();

    /**
     * Creates a new {@code Roll20From5eToolsImporter} object.
     */
    public Roll20From5eToolsImporter() {
        jsonFile = new File("C:\\Dragon of Icespire Peak\\roll20-module-dip.json");
    }

    /**
     * Import the json file and craate a new {@link Zone} which is added to the campaign.
     *
     * @throws IOException if an error occurs during the import.
     */
    public void importJson() throws Exception {
        try {
            JsonObject json;
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFile))) {
                json = JsonParser.parseReader(reader).getAsJsonObject();
            }

            var moduleFile = jsonFile.getName();
            var pattern = "roll20-module-([a-z]+)\\.json";
            var moduleShort = moduleFile.replaceAll(pattern, "$1");
            fetchMonsterData(moduleShort);

            var mon = JsonParser.parseString("{\n" +
                    "\t\"name\": \"Flail Snail\",\n" +
                    "\t\"source\": \"VGM\",\n" +
                    "\t\"page\": 144,\n" +
                    "\t\"otherSources\": [\n" +
                    "\t\t{\n" +
                    "\t\t\t\"source\": \"ToA\"\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"source\": \"DIP\"\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"source\": \"SLW\"\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"size\": \"L\",\n" +
                    "\t\"type\": \"elemental\",\n" +
                    "\t\"alignment\": [\n" +
                    "\t\t\"U\"\n" +
                    "\t],\n" +
                    "\t\"ac\": [\n" +
                    "\t\t{\n" +
                    "\t\t\t\"ac\": 16,\n" +
                    "\t\t\t\"from\": [\n" +
                    "\t\t\t\t\"natural armor\"\n" +
                    "\t\t\t]\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"hp\": {\n" +
                    "\t\t\"average\": 52,\n" +
                    "\t\t\"formula\": \"5d10 + 25\"\n" +
                    "\t},\n" +
                    "\t\"speed\": {\n" +
                    "\t\t\"walk\": 10\n" +
                    "\t},\n" +
                    "\t\"str\": 17,\n" +
                    "\t\"dex\": 5,\n" +
                    "\t\"con\": 20,\n" +
                    "\t\"int\": 3,\n" +
                    "\t\"wis\": 10,\n" +
                    "\t\"cha\": 5,\n" +
                    "\t\"senses\": [\n" +
                    "\t\t\"darkvision 60 ft.\",\n" +
                    "\t\t\"tremorsense 60 ft.\"\n" +
                    "\t],\n" +
                    "\t\"passive\": 10,\n" +
                    "\t\"immune\": [\n" +
                    "\t\t\"fire\",\n" +
                    "\t\t\"poison\"\n" +
                    "\t],\n" +
                    "\t\"conditionImmune\": [\n" +
                    "\t\t\"poisoned\"\n" +
                    "\t],\n" +
                    "\t\"cr\": \"3\",\n" +
                    "\t\"trait\": [\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Antimagic Shell\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"The snail has advantage on saving throws against spells, and any creature making a spell attack against the snail has disadvantage on the attack roll. If the snail succeeds on its saving throw against a spell or a spell attack misses it, an additional effect might occur, as determined by rolling a {@dice d6}:\",\n" +
                    "\t\t\t\t{\n" +
                    "\t\t\t\t\t\"type\": \"list\",\n" +
                    "\t\t\t\t\t\"style\": \"list-hang-notitle\",\n" +
                    "\t\t\t\t\t\"items\": [\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"type\": \"item\",\n" +
                    "\t\t\t\t\t\t\t\"name\": \"1–2.\",\n" +
                    "\t\t\t\t\t\t\t\"style\": \"italic\",\n" +
                    "\t\t\t\t\t\t\t\"entry\": \"If the spell affects an area or has multiple targets, it fails and has no effect. If the spell targets only the snail, it has no effect on the snail and is reflected back at the caster, using the spell slot level, spell save DC, attack bonus, and spellcasting ability of the caster.\"\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"type\": \"item\",\n" +
                    "\t\t\t\t\t\t\t\"name\": \"3–4.\",\n" +
                    "\t\t\t\t\t\t\t\"style\": \"italic\",\n" +
                    "\t\t\t\t\t\t\t\"entry\": \"No additional effect.\"\n" +
                    "\t\t\t\t\t\t},\n" +
                    "\t\t\t\t\t\t{\n" +
                    "\t\t\t\t\t\t\t\"type\": \"item\",\n" +
                    "\t\t\t\t\t\t\t\"name\": \"5–6.\",\n" +
                    "\t\t\t\t\t\t\t\"style\": \"italic\",\n" +
                    "\t\t\t\t\t\t\t\"entry\": \"The snail's shell converts some of the spell's energy into a burst of destructive force. Each creature within 30 feet of the snail must make a {@dc 15} Constitution saving throw, taking {@dice 1d6} force damage per level of the spell on a failed save, or half as much damage on a successful one.\"\n" +
                    "\t\t\t\t\t\t}\n" +
                    "\t\t\t\t\t]\n" +
                    "\t\t\t\t}\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Flail Tentacles\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"The flail snail has five flail tentacles. Whenever the snail takes 10 damage or more on a single turn, one of its tentacles dies. If even one tentacle remains, the snail regrows all dead ones within {@dice 1d4} days. If all its tentacles die, the snail retracts into its shell, gaining total cover, and it begins wailing, a sound that can be heard for 600 feet, stopping only when it dies {@dice 5d6} minutes later. Healing magic that restores limbs, such as the regenerate spell, can halt this dying process.\"\n" +
                    "\t\t\t]\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"action\": [\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Multiattack\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"The flail snail makes as many flail tentacle attacks as it has flail tentacles, all against the same target.\"\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Flail Tentacle\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"{@atk mw} {@hit 5} to hit, reach 10 ft., one target. {@h}6 ({@damage 1d6 + 3}) bludgeoning damage.\"\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Scintillating Shell (Recharges after a Short or Long Rest)\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"The snail's shell emits dazzling, colored light until the end of the snail's next turn. During this time, the shell sheds bright light in a 30-foot radius and dim light for an additional 30 feet, and creatures that can see the snail have disadvantage on attack rolls against it. In addition, any creature within the bright light and able to see the snail when this power is activated must succeed on a {@dc 15} Wisdom saving throw or be {@condition stunned} until the light ends.\"\n" +
                    "\t\t\t]\n" +
                    "\t\t},\n" +
                    "\t\t{\n" +
                    "\t\t\t\"name\": \"Shell Defense\",\n" +
                    "\t\t\t\"entries\": [\n" +
                    "\t\t\t\t\"The flail snail withdraws into its shell, gaining a +4 bonus to AC until it emerges. It can emerge from its shell as a bonus action on its turn.\"\n" +
                    "\t\t\t]\n" +
                    "\t\t}\n" +
                    "\t],\n" +
                    "\t\"environment\": [\n" +
                    "\t\t\"forest\",\n" +
                    "\t\t\"swamp\",\n" +
                    "\t\t\"underdark\"\n" +
                    "\t],\n" +
                    "\t\"hasToken\": true,\n" +
                    "\t\"soundClip\": {\n" +
                    "\t\t\"type\": \"internal\",\n" +
                    "\t\t\"path\": \"bestiary/flail-snail.mp3\"\n" +
                    "\t},\n" +
                    "\t\"senseTags\": [\n" +
                    "\t\t\"D\",\n" +
                    "\t\t\"T\"\n" +
                    "\t],\n" +
                    "\t\"actionTags\": [\n" +
                    "\t\t\"Multiattack\"\n" +
                    "\t],\n" +
                    "\t\"damageTags\": [\n" +
                    "\t\t\"B\"\n" +
                    "\t],\n" +
                    "\t\"miscTags\": [\n" +
                    "\t\t\"AOE\",\n" +
                    "\t\t\"MW\",\n" +
                    "\t\t\"RCH\"\n" +
                    "\t],\n" +
                    "\t\"conditionInflict\": [\n" +
                    "\t\t\"stunned\"\n" +
                    "\t]\n" +
                    "}");
            var test = getStatBlock(mon.getAsJsonObject());

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
                        .replaceAll("’", "").replaceAll(":", "")
                        .replaceAll("'", "").trim();

                JsonObject background = findMapGraphics(graphics);
                String backgroundId = null;
                Asset asset = null;
                int pixelsPerCell = 70;
                if (background != null) {

                    backgroundId = background.get("id").getAsString();

                    double backgroundWidth = background.get("width").getAsDouble();
                    double gridWidth = mapAttributes.get("width").getAsDouble();
                    pixelsPerCell = (int) (backgroundWidth / gridWidth);

                    String backGroundImageFile = getImageFromGraphics(background);

                    Path imagePath = Paths.get(moduleDir, mapName, backGroundImageFile);
                    BufferedImage backGroundImage = null;
                    try {
                        backGroundImage = ImageIO.read(imagePath.toFile());
                    } catch (Exception e) {
                        MapTool.showError(mapName + " " + e.toString());
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
                if (asset != null)
                    dialog.forceMap(asset);
                else
                    dialog.getNameTextField().setText(mapName);

                dialog.setVisible(true);
                if (dialog.getStatus() == MapPropertiesDialog.Status.OK) {
                    MapTool.addZone(zone);
                } else
                    return;

                placeTokens(zone, backgroundId, moduleDir, mapName, graphics);

                placeTexts(zone, texts);

                // Handle Walls

                for (JsonElement p : paths)
                    RenderPath(p.getAsJsonObject(), zone);
            }

            MapTool.showInformation("Done.");
        } catch (Exception e) {
            System.out.println(e.toString());
        }


    }

    private void fetchMonsterData(String module)
    {
        try {
            var url = etools + "/data/bestiary/bestiary-" + module + ".json";
            var urlFluff = etools + "/data/bestiary/fluff-bestiary-" + module + ".json";
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
                token.setGMNotes(getStatBlock(name));
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

            return getAsset(name + "_img", new URL( etools + "/img/" +
                                path.replaceAll(" ", "%20")));

        } else {
            System.out.println();
        }

        return null;
    }

    private Map<String, String> sizeMap =  Map.of(
            "T","Tiny",
            "S","Small",
            "M","Medium",
            "L","Large",
            "H","Huge",
            "G","Gargantuan"
    );

    private Map<String, String> alignmentMap =  Map.of(
            "N","neutral",
            "E","evil",
            "G","good",
            "L","lawful",
            "C","chaotic",
            "U","unaligned",
            "A","any"
    );

    private String mapAlignment(JsonArray alignment)
    {
        var alignList = new ArrayList<String>();
        for(JsonElement j: alignment)
            alignList.add(j.getAsString());

        if (alignList.size() == 1) return alignmentMap.get(alignList.get(0));
        if (alignList.size() == 2) {
            return alignmentMap.get(alignList.get(0)) + " " + alignmentMap.get(alignList.get(1));
        }
        if (alignList.size() == 3) {
            if (alignList.contains("NX") && alignList.contains("NY") && alignList.contains("N"))
                return "any neutral alignment";
        }
        if (alignList.size() == 5) {
            if (!alignList.contains("G"))
                return "any non-good alignment";

            if (!alignList.contains("E"))
                return "any non-evil alignment";

            if (!alignList.contains("L"))
                return "any non-lawful alignment";

            if (!alignList.contains("C"))
                return "any non-chaotic alignment";
        }
        if (alignList.size() == 4) {
            if (!alignList.contains("L") && !alignList.contains("NX"))
                return "any chaotic alignment";

            if (!alignList.contains("G") && !alignList.contains("NY"))
                return "any evil alignment";

            if (!alignList.contains("C") && !alignList.contains("NX"))
                return "any lawful alignment";

            if (!alignList.contains("E") && !alignList.contains("NY"))
                return "any good alignment";
        }
        return "NO MAPPING FOUND";
    }

    private String mapType(JsonElement type)
    {
        var builder = new StringBuilder();
        if(type.isJsonPrimitive())
            builder.append(type.getAsString()).append(", ");
        else {
            var jsonObject = type.getAsJsonObject();
            builder.append(jsonObject.get("type").getAsString()).append(" (");
            var tags = jsonObject.get("tags").getAsJsonArray();
            boolean isFirst = true;
            for(var tag: tags) {
                if(!isFirst) {
                    isFirst = false;
                    builder.append(", ");
                }

                builder.append(tag.getAsString());
            }
            builder.append("), ");
        }
        return builder.toString();
    }

    private String mapXp(String Cr)
    {
        switch(Cr)
        {
            case "0":
                return "10";
            case "1/8":
                return "25";
            case "1/4":
                return "50";
            case "1/2":
                return "100";
            case "1":
                return "200";
            case "2":
                return "450";
            case "3":
                return "700";
            case "4":
                return "1,100";
            case "5":
                return "1,800";
            case "6":
                return "2,300";
            case "7":
                return "2,900";
            case "8":
                return "3,900";
            case "9":
                return "5,000";
            case "10":
                return "5,900";
            case "11":
                return "7,200";
            case "12":
                return "8,400";
            case "13":
                return "10,000";
            case "14":
                return "11,500";
            case "15":
                return "13,000";
            case "16":
                return "15,00";
            case "17":
                return "18,000";
            case "18":
                return "20,000";
            case "19":
                return "22,000";
            case "20":
                return "25,000";
            case "21":
                return "33,000";
            case "22":
                return "41,00";
            case "23":
                return "50,000";
            case "24":
                return "62,000";
            case "25":
                return "75,000";
            case "26":
                return "90,000";
            case "27":
                return "105,000";
            case "28":
                return "120,000";
            case "29":
                return "135,000";
            case "30":
                return "155,000";
        }
        return "undefined";
    }

    private String replaceTag(String desc, String tag) {
        return replaceTag(desc, tag, null, "", "", "phb");
    }

    private String replaceTag(String desc, String tag, String urlPart) {
        return replaceTag(desc, tag, urlPart, "", "", "phb");
    }

    private String replaceTag(String desc, String tag, String prefix, String postfix) {
        return replaceTag(desc, tag, null, prefix, postfix, "phb");
    }

    private String replaceTag(String desc, String tag, String urlPart, String prefix, String postfix,
                              String defOrigin)
    {
        var end = 0;
        var start = desc.indexOf(tag, end);
        while(start >= 0) {

            end = desc.indexOf("}", start);

            var wholeTag = desc.substring(start, end +1);
            var tagContent = desc.substring(start + tag.length(), end);
            var parts = tagContent.split("\\|");
            var name = parts[0];
            var src = defOrigin;
            if (parts.length > 1)
                src = parts[1];

            if(urlPart != null) {
                var link = "<a href=\"" + etools + urlPart;
                link += name.replace(" ", "%20") + "_" + src + "\">";
                link += prefix + name + postfix + "</a>";
                desc = desc.replace(wholeTag, link);
            } else
                desc = desc.replace(wholeTag, prefix + name + postfix);

            start = desc.indexOf(tag, end);
        }
        return desc;
    }

    private String replaceTags(String desc)
    {
        desc = replaceTag(desc,"{@spell ", "spells.html#");
        desc = replaceTag(desc,"{@item ", "items.html#");
        desc = replaceTag(desc,"{@creature ", "bestiary.html#", "", "", "mm");
        desc = replaceTag(desc,"{@recharge ", "(Recharge ", "-6)");
        desc = replaceTag(desc,"{@damage ");
        desc = replaceTag(desc,"{@condition ");
        desc = replaceTag(desc,"{@skill ");
        desc = replaceTag(desc,"{@dc ", "DC ", "");
        desc = replaceTag(desc,"{@hit ", "+", "");
        desc = replaceTag(desc,"{@h", "<span class=\"description\">Hit:</span> ", "");
        desc = replaceTag(desc,"{@atk mw,rw", "<span class=\"description\">Melee or Ranged Weapon Attack:", "</span>");
        desc = replaceTag(desc,"{@atk mw", "<span class=\"description\">Melee Weapon Attack:", "</span>");
        desc = replaceTag(desc,"{@atk rw", "<span class=\"description\">Ranged Weapon Attack:", "</span>");
        desc = replaceTag(desc,"{@atk ms", "<span class=\"description\">Melee Spell Attack:", "</span>");
        desc = replaceTag(desc,"{@atk rs", "<span class=\"description\">Ranged Spell Attack:", "</span>");

        return desc;
    }

    private String mapAC(JsonElement acElement)
    {
        if(acElement.isJsonPrimitive())
            return acElement.getAsString();

        StringBuilder builder = new StringBuilder();

        var acObj = acElement.getAsJsonObject();
        var ac = acObj.get("ac").getAsString();
        builder.append(ac).append(" (");
        var conditionElement = acObj.get("condition");
        boolean isFirst = true;
        if(conditionElement != null) {
            var condition = replaceTags(conditionElement.getAsString());
            builder.append(condition);
            isFirst = false;
        }

        var fromElement = acObj.get("from");
        if(fromElement != null) {
            for (var from : fromElement.getAsJsonArray()) {
                if (isFirst)
                    isFirst = false;
                else
                    builder.append(", ");
                builder.append(replaceTags(from.getAsString()));
            }
        }

        builder.append(")");
        return builder.toString();
    }

    private String mapAC(JsonArray acs)
    {
        var builder = new StringBuilder();
        boolean isFirst = true;
        for(var j: acs) {
            if(isFirst)
                isFirst = false;
            else
                builder.append(", ");

            builder.append(mapAC(j));
        }
        return builder.toString();
    }

    private String renderSpeeds(JsonObject speeds)
    {
        var builder = new StringBuilder();
        boolean isFirst = true;
        for(var speed: speeds.keySet()) {
            if(isFirst)
                isFirst = false;
            else
                builder.append(", ");

            if(!speed.equals("walk"))
                builder.append(speed).append(" ");

            var speedElement = speeds.get(speed);
            if(speedElement.isJsonPrimitive())
                builder.append(speedElement.getAsString()).append(" ft.");
            else {
                var obj = speedElement.getAsJsonObject();
                var number = obj.get("number").getAsString();
                var condition = obj.get("condition").getAsString();

                builder.append(number).append(" ft.");
                builder.append(" ").append(condition);

            }
        }

        return builder.toString();
    }

    private String renderValue(JsonObject monster, String attribute) {
        var builder = new StringBuilder();
        var value = monster.get(attribute).getAsDouble();
        var mod = (int)Math.floor((value - 10.0)/2.0);
        var sign = mod > 0 ? "+" : "-";
        builder.append("      <td class=\"td\">").append((int)value);
        builder.append(" (").append(sign).append((int)mod).append(")</td>\n");
        return builder.toString();
    }

    private String renderObj(JsonObject monster, String tag, String name)
    {
        var saveElement = monster.get(tag);
        if(saveElement == null)
            return "";

        var builder = new StringBuilder();
        builder.append("  <div class=\"commonTrait\">\n");
        builder.append("    <span class=\"bold\">").append(name).append("</span>\n");

        builder.append("    <span>");
        boolean isFirst = true;
        var saves = saveElement.getAsJsonObject();
        for(var save: saves.keySet()) {
            if(isFirst)
                isFirst = false;
            else
                builder.append(", ");

            builder.append(StringUtils.capitalize(save)).append(" ");
            builder.append(saves.get(save).getAsString());
        }
        builder.append("</span>\n");
        builder.append("  </div>\n");
        return builder.toString();
    }

    private String renderObjArray(JsonObject monster, String tag, String name) {
        return renderObjArray(monster, tag, name, null);
    }

    private String renderObjArray(JsonObject monster, String tag, String name, String extra)
    {
        var element = monster.get(tag);
        if(element == null)
            return "";

        var builder = new StringBuilder();
        builder.append("  <div class=\"commonTrait\">\n");
        builder.append("    <span class=\"bold\">").append(name).append("</span>\n");

        builder.append("    <span>");
        boolean isFirst = true;
        var jsonArray = element.getAsJsonArray();

        for(var je: jsonArray)
        {
            if(isFirst)
                isFirst = false;
            else if(je.isJsonPrimitive())
                builder.append(", ");
            else
                builder.append("; ");

            if(je.isJsonPrimitive()) {
                builder.append(je.getAsString());
                continue;
            }

            var obj = je.getAsJsonObject();

            var special = obj.get("special");;
            if(special != null){
                builder.append(special.getAsString());
                continue;
            }

            var preNote = obj.get("prenote");;
            if(preNote != null)
                builder.append(preNote.getAsString()).append(" ");

            var valuesArray = obj.get(tag).getAsJsonArray();
            for(int i=0; i<valuesArray.size(); i++) {
                if(i>0 && i == valuesArray.size()-1)
                    builder.append(" and ");
                else if(i>0)
                    builder.append(", ");

                builder.append(valuesArray.get(i).getAsString());
            }
            var note = obj.get("note");
            if(note != null)
                builder.append(" ").append(note.getAsString());
        }

        if(extra != null)
        {
            if(!isFirst)
                builder.append(", ");
            builder.append(extra);
        }

        builder.append("</span>\n");
        builder.append("  </div>\n");
        return builder.toString();
    }

    private String renderTraits(JsonObject monster)
    {
        var spellTraitSet = new TreeSet<String>();
        var traitMap = new TreeMap<String, JsonObject>();
        
        var traits = monster.get("trait");
        if(traits != null) {
            for (var traitElement : traits.getAsJsonArray()) {
                var trait = traitElement.getAsJsonObject();
                traitMap.put(trait.get("name").getAsString(), trait);
            }
        }
        
        var spellTraits = monster.get("spellcasting");
        if(spellTraits != null) {
            for (var spellTraitElement : spellTraits.getAsJsonArray()) {
                var spellTrait = spellTraitElement.getAsJsonObject();
                var name = spellTrait.get("name").getAsString();
                traitMap.put(name, spellTrait);
                spellTraitSet.add(name);
            }
        }
        return renderTraits(traitMap, spellTraitSet);
    }

    private String renderTraits(TreeMap<String, JsonObject> traitMap, TreeSet<String> spellTraitSet) {
        var builder = new StringBuilder();
        for(var traitName: traitMap.keySet()) {
            if(spellTraitSet != null && spellTraitSet.contains(traitName))
                builder.append(renderSpellTrait(traitMap.get(traitName)));
            else
                builder.append(renderNamedEntries(traitMap.get(traitName),"attack" , "attackname"));
        }
        
        
        return builder.toString();
    }

    private String renderSpellTrait(JsonObject trait) {
        var builder = new StringBuilder();
        var name = trait.get("name").getAsString();

        var hidden = new HashSet<String>();
        var hiddenElement = trait.get("hidden");
        if(hiddenElement != null) {
            for (var hiddenSection : hiddenElement.getAsJsonArray())
                hidden.add(hiddenSection.getAsString());
        }

        builder.append("  <div class=\"attack\">\n");
        builder.append("    <span class=\"attackname\">").append(name).append("</span>\n");
        builder.append("    <span>");
        var headerEntries = trait.get("headerEntries").getAsJsonArray();
        for(var entryElement: headerEntries) {
            builder.append(replaceTags(entryElement.getAsString()));
        }
        builder.append("    </span>\n");

        var spells = trait.get("spells");
        if(spells != null) {
            var spellsObj = spells.getAsJsonObject();
            for(var level: spellsObj.keySet()) {
                builder.append(renderSpellLevel(level, spellsObj.get(level).getAsJsonObject()));
            }
        }


        builder.append(renderSpellSection("will", trait.get("will"), hidden));
        var dailySpells = trait.get("daily");
        if(dailySpells != null) {
            var dailySpellsObj = dailySpells.getAsJsonObject();
            for(var amount: dailySpellsObj.keySet())
                builder.append(renderSpellSection("daily-"+amount, dailySpellsObj.get(amount), hidden));
        }

        builder.append("  </div>\n");
        var footerEntriesElement = trait.get("footerEntries");
        if(footerEntriesElement != null) {
            builder.append("  <div class=\"attack\"><span>\n");
            for(var footer: footerEntriesElement.getAsJsonArray())
                builder.append(replaceTags(footer.getAsString()));
            builder.append("  </span></div>\n");
        }
        return builder.toString();
    }

    private String renderSpellLevel(String level, JsonObject jsonObject) {
        var slotsElement = jsonObject.get("slots");
        var slots = "";
        if(slotsElement != null)
            slots = slotsElement.getAsString();

        switch(level) {
            case "0":
                return renderSpellSection("Cantrips (at will)", jsonObject.get("spells"));
            case "1":
                return renderSpellSection("1st level (" + slots + " slots)", jsonObject.get("spells"));
            case "2":
                return renderSpellSection("2nd level (" + slots + " slots)", jsonObject.get("spells"));
            case "3":
                return renderSpellSection("3rd level (" + slots + " slots)", jsonObject.get("spells"));
            default:
                return renderSpellSection(level +"th level (" + slots + " slots)", jsonObject.get("spells"));
        }
    }

    private String mapSpellSectionName(String section)
    {
        switch(section)
        {
            case "will":
                return "At will";
            case "daily-1":
                return "1/day";
            case "daily-1e":
                return "1/day each";
            case "daily-2":
                return "2/day";
            case "daily-2e":
                return "2/day each";
            case "daily-3":
                return "3/day";
            case "daily-3e":
                return "3/day each";
            case "daily-4":
                return "4/day";
            case "daily-4e":
                return "4/day each";
            case "daily-5":
                return "5/day";
            case "daily-5e":
                return "5/day each";
            default:
                return section;
        }
    }

    private String renderSpellSection(String section, JsonElement spells) {
        return renderSpellSection(section, spells, null);
    }

    private String renderSpellSection(String section, JsonElement spells, Set<String> hidden) {
        if(spells == null)
            return "";

        if(hidden != null && hidden.contains(section))
            return "";

        var builder = new StringBuilder();
        builder.append("<div class=\"spellSection\"><span >\n");
        builder.append(mapSpellSectionName(section)).append(": ");
        boolean isFirst = true;
        for(var spellElement: spells.getAsJsonArray()) {
            if (isFirst)
                isFirst = false;
            else
                builder.append(", ");
            builder.append(replaceTags(spellElement.getAsString()));
        }
        builder.append("</span></div>\n");
        return builder.toString();
    }

    private String getStatBlock(String name) throws MalformedURLException {
        var monsterData = monsters.get(name);
        if (monsterData == null)
            return "";

        return getStatBlock(monsterData);
    }

    private String getStatBlock(JsonObject monsterData) {
        var builder = new StringBuilder();
        var name = monsterData.get("name").getAsString();
        var source = monsterData.get("source").getAsString().toLowerCase();
        var escapedName = name.replace(" ", "%20");

        builder.append("<div contenteditable=\"true\" style=\"width:400px;font-family:Arial,Helvetica,sans-serif;font-size:11px;\">\n");
        builder.append("  <div class=\"name\"><a href=\"" +etools + "bestiary.html#" +
                escapedName + "_" + source + "\">");
        builder.append(name);
        builder.append("</a></div>\n");
        builder.append("  <div class=\"description\">");
        builder.append(sizeMap.get(monsterData.get("size").getAsString()));
        builder.append(" ");
        builder.append(mapType(monsterData.get("type")));
        builder.append(mapAlignment(monsterData.get("alignment").getAsJsonArray()));
        builder.append("</div>\n");
        builder.append("  <div class=\"gradient\"></div>\n");
        builder.append("  <div class=\"red\">\n");
        builder.append("    <div>\n");
        builder.append("      <span class=\"bold red\">Armor Class</span>\n");
        builder.append("      <span> ").append(mapAC(monsterData.get("ac").getAsJsonArray())).append("</span>\n");
        builder.append("    </div>\n");
        builder.append("    <div>\n");
        builder.append("      <span class=\"bold red\">Hit Points</span>\n");
        var hpObj = monsterData.get("hp").getAsJsonObject();
        builder.append("      <span> ").append(hpObj.get("average").getAsString()).append("  (");
        builder.append(hpObj.get("formula").getAsString()).append(")</span>\n");
        builder.append("    </div>\n");
        builder.append("    <div>\n");
        builder.append("      <span class=\"bold red\">Speed</span>\n");
        builder.append("      <span> ");
        var speeds =  monsterData.get("speed").getAsJsonObject();
        builder.append(renderSpeeds(speeds)).append("</span>\n");
        builder.append("    </div>\n");
        builder.append("  </div>\n");
        builder.append("  <div class=\"gradient\"></div>\n");
        builder.append("  <table class=\"table\">\n");
        builder.append("    <tr class=\"tr\">\n");
        builder.append("      <th class=\"th\">STR</th>\n");
        builder.append("      <th class=\"th\">DEX</th>");
        builder.append("      <th class=\"th\">CON</th>\n");
        builder.append("      <th class=\"th\">INT</th>\n");
        builder.append("      <th class=\"th\">WIS</th>\n");
        builder.append("      <th class=\"th\">CHA</th>\n");
        builder.append("    </tr>\n");
        builder.append("    <tr class=\"tr\">\n");
        builder.append(renderValue(monsterData, "str"));
        builder.append(renderValue(monsterData, "dex"));
        builder.append(renderValue(monsterData, "con"));
        builder.append(renderValue(monsterData, "int"));
        builder.append(renderValue(monsterData, "wis"));
        builder.append(renderValue(monsterData, "cha"));
        builder.append("    </tr>\n");
        builder.append("  </table>\n");
        builder.append("  <div class=\"gradient\"></div>\n");
        builder.append(renderObj(monsterData, "save", "Saving Throws"));
        builder.append(renderObj(monsterData, "skill", "Skills"));
        builder.append(renderObjArray(monsterData, "vulnerable", "Damage Vulnerabilities"));
        builder.append(renderObjArray(monsterData, "resist", "Damage Resistances"));
        builder.append(renderObjArray(monsterData, "immune", "Damage Immunities"));
        builder.append(renderObjArray(monsterData, "conditionImmune", "Condition Immunities"));
        var passive = "passive Perception " + monsterData.get("passive").getAsString();
        builder.append(renderObjArray(monsterData, "senses", "Senses", passive));
        builder.append(renderObjArray(monsterData, "languages", "Languages"));
        builder.append("  <div>\n");
        builder.append("    <span class=\"bold\">Challenge</span>\n");
        String cr = monsterData.get("cr").getAsString();
        builder.append("    <span>").append(cr).append(" (").append(mapXp(cr)).append(" XP)</span>\n");
        builder.append("  </div>\n");
        builder.append("  <div class=\"gradient\"></div>\n");
        builder.append(renderTraits(monsterData));
        builder.append(renderSection(monsterData, "action", "Actions","", "attack", "attackname"));
        builder.append(renderSection(monsterData, "variant", "", "", "variant", "variantname"));
        builder.append(renderSection(monsterData, "legendary", "Legendary Actions", "mon.legendaryHeader", "attack", "attackname"));
        builder.append(renderSection(monsterData, "mythic", "Mythic Actions", "mythicHeader", "attack", "attackname"));
        builder.append("</div>");

        return builder.toString();
    }

    private String renderNamedEntries(JsonObject namedWithEntries, String css4main, String css4name)
    {
        try {
            var builder = new StringBuilder();

            var namePrefix = "";
            var type = namedWithEntries.get("type");
            if (type != null) {
                var typeString = type.getAsString();
                if(!typeString.equals("item"))
                    namePrefix = StringUtils.capitalize(type.getAsString() + ": ");
            }


            builder.append("  <div class=\"" + css4main + "\">\n");
            builder.append("    <span class=\"" + css4name + "\">");
            builder.append(namePrefix + replaceTags(namedWithEntries.get("name").getAsString())).append("</span>\n");

            var entriesElement = namedWithEntries.get("entries");
            for (var entryElement : namedWithEntries.get("entries").getAsJsonArray()) {
                if (entryElement.isJsonPrimitive())
                    builder.append("<span>").append(replaceTags(entryElement.getAsString())).append("</span>\n");
                else {
                    var entry = entryElement.getAsJsonObject();
                    var entryType = entry.get("type").getAsString();
                    if(entryType.equals("list")) {
                        <ul> <li>
                        var items = entry.get("items").getAsJsonArray();
                        for(var item: items)
                            builder.append(renderNamedEntries(item.getAsJsonObject(), "listItem", "listItemName"));
                    } else
                        builder.append(renderNamedEntries(entry, "attack", "attackname"));

                }
            }

            builder.append("  </div>\n");
            return builder.toString();
        }catch (Exception e)    {
            System.out.println(e.toString());
            return "";
        }
    }

    private String getMonsterShortName(JsonObject mon, boolean isTitleCase) {
        var isNamedCreatureElement = mon.get("isNamedCreature");
        Boolean isNamedCreature = false;
        if(isNamedCreatureElement != null)
            isNamedCreature = isNamedCreatureElement.getAsBoolean();

		String prefix = isNamedCreature ? "" : isTitleCase ? "The " : "the ";

		String name = mon.get("name").getAsString();

		String base = name.split(",")[0];
        var out = base.replaceAll("(?:Adult|Ancient|Young) \\w+ (Dragon|Dracolich)", "$1");
        out = isNamedCreature ? out.split(" ")[0] : out.toLowerCase();
        return prefix + out;
    }

    private String renderSection(JsonObject monster, String tag, String title, String introTag, String cssMain, String cssName) {
        var actions = monster.get(tag);
        if(actions == null)
            return "";

        var builder = new StringBuilder();
        if(!title.isEmpty()) {
            builder.append("  <div class=\"actions red\">").append(title).append("</div>\n");
            builder.append("  <div class=\"hr\"></div>\n");
        }

        var introElement = monster.get(introTag);
        if(introElement != null)
            builder.append("<span>").append(introElement.getAsString()).append("</span>");
        else if (tag.equals("legendary"))
        {
            int count = 3;
            var legendaryActionsElement = monster.get("legendaryActions");
            if(legendaryActionsElement != null)
                count = legendaryActionsElement.getAsInt();

			var legendaryNameTitle = getMonsterShortName(monster, true);
            builder.append("<span>" + legendaryNameTitle + " can take " + count + " legendary action" + ((count>1)?"s":"") +
                    " choosing from the options below. Only one legendary action can be used at a time " +
                    "and only at the end of another creature's turn. " + legendaryNameTitle + " regains " +
                    "spent legendary actions at the start of its turn.</span>");
        }

        for(var actionElement: actions.getAsJsonArray()) {
            var action = actionElement.getAsJsonObject();
            builder.append(renderNamedEntries(action, cssMain, cssName));
        }

        return builder.toString();
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