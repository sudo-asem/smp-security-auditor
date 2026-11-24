package dev.smpaudit;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.SharedSuggestionProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class AuditExporter {
    private static final List<String> SENSITIVE_WORDS = List.of(
            "op", "deop", "ban", "pardon", "kick", "whitelist", "gamemode", "give",
            "tp", "teleport", "stop", "reload", "restart", "lp", "luckperms",
            "permission", "permissions", "perm", "admin", "console", "plugman",
            "essentials", "sudo", "execute", "minecraft:op", "bukkit:op", "paper:"
    );

    private AuditExporter() {}

    public static synchronized void capture(CommandDispatcher<SharedSuggestionProvider> dispatcher) {
        try {
            List<Entry> entries = new ArrayList<>();
            for (CommandNode<SharedSuggestionProvider> child : dispatcher.getRoot().getChildren()) {
                walk(child, "/" + child.getName(), entries);
            }
            entries.sort(Comparator.comparing(Entry::path));

            Path outDir = FabricLoader.getInstance().getGameDir().resolve("smp-audit");
            Files.createDirectories(outDir);
            String stamp = Instant.now().toString();
            Files.writeString(outDir.resolve("commands.txt"), buildText(entries, stamp), StandardCharsets.UTF_8);
            Files.writeString(outDir.resolve("commands.json"), buildJson(entries, stamp), StandardCharsets.UTF_8);

            long flagged = entries.stream().filter(Entry::sensitiveLooking).count();
            SmpAuditClient.LOGGER.info("Saved {} server-visible command nodes ({} sensitive-looking) to {}",
                    entries.size(), flagged, outDir.toAbsolutePath());
        } catch (Exception e) {
            SmpAuditClient.LOGGER.error("Could not export the server command tree", e);
        }
    }

    private static void walk(CommandNode<SharedSuggestionProvider> node, String path, List<Entry> out) {
        String type = node instanceof LiteralCommandNode<?> ? "literal" : "argument";
        boolean executable = node.getCommand() != null;
        boolean redirect = node.getRedirect() != null;
        out.add(new Entry(path, type, executable, redirect, node.getChildren().size(), isSensitive(path)));
        for (CommandNode<SharedSuggestionProvider> child : node.getChildren()) {
            walk(child, path + " " + child.getName(), out);
        }
    }

    private static boolean isSensitive(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        for (String word : SENSITIVE_WORDS) {
            if (lower.equals("/" + word) || lower.startsWith("/" + word + " ")
                    || lower.contains(" " + word) || lower.contains("/" + word + ":")) {
                return true;
            }
        }
        return false;
    }

    private static String buildText(List<Entry> entries, String stamp) {
        StringBuilder b = new StringBuilder();
        b.append("SMP Security Auditor - command tree export\n");
        b.append("Captured: ").append(stamp).append('\n');
        b.append("Minecraft: 26.1.2 / Fabric client\n");
        b.append("IMPORTANT: These are command nodes sent to this player. Visibility does NOT prove that execution is permitted.\n\n");
        b.append("FLAGGED (sensitive-looking names; review manually)\n");
        for (Entry e : entries) if (e.sensitiveLooking()) b.append("[REVIEW] ").append(e.path()).append(" [").append(e.type()).append("]\n");
        b.append("\nALL COMMAND NODES (parent before child)\n");
        for (Entry e : entries) {
            b.append(e.path()).append(" | type=").append(e.type())
                    .append(" | executable-node=").append(e.executable())
                    .append(" | redirect=").append(e.redirect())
                    .append(" | children=").append(e.children())
                    .append(e.sensitiveLooking() ? " | REVIEW" : "")
                    .append('\n');
        }
        return b.toString();
    }

    private static String buildJson(List<Entry> entries, String stamp) {
        StringBuilder b = new StringBuilder();
        b.append("{\n  \"captured\": \"").append(escape(stamp)).append("\",\n");
        b.append("  \"minecraft\": \"26.1.2\",\n");
        b.append("  \"warning\": \"Visibility does not prove command execution permission.\",\n");
        b.append("  \"commands\": [\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            b.append("    {\"path\": \"").append(escape(e.path())).append("\", ")
                    .append("\"type\": \"").append(e.type()).append("\", ")
                    .append("\"executableNode\": ").append(e.executable()).append(", ")
                    .append("\"redirect\": ").append(e.redirect()).append(", ")
                    .append("\"children\": ").append(e.children()).append(", ")
                    .append("\"sensitiveLooking\": ").append(e.sensitiveLooking()).append("}");
            if (i + 1 < entries.size()) b.append(',');
            b.append('\n');
        }
        b.append("  ]\n}\n");
        return b.toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r");
    }

    private record Entry(String path, String type, boolean executable, boolean redirect,
                         int children, boolean sensitiveLooking) {}
}
