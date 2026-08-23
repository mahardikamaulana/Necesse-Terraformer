package constructors.container;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.tileItem.TileItem;

public class TerraformerContainer extends ConstructorContainer {
	public TerraformerContainer(NetworkClient client, int uniqueSeed, Packet content) {
		super(client, uniqueSeed, content);
	}

	@Override
	protected boolean isValidItem(InventoryItem item) {
		return item != null && item.item instanceof TileItem;
	}

	@Override
	protected String getInvalidItemErrorMessage() {
		return "Must be a tile.";
	}
}
