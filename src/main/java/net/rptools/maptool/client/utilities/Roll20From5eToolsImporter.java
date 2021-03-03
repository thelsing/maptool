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
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;

import com.samskivert.util.StringUtil;
import javafx.util.Pair;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.ui.MapPropertiesDialog;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.Zone.Layer;
import net.rptools.maptool.model.Zone.TopologyMode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;

/** Class for importing Roll20 Module json format. */
public class Roll20From5eToolsImporter {


    private String etools = "https://5e.tools/";
    //private String etools = "http://immerlund/";

    private final String cacheDir = "C:\\MT\\";

    private java.nio.file.Path currentMapDir;
    private Map<String, JsonObject> monsters = new HashMap<>();
    private Map<Pair<String, String>, JsonObject> monsterWithSource= new HashMap<>();
    private Map<String, JsonObject> monstersFluff = new HashMap<>();
    private Map<String, JsonObject> characters = new HashMap<>();

    /**
     * Creates a new {@code Roll20From5eToolsImporter} object.
     */
    public Roll20From5eToolsImporter() {

    }

    /**
     * Import the json file and craate a new {@link Zone} which is added to the campaign.
     *
     * @throws IOException if an error occurs during the import.
     */
    public void importJson() throws Exception {
        try {
            JsonObject json;
            //try (InputStreamReader reader = new InputStreamReader(new FileInputStream(jsonFile))) {
            //    json = JsonParser.parseReader(reader).getAsJsonObject();
            //}
            var url = new URL("http://immerlund/data/roll20-module/roll20-module-bgdia.json");
            json = readJsonFromUrl(url);

            var moduleFile = url.getFile();
            moduleFile = moduleFile.substring(moduleFile.lastIndexOf("/") + 1);
            var pattern = "roll20-module-([a-z]+)\\.json";
            var moduleShort = moduleFile.replaceAll(pattern, "$1");

            if(moduleShort.equals("ootw"))
                moduleShort = "oow";

            if(moduleShort.startsWith("tftyp"))
                fetchMonsterData("tftyp");

            fetchMonsterData("mm");
            fetchMonsterData(moduleShort);
            if(moduleShort.equals("dip"))
            {
                fetchMonsterData("slw");
                fetchMonsterData("sdw");
                fetchMonsterData("dc");
            }

            var charactersArray = json.get("characters").getAsJsonArray();
            for(var element: charactersArray) {
                var obj = element.getAsJsonObject();
                var attrObj = obj.get("attributes").getAsJsonObject();
                var id = attrObj.get("id").getAsString();
                characters.put(id, obj);
            }


            //var mon = JsonParser.parseString("{\"name\":\"Sea Hag\",\"source\":\"MM\",\"page\":179,\"srd\":true,\"otherSources\":[{\"source\":\"PotA\"},{\"source\":\"TftYP\"},{\"source\":\"ToA\"},{\"source\":\"WDMM\"},{\"source\":\"GoS\"},{\"source\":\"DIP\"},{\"source\":\"SLW\"},{\"source\":\"MOT\"},{\"source\":\"IDRotF\"}],\"size\":\"M\",\"type\":\"fey\",\"alignment\":[\"C\",\"E\"],\"ac\":[{\"ac\":14,\"from\":[\"natural armor\"]}],\"hp\":{\"average\":52,\"formula\":\"7d8 + 21\"},\"speed\":{\"walk\":30,\"swim\":40},\"str\":16,\"dex\":13,\"con\":16,\"int\":12,\"wis\":12,\"cha\":13,\"senses\":[\"darkvision 60 ft.\"],\"passive\":11,\"languages\":[\"Aquan\",\"Common\",\"Giant\"],\"cr\":{\"cr\":\"2\",\"coven\":\"4\"},\"spellcasting\":[{\"name\":\"Shared Spellcasting (Coven Only)\",\"headerEntries\":[\"While all three members of a hag coven are within 30 feet of one another, they can each cast the following spells from the wizard's spell list but must share the spell slots among themselves:\"],\"spells\":{\"1\":{\"slots\":4,\"spells\":[\"{@spell identify}\",\"{@spell ray of sickness}\"]},\"2\":{\"slots\":3,\"spells\":[\"{@spell hold person}\",\"{@spell locate object}\"]},\"3\":{\"slots\":3,\"spells\":[\"{@spell bestow curse}\",\"{@spell counterspell}\",\"{@spell lightning bolt}\"]},\"4\":{\"slots\":3,\"spells\":[\"{@spell phantasmal killer}\",\"{@spell polymorph}\"]},\"5\":{\"slots\":2,\"spells\":[\"{@spell contact other plane}\",\"{@spell scrying}\"]},\"6\":{\"slots\":1,\"spells\":[\"{@spell eyebite}\"]}},\"footerEntries\":[\"For casting these spells, each hag is a 12th-level spellcaster that uses Intelligence as her spellcasting ability. The spell save DC is 12 + the hag's Intelligence modifier, and the spell attack bonus is 4 + the hag's Intelligence modifier.\"]}],\"trait\":[{\"name\":\"Amphibious\",\"entries\":[\"The hag can breathe air and water.\"]},{\"name\":\"Horrific Appearance\",\"entries\":[\"Any humanoid that starts its turn within 30 feet of the hag and can see the hag's true form must make a {@dc 11} Wisdom saving throw. On a failed save, the creature is {@condition frightened} for 1 minute. A creature can repeat the saving throw at the end of each of its turns, with disadvantage if the hag is within line of sight, ending the effect on itself on a success. If a creature's saving throw is successful or the effect ends for it, the creature is immune to the hag's Horrific Appearance for the next 24 hours.\",\"Unless the target is surprised or the revelation of the hag's true form is sudden, the target can avert its eyes and avoid making the initial saving throw. Until the start of its next turn, a creature that averts its eyes has disadvantage on attack rolls against the hag.\"]},{\"name\":\"Hag Coven\",\"entries\":[\"When hags must work together, they form covens, in spite of their selfish natures. A coven is made up of hags of any type, all of whom are equals within the group. However, each of the hags continues to desire more personal power.\",\"A coven consists of three hags so that any arguments between two hags can be settled by the third. If more than three hags ever come together, as might happen if two covens come into conflict, the result is usually chaos.\"]},{\"name\":\"Hag Eye (Coven Only)\",\"entries\":[\"A hag coven can craft a magic item called a hag eye, which is made from a real eye coated in varnish and often fitted to a pendant or other wearable item. The hag eye is usually entrusted to a minion for safekeeping and transport. A hag in the coven can take an action to see what the hag eye sees if the hag eye is on the same plane of existence. A hag eye has AC 10, 1 hit point, and darkvision with a radius of 60 feet. If it is destroyed, each coven member takes {@dice 3d10} psychic damage and is {@condition blinded} for 24 hours.\",\"A hag coven can have only one hag eye at a time, and creating a new one requires all three members of the coven to perform a ritual. The ritual takes 1 hour, and the hags can't perform it while {@condition blinded}. During the ritual, if the hags take any action other than performing the ritual, they must start over.\"]}],\"action\":[{\"name\":\"Claws\",\"entries\":[\"{@atk mw} {@hit 5} to hit, reach 5 ft., one target. {@h}10 ({@damage 2d6 + 3}) slashing damage.\"]},{\"name\":\"Death Glare\",\"entries\":[\"The hag targets one {@condition frightened} creature she can see within 30 feet of her. If the target can see the hag, it must succeed on a {@dc 11} Wisdom saving throw against this magic or drop to 0 hit points.\"]},{\"name\":\"Illusory Appearance\",\"entries\":[\"The hag covers herself and anything she is wearing or carrying with a magical illusion that makes her look like an ugly creature of her general size and humanoid shape. The effect ends if the hag takes a bonus action to end it or if she dies.\",\"The changes wrought by this effect fail to hold up to physical inspection. For example, the hag could appear to have no claws, but someone touching her hand might feel the claws. Otherwise, a creature must take an action to visually inspect the illusion and succeed on a {@dc 16} Intelligence (Investigation) check to discern that the hag is disguised.\"]}],\"environment\":[\"underwater\",\"coastal\"],\"hasToken\":true,\"soundClip\":{\"type\":\"internal\",\"path\":\"bestiary/sea-hag.mp3\"},\"traitTags\":[\"Amphibious\"],\"senseTags\":[\"D\"],\"languageTags\":[\"AQ\",\"C\",\"GI\"],\"damageTags\":[\"S\"],\"spellcastingTags\":[\"CW\",\"S\"],\"miscTags\":[\"MW\"],\"conditionInflict\":[\"frightened\"],\"conditionInflictSpell\":[\"frightened\",\"paralyzed\",\"poisoned\",\"unconscious\"],\"hasFluff\":true,\"hasFluffImages\":true}");
            //var test = getStatBlock(mon.getAsJsonObject());



            JsonArray maps = json.getAsJsonArray("maps");
            int i = 0;
            for (JsonElement m : maps) {
                i++;
    //            if(i>2)
      //              break;
      //          if(i < 5)
      //              continue;

                Zone zone = ZoneFactory.createZone();
                JsonObject map = m.getAsJsonObject();

                JsonObject mapAttributes = map.getAsJsonObject("attributes");

                JsonArray paths = map.getAsJsonArray("paths");

                JsonArray texts = map.getAsJsonArray("text");

                String mapName = mapAttributes.get("name").getAsString()
                        .replaceAll("’", "").replaceAll(":", "")
                        .replaceAll("'", "").trim();

                currentMapDir = Paths.get(cacheDir, moduleShort, mapName);
                currentMapDir.toFile().mkdirs();

                Asset asset = null;
                int pixelsPerCell = 70;

                MapPropertiesDialog dialog =
                        MapPropertiesDialog.createMapPropertiesImportDialog(MapTool.getFrame());
                dialog.setZone(zone);
                dialog.forcePixelsPerCell(pixelsPerCell);

                //dialog.forceGridType(GridFactory.SQUARE);
                if (asset != null)
                    dialog.forceMap(asset);
                else
                    dialog.getNameTextField().setText(mapName);

                dialog.accept();
                //dialog.setVisible(true);
                if (dialog.getStatus() == MapPropertiesDialog.Status.OK) {
                    MapTool.addZone(zone);
                } else
                    return;

                placeTokens(zone, map);

                placeTexts(zone, texts);

                // Handle Walls

                for (JsonElement p : paths)
                    RenderPath(p.getAsJsonObject(), zone);
            }

            MapTool.showInformation("Done.");
        } catch (Exception e) {
            System.out.println(e.toString());
            throw e;
        }


    }

    private void fetchMonsterData(String module)
    {
        try {
            var url = etools + "/data/bestiary/bestiary-" + module + ".json";
            var urlFluff = etools + "/data/bestiary/fluff-bestiary-" + module + ".json";
            var dataJson = readJsonFromUrl(new URL(url));
            var fluffJson = readJsonFromUrl(new URL(urlFluff));
            parseMonsterFluff(fluffJson);
            parseMonsterData(dataJson, module);
        }
        catch (Exception e)
        {
            MapTool.showInformation(e.toString());
        }
    }

    private void parseMonsterData(JsonObject dataJson, String module) {
        var monsterDatas = dataJson.getAsJsonArray("monster");
        for(var m: monsterDatas) {
            var monster = m.getAsJsonObject();
            var name = monster.get("name").getAsString();
            monsters.put(name, monster);
            monsterWithSource.put(new Pair<>(name, module.toLowerCase()), monster);
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

    public static JsonObject readJsonFromUrl(URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            var json = JsonParser.parseReader(reader).getAsJsonObject();
            return json;
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

    private URL getImageUriFromGraphics(JsonObject graphic) throws Exception
    {
        var urlString = graphic.get("imgsrc").getAsString();
        if(urlString.isEmpty())
            return null;

        var extension= getFileExtension(new URL(urlString));
        var newUrl = urlString.substring(0, urlString.lastIndexOf("/") + 1) + "original" + extension;
        return new URL(newUrl);
    }
    /*
    private BufferedImage getImageFromGraphics(JsonObject graphic, String mapName) throws Exception {
        String file = graphic.get("name").getAsString();
        if(file.isEmpty())
            file = graphic.get("id").getAsString();

        file = file.replace("\"", "");

        URI uri = new URI(graphic.get("imgsrc").getAsString());
        String ext = getFileExtension(uri.toURL()).get();
        Path imagePath = Paths.get(moduleDir, mapName, file + "." + ext);
        return ImageIO.read(imagePath.toFile());
    }
*/
    public static String getFileExtension(final URL url) {
        Objects.requireNonNull(url, "url is null");

        final String file = url.getFile();

        if (file.contains(".")) {

            final String sub = file.substring(file.lastIndexOf('.'));

            if (sub.length() == 0) {
                return "";
            }

            if (sub.contains("?")) {
                return sub.substring(0, sub.indexOf('?'));
            }

            return sub;
        }

        return "";
    }

    private void placeTokens(Zone zone, JsonObject map) throws Exception {
        JsonObject mapAttributes = map.getAsJsonObject("attributes");
        JsonArray graphics = map.getAsJsonArray("graphics");
        var graphicsMap = new HashMap<String, JsonObject>();
        var set = new HashSet<JsonObject>();

        for (JsonElement ele: graphics) {
            var graphic = ele.getAsJsonObject();
            set.add(graphic);
            String id = graphic.get("id").getAsString();
            graphicsMap.put(id, graphic);
        }

        var zorder = mapAttributes.get("zorder").getAsString();
        for(var id: zorder.split(",")) {
            JsonObject graphic = graphicsMap.get(id);
            if(graphic == null)
                continue;

            set.remove(graphic);

            handleGraphic(zone, graphic);
        }

        for(var graphic: set) {
            handleGraphic(zone, graphic);
        }

    }

    private void handleGraphic(Zone zone, JsonObject graphic) throws Exception {
        String layer = graphic.get("layer").getAsString();
        String id = graphic.get("id").getAsString();
        String name = graphic.get("name").getAsString();
        String sidesValue = graphic.get("sides").getAsString();

        var representsElement = graphic.get("represents");
        var represents = "";
        var isCharacter = false;
        var monsterName = name;
        if(representsElement != null) {
            represents = representsElement.getAsString();
            var obj = characters.get(represents);
            if(obj != null) {
                isCharacter = true;
                var attributes = obj.get("attributes").getAsJsonObject();
                monsterName = attributes.get("name").getAsString();
            }

        }

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
            fullImg = getFullImg(name, monsterName);
        }

        Asset asset = getAsset(name, "token", getImageUriFromGraphics(graphic));
        if (asset == null)
            return;

        Token token = new Token(name, asset.getId());

        //if(!name.equals(translatedName))
        token.setGMName(translatedName);

        if(isCharacter) {
            token.setName("?");
            token.setSnapToGrid(true);
            token.setSnapToScale(true);
            token.setGMNotes(getStatBlock(name, monsterName));
        } else {
            token.setSnapToGrid(false);
            token.setSnapToScale(false);
        }

        token.setVisible(true);

        if(layer.equals("gmlayer"))
            token.setLayer(Layer.GM);
        else if(layer.equals("map"))
            token.setLayer(Layer.BACKGROUND);
        else if(layer.equals("objects")) {
            //if(!isCharacter)
            //    token.setLayer(Layer.OBJECT);
            //else
                token.setLayer(Layer.TOKEN);
        } else if(layer.equals("walls")) {
            // This is the dynamic lightning layer. So Token on this layer are transparent
            token.setLayer(Layer.OBJECT);
            token.setTokenOpacity(0.5f);
            token.setVisible(false);
            //TODO: set Light to token.
        }
        else
            throw new Exception("unknown layer: " + layer);


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

        token.setProperty("roll20TokenId", id);
        if(representsElement != null)
            token.setProperty("roll20CharacterId", representsElement.getAsString());

        if(isCharacter) {
            var macro = new MacroButtonProperties(token, 0, "");
            macro.setCommand("[setName(getGMName())]\n[abort(0)]");
            macro.setLabel("Namen freigeben");
            macro.setAllowPlayerEdits(false);

        }

        if(fullImg != null) {
            token.setProperty("fullImg", fullImg.getId());
            var macro = new MacroButtonProperties(token, 1, "");
            macro.setCommand("[setTokenHandout(\"asset://\" + getProperty(\"fullImg\"))]\n" +
                    "[setTokenPortrait(\"asset://\" + getProperty(\"fullImg\"))]\n" +
                    "[abort(0)]");
            macro.setLabel("Bild freigeben");
            macro = new MacroButtonProperties(token, 2, "");
            macro.setCommand("[setName(getGMName())]\n" +
                    "[setTokenHandout(\"asset://\" + getProperty(\"fullImg\"))]\n" +
                    "[setTokenPortrait(\"asset://\" + getProperty(\"fullImg\"))]\n" +
                    "[abort(0)]");
            macro.setLabel("Namen und Bild freigeben");
            macro.setAllowPlayerEdits(false);


            var table = MapTool.getCampaign().getLookupTableMap().get("monsters");
            if(table == null)
            {
                table = new LookupTable();
                table.setName("monsters");
                MapTool.getCampaign().getLookupTableMap().put("monsters", table);
            }

            var assetIds = table.getAllAssetIds();
            if(!assetIds.contains(fullImg.getId()))
            {
                var idx = assetIds.size();
                table.addEntry(idx, idx, name, fullImg.getId());
            }
        }


        if(!sidesValue.isEmpty()) {
            var sideList = sidesValue.split("\\|");
            var macro = new MacroButtonProperties(token, 3, "");
            macro.setCommand(
                "<!--\n" +
                "[formList=\"\"]\n" +
                "[tableName=\"" + monsterName + "\"]\n" +
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
            macro.setAllowPlayerEdits(false);

            var table = MapTool.getCampaign().getLookupTableMap().get(monsterName);
            if(table != null)
                return;

            table = new LookupTable();
            table.setName(monsterName);


            for(int i = 0; i< sideList.length; i++)
            {
                var decodedUrl = URLDecoder.decode(sideList[i]);
                var uri = new URI(decodedUrl);
                asset = getAsset(monsterName+ "_" + i, "token", uri.toURL());
                table.addEntry(i,i, String.valueOf(i), asset.getId());
            }
            MapTool.getCampaign().getLookupTableMap().put(monsterName, table);
        }
    }

    @Nullable
    private Asset getFullImg(String name, String monstername) throws MalformedURLException {
        Asset fullImg = null;
        var fluffJson = monstersFluff.get(name);
        if(fluffJson == null) {
            if (monstername != null)
                return getFullImg(monstername, null);
            return null;
        }

        var imagesElement = fluffJson.get("images");
        if(imagesElement == null || imagesElement.isJsonNull())
            return null;

        var images = imagesElement.getAsJsonArray();

        if(images.size() == 1)
        {
            var href = images.get(0).getAsJsonObject().get("href").getAsJsonObject();
            if(href == null)
                return null;

            var path = href.get("path").getAsString();
            if(path == null | path.isEmpty())
                return null;

            return getAsset(name + "_img", "monsterImg", new URL( etools + "/img/" +
                                path.replaceAll(" ", "%20")));

        } else {
            System.out.println();
        }

        return null;
    }

    private Map<String, String> sizeMap =  Map.of(
            "T","tiny",
            "S","small",
            "M","medium",
            "L","large",
            "H","huge",
            "G","gargantuan"
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
        var builder = new StringBuilder();
        var alignList = new ArrayList<String>();
        var isFirst = true;
        var isComplex = false;
        for(JsonElement j: alignment) {

            if(j.isJsonPrimitive())
                alignList.add(j.getAsString());
            else
            {
                if(isFirst) {
                    isFirst = false;
                } else
                    builder.append(" or ");

                isComplex = true;
                var jsonObject = j.getAsJsonObject();
                var specialElement = jsonObject.get("special");
                if(specialElement != null)
                    return specialElement.getAsString();

                builder.append(mapAlignment(jsonObject.getAsJsonArray("alignment")));
                var chanceElement = jsonObject.get("chance");
                if(chanceElement != null)
                    builder.append(" ("+ chanceElement.getAsString() + "%) ");
            }
        }

        if(isComplex)
            return builder.toString();

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

            var swarmSizeElement = jsonObject.get("swarmSize");
            if(swarmSizeElement != null)
            {
                builder.append("swarm of ");
                builder.append(sizeMap.get(swarmSizeElement.getAsString())).append(" ");
                builder.append(jsonObject.get("type").getAsString()).append("s");
            } else
                builder.append(jsonObject.get("type").getAsString());


            var tagsElement = jsonObject.get("tags");
            if(tagsElement != null) {
                var tags = tagsElement.getAsJsonArray();
                builder.append(" (");
                boolean isFirst = true;
                for (var tag : tags) {
                    if (!isFirst) {
                        isFirst = false;
                        builder.append(", ");
                    }

                    if(tag.isJsonPrimitive())
                        builder.append(tag.getAsString());
                    else {
                        var tagObj = tag.getAsJsonObject();
                        builder.append(tagObj.get("prefix").getAsString()).append(" ");
                        builder.append(tagObj.get("tag").getAsString());
                    }
                }
                builder.append(")");
            }
            builder.append(", ");
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
        desc = replaceTag(desc,"{@recharge", "(Recharge ", " 6)");
        desc = replaceTag(desc,"{@damage ");
        desc = replaceTag(desc,"{@condition ");
        desc = replaceTag(desc,"{@skill ");
        desc = replaceTag(desc,"{@dice ");
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
        if(element == null && tag.equals("languages")) {
            element = JsonParser.parseString("[ \"-\" ]");
        } else if(element == null)
            return "";

        var builder = new StringBuilder();
        if(name != null) {
            builder.append("  <div class=\"commonTrait\">\n");
            builder.append("    <span class=\"bold\">").append(name).append("</span>\n");

            builder.append("    <span>");
        }

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

                var valueObj = valuesArray.get(i);

                if(valueObj.isJsonPrimitive())
                    builder.append(valueObj.getAsString());
                else
                    builder.append(renderObjArray(valueObj.getAsJsonObject(), tag, null));
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

    private Map<String, String> replaceForStat =  Map.of(
            "Doppleganger","Doppelganger",
            "Glasstaff","Evil Mage"
    );

    private String getStatBlock(String name, String monsterName) throws Exception {
        var replName = replaceForStat.get(name);
        if(replName != null)
            name = replName;

        var monsterData = monsters.get(name);
        if (monsterData == null) {
            if (monsterName != null)
                return getStatBlock(monsterName, null);
            return "";
        }

        return getStatBlock(monsterData);
    }

    private String getStatBlock(JsonObject monsterData) throws Exception {
     //   try {
        var copyElement = monsterData.get("_copy");
        if(copyElement != null)
        {
            var copy = copyElement.getAsJsonObject();
            var key = new Pair<>(copy.get("name").getAsString(), copy.get("source").getAsString().toLowerCase());
            monsterData = monsterWithSource.get(key);
            if (monsterData == null)
                return "";
            return "<h1>Unvollständig!!</h1>\n" + getStatBlock(monsterData);
        }

            var builder = new StringBuilder();
            var name = monsterData.get("name").getAsString();
            var source = monsterData.get("source").getAsString().toLowerCase();
            var escapedName = name.replace(" ", "%20");

            builder.append("<div contenteditable=\"true\" style=\"width:400px;font-family:Arial,Helvetica,sans-serif;font-size:11px;\">\n");
            builder.append("  <div class=\"name\"><a href=\"").append(etools).append("bestiary.html#").append(escapedName).append("_").append(source).append("\">");
            builder.append(name);
            builder.append("</a></div>\n");
            builder.append("  <div class=\"description\">");
            builder.append(StringUtil.capitalize(sizeMap.get(monsterData.get("size").getAsString())));
            builder.append(" ");
            builder.append(mapType(monsterData.get("type")));
            var alignmentElement = monsterData.get("alignment");
            if(alignmentElement != null)
                builder.append(mapAlignment(alignmentElement.getAsJsonArray()));
            builder.append("</div>\n");
            builder.append("  <div class=\"gradient\"></div>\n");
            builder.append("  <div class=\"red\">\n");
            builder.append("    <div>\n");
            builder.append("      <span class=\"bold red\">Armor Class</span>\n");
            builder.append("      <span> ").append(mapAC(monsterData.get("ac").getAsJsonArray())).append("</span>\n");
            builder.append("    </div>\n");
            builder.append("    <div>\n");
            builder.append("      <span class=\"bold red\">Hit Points</span>\n<span>");
            var hpObj = monsterData.get("hp").getAsJsonObject();
            var hpSpecial = hpObj.get("special");
            if(hpSpecial != null)
                builder.append(hpSpecial.getAsString());
            else {
                builder.append("       ").append(hpObj.get("average").getAsString()).append("  (");
                builder.append(hpObj.get("formula").getAsString()).append(")");
            }
            builder.append("    </span>\n</div>\n");
            builder.append("    <div>\n");
            builder.append("      <span class=\"bold red\">Speed</span>\n");
            builder.append("      <span> ");
            var speeds = monsterData.get("speed").getAsJsonObject();
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
            var crElement = monsterData.get("cr");
            if(crElement != null) {
                builder.append("  <div>\n");
                builder.append("    <span class=\"bold\">Challenge</span>\n");
                if(crElement.isJsonPrimitive()) {
                    String cr = crElement.getAsString();
                    builder.append("    <span>").append(cr).append(" (").append(mapXp(cr)).append(" XP)</span>\n");
                } else {
                    builder.append("    <span>");
                    var jsonObj = crElement.getAsJsonObject();
                    for(var key:jsonObj.keySet())
                    {
                        var cr = jsonObj.get(key).getAsString();
                        switch (key) {
                            case "cr":
                                builder.append(cr).append(" (").append(mapXp(cr)).append(" XP)");
                                break;
                            case "coven":
                                builder.append(" or ");
                                builder.append(cr).append(" (").append(mapXp(cr)).append(" XP)");
                                builder.append(" when part of a coven");
                                break;
                            case "lair":
                                builder.append(" or ");
                                builder.append(cr).append(" (").append(mapXp(cr)).append(" XP)");
                                builder.append(" when encountered in lair");
                                break;

                            default:
                                throw new Exception("unknown cr type");
                        }
                    }
                }
                builder.append("  </div>\n");
            }
            builder.append("  <div class=\"gradient\"></div>\n");
            builder.append(renderTraits(monsterData));
            builder.append(renderSection(monsterData, "action", "Actions", "", "attack", "attackname"));
            builder.append(renderSection(monsterData, "variant", "", "", "variant", "variantname"));
            builder.append(renderSection(monsterData, "legendary", "Legendary Actions", "mon.legendaryHeader", "attack", "attackname"));
            builder.append(renderSection(monsterData, "mythic", "Mythic Actions", "mythicHeader", "attack", "attackname"));
            builder.append("</div>");

            return builder.toString();
      /*  }catch(Exception e)
        {
            return "";
        }*/
    }

    private String renderNamedEntries(JsonObject namedWithEntries, String css4main, String css4name)
    {
        var builder = new StringBuilder();

        var namePrefix = "";
        var type = namedWithEntries.get("type");
        if (type != null) {
            var typeString = type.getAsString();
            if(!typeString.equals("item") && !typeString.equals("entries"))
                namePrefix = StringUtils.capitalize(type.getAsString() + ": ");
        }


        builder.append("  <div class=\"" + css4main + "\">\n");

        var nameElement = namedWithEntries.get("name");
        if(nameElement != null) {
            builder.append("    <span class=\"" + css4name + "\">");
            builder.append(namePrefix + replaceTags(nameElement.getAsString())).append("</span>\n");
        }

        var entriesElement = namedWithEntries.get("entries");
        for (var entryElement : namedWithEntries.get("entries").getAsJsonArray()) {
            if (entryElement.isJsonPrimitive())
                builder.append("<span>").append(replaceTags(entryElement.getAsString())).append("</span>\n");
            else {
                var entry = entryElement.getAsJsonObject();
                var entryType = entry.get("type").getAsString();
                if(entryType.equals("list")) {
                    builder.append("<ul class=\"rd_list");
                    var styleElement = entry.get("style");
                    if (styleElement != null) {
                        builder.append(" ").append(styleElement.getAsString());
                    }
                    builder.append("\">\n");
                    var items = entry.get("items").getAsJsonArray();
                    for (var itemEntryElement : items) {
                        if(itemEntryElement.isJsonPrimitive()) {
                            builder.append("<li><p>");
                            builder.append(itemEntryElement.getAsString());
                            builder.append("</p></li>");
                            continue;
                        }


                        var itemEntry = itemEntryElement.getAsJsonObject();
                        builder.append("<li><p><span ");
                        builder.append("class=\"rd_listitem");
                        var itemStyleElement = itemEntry.get("style");

                        if (itemStyleElement != null) {
                            builder.append(" ").append(itemStyleElement.getAsString());
                        }
                        builder.append("\"> ");

                        builder.append(itemEntry.get("name").getAsString()).append("</span>");
                        var entryElement2 = itemEntry.get("entry");
                        if(entryElement2 != null)
                            builder.append(replaceTags(entryElement2.getAsString()));
                        else
                            for(var entryElement3: itemEntry.getAsJsonArray("entries"))
                                builder.append(replaceTags(entryElement3.getAsString()));

                        builder.append("</p></li>");
                    }
                    builder.append("</ul>\n");
                } else if (entryType.equals("table")) {
                    builder.append("<table class=\"stripe-odd-table\">\n");
                    builder.append("<thead><tr>");
                    var labels = entry.getAsJsonArray("colLabels");
                    for(var labelElement: labels)
                    {
                        builder.append("<th>"+ labelElement.getAsString() + "</th>\n");
                    }
                    builder.append("</thead></tr>");
                    builder.append("<tbody><tr>");
                    var rows = entry.getAsJsonArray("rows");
                    for(var row: rows)
                    {
                        builder.append("<tr>");
                        for(var item: row.getAsJsonArray())
                            builder.append("<td>"+ item.getAsString() + "</td>\n");
                        builder.append("</tr>");
                    }
                    builder.append("</tbody></tr>");
                    builder.append("</table>\n");
                } else if (entryType.equals("link")) {
                    builder.append("<h href=\"");
                    builder.append(etools);
                    var href = entry.getAsJsonObject("href");
                    builder.append(href.get("path").getAsString()).append("#");
                    builder.append(href.get("hash").getAsString()).append("\">");
                    builder.append(entry.get("text").getAsString());
                    builder.append("</a>");
                } else
                    builder.append(renderNamedEntries(entry, "attack", "attackname"));

            }
        }

        builder.append("  </div>\n");
        return builder.toString();
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

    private Map<String, String> replaceForTrans =  Map.of(
            "Doppleganger","Doppelganger"
    );

    private String translateName(String name) throws Exception {
        String translatedName = name;
        var replName = replaceForTrans.get(name);
        if(replName != null)
            name = replName;

        var transURl = new URL("https://www.dnddeutsch.de/tools/json.php?s="+ URLEncoder.encode(name) +
                "&o=dict&mi=off&mo=on&sp=off&it=off&misc=off");
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
    private Asset getAsset(String name, String type, URL imagePath) {
        if(imagePath == null)
            return null;

        BufferedImage image = null;
        try {
            var ext = getFileExtension(imagePath);
            var file = Paths.get(currentMapDir.toString(), type, name+ext).toFile();
            if(file.exists())
                image = ImageIO.read(file);
            else {
                FileUtils.copyURLToFile(imagePath,file);
                image = ImageIO.read(file);
            }
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