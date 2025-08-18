package treeone.eflyautojump;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.command.Command;
import baritone.api.command.ICommand;
import baritone.api.command.argument.IArgConsumer;
import baritone.api.command.exception.CommandException;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.NumberSetting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.stream.Stream;

public class EFlyAutoJump extends ToggleableModule {

    private ICommand originalElytraCommand, originalStopCommand;
    private CustomElytraCommand customElytraCommand;
    private CustomStopCommand customStopCommand;

    private static boolean isSpaceSpamming = false;
    private static long lastToggleTime = 0;
    private static long spamStartTime = 0;
    private static boolean pressState = false;

    private static final long TOGGLE_INTERVAL = 50L;

    private final NumberSetting<Long> stopDelay = new NumberSetting<>("StopDelay", "Delay before stopping space spam after path ends (ms)", 50L, 0L, 5000L);

    private final KeyMapping jumpKey = Minecraft.getInstance().options.keyJump;

    public EFlyAutoJump() {
        super("EFlyAutoJump", "Enhances Baritone elytra flight with smart auto-jump start/stop and visual display, compatible with Xaero Plus “Baritone Elytra Here”, also supports manual Baritone commands input in addition to automation.", ModuleCategory.CHAT);
        this.registerSettings(stopDelay);
    }

    @Override
    public void onEnable() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        var registry = baritone.getCommandManager().getRegistry();

        originalElytraCommand = findCommand(registry.stream(), "elytra");
        originalStopCommand = findCommand(registry.stream(), "stop");

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
        stopSpaceSpam(false);
        restoreOriginalCommands();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!isSpaceSpamming) return;

        long currentTime = System.currentTimeMillis();

        if (shouldCheckBaritoneStatus(currentTime) && shouldStopSpam()) {
            stopSpaceSpam(true);
            return;
        }

        if (shouldToggleJump(currentTime)) {
            toggleJumpKey();
        }
    }

    public static void startSpaceSpam() {
        if (isSpaceSpamming) return;

        isSpaceSpamming = true;
        pressState = false;
        long currentTime = System.currentTimeMillis();
        spamStartTime = currentTime;
        lastToggleTime = currentTime;

        displayMessage("§a[EFlyAutoJump] Space spam started", true);
    }

    public void stopSpaceSpam() {
        stopSpaceSpam(false);
    }

    private void stopSpaceSpam(boolean auto) {
        if (!isSpaceSpamming) return;

        isSpaceSpamming = false;
        jumpKey.setDown(false);

        String message = auto
                ? "§e[EFlyAutoJump] Path ended, space spam stopped"
                : "§c[EFlyAutoJump] Space spam stopped";
        displayMessage(message, true);
    }

    private ICommand findCommand(Stream<ICommand> registry, String commandName) {
        return registry
                .filter(cmd -> cmd.getNames().contains(commandName))
                .findFirst()
                .orElse(null);
    }

    private boolean shouldCheckBaritoneStatus(long currentTime) {
        return currentTime - spamStartTime > stopDelay.getValue();
    }

    private boolean shouldStopSpam() {
        try {
            IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
            if (baritone == null) return false;
            return baritone.getElytraProcess().currentDestination() == null;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean shouldToggleJump(long currentTime) {
        return currentTime - lastToggleTime >= TOGGLE_INTERVAL;
    }

    private void toggleJumpKey() {
        pressState = !pressState;
        jumpKey.setDown(pressState);
        lastToggleTime = System.currentTimeMillis();
    }

    private static void displayMessage(String message, boolean actionBar) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), actionBar);
        }
    }

    private void restoreOriginalCommands() {
        IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
        var registry = baritone.getCommandManager().getRegistry();

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
        public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
            return originalCommand != null ? originalCommand.tabComplete(label, args) : Stream.empty();
        }

        @Override
        public String getShortDesc() {
            return "";
        }

        @Override
        public List<String> getLongDesc() {
            return List.of("");
        }
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
        public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
            return originalCommand != null ? originalCommand.tabComplete(label, args) : Stream.empty();
        }

        @Override
        public String getShortDesc() {
            return "";
        }

        @Override
        public List<String> getLongDesc() {
            return List.of("");
        }
    }
}