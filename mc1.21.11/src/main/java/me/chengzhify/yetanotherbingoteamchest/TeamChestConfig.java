package me.chengzhify.yetanotherbingoteamchest;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TeamChestConfig {

    private static final String FILE_NAME = "yetanotherbingo-teamchest.toml";
    private static final String TEMPLATE_NAME = "yetanotherbingo-teamchest-default.toml";
    private static final Pattern ROWS_PATTERN = Pattern.compile("^\\s*rows\\s*=\\s*(\\d+)\\s*(#.*)?$");

    private static final int DEFAULT_ROWS = 3;
    private static final int MIN_ROWS = 1;
    private static final int MAX_ROWS = 6;

    private static int rows = DEFAULT_ROWS;

    private TeamChestConfig() {}

    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);

        try {
            copyTemplateIfMissing(configPath);
            rows = parseRows(configPath);
        } catch (IOException e) {
            rows = DEFAULT_ROWS;
            System.err.println("[YetAnotherBingo-TeamChest] Failed to load config, using default rows=" + DEFAULT_ROWS);
            e.printStackTrace();
        }
    }

    public static int getRows() {
        return rows;
    }

    public static int getSize() {
        return rows * 9;
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

    private static int parseRows(Path configPath) throws IOException {
        List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
        for (String line : lines) {
            Matcher matcher = ROWS_PATTERN.matcher(line);
            if (matcher.matches()) {
                return clampRows(Integer.parseInt(matcher.group(1)));
            }
        }

        return DEFAULT_ROWS;
    }

    private static int clampRows(int value) {
        return Math.max(MIN_ROWS, Math.min(MAX_ROWS, value));
    }
}
