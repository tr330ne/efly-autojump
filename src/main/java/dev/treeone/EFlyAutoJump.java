package dev.treeone;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.stream.Stream;

public class EFlyAutoJump extends ToggleableModule {

    private ICommand originalElytraCommand, originalStopCommand;
    private CustomElytraCommand customElytraCommand;
    private CustomStopCommand customStopCommand;

    private static boolean isSpaceSpamming = false;
    private static long lastToggleTime = 0;
    private static boolean pressState = false;
    private final KeyMapping jumpKey = Minecraft.getInstance().options.keyJump;

    public EFlyAutoJump() {
        super("EFlyAutoJump", "Auto jump when #elytra is used", ModuleCategory.CHAT);
    }

    @Override
    public void onEnable() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        var registry = baritone.getCommandManager().getRegistry();

        originalElytraCommand = registry.stream().filter(cmd -> cmd.getNames().contains("elytra")).findFirst().orElse(null);
        originalStopCommand = registry.stream().filter(cmd -> cmd.getNames().contains("stop")).findFirst().orElse(null);

        if (originalElytraCommand != null) {
            registry.unregister(originalElytraCommand);
            customElytraCommand = new CustomElytraCommand(baritone, originalElytraCommand);
            registry.register(customElytraCommand);
        }

        if (originalStopCommand != null) {
            registry.unregister(originalStopCommand);
            customStopCommand = new CustomStopCommand(baritone, originalStopCommand);
            registry.register(customStopCommand);
        }
    }

    @Override
    public void onDisable() {
        stopSpaceSpam();
        var registry = BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().getRegistry();

        if (customElytraCommand != null) {
            registry.unregister(customElytraCommand);
            customElytraCommand = null;
        }
        if (customStopCommand != null) {
            registry.unregister(customStopCommand);
            customStopCommand = null;
        }
        if (originalElytraCommand != null) {
            registry.register(originalElytraCommand);
            originalElytraCommand = null;
        }
        if (originalStopCommand != null) {
            registry.register(originalStopCommand);
            originalStopCommand = null;
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (!isSpaceSpamming) return;
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime >= 50) {
            pressState = !pressState;
            jumpKey.setDown(pressState);
            lastToggleTime = currentTime;
        }
    }

    public static void startSpaceSpam() {
        if (isSpaceSpamming) return;
        isSpaceSpamming = true;
        pressState = false;
        lastToggleTime = System.currentTimeMillis();
    }

    public void stopSpaceSpam() {
        if (!isSpaceSpamming) return;
        isSpaceSpamming = false;
        jumpKey.setDown(false);
    }

    private static class CustomElytraCommand extends Command {
        private final ICommand originalCommand;

        public CustomElytraCommand(IBaritone baritone, ICommand originalCommand) {
            super(baritone, "elytra");
            this.originalCommand = originalCommand;
        }

        @Override
        public void execute(String label, IArgConsumer args) {
            try {
                String rawArgs = args.rawRest();
                var registry = baritone.getCommandManager().getRegistry();

                registry.unregister(this);
                registry.register(originalCommand);
                baritone.getCommandManager().execute("elytra " + rawArgs);
                registry.unregister(originalCommand);
                registry.register(this);

                startSpaceSpam();
            } catch (Exception e) {
                try {
                    baritone.getCommandManager().getRegistry().unregister(originalCommand);
                    baritone.getCommandManager().getRegistry().register(this);
                } catch (Exception ignored) {}
            }
        }

        @Override
        public Stream<String> tabComplete(String label, IArgConsumer args) {
            return originalCommand != null ? originalCommand.tabComplete(label, args) : Stream.empty();
        }
        @Override
        public String getShortDesc() { return ""; }
        @Override
        public List<String> getLongDesc() { return List.of(""); }
    }

    private class CustomStopCommand extends Command {
        private final ICommand originalCommand;

        public CustomStopCommand(IBaritone baritone, ICommand originalCommand) {
            super(baritone, "stop");
            this.originalCommand = originalCommand;
        }

        @Override
        public void execute(String label, IArgConsumer args) {
            try {
                String rawArgs = args.rawRest();
                var registry = baritone.getCommandManager().getRegistry();

                registry.unregister(this);
                registry.register(originalCommand);
                baritone.getCommandManager().execute("stop " + rawArgs);
                registry.unregister(originalCommand);
                registry.register(this);

                EFlyAutoJump.this.stopSpaceSpam();
            } catch (Exception e) {
                try {
                    baritone.getCommandManager().getRegistry().unregister(originalCommand);
                    baritone.getCommandManager().getRegistry().register(this);
                } catch (Exception ignored) {}
            }
        }

        @Override
        public Stream<String> tabComplete(String label, IArgConsumer args) {
            return originalCommand != null ? originalCommand.tabComplete(label, args) : Stream.empty();
        }

        @Override
        public String getShortDesc() { return ""; }

        @Override
        public List<String> getLongDesc() { return List.of(""); }
    }
}