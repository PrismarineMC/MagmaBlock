package cn.nukkit.event.inventory;

import cn.nukkit.Player;
import cn.nukkit.event.Cancellable;
import cn.nukkit.event.HandlerList;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.item.Item;

public class InventoryClickEvent extends InventoryEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlers() {
        return handlers;
    }

    private final Player who;
    private final int slot;
    private final Item item;

    public InventoryClickEvent(Inventory inventory, Player who, int slot, Item item) {
        super(inventory);
        this.who = who;
        this.slot = slot;
        this.item = item;
    }

    public Player getWhoClicked() {
        return this.who;
    }

    public int getSlot() {
        return this.slot;
    }

    public Item getItem() {
        return this.item;
    }
}