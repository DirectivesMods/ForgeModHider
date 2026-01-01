# ForgeModHider

ForgeModHider is a Forge 1.8.9 mod that hides your mod list during the FML handshake.

By default, all mods are hidden (except `FML`, `Forge`, `mcp`) and it is the responsibility of the user to manually whitelist the mods they want.

<img width="1161" height="692" alt="2025-12-27_00 29 52" src="https://github.com/user-attachments/assets/96f9f9bd-fbad-4abe-809c-4788324c95a6" />

---

## Instructions:
1. Drop the jar in your `mods` folder.
2. Open the config GUI:
   - `Mods` -> `ForgeModHider` -> `Config`
   - Or run `/forgemodhider` (alias `/fmh`)
3. Toggle visibility per mod and click `Save`.

> [!CAUTION]
> It is highly not recommended to hide `FML`, `Forge`, or `mcp` because these mods come with Forge by default. Additionally, it is highly not recommended to show `modhider` because that defeats the point of hiding your mods in the first place. Servers can flag this behavior and will likely punish you.

---

## Why this mod is better than others:
Most mod hiders patch `FMLHandshakeMessage.ModList` with mixins, which is prone to conflicts or overrides.

ForgeModHider avoids this problem entirely by running late at the Netty pipeline level, so it does not compete for the ModList mixin slot.

If filtering fails, the connection is closed to avoid leaking the full list.

---

## Potential conflicts:
If you have another mod that hides things, those items will stay hidden alongside ForgeModHider’s filtering (not really a conflict but still good to note).

Mods that aggressively reorder or replace NetworkManager handlers could still interfere, though this is very rare.

---

## Limitations (by design):
This mod only hides the FML handshake mod list. It does not hide: custom payloads sent by other mods, plugin channel registrations, or other behavioral fingerprints.

If another mod is somehow self-identifying, that is the mod's responsibility, not the hider's.
