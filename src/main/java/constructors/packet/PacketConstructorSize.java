package constructors.packet;

import constructors.item.ConstructorItem;
import necesse.engine.network.NetworkPacket;
import necesse.engine.network.Packet;
import necesse.engine.network.PacketReader;
import necesse.engine.network.PacketWriter;
import necesse.engine.network.server.Server;
import necesse.engine.network.server.ServerClient;
import necesse.inventory.InventoryItem;

public class PacketConstructorSize extends Packet {
	public final int slotIndex;
	public final int newSize;

	public PacketConstructorSize(byte[] data) {
		super(data);
		PacketReader reader = new PacketReader(this);
		this.slotIndex = reader.getNextInt();
		this.newSize = reader.getNextInt();
	}

	public PacketConstructorSize(int slotIndex, int newSize) {
		this.slotIndex = slotIndex;
		this.newSize = newSize;
		PacketWriter writer = new PacketWriter(this);
		writer.putNextInt(slotIndex);
		writer.putNextInt(newSize);
	}

	@Override
	public void processServer(NetworkPacket packet, Server server, ServerClient client) {
		if (client != null && client.playerMob != null) {
			InventoryItem item = client.playerMob.getInv().main.getItem(this.slotIndex);
			if (item != null && item.item instanceof ConstructorItem) {
				ConstructorItem constructorItem = (ConstructorItem) item.item;
				constructorItem.setShapeSize(item, this.newSize);
			}
		}
	}
}
