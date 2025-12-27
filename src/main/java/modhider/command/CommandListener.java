package modhider.command;

import modhider.config.ConfigGui;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;

public class CommandListener extends CommandBase {
    @Override
    public String getCommandName() {
        return "forgemodhider";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/forgemodhider";
    }

    @Override
    public List<String> getCommandAliases() {
        return Collections.singletonList("fmh");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Minecraft mc = Minecraft.getMinecraft();
        MinecraftForge.EVENT_BUS.register(new Object() {
            @SubscribeEvent
            public void onClientTick(TickEvent.ClientTickEvent event) {
                if (event.phase != TickEvent.Phase.END) {
                    return;
                }
                MinecraftForge.EVENT_BUS.unregister(this);
                mc.displayGuiScreen(new ConfigGui(mc.currentScreen));
            }
        });
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
