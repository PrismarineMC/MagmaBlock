package cn.nukkit.network.protocol;

/**
 * author: MrGenga
 * MagmaBlock
 */
public class NewBatchPacket extends BatchPacket {

    @Override
    public void decode() {
        this.payload = this.get(this.getShort());
    }

    @Override
    public void encode() {
        this.reset();
        this.putShort(this.payload.length);
        this.put(this.payload);
    }
}
