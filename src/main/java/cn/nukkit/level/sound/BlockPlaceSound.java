package cn.nukkit.level.sound;

import cn.nukkit.block.Block;
import cn.nukkit.network.protocol.DataPacket;
import cn.nukkit.network.protocol.LevelEventPacket;

public class BlockPlaceSound extends GenericSound {

    protected int block;

    public BlockPlaceSound(Block bl) {
        super(bl, LevelEventPacket.EVENT_SOUND_BLOCK_PLACE, 0);
        this.block = bl.getId();
    }

    @Override 
    public DataPacket[] encode() { 
        LevelEventPacket pk = new LevelEventPacket(); 
        pk.evid = this.id; 
        pk.x = (float) this.x; 
        pk.y = (float) this.y; 
        pk.z = (float) this.z; 
        pk.data = (int) this.block; 
        return new DataPacket[]{pk}; 
    }
}
