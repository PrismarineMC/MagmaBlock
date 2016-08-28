/**
 *
 *  __  __                             ____  _            _
 * |  \/  | __ _  __ _ _ __ ___   __ _| __ )| | ___   ___| | __
 * | |\/| |/ _` |/ _` | '_ ` _ \ / _` |  _ \| |/ _ \ / __| |/ /
 * | |  | | (_| | (_| | | | | | | (_| | |_) | | (_) | (__|   <
 * |_|  |_|\__,_|\__, |_| |_| |_|\__,_|____/|_|\___/ \___|_|\_\
 *               |___/
 *
 * MagmaBlock is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * @author Prismarine Team
 * @link   https://github.com/PrismarineMC/MagmaBlock
 *
 *
 */

package cn.nukkit.inventory;

import cn.nukkit.Player;
import cn.nukkit.math.Vector3;
import cn.nukkit.network.protocol.UpdateBlockPacket;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.network.protocol.BlockEntityDataPacket;

public class WindowInventory extends CustomInventory{

    protected String customName = "";
    protected String tile;
    protected int block;

    protected static InventoryType getInventoryType(int size){
        InventoryType type = InventoryType.get(InventoryType.CHEST);
        switch(size){
            case 5:
                 type = InventoryType.get(InventoryType.HOPPER);
                 break;
             case 9:
                 type = InventoryType.get(InventoryType.DISPENSER);
                 break;
             case 27:
                 type = InventoryType.get(InventoryType.CHEST);
                 break;
             case 54:
                 type = InventoryType.get(InventoryType.DOUBLE_CHEST);
                 break;
        }
        return type;
    }

    public WindowInventory(Player player){
        this(player, 27, "");
    }

    public WindowInventory(Player player, int size){
        this(player, size, "");
    }

    public WindowInventory(Player player, int size, String name) {
        super(new WindowHolder(player.getFloorX(), player.getFloorY() - 3, player.getFloorZ(), null), WindowInventory.getInventoryType(size));
		((WindowHolder)this.holder).setInventory(this);
        this.tile = BlockEntity.CHEST;
        this.block = 54;
        InventoryType type = InventoryType.get(InventoryType.CHEST);
        switch(size){
            case 5:
                 this.tile = BlockEntity.HOPPER;
                 this.block = 154;
                 type = InventoryType.get(InventoryType.HOPPER);
                 break;
             case 9:
                 this.tile = BlockEntity.DISPENSER;
                 this.block = 23;
                 type = InventoryType.get(InventoryType.DISPENSER);
                 break;
             case 27:
                 type = InventoryType.get(InventoryType.CHEST);
                 this.tile = BlockEntity.CHEST;
                 this.block = 54;
                 break;
             case 54:
                 type = InventoryType.get(InventoryType.DOUBLE_CHEST);
                 this.tile = BlockEntity.CHEST;
                 this.block = 54;
                 break;
             default:
                 player.getServer().getLogger().notice("Unknown window size. If must be one from: 5, 9, 27, 54. Using default size(27).");
        }
        this.customName = name;
    }

    @Override
    public void onOpen(Player who){
        this.holder = new WindowHolder(who.getFloorX(), who.getFloorY() - 3, who.getFloorZ(), this);
        WindowHolder holder = (WindowHolder) this.holder;
        UpdateBlockPacket pk = new UpdateBlockPacket();
        pk.records = new UpdateBlockPacket.Entry[]{new UpdateBlockPacket.Entry((int)holder.x, (int)holder.y, (int)holder.z, this.block, 0, UpdateBlockPacket.FLAG_ALL)};
        who.dataPacket(pk);
        CompoundTag c = new CompoundTag("")
            .putString("id", this.tile)
            .putInt("x", (int) holder.x)
            .putInt("y", (int) holder.y)
            .putInt("z", (int) holder.z);
        if(this.name != ""){
            c.putString("CustomName", this.customName);
        }
		try{
            BlockEntityDataPacket pk1 = new BlockEntityDataPacket();
            pk1.x = (int) holder.x;
            pk1.y = (int) holder.y;
            pk1.z = (int) holder.z;
            pk1.namedTag = NBTIO.write(c);
            who.dataPacket(pk1);
		} catch(Exception e){}
        super.onOpen(who);
        this.sendContents(who);
    }

    @Override
    public void onClose(Player who){
        WindowHolder holder = (WindowHolder) this.holder;
        UpdateBlockPacket pk = new UpdateBlockPacket();
        pk.records = new UpdateBlockPacket.Entry[]{new UpdateBlockPacket.Entry((int)holder.x, (int)holder.y, (int)holder.z, (int)who.getLevel().getBlockIdAt((int)holder.x, (int)holder.y, (int)holder.z), (int)who.getLevel().getBlockDataAt((int)holder.x, (int)holder.y, (int)holder.z), UpdateBlockPacket.FLAG_ALL)};
        who.dataPacket(pk);
        super.onClose(who);
    }
}
