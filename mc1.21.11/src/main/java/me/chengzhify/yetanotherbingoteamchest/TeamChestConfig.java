package me.chengzhify.yetanotherbingoteamchest;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

public final class TeamChestConfig {

    private static final String FILE_NAME = "yetanotherbingo-teamchest.toml";
    private static final String TEMPLATE_NAME = "yetanotherbingo-teamchest-default.toml";
    private static final String SECTION_TEAM_CHEST = "team_chest";
    private static final String SECTION_TEAM_TELEPORT = "team_teleport";

    private static final int DEFAULT_ROWS = 3;
    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 6;
    private static final boolean DEFAULT_TEAM_CHEST_ENABLED = true;
    private static final boolean DEFAULT_TEAM_TELEPORT_ENABLED = true;

    private static int rows = DEFAULT_ROWS;
    private static boolean teamChestEnabled = DEFAULT_TEAM_CHEST_ENABLED;
    private static boolean teamTeleportEnabled = DEFAULT_TEAM_TELEPORT_ENABLED;

    private TeamChestConfig() {}

    public static synchronized void load() {
        Path configPath = getConfigPath();

        try {
            copyTemplateIfMissing(configPath);
            parseConfig(configPath);
        } catch (IOException e) {
            rows = DEFAULT_ROWS;
            teamChestEnabled = DEFAULT_TEAM_CHEST_ENABLED;
            teamTeleportEnabled = DEFAULT_TEAM_TELEPORT_ENABLED;
            System.err.println("[YetAnotherBingo-TeamChest] Failed to load config, using default values.");
            e.printStackTrace();
        }
    }

    public static synchronized int getRows() {
        return rows;
    }

    public static synchronized int getSize() {
        return rows * 9;
    }

    public static synchronized boolean isTeamChestEnabled() {
        return teamChestEnabled;
    }

    public static synchronized boolean isTeamTeleportEnabled() {
        return teamTeleportEnabled;
    }

    public static synchronized void persistToggleStates(boolean teamChestEnabled, boolean teamTeleportEnabled) {
        TeamChestConfig.teamChestEnabled = teamChestEnabled;
        TeamChestConfig.teamTeleportEnabled = teamTeleportEnabled;

        try {
            writeConfig(getConfigPath());
        } catch (IOException e) {
            System.err.println("[YetAnotherBingo-TeamChest] Failed to persist toggle config.");
            e.printStackTrace();
        }
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
    }

    private static void copyTemplateIfMissing(Path configPath) throws IOException {
        if (Files.exists(configPath)) {
            return;
        }

        Files.createDirectories(configPath.getParent());
        try (InputStream input = TeamChestConfig.class.getClassLoader().getResourceAsStream(TEMPLATE_NAME)) {
            if (input == null) {
                throw new IOException("Missing config template resource: " + TEMPLATE_NAME);
            }
            Files.copy(input, configPath);
        }
    }

    private static void parseConfig(Path configPath) throws IOException {
        List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
        int parsedRows = DEFAULT_ROWS;
        boolean parsedTeamChestEnabled = DEFAULT_TEAM_CHEST_ENABLED;
        boolean parsedTeamTeleportEnabled = DEFAULT_TEAM_TELEPORT_ENABLED;
        String section = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                section = trimmed.substring(1, trimmed.length() - 1).trim();
                continue;
            }

            String content = stripInlineComment(trimmed);
            int eqIndex = content.indexOf('=');
            if (eqIndex <= 0) {
                continue;
            }

            String key = content.substring(0, eqIndex).trim();
            String value = content.substring(eqIndex + 1).trim();

            if (SECTION_TEAM_CHEST.equals(section)) {
                if ("rows".equals(key)) {
                    try {
                        parsedRows = Integer.parseInt(value);
                    } catch (NumberFormatException ignored) {
                        parsedRows = DEFAULT_ROWS;
                    }
                } else if ("enabled".equals(key)) {
                    parsedTeamChestEnabled = parseBoolean(value, DEFAULT_TEAM_CHEST_ENABLED);
                }
            } else if (SECTION_TEAM_TELEPORT.equals(section) && "enabled".equals(key)) {
                parsedTeamTeleportEnabled = parseBoolean(value, DEFAULT_TEAM_TELEPORT_ENABLED);
            }
        }

        rows = clampRows(parsedRows);
        teamChestEnabled = parsedTeamChestEnabled;
        teamTeleportEnabled = parsedTeamTeleportEnabled;
    }

    private static String stripInlineComment(String line) {
        int index = line.indexOf('#');
        if (index < 0) {
            return line;
        }
        return line.substring(0, index).trim();
    }

    private static boolean parseBoolean(String value, boolean defaultValue) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return defaultValue;
    }

    private static void writeConfig(Path configPath) throws IOException {
        Files.createDirectories(configPath.getParent());
        String content = "# YetAnotherBingo-TeamChest configuration\n"
                + "\n"
                + "[team_chest]\n"
                + "# Team chest rows. Valid range: 1..6\n"
                + "rows = " + rows + "\n"
                + "# Whether /teamchest and /tc are enabled\n"
                + "enabled = " + teamChestEnabled + "\n"
                + "\n"
                + "[team_teleport]\n"
                + "# Whether /teamtp and /ttp are enabled\n"
                + "enabled = " + teamTeleportEnabled + "\n";

        Files.writeString(
                configPath,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );
    }

    private static int clampRows(int value) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, value));
    }
}
