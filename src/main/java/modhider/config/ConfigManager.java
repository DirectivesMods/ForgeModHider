package modhider.config;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.Loader;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    public static final ConfigManager instance = new ConfigManager();
    private static final String CATEGORY = "Mod Visibility";
    private static final String[] DEFAULT_WHITELISTED_MODS = new String[]{"FML", "mcp", "Forge"};
    private static final String WHITELISTED_KEY = "whitelisted_mods";
    private final File configFile;
    private final Configuration config;

    public ConfigManager() {
        this.configFile = new File(Loader.instance().getConfigDir(), "forgemodhider.cfg");
        this.config = new Configuration(configFile);

        config.load();
    }

    public void ensureConfigExists() {
        getWhitelistedMods();
        isFilterEnabled();
        if (!configFile.exists() || config.hasChanged()) {
            config.save();
        }
    }

    public String[] getWhitelistedMods() {
        Property property = config.get(
                CATEGORY,
                WHITELISTED_KEY,
                DEFAULT_WHITELISTED_MODS,
                "Whitelisted mods list. All other mods are hidden."
        );
        return property.getStringList();
    }

    public void setWhitelistedMods(String[] mods) {
        Property property = config.get(
                CATEGORY,
                WHITELISTED_KEY,
                DEFAULT_WHITELISTED_MODS,
                "Whitelisted mods list. All other mods are hidden."
        );
        property.set(mods);
        config.save();
    }

    public void addWhitelistedMod(String modId) {
        String[] whitelistedMods = getWhitelistedMods();
        whitelistedMods = Arrays.copyOf(whitelistedMods, whitelistedMods.length + 1);
        whitelistedMods[whitelistedMods.length - 1] = modId;
        setWhitelistedMods(whitelistedMods);
    }

    public void removeWhitelistedMod(String modId) {
        String[] whitelistedMods = getWhitelistedMods();
        List<String> list = Arrays.stream(whitelistedMods).collect(Collectors.toList());
        list.removeIf(next -> next.equals(modId));
        setWhitelistedMods(list.toArray(new String[0]));
    }

    public boolean isFilterEnabled() {
        Property property = config.get(CATEGORY, "enabled", true, "Enable mod hiding.");
        return property.getBoolean();
    }

    public void setFilterEnabled(boolean enabled) {
        Property property = config.get(CATEGORY, "enabled", true, "Enable mod hiding.");
        property.set(enabled);
        config.save();
    }

}
