package constructors.container;

import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;

public class BuilderContainer extends ConstructorContainer {
	public BuilderContainer(NetworkClient client, int uniqueSeed, Packet content) {
		super(client, uniqueSeed, content);
	}

	@Override
	protected boolean isValidItem(InventoryItem item) {
		return item != null && item.item instanceof ObjectItem;
	}

	@Override
	protected String getInvalidItemErrorMessage() {
		return "Must be an object.";
	}
}
