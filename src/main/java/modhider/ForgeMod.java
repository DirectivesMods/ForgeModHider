package modhider;

import modhider.command.CommandListener;
import modhider.config.ConfigManager;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = "modhider", name = "ForgeModHider", version = "1.2", guiFactory = "modhider.config.GuiFactory")
public class ForgeMod {
    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent event) {
        ConfigManager.instance.ensureConfigExists();
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        if (event.getSide() != Side.CLIENT) {
            return;
        }
        ClientCommandHandler.instance.registerCommand(new CommandListener());
    }
}
