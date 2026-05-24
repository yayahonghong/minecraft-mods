package com.ysh.onlinemodefix;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlineModeFixMod implements ModInitializer {
    public static final String MOD_ID = "onlinemodefix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("OnlineModeFix loaded — offline players will be allowed when online-mode=true");
    }
}
