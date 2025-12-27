package modhider.mixins;

import modhider.network.HandshakeModListFilterHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class MixinNetworkManager {

    @Inject(method = "channelActive", at = @At("TAIL"), remap = false)
    private void modhider$channelActive(ChannelHandlerContext context, CallbackInfo callback) {
        ChannelPipeline pipeline = context.pipeline();
        ChannelHandler existing = pipeline.get(HandshakeModListFilterHandler.HANDLER_NAME);
        if (existing != null) {
            pipeline.remove(existing);
        }

        ChannelHandler handler = existing != null ? existing : new HandshakeModListFilterHandler();
        if (pipeline.get("encoder") != null) {
            pipeline.addAfter("encoder", HandshakeModListFilterHandler.HANDLER_NAME, handler);
            return;
        }
        if (pipeline.get("packet_handler") != null) {
            pipeline.addBefore("packet_handler", HandshakeModListFilterHandler.HANDLER_NAME, handler);
            return;
        }
        pipeline.addLast(HandshakeModListFilterHandler.HANDLER_NAME, handler);
    }

}
