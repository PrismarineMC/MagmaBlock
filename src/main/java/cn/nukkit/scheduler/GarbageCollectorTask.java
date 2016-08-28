package cn.nukkit.scheduler;

import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Level;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.Server;

public class GarbageCollectorTask extends AsyncTask {

    private CommandSender sender;
    private int chunksCollected = 0;
    private int entitiesCollected = 0;
    private int tilesCollected = 0;
    private long freedMemory = 0;

    public GarbageCollectorTask(CommandSender sender) {
        this.sender = sender;
    }

    @Override
    public void onRun() {
        long memory = Runtime.getRuntime().freeMemory();

        for (Level level : Server.getInstance().getLevels().values()) {
            int chunksCount = level.getChunks().size();
            int entitiesCount = level.getEntities().length;
            int tilesCount = level.getBlockEntities().size();
            level.doChunkGarbageCollection();
            level.unloadChunks(true);
            chunksCollected += chunksCount - level.getChunks().size();
            entitiesCollected += entitiesCount - level.getEntities().length;
            tilesCollected += tilesCount - level.getBlockEntities().size();
            level.clearCache(true);
        }

        System.gc();

        freedMemory = Runtime.getRuntime().freeMemory() - memory;
    }

    @Override
    public void onCompletion(Server server){
        if(sender == null) return;
        sender.sendMessage(TextFormat.GREEN + "---- " + TextFormat.WHITE + "Garbage collection result" + TextFormat.GREEN + " ----");
        sender.sendMessage(TextFormat.GOLD + "Chunks: " + TextFormat.RED + chunksCollected);
        sender.sendMessage(TextFormat.GOLD + "Entities: " + TextFormat.RED + entitiesCollected);
        sender.sendMessage(TextFormat.GOLD + "Block Entities: " + TextFormat.RED + tilesCollected);
        sender.sendMessage(TextFormat.GOLD + "Memory freed: " + TextFormat.RED + NukkitMath.round((freedMemory / 1024d / 1024d), 2) + " MB");
    }
}
