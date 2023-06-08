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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rometools.utils.Strings;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.rptools.lib.MD5Key;
import net.rptools.maptool.client.MapTool;
import net.rptools.maptool.client.functions.MacroFunctions;
import net.rptools.maptool.client.ui.mappropertiesdialog.MapPropertiesDialog;
import net.rptools.maptool.client.ui.theme.Images;
import net.rptools.maptool.client.ui.theme.RessourceManager;
import net.rptools.maptool.model.*;
import net.rptools.maptool.model.Zone.Layer;
import net.rptools.parser.ParserException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

public class FoundryModuleImporter {

  private static final int WALL_VBL_WIDTH = 3;

  /** Stroke to use to create VBL path for walls. */
  private static final BasicStroke WALL_VBL_STROKE =
      new BasicStroke(WALL_VBL_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);

  /** Width of the Light source icon. */
  private static final int LIGHT_WIDTH = 20;
  /** Height of the Light source icon. */
  private static final int LIGHT_HEIGHT = 20;

  /** Asset to use to represent Light sources. */
  private static final Asset lightSourceAsset =
      Asset.createImageAsset("LightSource", RessourceManager.getImage(Images.LIGHT_SOURCE));

  private static final Asset noteAsset =
      Asset.createImageAsset("note", RessourceManager.getImage(Images.LOOKUP_TABLE_DEFAULT));

  static {
    AssetManager.putAsset(lightSourceAsset);
    AssetManager.putAsset(noteAsset);
  }

  private Path currentModuleRootDir;
  private Path systemDir;

  private File moduleFile;
  private String moduleName = "";
  private Map<String, String> foundryId2Link = new HashMap<>();

  private Map<String, String> entryNames = new HashMap<>();
  private Map<Pair<String, String>, String> pageNames = new HashMap<>();
  private Map<Pair<String, String>, MD5Key> pageImages = new HashMap<>();

  public FoundryModuleImporter(File file) {
    moduleFile = file;
  }

  private Zone libTokenZone;

  public void importVTT() throws IOException {
    libTokenZone =
        MapTool.getCampaign().getZones().stream()
            .filter(z -> z.getName().equals("00.DM"))
            .findFirst()
            .get();
    var path =
        Paths.get(/*"C:\\Users\\tkunze\\Downloads\\cos-bluewater-pk.zip" */moduleFile.getPath());
    var system = Paths.get("C:\\Users\\tkunze\\Downloads\\dnd5e-release-2.1.5.zip");
    try (var systemFileSystem = FileSystems.newFileSystem(system, new HashMap<>(), null)) {
      systemDir = systemFileSystem.getRootDirectories().iterator().next();
      try (var fileSystem = FileSystems.newFileSystem(path, new HashMap<>(), null)) {
        for (var dir : fileSystem.getRootDirectories()) {
          checkForModuleJson(dir);
        }
      }
    }
  }

  private void checkForModuleJson(Path dir) throws IOException {
    var tmp = dir.resolve("module.json");
    if (Files.exists(tmp)) {
      importModule(tmp);
    } else {
      for (var subdir : Files.list(dir).filter(Files::isDirectory).toList()) {
        checkForModuleJson(subdir);
      }
    }
  }

  private void importModule(Path moduleJson) throws IOException {
    try (InputStreamReader reader = new InputStreamReader(Files.newInputStream(moduleJson))) {
      var moduleFile = JsonParser.parseReader(reader).getAsJsonObject();
      moduleName = moduleFile.getAsJsonPrimitive("title").getAsString();
      currentModuleRootDir = moduleJson.getParent();
      for (var pack : moduleFile.getAsJsonArray("packs")) {
        importPack(pack.getAsJsonObject());
      }
    }
  }

  private void importPack(JsonObject pack) throws IOException {
    var label = pack.getAsJsonPrimitive("label").getAsString();
    var pathString = pack.getAsJsonPrimitive("path").getAsString();
    if (pathString.startsWith("/")) {
      pathString = pathString.substring(1);
    }
    var packPath = currentModuleRootDir.resolve(pathString);
    var lines = Files.readAllLines(packPath);
    for (var line : lines) {
      if (Strings.isBlank(line)) {
        continue;
      }
      var packFile = JsonParser.parseString(line).getAsJsonObject();

      if (packFile.has("actors")) {
        var actors = packFile.get("actors");
        if (actors.isJsonArray()) {
          for (var actor : actors.getAsJsonArray()) {
            importActor(actor.getAsJsonObject());
          }
        }
      }

      if (packFile.has("items")) {
        var items = packFile.get("items");
        if (items.isJsonArray()) {
          for (var item : items.getAsJsonArray()) {
            importItem(item.getAsJsonObject());
          }
        }
      }

      if (packFile.has("journal")) {
        var journal = packFile.get("journal");
        if (journal.isJsonArray()) {
          importJournal(label, journal.getAsJsonArray());
        }
      }

      if (packFile.has("scenes")) {
        var scenes = packFile.get("scenes");
        if (scenes.isJsonArray()) {
          for (var scene : scenes.getAsJsonArray()) {
            importScene(scene.getAsJsonObject());
        //    break;
          }
        }
      }
    }
  }

  private void importActor(JsonObject actor) {
    var id = actor.getAsJsonPrimitive("_id").getAsString();
    var name = actor.getAsJsonPrimitive("name").getAsString();
    foundryId2Link.put("Actor.%s".formatted(id), "npc \"%s\"".formatted(name));
  }

  private void importItem(JsonObject item) {
    var id = item.getAsJsonPrimitive("_id").getAsString();
    var name = item.getAsJsonPrimitive("name").getAsString();
    foundryId2Link.put("Item.%s".formatted(id), "item \"%s\"".formatted(name));
  }

  private int xOffset = 0;
  private int yOffset = 0;

  private void importScene(JsonObject scene) throws IOException {
    String mapName = scene.getAsJsonPrimitive("name").getAsString();

    // I don't want the theater of mind maps for now
    if(mapName.endsWith(" M"))
      return;

    var background = scene.getAsJsonObject("background");
    var grid = scene.getAsJsonObject("grid");

    Zone zone = ZoneFactory.createZone();

    MapPropertiesDialog dialog =
        MapPropertiesDialog.createMapPropertiesImportDialog(MapTool.getFrame());
    dialog.setZone(zone);
    dialog.forcePixelsPerCell(grid.getAsJsonPrimitive("size").getAsInt());
    dialog.forceGridType(GridFactory.SQUARE);
    //   dialog.forceMap(asset);
    //   dialog.setVisible(true);
    //   if (dialog.getStatus() != MapPropertiesDialog.Status.OK) {
    //     return;
    //   }
    dialog.accept();

    var imageString =
        background.getAsJsonPrimitive("src").getAsString().substring("modules".length());
    imageString = URLDecoder.decode(imageString, "UTF-8");
    var backgroundImagePath = currentModuleRootDir.resolve(imageString);

    byte[] imageBytes = Files.readAllBytes(backgroundImagePath);
    Asset asset = Asset.createImageAsset(backgroundImagePath.getFileName().toString(), imageBytes);
    AssetManager.putAsset(asset);
    zone.setName("%s: %s".formatted(moduleName, mapName));
    int width = scene.getAsJsonPrimitive("width").getAsInt();
    int height = scene.getAsJsonPrimitive("height").getAsInt();
    double padding = scene.getAsJsonPrimitive("padding").getAsDouble();

    var mapToken = new Token("map", asset.getMD5Key());
    mapToken.setLayer(Layer.BACKGROUND);
    mapToken.setSnapToScale(false);
    mapToken.setSnapToGrid(false);
    mapToken.setWidth(width);
    mapToken.setHeight(height);
    xOffset = (int) (width * padding);
    yOffset = (int) (height * padding);

    zone.putToken(mapToken);

    // handle token
    var tokens = scene.getAsJsonArray("tokens");
    for (int i = 0; i < tokens.size(); i++) {
      handleToken(tokens.get(i).getAsJsonObject(), zone, i);
    }

    var tiles = scene.getAsJsonArray("tiles");
    for (int i = 0; i < tiles.size(); i++) {
      handleToken(tiles.get(i).getAsJsonObject(), zone, i);
    }

    // Handle Walls
    JsonArray vbl = scene.getAsJsonArray("walls");
    if (vbl != null) {
      vbl.forEach(
          v -> {
            Area vblArea =
                new Area(
                    WALL_VBL_STROKE.createStrokedShape(
                        getVBLPath(v.getAsJsonObject().getAsJsonArray("c"))));
            zone.addTopology(vblArea, Zone.TopologyType.WALL_VBL);
            zone.addTopology(vblArea, Zone.TopologyType.MBL);
          });
    }

    var notes = scene.getAsJsonArray("notes");
    for (int i = 0; i < notes.size(); i++) {
      placeNote(zone, notes.get(i).getAsJsonObject(), i);
    }

    var lights = scene.getAsJsonArray("lights");
    for (int i = 0; i < lights.size(); i++) {
      placeLights(zone, lights.get(i).getAsJsonObject(), i);
    }

    // If everything has been successful, we can add the zone to the campaign.
    MapTool.addZone(zone);
  }

  private void placeNote(Zone zone, JsonObject noteObj, int i) {
    var name = noteObj.getAsJsonPrimitive("text").getAsString();
    var size = noteObj.getAsJsonPrimitive("iconSize").getAsInt();
    var entryId = noteObj.getAsJsonPrimitive("entryId").getAsString();
    var pageId = "";
    var pageIdTag = noteObj.get("pageId");
    if(!pageIdTag.isJsonNull()) {
      pageId = pageIdTag.getAsString();
    }

    var entryName = entryNames.get(entryId);
    if(Strings.isEmpty(name) && !Strings.isEmpty(entryName)) {
      name = entryName;
    }

    Token noteToken = new Token(name, noteAsset.getMD5Key());
    noteToken.setLayer(Layer.OBJECT);
    noteToken.setVisible(false);
    noteToken.setSnapToGrid(false);
    noteToken.setSnapToScale(false);
    noteToken.setWidth(size);
    noteToken.setHeight(size);
    noteToken.setX(noteObj.get("x").getAsInt() - xOffset - LIGHT_WIDTH / 2);
    noteToken.setY(noteObj.get("y").getAsInt() - yOffset - LIGHT_HEIGHT / 2);

    var pageKey = Pair.with(entryId, pageId);
    if(pageImages.containsKey(pageKey))
    {
      noteToken.setPortraitImage(pageImages.get(pageKey));
    }

    var pageName = pageNames.get(pageKey);

    var libTokenName = "Lib:%s:%s".formatted(moduleName, entryName);
    MacroButtonProperties mbp = new MacroButtonProperties(noteToken.getMacroNextIndex());

    mbp.setLabel("Notebook");
    mbp.setSaveLocation("Token");
    if(Strings.isEmpty(pageId)) {
      mbp.setCommand("[macro('Index@lib:Notebook'):'%s']".formatted(libTokenName));
    } else {

      mbp.setCommand(
              "\n"
                      + "[h:value=getLibProperty(\"Value\",\"%s\")]\n".formatted(libTokenName)
                      + "[h:description=json.get(value,\"%s\")]\n".formatted(pageName)
                      + "[h,if(isGM()==1):share=0;share=1]\n"
                      + "[macro(\"Content@Lib:Notebook\"):\"key=%s;description=\"+encode(description)+\";tokenName=%s;share=\"+share]"
                      .formatted(pageName, libTokenName));
    }
    try {
      MacroFunctions.setMacroProps(mbp, "minWidth=120;fontColor=white;color=gray50;", ";");
    } catch (ParserException e) {
      throw new RuntimeException(e);
    }
    mbp.setAllowPlayerEdits(false);
    noteToken.saveMacro(mbp);

    zone.putToken(noteToken);
  }

  private void handleToken(JsonObject tokenObj, Zone zone, int number) throws IOException {
    var tokenTexture = tokenObj.getAsJsonObject("texture");
    var src = URLDecoder.decode(tokenTexture.getAsJsonPrimitive("src").getAsString());
    Path tokenImagePath = null;
    Asset asset = null;
    try {
      if (src.startsWith("modules")) {
        tokenImagePath = currentModuleRootDir.resolve(src.substring("modules".length()));
      } else {
        tokenImagePath = systemDir.resolve(src.substring("systems/dnd5e".length()));
      }
      byte[] tokenImageBytes = Files.readAllBytes(tokenImagePath);
      asset = Asset.createImageAsset(tokenImagePath.getFileName().toString(), tokenImageBytes);

    } catch (Exception ex) {
      MapTool.showInformation("could not find image: %s ; skipping".formatted(src));
      asset = Asset.createImageAsset("unknown", RessourceManager.getImage(Images.UNKNOWN));
    }
    AssetManager.putAsset(asset);

    var grid = zone.getGrid();
    var nameObj = tokenObj.getAsJsonPrimitive("name");
    var name = "Tile %d".formatted(number);
    var isTile = true;
    if (nameObj != null && !nameObj.isJsonNull()) {
      name = nameObj.getAsString();
      isTile = false;
    }



    var token = new Token(name, asset.getMD5Key());
    if (isTile) {
      token.setLayer(Layer.BACKGROUND);
      token.setSnapToScale(false);
    } else {
      token.setSnapToScale(true);
      token.setLayer(Layer.GM);
    }
    token.setSnapToGrid(false);
    var x = tokenObj.getAsJsonPrimitive("x").getAsInt();
    var y = tokenObj.getAsJsonPrimitive("y").getAsInt();
    var w = tokenObj.getAsJsonPrimitive("width").getAsInt();
    var h = tokenObj.getAsJsonPrimitive("height").getAsInt();
    String filter =
        switch (w) {
          case 1 -> "Medium";
          case 2 -> "Large";
          case 3 -> "Huge";
          case 4 -> "Gargantuan";
          case 5 -> "Colossal";
          default -> "";
        };
    if (!Strings.isBlank(filter)) {
      var footPrint =
          grid.getFootprints().stream().filter(fp -> fp.getName().equals(filter)).findFirst();
      if (footPrint.isPresent()) {
        token.setFootprint(grid, footPrint.get());
      }
    } else {
      token.setWidth(w);
      token.setHeight(h);
    }
    token.setX(x - xOffset);
    token.setY(y - yOffset);
    zone.putToken(token);
  }

  private void importJournal(String label, JsonArray journal) throws IOException {
 /*   var journalDir = Paths.get("C:\\Users\\tkunze\\OneDrive\\Desktop\\lobjournal");
    var moduleJournalDir = journalDir.resolve(label);
    if (!Files.exists(moduleJournalDir)) {
      Files.createDirectory(moduleJournalDir);
    }*/

    // build index first
    for (var entry : journal) {
      var entryObject = entry.getAsJsonObject();
      var entryId = entryObject.getAsJsonPrimitive("_id").getAsString();
      var name = entryObject.getAsJsonPrimitive("name").getAsString();
      entryNames.put(entryId, name);

      for (var page : entryObject.getAsJsonArray("pages")) {
        var pageObject = page.getAsJsonObject();
        var pageId = pageObject.getAsJsonPrimitive("_id").getAsString();
        var pageName = pageObject.getAsJsonPrimitive("name").getAsString();
        pageNames.put(Pair.with(entryId, pageId), pageName);
        foundryId2Link.put(
            "JournalEntry.%s.JournalEntryPage.%s".formatted(entryId, pageId),
            "note \"%s@%s:%s\"".formatted(pageName, moduleName, name));
      }
    }

    for (var entry : journal) {
      var entryObject = entry.getAsJsonObject();
      var entryId = entryObject.getAsJsonPrimitive("_id").getAsString();
      var name = entryObject.getAsJsonPrimitive("name").getAsString();
      var pageSet = new HashMap<Integer, String>();
      var jsonExport = new JsonObject();

      for (var page : entryObject.getAsJsonArray("pages")) {
        var pageObject = page.getAsJsonObject();
        var sort = pageObject.getAsJsonPrimitive("sort").getAsInt();
        var type = pageObject.getAsJsonPrimitive("type").getAsString();
        var pageId = pageObject.getAsJsonPrimitive("_id").getAsString();
        var pageName = pageObject.getAsJsonPrimitive("name").getAsString();
        var titleObject = pageObject.getAsJsonObject("title");
        var builder = new StringBuilder();
        var titleLevel = titleObject.getAsJsonPrimitive("level").getAsInt();
        builder.append("<h%d>%s</h%d>\n".formatted(titleLevel, pageName, titleLevel));
        switch (type) {
          case "text" -> {
            var content =
                pageObject.getAsJsonObject("text").getAsJsonPrimitive("content").getAsString();
            ;

            content = FixUpLinks(entryId, content);
            content = FixUpImages(Pair.with(entryId, pageId), content);
            content = FixRolls(content);
            builder.append(content);
            jsonExport.addProperty(pageName, builder.toString());
          }
          case "pdf" -> {
            var src = pageObject.get("src");
            if (src.isJsonPrimitive()) {
              builder.append(
                  "<a href=\"%s\">PDF</a>"
                      .formatted(pageObject.getAsJsonPrimitive("src").getAsString()));
            }
          }
          case "video" -> {
            builder.append(
                "<a href=\"%s\">VIDEO</a>"
                    .formatted(pageObject.getAsJsonPrimitive("src").getAsString()));
          }
          case "image" -> {
            builder.append(
                "<img src=\"%s\"/>".formatted(pageObject.getAsJsonPrimitive("src").getAsString()));
          }
          default -> {
            System.out.println("unhandled pagetype %s".formatted(type));
          }
        }
        pageSet.put(sort, builder.toString());
      }
/*
      var entryFile = moduleJournalDir.resolve("%s.html".formatted(name));
      try (var fs =
          new PrintWriter(
              Files.newOutputStream(
                  entryFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
        fs.println("<html>");
        fs.println("<body>");
        for (var sortedKey : pageSet.keySet().stream().sorted().toList()) {
          fs.println(pageSet.get(sortedKey));
        }
        fs.println("</body>");
        fs.println("</html>");
      }*/

      var notebookToken =
          new Token(
              "Lib:%s:%s".formatted(moduleName, name),
              new MD5Key("d3b2ba7ef282bf9ebe403b793fccffe7"));
      notebookToken.setProperty("Settings", "{\"theme\":\"GitHub\"}");
      notebookToken.setProperty("Value", jsonExport.toString());
      addNotebookMacro(notebookToken, notebookToken.getName());
      notebookToken.setPropertyType("Notebook");
      libTokenZone.putToken(notebookToken);
    }
  }

  private static void addNotebookMacro(Token notebookToken, String targetTokeName) {
    MacroButtonProperties mbp = new MacroButtonProperties(notebookToken.getMacroNextIndex());

    mbp.setLabel("Notebook");
    mbp.setSaveLocation("Token");
    mbp.setCommand("[macro('Index@lib:Notebook'):'%s']".formatted(targetTokeName));

    try {
      MacroFunctions.setMacroProps(mbp, "minWidth=120;fontColor=white;color=gray50;", ";");
    } catch (ParserException e) {
      throw new RuntimeException(e);
    }
    mbp.setAllowPlayerEdits(false);
    notebookToken.saveMacro(mbp);
  }

  private String FixUpImages(Pair<String, String> pageKey, String content) throws IOException {
    var pattern = Pattern.compile("<img src=\\\"([^\\ ]+)\\\"");
    var matcher = pattern.matcher(content);
    while (matcher.find()) {
      var toReplace = matcher.group(1);
      var match = toReplace.substring("modules".length());
      var imagePath = currentModuleRootDir.resolve(URLDecoder.decode(match));

      byte[] tokenImageBytes = Files.readAllBytes(imagePath);
      Asset asset = Asset.createImageAsset(imagePath.getFileName().toString(), tokenImageBytes);
      AssetManager.putAsset(asset);
      if(!pageImages.containsKey(pageKey)) {
        pageImages.put(pageKey, asset.getMD5Key());
      }
      content = content.replace(toReplace, "asset://%s".formatted(asset.getMD5Key()));
    }
    return content;
  }

  @NotNull
  private String FixUpLinks(String entryId, String content) {
    var pattern = Pattern.compile("@UUID\\[([^]]+)]\\{([^}]+)}");
    var matcher = pattern.matcher(content);
    while (matcher.find()) {
      var toReplace = matcher.group();
      var key = matcher.group(1);
      var value = matcher.group(2);

      if (key.startsWith(".")) {
        key = "JournalEntry.%s.JournalEntryPage%s".formatted(entryId, key);
      }

      if (!foundryId2Link.containsKey(key)) {
        System.out.println();
      } else {
        content = content.replace(toReplace, "[%s](%s)".formatted(value, foundryId2Link.get(key)));
      }
    }
    return content;
  }

  @NotNull
  private String FixRolls(String content) {
    var pattern = Pattern.compile("\\[\\[/r ([^]]+)]]");
    var matcher = pattern.matcher(content);
    while (matcher.find()) {
      var toReplace = matcher.group();
      var key = matcher.group(1);

      content = content.replace(toReplace, key);
    }
    return content;
  }

  private void placeLights(Zone zone, JsonObject light, int number) {
    Token lightToken = new Token("light %d".formatted(number), lightSourceAsset.getMD5Key());
    lightToken.setLayer(Layer.OBJECT);
    lightToken.setVisible(false);
    lightToken.setSnapToGrid(false);
    lightToken.setSnapToScale(false);
    lightToken.setWidth(LIGHT_WIDTH);
    lightToken.setHeight(LIGHT_HEIGHT);
    lightToken.setX(light.get("x").getAsInt() - xOffset - LIGHT_WIDTH / 2);
    lightToken.setY(light.get("y").getAsInt() - yOffset - LIGHT_HEIGHT / 2);
    lightToken.setGMNotes(light.toString());
    zone.putToken(lightToken);
  }

  private Path2D getVBLPath(JsonArray vblArray) {
    boolean first = true;
    Path2D path = new GeneralPath();
    for (int i = 0; i < vblArray.size(); i += 2) {
      int x = vblArray.get(i).getAsInt() - xOffset;
      int y = vblArray.get(i + 1).getAsInt() - yOffset;
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
