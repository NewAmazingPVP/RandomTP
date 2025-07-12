package nap.randomtp;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class RandomTP extends JavaPlugin implements TabExecutor {

    private static final int INTERVAL_SECONDS = 60;
    private static final double WORLD_LIMIT = 29_999_984;      // vanilla hard border
    private static final String PERMISSION = "randomtp.start";
    private static final String ARROW = "➤";

    private final Random rng = ThreadLocalRandom.current();

    private BukkitTask loopTask;
    private int secondsLeft;

    @Override
    public void onEnable() {
        getCommand("startrandomtp").setExecutor(this);
    }

    @Override
    public void onDisable() {
        stopLoop();
    }

    @Override
    public boolean onCommand(CommandSender sender,
                             Command cmd,
                             String label,
                             String[] args) {

        if (!cmd.getName().equalsIgnoreCase("startrandomtp"))
            return true;

        if (loopTask == null) {
            startLoop();
            sender.sendMessage(Component.text("Random-teleport loop started!", NamedTextColor.GREEN));
        } else {
            stopLoop();
            sender.sendMessage(Component.text("Random-teleport loop stopped.", NamedTextColor.RED));
        }
        return true;
    }

    @Override public List<String> onTabComplete(CommandSender s, Command c, String l, String[] a) { return List.of(); }

    private void startLoop() {
        secondsLeft = INTERVAL_SECONDS;
        loopTask = Bukkit.getScheduler().runTaskTimer(this, () -> {

            if (--secondsLeft <= 0) {
                performRandomTeleport();
                secondsLeft = INTERVAL_SECONDS;
            }
            sendActionBarCountdown();

        }, 20L, 20L);
    }

    private void stopLoop() {
        if (loopTask != null) {
            loopTask.cancel();
            loopTask = null;
            clearActionBars();
        }
    }

    private void sendActionBarCountdown() {
        Component bar = Component.text()
                .append(Component.text(ARROW + ARROW + " ", NamedTextColor.GOLD))
                .append(Component.text("⏳ Next TP in ", NamedTextColor.YELLOW))
                .append(Component.text(secondsLeft + "s", NamedTextColor.AQUA))
                .append(Component.text(" " + ARROW + ARROW, NamedTextColor.GOLD))
                .build();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendActionBar(bar);
        }
    }

    private void clearActionBars() {
        Component blank = Component.text("");
        Bukkit.getOnlinePlayers().forEach(p -> p.sendActionBar(blank));
    }

    private void performRandomTeleport() {
        List<World> worlds = Bukkit.getWorlds();
        if (worlds.isEmpty()) return;

        World world = worlds.get(rng.nextInt(worlds.size()));

        double x = rng.nextDouble(-WORLD_LIMIT, WORLD_LIMIT);
        double z = rng.nextDouble(-WORLD_LIMIT, WORLD_LIMIT);
        int y = rng.nextInt(-64, 321);

        Location target = new Location(world, x, y, z);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.teleportAsync(target).thenAccept(v ->
                    p.sendActionBar(Component.text("💨 Teleported!", NamedTextColor.AQUA))
            );
        }
    }
}
