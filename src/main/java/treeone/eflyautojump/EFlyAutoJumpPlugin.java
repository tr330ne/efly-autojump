package treeone.eflyautojump;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class EFlyAutoJumpPlugin extends Plugin {

    @Override
    public void onLoad() {
        try {
            final EFlyAutoJump eFlyAutoJump = new EFlyAutoJump();
            RusherHackAPI.getModuleManager().registerFeature(eFlyAutoJump);
        } catch (Exception e) {
            getLogger().error("Failed to load EFlyAutoJump plugin", e);
        }
    }

    @Override
    public void onUnload() {
    }
}