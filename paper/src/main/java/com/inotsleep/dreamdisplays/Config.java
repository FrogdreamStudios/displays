package com.inotsleep.dreamdisplays;

import me.inotsleep.utils.config.AbstractConfig;
import me.inotsleep.utils.config.Comment;
import me.inotsleep.utils.config.Path;
import me.inotsleep.utils.config.SerializableObject;
import me.inotsleep.utils.storage.StorageSettings;
import org.bukkit.Material;

import java.util.List;

public class Config extends AbstractConfig {
    @Path("settings")
    public Settings settings = new Settings();

    @Path("messages")
    public Messages messages = new Messages();

    @Path("storage")
    public StorageSettings storageSettings = new StorageSettings();

    @Path("permissions")
    public Permissions permissions = new Permissions();

    public static class Permissions extends SerializableObject {
        @Path("premium")
        @Comment("Permission for premium users")
        public String premiumPermission = "group.premium";

        @Path("delete")
        @Comment("Permission to delete any display (including other players' displays)")
        public String deletePermission = "dreamdisplays.delete";

        @Path("list")
        @Comment("Permission to list all displays")
        public String listPermission = "dreamdisplays.list";

        @Path("reload")
        @Comment("Permission to reload the config")
        public String reloadPermission = "dreamdisplays.reload";
    }

    public static class Settings extends SerializableObject {

        @Path("webhook_url")
        @Comment("Discord webhook URL for reports (leave empty to disable)")
        public String webhookUrl = "";

        @Comment("Time between reports in milliseconds (default: 15000 ms = 15 seconds)")
        @Path("report_cooldown")
        public int reportCooldown = 15000;

        @Path("repo_name")
        @Comment("GitHub repository name for mod updates")
        public String repoName = "dreamdisplays";

        @Path("repo_owner")
        @Comment("GitHub repository owner for mod updates")
        public String repoOwner = "arsmotorin";

        @Path("selection_material")
        @Comment("Item for selecting display regions (default: DIAMOND_AXE)")
        private String selectionMaterialR = "DIAMOND_AXE";
        public Material selectionMaterial = Material.DIAMOND_AXE;

        @Path("base_material")
        @Comment("Block material for displays (default: BLACK_CONCRETE, recommended)")
        private String baseMaterialR = "BLACK_CONCRETE";
        public Material baseMaterial = Material.BLACK_CONCRETE;

        @Path("CUI_particle_render_delay")
        @Comment("Particle render delay in ticks (default: 2, disabled on Folia)")
        public int particleRenderDelay = 2;

        @Path("CUI_particles_per_block")
        @Comment("Number of particles per block edge (Default: 3, range: 1-10)")
        public int particlesPerBlock = 3;

        @Path("CUI_particles_color")
        @Comment("Particle color in hex (default: 0x00FF00 = green)")
        public int particlesColor = 0x00FF00;

        @Path("min_width")
        @Comment("Minimum display width in blocks")
        public int minWidth = 1;

        @Path("min_height")
        @Comment("Minimum display height in blocks")
        public int minHeight = 1;

        @Path("max_width")
        @Comment("Maximum display width in blocks (warning: large displays may lag)")
        public int maxWidth = 32;

        @Path("max_height")
        @Comment("Maximum display height in blocks (warning: large displays may lag)")
        public int maxHeight = 24;

        @Path("maxRenderDistance")
        @Comment("Max distance to send displays to players in blocks")
        public double maxRenderDistance = 96;

        public void mutateDeserialization() {
            selectionMaterial = Material.matchMaterial(selectionMaterialR);
            baseMaterial = Material.matchMaterial(baseMaterialR);
        }

        public void mutateSerialization() {
            selectionMaterialR = selectionMaterial.toString();
            baseMaterialR = baseMaterial.toString();
        }
    }

    public static class Messages extends SerializableObject {
        @Path("no_display_territories")
        @Comment("Message when no points selected (supports color codes: &a = green, &c = red)")
        public String noDisplayTerritories = "&7D |&f You haven't selected any display territories yet! Please select two points with a diamond axe";

        @Path("create_display_command")
        @Comment("Message shown after selecting two valid points")
        public String createDisplayCommand = "&7D |&f Now type /display create to create a display";

        @Path("second_point_not_selected")
        @Comment("Message when only one point is selected")
        public String secondPointNotSelected = "&7D |&f You haven't selected the second point yet!";

        @Path("display_overlap")
        @Comment("Message when selected area overlaps with existing display")
        public String displayOverlap = "&7D |&f There is already a display in this area!";

        @Path("structure_too_large")
        @Comment("Message when selection exceeds max_width or max_height")
        public String structureTooLarge = "&7D |&f You can't create a display larger than 32x24 blocks";

        @Path("structure_too_small")
        @Comment("Message when selection is smaller than min_width or min_height")
        public String structureTooSmall = "&7D |&f You can't create a display smaller than 1x1 blocks";

        @Path("first_point_selected")
        @Comment("Message when first point is selected (left-click)")
        public String firstPointSelected = "&7D |&f First point selected! Now select the second point";

        @Path("second_point_selected")
        @Comment("Message when second point is selected (right-click)")
        public String secondPointSelected = "&7D |&f Second point selected!";

        @Path("structure_wrong_depth")
        @Comment("Message when selection is not flat (must be 1 block deep)")
        public String structureWrongDepth = "&7D |&f Selection must be at least 1 block long";

        @Path("structure_wrong_structure")
        @Comment("Message when selected area contains wrong blocks (must be all base_material)")
        public String wrongStructure = "&7D |&f Only black concrete blocks can be used for displays";

        @Path("selection_clear")
        @Comment("Message when selection is cleared (Shift + Right-click)")
        public String selectionClear = "&7D |&f Selection has been cleared!";

        @Path("display_command_help")
        @Comment("Help message for incorrect command usage")
        public List<String> displayCommandHelp = List.of(
                "&7D |&f You entered something wrong!"
        );

        @Path("successfulCreation")
        @Comment("Message when display is successfully created")
        public String successfulCreation = "&7D |&f Display has been created! Type /display video <youtube link> [language] to set a video";

        @Path("noDisplayTargeted")
        @Comment("Message when no display is found")
        public String noDisplay = "&7D |&f Display has not been found!";

        @Path("settedURL")
        @Comment("Message when video URL is set successfully")
        public String settedURL = "&7D |&f Video has been loaded!";

        @Path("invalidURL")
        @Comment("Message when YouTube URL is invalid")
        public String invalidURL = "&7D |&f Your URL is invalid! Please use a valid YouTube link";

        @Path("report_too_quickly")
        @Comment("Message when reporting too frequently (cooldown: settings.report_cooldown)")
        public String reportTooQuickly = "&7D |&f You are sending reports too quickly! Please wait  before sending another report";

        @Path("report_sent")
        @Comment("Message when report is sent successfully (requires webhook_url)")
        public String reportSent = "&7D |&f Report has been sent!";

        @Path("new_version")
        @Comment("Message for new version available (%s = version number)")
        public String newVersion = "&7D |&f New version of Dream Displays (%s)! Please update it!";

        @Path("usage_header")
        @Comment("Header for /display usage command ({0} = loaded displays, {1} = max displays)")
        public List<String> usageHeader = List.of(
                "&a-== Dream Displays ==-",
                "&aInstalled: &f{0}&7/&f{1}",
                "&a-== Versions ==-"
        );

        @Path("usage_version_entry")
        @Comment("Version entry format ({0} = type, {1} = version number)")
        public String usageVersionEntry = " &7- &f{0}&a: &f{1}";

        @Path("usage_footer")
        @Comment("Footer for /display usage command")
        public String usageFooter = "&a-====== ======-";

        @Path("config_reloaded")
        @Comment("Message when config is reloaded")
        public String configReloaded = "&7D | &fDream Displays has been reloaded!";

        @Path("no_displays_found")
        @Comment("Message when no displays found")
        public String noDisplaysFound = "&7D |&f No displays found";

        @Path("display_list_header")
        @Comment("Header for display list")
        public String displayListHeader = "&7D |&f Displays:";

        @Path("display_list_entry")
        @Comment("Format for display list entry ({0} = ID, {1} = Owner, {2} = X, {3} = Y, {4} = Z, {5} = URL)")
        public String displayListEntry = "&7D |&f - ID: {0}, Owner: {1}, Location: {2}, {3}, {4}, URL: {5}";
    }

    public Config(DreamDisplaysPlugin plugin) {
        super(plugin.getDataFolder(), "config.yml");
    }

    public void mutateDeserialization() {
    }

    public void mutateSerialization() {
    }
}
