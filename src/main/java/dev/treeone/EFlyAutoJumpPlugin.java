package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;
public class EFlyAutoJumpPlugin extends Plugin {

    @Override
    public void onLoad() {

        this.getLogger().info("EFlyAutoJump plugin loaded");

        final EFlyAutoJump EFlyAutoJump = new EFlyAutoJump();
        RusherHackAPI.getModuleManager().registerFeature(EFlyAutoJump);
    }

    @Override
    public void onUnload() {
        this.getLogger().info("EFlyAutoJump plugin unloaded");
    }
}