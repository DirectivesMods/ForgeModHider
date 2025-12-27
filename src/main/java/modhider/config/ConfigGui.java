package modhider.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ConfigGui extends GuiScreen {

    private static class Mod {

        private String modId;
        private boolean whitelisted = false;

        public Mod(String modId, boolean whitelisted) {
            this.modId = modId;
            this.whitelisted = whitelisted;
        }

        public String getModId() {
            return modId;
        }

        public boolean isWhitelisted() {
            return whitelisted;
        }

        public Mod setWhitelisted(boolean whitelisted) {
            this.whitelisted = whitelisted;
            return this;
        }
    }

    private static final int ID_TOGGLE_FILTER = 1;
    private static final int ID_DONE = 2;
    private static final int ID_SAVE = 3;
    private static final int ID_MOD_BASE = 1000;
    private static final int LIST_MARGIN = 20;
    private static final int BUTTON_Y_OFFSET = 4;
    private static final int BUTTON_SPACING = 4;
    private static final int LIST_EDGE_SPACING = 6;
    private static final int ACTION_BUTTON_HEIGHT = 20;
    private static final int MAX_ACTION_BUTTON_WIDTH = 120;
    private static final int WARNING_DURATION_TICKS = 200;
    private static final int WARNING_PADDING = 4;
    private static final String MODHIDER_MOD_ID = "modhider";
    private static final Set<String> ESSENTIAL_MODS =
            new HashSet<>(Arrays.asList("fml", "mcp", "forge"));
    private static final String ESSENTIAL_WARNING =
            EnumChatFormatting.RED.toString()
                    + EnumChatFormatting.BOLD
                    + "WARNING: Removing FML/mcp/Forge from the whitelist can be detected by servers and is highly not recommended."
                    + EnumChatFormatting.RESET;
    private static final String MODHIDER_WARNING =
            EnumChatFormatting.RED.toString()
                    + EnumChatFormatting.BOLD
                    + "WARNING: Whitelisting modhider can expose it to servers and is highly not recommended."
                    + EnumChatFormatting.RESET;
    private static final String FILTER_DISABLED_WARNING =
            EnumChatFormatting.YELLOW.toString()
                    + EnumChatFormatting.BOLD
                    + "CAUTION: Disabling the mod means servers can see your full mods list."
                    + EnumChatFormatting.RESET;

    private final GuiScreen parent;
    private final List<Mod> mods = new ArrayList<>();
    private final Set<String> initialWhitelistedMods = new LinkedHashSet<>();
    private int scroll = 0;
    private int rowsPerPage = 0;
    private boolean filterEnabled = false;
    private boolean initialFilterEnabled = false;
    private boolean dirty = false;
    private int warningTicks = 0;
    private String warningMessage = null;

    public ConfigGui(GuiScreen parent) {
        this.parent = parent;
        initialFilterEnabled = ConfigManager.instance.isFilterEnabled();
        filterEnabled = initialFilterEnabled;
        initialWhitelistedMods.addAll(Arrays.asList(ConfigManager.instance.getWhitelistedMods()));
        for (ModContainer modContainer : Loader.instance().getActiveModList()) {
            mods.add(new Mod(modContainer.getModId(), initialWhitelistedMods.contains(modContainer.getModId())));
        }
        updateDirtyFlag();
    }

    @Override
    public void initGui() {
        super.initGui();
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.buttonList.clear();
        ScaledResolution sr = new ScaledResolution(mc);
        int width = sr.getScaledWidth();
        int height = sr.getScaledHeight();
        int listWidth = Math.min(width - LIST_MARGIN * 2, 200);
        int listLeft = width / 2 - listWidth / 2;
        int warningHeight = getWarningHeight(listWidth);
        int topButtonY = 25 + BUTTON_Y_OFFSET + warningHeight;
        int actionButtonsY = height - 25 + BUTTON_Y_OFFSET - 6;
        int listBottom = actionButtonsY - LIST_EDGE_SPACING;
        int rowHeight = 20;
        int rowSpacing = 2;
        int listTop = topButtonY + ACTION_BUTTON_HEIGHT + LIST_EDGE_SPACING;
        int available = listBottom - listTop;
        rowsPerPage = Math.max(1, available / (rowHeight + rowSpacing));
        int maxScroll = Math.max(0, mods.size() - rowsPerPage);
        if (scroll > maxScroll) {
            scroll = maxScroll;
        }

        this.buttonList.add(new GuiButton(
                ID_TOGGLE_FILTER,
                listLeft,
                topButtonY,
                listWidth,
                ACTION_BUTTON_HEIGHT,
                getFilterLabel()
        ));

        int bottomButtonWidth = Math.min((listWidth - BUTTON_SPACING) / 2, MAX_ACTION_BUTTON_WIDTH);
        int bottomRowWidth = bottomButtonWidth * 2 + BUTTON_SPACING;
        int bottomLeft = listLeft + (listWidth - bottomRowWidth) / 2;
        this.buttonList.add(new GuiButton(
                ID_SAVE,
                bottomLeft,
                actionButtonsY,
                bottomButtonWidth,
                ACTION_BUTTON_HEIGHT,
                "Save"
        ));
        this.buttonList.add(new GuiButton(
                ID_DONE,
                bottomLeft + bottomButtonWidth + BUTTON_SPACING,
                actionButtonsY,
                bottomButtonWidth,
                ACTION_BUTTON_HEIGHT,
                "Done"
        ));

        int y = listTop;
        int end = Math.min(mods.size(), scroll + rowsPerPage);
        for (int index = scroll; index < end; index++) {
            Mod mod = mods.get(index);
            this.buttonList.add(new GuiButton(ID_MOD_BASE + index, listLeft, y, listWidth, rowHeight, getModLabel(mod)));
            y += rowHeight + rowSpacing;
        }
    }

    private String getFilterLabel() {
        String status = filterEnabled ? EnumChatFormatting.GREEN + "ENABLED" : EnumChatFormatting.RED + "DISABLED";
        return "Mod: " + status + EnumChatFormatting.RESET;
    }

    private String getModLabel(Mod mod) {
        String status = mod.isWhitelisted() ? EnumChatFormatting.GREEN + "VISIBLE" : EnumChatFormatting.RED + "HIDDEN";
        return mod.getModId() + " - " + status + EnumChatFormatting.RESET;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float p_drawScreen_3_) {
        ScaledResolution sr = new ScaledResolution(mc);
        drawDefaultBackground();

        String title = "ForgeModHider";
        fontRendererObj.drawStringWithShadow(
                title,
                sr.getScaledWidth() / 2f - fontRendererObj.getStringWidth(title) / 2f,
                10,
                0xFFFFFF
        );

        int listWidth = Math.min(sr.getScaledWidth() - LIST_MARGIN * 2, 200);
        int listLeft = sr.getScaledWidth() / 2 - listWidth / 2;
        drawWarning(listLeft, listWidth, 25 + BUTTON_Y_OFFSET);

        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int previous = scroll;
            if (dWheel < 0) {
                scroll++;
            }
            if (dWheel > 0) {
                scroll--;
            }
            if (scroll < 0) scroll = 0;
            int maxScroll = Math.max(0, mods.size() - rowsPerPage);
            if (scroll > maxScroll) scroll = maxScroll;
            if (previous != scroll) {
                rebuildButtons();
            }
        }

        super.drawScreen(mouseX, mouseY, p_drawScreen_3_);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == ID_TOGGLE_FILTER) {
            filterEnabled = !filterEnabled;
            if (!filterEnabled) {
                triggerWarning(FILTER_DISABLED_WARNING);
            }
            updateDirtyFlag();
            rebuildButtons();
            return;
        }

        if (button.id == ID_SAVE) {
            saveChanges();
            return;
        }

        if (button.id == ID_DONE) {
            mc.displayGuiScreen(parent);
            return;
        }

        if (button.id >= ID_MOD_BASE) {
            int index = button.id - ID_MOD_BASE;
            if (index >= 0 && index < mods.size()) {
                Mod mod = mods.get(index);
                boolean wasWhitelisted = mod.isWhitelisted();
                mod.setWhitelisted(!wasWhitelisted);
                if (wasWhitelisted && !mod.isWhitelisted() && isEssentialMod(mod.getModId())) {
                    triggerEssentialWarning();
                } else if (!wasWhitelisted && mod.isWhitelisted() && isModhider(mod.getModId())) {
                    triggerModhiderWarning();
                }
                updateDirtyFlag();
                rebuildButtons();
            }
        }
    }

    private void updateDirtyFlag() {
        boolean changed = filterEnabled != initialFilterEnabled;
        if (!changed) {
            for (Mod mod : mods) {
                boolean initialWhitelisted = initialWhitelistedMods.contains(mod.getModId());
                if (mod.isWhitelisted() != initialWhitelisted) {
                    changed = true;
                    break;
                }
            }
        }
        dirty = changed;
    }

    private void saveChanges() {
        if (!dirty) {
            return;
        }

        if (filterEnabled != initialFilterEnabled) {
            ConfigManager.instance.setFilterEnabled(filterEnabled);
            initialFilterEnabled = filterEnabled;
        }

        Set<String> newWhitelistedMods = new LinkedHashSet<>(initialWhitelistedMods);
        for (Mod mod : mods) {
            if (mod.isWhitelisted()) {
                newWhitelistedMods.add(mod.getModId());
            } else {
                newWhitelistedMods.remove(mod.getModId());
            }
        }
        if (!newWhitelistedMods.equals(initialWhitelistedMods)) {
            ConfigManager.instance.setWhitelistedMods(newWhitelistedMods.toArray(new String[0]));
            initialWhitelistedMods.clear();
            initialWhitelistedMods.addAll(newWhitelistedMods);
        }

        updateDirtyFlag();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (warningTicks > 0) {
            warningTicks--;
            if (warningTicks == 0) {
                warningMessage = null;
                rebuildButtons();
            }
        }
    }

    private void triggerEssentialWarning() {
        triggerWarning(ESSENTIAL_WARNING);
    }

    private void triggerModhiderWarning() {
        triggerWarning(MODHIDER_WARNING);
    }

    private void triggerWarning(String message) {
        warningMessage = message;
        warningTicks = WARNING_DURATION_TICKS;
        rebuildButtons();
    }

    private boolean isEssentialMod(String modId) {
        if (modId == null) {
            return false;
        }
        String modIdKey = modId.toLowerCase(Locale.ROOT);
        return ESSENTIAL_MODS.contains(modIdKey);
    }

    private boolean isModhider(String modId) {
        if (modId == null) {
            return false;
        }
        return MODHIDER_MOD_ID.equals(modId.toLowerCase(Locale.ROOT));
    }

    private int getWarningHeight(int maxWidth) {
        if (warningTicks <= 0 || warningMessage == null) {
            return 0;
        }
        List<String> lines = fontRendererObj.listFormattedStringToWidth(warningMessage, maxWidth);
        return lines.size() * fontRendererObj.FONT_HEIGHT + WARNING_PADDING;
    }

    private void drawWarning(int listLeft, int listWidth, int y) {
        if (warningTicks <= 0 || warningMessage == null) {
            return;
        }
        List<String> lines = fontRendererObj.listFormattedStringToWidth(warningMessage, listWidth);
        int textY = y;
        for (String line : lines) {
            drawCenteredString(fontRendererObj, line, listLeft + listWidth / 2, textY, 0xFFFFFF);
            textY += fontRendererObj.FONT_HEIGHT;
        }
    }

}
