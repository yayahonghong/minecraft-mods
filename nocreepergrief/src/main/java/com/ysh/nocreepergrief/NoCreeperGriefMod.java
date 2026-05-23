package com.ysh.nocreepergrief;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoCreeperGriefMod implements ModInitializer {
    public static final String MOD_ID = "nocreepergrief";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("NoCreeperGrief loaded — creeper terrain destruction disabled");
    }
}
