package constructors.container;

import constructors.item.ConstructorItem;
import constructors.item.ConstructorItem.Shape;
import necesse.engine.network.NetworkClient;
import necesse.engine.network.Packet;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.ContainerTransferResult;
import necesse.inventory.container.SlotIndexRange;
import necesse.inventory.container.customAction.EmptyCustomAction;
import necesse.inventory.container.customAction.IntCustomAction;
import necesse.inventory.container.customAction.StringCustomAction;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.container.slots.ContainerSlot;

public abstract class ConstructorContainer extends ItemInventoryContainer {
	public final EmptyCustomAction clearMaterialSlot;
	public final StringCustomAction setShapeAction;
	public final IntCustomAction setShapeSizeAction;

	public ConstructorContainer(NetworkClient client, int uniqueSeed, Packet content) {
		super(client, uniqueSeed, content);
		this.clearMaterialSlot = (EmptyCustomAction) this.registerAction(new EmptyCustomAction() {
			@Override
			protected void run() {				
				ContainerSlot ingredientSlot = ConstructorContainer.this.getSlot(1);
				InventoryItem item = ingredientSlot.getItem();
				
				if (item != null) {
					PlayerMob player = this.getContainer().client.playerMob;
					player.getInv().addItemsDropRemaining(item, "addback", player, false, true);
					ingredientSlot.setItem(null);
				}
			}
		});

		this.setShapeAction = (StringCustomAction) this.registerAction(new StringCustomAction() {
			@Override
			protected void run(String shapeName) {
				InventoryItem item = getInventoryItem();
				if (item != null && item.item instanceof ConstructorItem) {
					try {
						Shape shape = Shape.valueOf(shapeName);
						((ConstructorItem) item.item).setShape(item, shape);
					} catch (Exception ignored) {}
				}
			}
		});

		this.setShapeSizeAction = (IntCustomAction) this.registerAction(new IntCustomAction() {
			@Override
			protected void run(int size) {
				InventoryItem item = getInventoryItem();
				if (item != null && item.item instanceof ConstructorItem) {
					((ConstructorItem) item.item).setShapeSize(item, size);
				}
			}
		});
	}

	protected abstract boolean isValidItem(InventoryItem item);
	protected abstract String getInvalidItemErrorMessage();

	@Override
	public ContainerTransferResult transferToSlots(ContainerSlot slot, Iterable<SlotIndexRange> ranges, int amount, String purpose) {
		InventoryItem slotItem = slot.getItem();
		if (!isValidItem(slotItem)) {
			return new ContainerTransferResult(amount, getInvalidItemErrorMessage());
		}
		return super.transferToSlots(slot, ranges, amount, purpose);
	}
}
