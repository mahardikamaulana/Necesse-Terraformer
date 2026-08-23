package constructors.form;

import java.util.Map.Entry;

import constructors.container.ConstructorContainer;
import constructors.item.ConstructorItem;
import constructors.item.ConstructorItem.Shape;
import constructors.item.ConstructorItem.ShapeSelection;
import necesse.engine.localization.message.GameMessage;
import necesse.engine.localization.message.LocalMessage;
import necesse.engine.localization.message.StaticMessage;
import necesse.engine.network.client.Client;
import necesse.gfx.forms.components.FormContentBox;
import necesse.gfx.forms.components.FormDropdownButton;
import necesse.gfx.forms.components.FormFlow;
import necesse.gfx.forms.components.FormIconButton;
import necesse.gfx.forms.components.FormInputSize;
import necesse.gfx.forms.components.containerSlot.FormContainerSlot;
import necesse.gfx.forms.components.localComponents.FormLocalLabel;
import necesse.gfx.forms.presets.containerComponent.ContainerForm;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;
import necesse.gfx.ui.ButtonStateTextures;
import necesse.gfx.ui.GameInterfaceStyle;
import necesse.inventory.InventoryItem;
import necesse.inventory.container.item.ItemInventoryContainer;

public abstract class ConstructorContainerForm<T extends ConstructorContainer> extends ContainerForm<T> {
	public FormContainerSlot materialSlot;
	private FormLocalLabel shapeSizeLabelText;
	private FormDropdownButton shapeSelector;

	public ConstructorContainerForm(Client client, String title, final T container) {
		super(client, 300, 100, container);
		GameInterfaceStyle defaultStyle = GameInterfaceStyle.getStyle(GameInterfaceStyle.defaultPath);
		ButtonStateTextures bstPlus = new ButtonStateTextures(defaultStyle, "button_plus");
		ButtonStateTextures bstMinus = new ButtonStateTextures(defaultStyle, "button_minus");

		this.addComponent(new FormLocalLabel((GameMessage) new StaticMessage(title), new FontOptions(20), -1, 10, 10));

		this.addComponent(
				this.materialSlot = new FormContainerSlot(client,
						this.container,
						((ItemInventoryContainer) this.container).INVENTORY_START,
						this.getWidth() - 60, this.getHeight() - 50));

		FormContentBox sizeAdjustmentWrapperBox = new FormContentBox(this.getX() + 30, 50, 200, 20);
		FormFlow flow = new FormFlow();
		FormLocalLabel adjustmentAreaText = new FormLocalLabel("terraformer", "shapeadjustment", new FontOptions(12), 40, 5, 0, 100);

		FormIconButton iconMinusComponent = new FormIconButton(0, 0, bstMinus, 20, 20, new LocalMessage("terraformer", "decreasesize"));
		iconMinusComponent.onClicked((event) -> {
			InventoryItem invItem = container.getInventoryItem();
			if (invItem != null && invItem.item instanceof ConstructorItem) {
				ConstructorItem currentItem = (ConstructorItem) invItem.item;
				int newSize = currentItem.getShapeSize(invItem) - 1;
				currentItem.setShapeSize(invItem, newSize);
				container.setShapeSizeAction.runAndSend(currentItem.getShapeSize(invItem));
				updateShapeSizeLabel(currentItem.getShapeSize(invItem));
			}
		});

		shapeSizeLabelText = new FormLocalLabel("shapesize", "0", new FontOptions(16), 0, 0, 0, 20);

		FormIconButton iconPlusComponent = new FormIconButton(0, 0, bstPlus, 20, 20, new LocalMessage("terraformer", "increasesize"));
		iconPlusComponent.onClicked((event) -> {
			InventoryItem invItem = container.getInventoryItem();
			if (invItem != null && invItem.item instanceof ConstructorItem) {
				ConstructorItem currentItem = (ConstructorItem) invItem.item;
				int newSize = currentItem.getShapeSize(invItem) + 1;
				currentItem.setShapeSize(invItem, newSize);
				container.setShapeSizeAction.runAndSend(currentItem.getShapeSize(invItem));
				updateShapeSizeLabel(currentItem.getShapeSize(invItem));
			}
		});

		InventoryItem initialItem = container.getInventoryItem();
		ConstructorItem constructorItem = (initialItem != null && initialItem.item instanceof ConstructorItem)
				? (ConstructorItem) initialItem.item
				: null;

		GameMessage defaultShapeMsg = new LocalMessage("terraformer", "shapeslector");
		if (constructorItem != null && initialItem != null) {
			Shape currentShape = constructorItem.getShape(initialItem);
			ShapeSelection ss = constructorItem.shapes.get(currentShape);
			if (ss != null) {
				defaultShapeMsg = new LocalMessage("constructor.shapes", ss.shapeName);
			}
		}

		this.shapeSelector = new FormDropdownButton(25, this.getBoundingBox().height - 30, FormInputSize.SIZE_16,
				ButtonColor.BASE, 200, defaultShapeMsg);

		if (constructorItem != null) {
			for (Entry<Shape, ShapeSelection> s : constructorItem.shapes.entrySet()) {
				shapeSelector.options.add(new LocalMessage("constructor.shapes", s.getValue().shapeName), () -> {
					InventoryItem invItem = container.getInventoryItem();
					if (invItem != null && invItem.item instanceof ConstructorItem) {
						ConstructorItem currentItem = (ConstructorItem) invItem.item;
						currentItem.setShape(invItem, s.getKey());
						container.setShapeAction.runAndSend(s.getKey().name());
						shapeSelector.setText(new LocalMessage("constructor.shapes", s.getValue().shapeName));
					}
				});
			}
		}

		sizeAdjustmentWrapperBox.addComponent(flow.nextX(adjustmentAreaText));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX(iconMinusComponent, 10));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX(shapeSizeLabelText));
		sizeAdjustmentWrapperBox.addComponent(flow.nextX(iconPlusComponent, 10));
		this.addComponent(sizeAdjustmentWrapperBox);
		this.addComponent(shapeSelector);

		if (constructorItem != null && initialItem != null) {
			updateShapeSizeLabel(constructorItem.getShapeSize(initialItem));
		}
	}

	public void updateShapeSizeLabel(int currentShapeSize) {
		if (this.shapeSizeLabelText != null) {
			this.shapeSizeLabelText.setText(String.valueOf(currentShapeSize));
		}
	}
}
