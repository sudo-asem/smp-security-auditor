package dev.smpaudit;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SmpAuditClient implements ClientModInitializer {
    public static final String MOD_ID = "smpaudit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("SMP Security Auditor loaded. It passively records the command tree supplied by the server.");
    }
}
