package ru.meowy.serverboundresourcepack;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public final class ServerBoundResourcePackClient implements ClientModInitializer {
    private static final String MOD_ID = "serverboundresourcepack";
    private static final String PACK_ID = "meowy_pack";
    private static final String PACK_PROFILE_NAME = "fabric/" + PACK_ID;
    private static final Identifier PACK_IDENTIFIER = Identifier.fromNamespaceAndPath(MOD_ID, PACK_ID);
    private static final Set<String> ALLOWED_SERVERS = Set.of(
            "45.93.200.38:25585",
            "play.meowy.ru",
            "meow.hobaboba.ru",
            "m.minecraft.in.net",
            "ihave.13cm.online",
            "meow.ru-mc.ru",
            "51.38.155.200:29420"
    );

    @Override
    public void onInitializeClient() {
        ResourceManagerHelper.registerBuiltinResourcePack(
                PACK_IDENTIFIER,
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(),
                Component.literal("Meowy embedded resource pack"),
                ResourcePackActivationType.NORMAL
        );

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> execute(client, () -> setPackEnabled(client, isAllowedServer(client))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> execute(client, () -> setPackEnabled(client, false)));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> setPackEnabled(client, false));
    }

    private static boolean isAllowedServer(Object client) {
        Optional<Object> serverInfo = callNoArg(client, "getCurrentServer", "getCurrentServerEntry", "getCurrentServerData");
        Optional<String> address = serverInfo.flatMap(ServerBoundResourcePackClient::readAddress);
        if (address.isEmpty()) {
            return false;
        }

        String normalized = normalizeAddress(address.get());
        if (ALLOWED_SERVERS.contains(normalized)) {
            return true;
        }

        int separator = normalized.lastIndexOf(':');
        return separator > 0 && ALLOWED_SERVERS.contains(normalized.substring(0, separator));
    }

    private static Optional<String> readAddress(Object serverInfo) {
        for (String fieldName : List.of("address", "ip")) {
            try {
                Field field = serverInfo.getClass().getField(fieldName);
                Object value = field.get(serverInfo);
                if (value instanceof String address && !address.isBlank()) {
                    return Optional.of(address);
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next known field name.
            }
        }
        return Optional.empty();
    }

    private static String normalizeAddress(String address) {
        return address.trim().toLowerCase(Locale.ROOT).replaceAll("\\.$", "");
    }

    private static void setPackEnabled(Object client, boolean enabled) {
        Optional<Object> packRepository = callNoArg(client, "getResourcePackRepository", "getResourcePackManager", "getPackRepository");
        if (packRepository.isEmpty()) {
            return;
        }

        Optional<Collection<?>> selectedPacks = callNoArg(packRepository.get(), "getSelectedPacks", "getEnabledProfiles")
                .filter(Collection.class::isInstance)
                .map(Collection.class::cast);
        if (selectedPacks.isEmpty()) {
            return;
        }

        List<String> enabledProfiles = new ArrayList<>();
        for (Object pack : selectedPacks.get()) {
            callNoArg(pack, "getId", "getName").map(Object::toString).ifPresent(enabledProfiles::add);
        }

        boolean currentlyEnabled = enabledProfiles.contains(PACK_PROFILE_NAME);
        if (enabled == currentlyEnabled) {
            return;
        }

        if (enabled) {
            enabledProfiles.add(PACK_PROFILE_NAME);
        } else {
            enabledProfiles.remove(PACK_PROFILE_NAME);
        }

        if (call(packRepository.get(), List.of("setSelected", "setEnabledProfiles"), enabledProfiles).isPresent()) {
            callNoArg(client, "reloadResourcePacks", "reloadResources");
        }
    }

    private static void execute(Object client, Runnable runnable) {
        if (call(client, List.of("execute", "tell"), runnable).isEmpty()) {
            runnable.run();
        }
    }

    private static Optional<Object> callNoArg(Object target, String... methodNames) {
        return call(target, List.of(methodNames));
    }

    private static Optional<Object> call(Object target, List<String> methodNames, Object... args) {
        for (String methodName : methodNames) {
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterCount() == args.length) {
                    try {
                        return Optional.ofNullable(method.invoke(target, args));
                    } catch (ReflectiveOperationException ignored) {
                        // Try another overload or name.
                    }
                }
            }
        }
        return Optional.empty();
    }
}
