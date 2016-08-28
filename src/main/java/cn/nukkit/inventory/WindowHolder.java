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

import cn.nukkit.math.Vector3;

public class WindowHolder extends Vector3 implements InventoryHolder{
    protected Inventory inventory;

    public WindowHolder(int x, int y, int z, Inventory inventory){
        super(x, y, z);
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory(){
        return this.inventory;
    }
	
	public void setInventory(Inventory inv){
		this.inventory = inv;
	}
}
