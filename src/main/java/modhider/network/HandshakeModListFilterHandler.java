package modhider.network;

import modhider.config.ConfigManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class HandshakeModListFilterHandler extends ChannelOutboundHandlerAdapter {
    public static final String HANDLER_NAME = "modhider:handshake_filter";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String HANDSHAKE_CHANNEL = "FML|HS";
    // Discriminator values are defined in FMLHandshakeCodec (1.8.9).
    private static final byte MODLIST_DISCRIMINATOR = 2;
    private static final String FILTER_FAILURE_MESSAGE =
            "Mod list filtering failed. Connection closed to prevent mod list leakage.";

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof FMLProxyPacket) {
            handleProxyPacket(ctx, (FMLProxyPacket) msg, promise);
            return;
        }

        if (msg instanceof C17PacketCustomPayload) {
            handleCustomPayload(ctx, (C17PacketCustomPayload) msg, promise);
            return;
        }

        ctx.write(msg, promise);
    }

    private void handleProxyPacket(ChannelHandlerContext ctx, FMLProxyPacket packet, ChannelPromise promise) {
        if (!HANDSHAKE_CHANNEL.equals(packet.channel())) {
            ctx.write(packet, promise);
            return;
        }

        if (!shouldFilter()) {
            ctx.write(packet, promise);
            return;
        }

        try {
            filterModList(packet.payload());
        } catch (RuntimeException e) {
            rejectHandshake(ctx, promise, packet, e);
            return;
        }
        ctx.write(packet, promise);
    }

    private void handleCustomPayload(ChannelHandlerContext ctx, C17PacketCustomPayload packet, ChannelPromise promise) {
        if (!HANDSHAKE_CHANNEL.equals(packet.getChannelName())) {
            ctx.write(packet, promise);
            return;
        }

        if (!shouldFilter()) {
            ctx.write(packet, promise);
            return;
        }

        try {
            filterModList(packet.getBufferData());
        } catch (RuntimeException e) {
            rejectHandshake(ctx, promise, packet, e);
            return;
        }
        ctx.write(packet, promise);
    }

    private boolean shouldFilter() {
        return ConfigManager.instance.isFilterEnabled() && !Minecraft.getMinecraft().isSingleplayer();
    }

    private boolean filterModList(ByteBuf payload) {
        ByteBuf read = payload.duplicate();
        read.readerIndex(0);
        if (read.readableBytes() < 1) {
            throw new IllegalStateException("Handshake ModList payload missing discriminator.");
        }

        byte discriminator = read.readByte();
        if (discriminator != MODLIST_DISCRIMINATOR) {
            return false;
        }

        int count = ByteBufUtils.readVarInt(read, 2);
        List<ModEntry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String modId = ByteBufUtils.readUTF8String(read);
            String version = ByteBufUtils.readUTF8String(read);
            entries.add(new ModEntry(modId, version));
        }

        if (read.isReadable()) {
            throw new IllegalStateException("Handshake ModList payload has trailing data.");
        }

        List<ModEntry> filtered = filterEntries(entries);
        if (filtered.size() == entries.size()) {
            LOGGER.debug("FML handshake ModList filter had no effect ({} entries).", entries.size());
            return false;
        }

        ByteBuf rebuilt = Unpooled.buffer();
        try {
            rebuilt.writeByte(discriminator);
            ByteBufUtils.writeVarInt(rebuilt, filtered.size(), 2);
            for (ModEntry entry : filtered) {
                ByteBufUtils.writeUTF8String(rebuilt, entry.modId);
                ByteBufUtils.writeUTF8String(rebuilt, entry.version);
            }

            payload.clear();
            payload.writeBytes(rebuilt);
        } finally {
            ReferenceCountUtil.release(rebuilt);
        }

        LOGGER.info(
                "Filtered {} mod(s) from FML handshake mod list.",
                entries.size() - filtered.size()
        );
        return true;
    }

    private void rejectHandshake(
            ChannelHandlerContext ctx,
            ChannelPromise promise,
            Object packet,
            RuntimeException error
    ) {
        LOGGER.error(
                "Failed to filter FML handshake mod list; closing connection to prevent leakage.",
                error
        );
        ReferenceCountUtil.release(packet);
        ChannelHandler handler = ctx.pipeline().get("packet_handler");
        if (handler instanceof NetworkManager) {
            ((NetworkManager) handler).closeChannel(new ChatComponentText(FILTER_FAILURE_MESSAGE));
        } else {
            ctx.close();
        }
        promise.tryFailure(error);
    }

    private List<ModEntry> filterEntries(List<ModEntry> entries) {
        Set<String> whitelisted = normalize(ConfigManager.instance.getWhitelistedMods());

        List<ModEntry> filtered = new ArrayList<>(entries.size());
        for (ModEntry entry : entries) {
            String modIdKey = entry.modId.toLowerCase(Locale.ROOT);
            boolean keep = whitelisted.contains(modIdKey);
            if (keep) {
                filtered.add(entry);
            }
        }

        return filtered;
    }

    private Set<String> normalize(String[] mods) {
        if (mods == null || mods.length == 0) {
            return Collections.emptySet();
        }

        Set<String> set = new HashSet<>();
        for (String mod : mods) {
            if (mod == null) {
                continue;
            }
            String trimmed = mod.trim();
            if (!trimmed.isEmpty()) {
                set.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
        return set;
    }

    private static final class ModEntry {
        private final String modId;
        private final String version;

        private ModEntry(String modId, String version) {
            this.modId = modId;
            this.version = version;
        }
    }
}
