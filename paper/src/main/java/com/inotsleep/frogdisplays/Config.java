package com.inotsleep.frogdisplays;

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
        @Comment("Permission to delete displays")
        public String deletePermission = "frogdisplays.delete";

        @Path("list")
        @Comment("Permission for list the displays")
        public String listPermission = "frogdisplays.list";
    }

    public static class Settings extends SerializableObject {

        @Path("webhook_url")
        public String webhookUrl = "";

        @Comment("Time between reports in milliseconds")
        @Path("report_cooldown")
        public int reportCooldown = 15000;

        @Path("repo_name")
        public String repoName = "frogdisplayswiki";

        @Path("repo_owner")
        public String repoOwner = "Frogdream";

        @Path("selection_material")
        @Comment("Предмет для выделения")
        private String selectionMaterialR = "DIAMOND_AXE";
        public Material selectionMaterial = Material.DIAMOND_AXE;

        @Path("base_material")
        @Comment("Предмет для выделения")
        private String baseMaterialR = "BLACK_CONCRETE";
        public Material baseMaterial = Material.BLACK_CONCRETE;

        @Path("CUI_particle_render_delay")
        @Comment("Как часто показывать частицы (тики)")
        public int particleRenderDelay = 2;

        @Path("CUI_particles_per_block")
        @Comment("Количество частиц на блок")
        public int particlesPerBlock = 3;

        @Path("CUI_particles_color")
        @Comment("Указывать в Decimal формате")
        public int particlesColor = 0x00FF00;

        @Path("min_width")
        public int minWidth = 1;

        @Path("min_height")
        public int minHeight = 1;

        @Path("max_width")
        public int maxWidth = 32;

        @Path("max_height")
        public int maxHeight = 24;

        @Path("maxRenderDistance")
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
        public String noDisplayTerritories = "&7D |&f You haven't selected any display territories yet! Please select two points with a diamond axe";

        @Path("create_display_command")
        public String createDisplayCommand = "&7D |&f Now type /display create to create a display";

        @Path("second_point_not_selected")
        public String secondPointNotSelected = "&7D |&f You haven't selected the second point yet!";

        @Path("display_overlap")
        public String displayOverlap = "&7D |&f There is already a display in this area!";

        @Path("structure_too_large")
        public String structureTooLarge = "&7D |&f You can't create a display larger than 32x24 blocks";

        @Path("structure_too_small")
        public String structureTooSmall = "&7D |&f You can't create a display smaller than 1x1 blocks";

        @Path("first_point_selected")
        public String firstPointSelected = "&7D |&f First point selected! Now select the second point";

        @Path("second_point_selected")
        public String secondPointSelected = "&7D |&f Second point selected!";

        @Path("structure_wrong_depth")
        public String structureWrongDepth = "&7D |&f Selection must be at least 1 block long";

        @Path("structure_wrong_structure")
        public String wrongStructure = "&7D |&f Only black concrete blocks can be used for displays";

        @Path("selection_clear")
        public String selectionClear = "&7D |&f Selection has been cleared!";

        @Path("display_command_help")
        public List<String> displayCommandHelp = List.of(
                "&7D |&f You entered something wrong!"
        );

        @Path("successfulCreation")
        public String successfulCreation = "&7D |&f Display has been created! Type /display video <youtube link> [language] to set a video";

        @Path("noDisplayTargeted")
        public String noDisplay = "&7D |&f Display has not been found!";

        @Path("settedURL")
        public String settedURL = "&7D |&f Video has been loaded!";

        @Path("invalidURL")
        public String invalidURL = "&7D |&f Your URL is invalid! Please use a valid YouTube link";

        @Path("report_too_quickly")
        public String reportTooQuickly = "&7D |&f You are sending reports too quickly! Please wait  before sending another report";

        @Path("report_sent")
        public String reportSent = "&7D |&f Report has been sent!";

        @Path("new_version")
        public String newVersion = "&7D |&f New version of Frog Displays (%s)! Please update it!";

        @Path("usage_header")
        public List<String> usageHeader = List.of(
                "&a-== Frog Displays ==-",
                "&aInstalled: &f{0}&7/&f{1}",
                "&a-== Versions ==-"
        );

        @Path("usage_version_entry")
        public String usageVersionEntry = " &7- &f{0}&a: &f{1}";

        @Path("usage_footer")
        public String usageFooter = "&a-====== ======-";
    }

    public Config(FrogdisplaysPlugin plugin) {
        super(plugin.getDataFolder(), "config.yml");
    }

    public void mutateDeserialization() {
    }

    public void mutateSerialization() {
    }
}