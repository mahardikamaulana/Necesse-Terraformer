package constructors.item;

import java.awt.geom.Line2D;
import java.util.ArrayList;

import constructors.ConstructorsMod;
import constructors.drawables.ConstructorTileDrawable;
import constructors.drawables.ConstructorTileDrawable.TileDrawableOptions;
import constructors.drawables.ConstructorTileDrawable.TileHighlightType;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ObjectRegistry;
import necesse.engine.util.GameBlackboard;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryAddConsumer;
import necesse.inventory.InventoryItem;
import necesse.inventory.PlayerInventorySlot;
import necesse.inventory.container.item.ItemInventoryContainer;
import necesse.inventory.item.Item;
import necesse.inventory.item.placeableItem.objectItem.ObjectItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameObject.GameObject;
import necesse.level.gameObject.ObjectPlaceOption;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;

public class BuilderItem extends ConstructorItem {
		
	public static final boolean SR_NO_MODIFY = true;	
					
	public BuilderItem() {
		super();	
	}
	
	@Override
	public void initializeShapes() {
		if (shapes_initialized) return;
		shapes_initialized = true;			
		
		shapes.put(Shape.SQUARE, new ShapeSelectionSquare(this));
		shapes.put(Shape.LINE_BOX, new ShapeSelectionLineBox(this));
		shapes.put(Shape.CHECKERBOARD, new ShapeSelectionCheckerboard(this));
		shapes.put(Shape.LINE, new ShapeSelectionLine(this));
		shapes.put(Shape.CIRCLE, new ShapeSelectionCircle(this));
		shapes.put(Shape.RING, new ShapeSelectionRing(this));
	}
	
	@Override
	public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
			GameBlackboard blackboard) {
		ListGameTooltips tooltips = new ListGameTooltips();			
		tooltips.add(Localization.translate("builder", "buildertip1"));
		tooltips.add(Localization.translate("builder", "buildertip2"));
		tooltips.add(Localization.translate("builder", "buildertip3"));
		tooltips.add(Localization.translate("builder", "buildertip4"));
		tooltips.add(Localization.translate("builder", "buildertip5"));
		tooltips.add(Localization.translate("builder", "buildertip6"));
		tooltips.add(Localization.translate("builder", "buildertip7"));
		tooltips.add(super.getPreEnchantmentTooltips(item, perspective, blackboard));
		return tooltips;
	}
			
	@Override
	public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {		
		
		if (!(attackerMob instanceof PlayerMob)) return me;
		PlayerMob p = (PlayerMob) attackerMob;
		
		ArrayList<ObjectItem> replacedObjects = new ArrayList<ObjectItem>();	
		LevelTile[][] targetTiles = this.getTargetTiles(me, p, new TilePosition(level, x / 32, y / 32));
		if (targetTiles != null && targetTiles.length > 0) {
			LevelTile[][] cloneTiles = targetTiles.clone();
			this.clearOutOfRangeTiles(cloneTiles, p, this.getMaxPlacementRange(me));
			
			int objectsExpended = 0;
			for (int i = 0; i < cloneTiles.length; i++) {
				for (int j = 0; j < cloneTiles[i].length; j++) {
					LevelTile targetTile = cloneTiles[i][j];
					if (targetTile == null) {
						continue;
					}
					
					ObjectItem highlightedObjectItem = level.getObject(targetTile.tileX, targetTile.tileY).getObjectItem();						
					ObjectItem objectInBucket = getCurrentObject(me);
					if (objectInBucket != null) {		
						int numTilesInBucket = getCurrentObjectAmount(me);
						if (numTilesInBucket >= objectsExpended + 1) {
							if (highlightedObjectItem == null || (highlightedObjectItem.getID() != objectInBucket.getID())) {	
								ObjectPlaceOption po = objectInBucket.getBestPlaceOption(level,								
										targetTile.tileX * 32, 
										targetTile.tileY * 32, 
										BuilderItem.this.getObjectInvItem(me),
										p, (Line2D) null, true);
								
								if (po != null) {
									String checkResult = objectInBucket.getObject().canPlace(level, po.tileX * 32, po.tileY * 32, po.rotation, true);
									boolean canPlace = !"liquid".equals(checkResult) && !"shore".equals(checkResult);
									
									if (canPlace) {			
										if (level.isServer()) {
											if (highlightedObjectItem != null &&
													highlightedObjectItem.getObject().getID() != ObjectRegistry.getObjectID("air")) {
												replacedObjects.add(highlightedObjectItem);
												level.entityManager.destroyObjectOverride(po.tileX, po.tileY, 0, false);
											}											
											objectsExpended += 1;		
											objectInBucket.getObject().placeObject(level, po.tileX, po.tileY, po.rotation, true);	
											level.sendObjectChangePacket(level.getServer(), po.tileX, po.tileY, objectInBucket.getObject().getID(), po.rotation);
										}		
									}
								}
							}
						}	
					}				
				}
			}
			
			if (level.isServer()) {
				if (objectsExpended > 0) {
					this.removeObjectsFromBucket(me, objectsExpended);
				}
				if (replacedObjects.size() > 0) {
					for (ObjectItem tile : replacedObjects) {
						p.getInv().addItem(new InventoryItem(tile), true, "give", (InventoryAddConsumer) null);
					}					
				}
			}
		}
		
		return me;
	}		

	@Override
	public InventoryItem onLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int seed, GNDItemMap mapContent) {		
		
		if (!(attackerMob instanceof PlayerMob)) return me;
		PlayerMob p = (PlayerMob) attackerMob;
		
		LevelTile[][] targetTiles = this.getTargetTiles(me, p, new TilePosition(level, x / 32, y / 32));
		if (targetTiles != null && targetTiles.length > 0) {
			LevelTile[][] cloneTiles = targetTiles.clone();
			this.clearOutOfRangeTiles(cloneTiles, p, this.getMaxPlacementRange(me));
			int addCurrentObjects = 0;
			for (int i = 0; i < cloneTiles.length; i++) {
				for (int j = 0; j < cloneTiles[i].length; j++) {
					LevelTile targetTile = cloneTiles[i][j];
					if (targetTile == null) {
						continue;
					}			
					GameObject gameObjOnTile = level.getObject(targetTile.tileX, targetTile.tileY);
					ObjectItem objectOnTile = gameObjOnTile.getObjectItem();
						
					if (!hasObject(me)) {
						if (level.isServer()) {
							if (objectOnTile != null && objectOnTile.getID() != ObjectRegistry.getObjectID("air")) { 
								InventoryItem newItem = new InventoryItem(objectOnTile);
								newItem.setAmount(1);
								setObject(level, p, me, newItem);
								level.entityManager.destroyObjectOverride(targetTile.tileX, targetTile.tileY, 0, false);
								level.sendObjectChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, 0);
							}
						}
					} else if (getCurrentObject(me) != null && objectOnTile != null && objectOnTile.getID() == getCurrentObject(me).getID()) {		
						if (level.isServer()) { 
							addCurrentObjects += 1;
							level.entityManager.destroyObjectOverride(targetTile.tileX, targetTile.tileY, 0, false);
							level.sendObjectChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, 0);
						}
					}		
				}
			}		
			if (level.isServer()) {
				if (addCurrentObjects > 0) {
					int remainder = addCurrentObjectAmount(me, addCurrentObjects);
					if (remainder > 0 && this.getCurrentObject(me) != null) {
						InventoryItem newItem = new InventoryItem(this.getCurrentObject(me));
						newItem.setAmount(remainder);
						p.getInv().addItem(newItem, true, "give", (InventoryAddConsumer) null);
					}
				}
			}
		}
		return me;
	}
			
	@Override
	public void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
			TilePosition pos, boolean isDebug) {
		
		if (perspective != null && perspective.getLevel() != null && pos != null) {
			LevelTile[][] targetTiles = this.getTargetTiles(me, perspective, pos);
			
			final ObjectItem objectInBucket;
			int objID = -1;
			if (hasObject(me)) {
				objectInBucket = getCurrentObject(me);
				if (objectInBucket != null) {
					objID = objectInBucket.getObject().getID();
				}
			} else {
				objectInBucket = null;
			}		
			
			ConstructorTileDrawable<GameObject> highlightDraw = new ConstructorTileDrawable<GameObject>(
					perspective.getLevel(),
					perspective,
					camera,
					targetTiles,
					objID,
					this.getMaxPlacementRange(me),
					(lvObj) -> lvObj.level.getObject(lvObj.tileX, lvObj.tileY),
					(lvObj, tgtObj) -> lvObj.getID() != tgtObj
			);	
			
			highlightDraw.perTileDrawStep = new TileDrawableOptions() {
				@Override
				public void draw(Level level, PlayerMob perspective, LevelTile tile, TileHighlightType highlightType) {
					if (tile == null || objectInBucket == null) return;
					if (highlightType == TileHighlightType.OUT_OF_RANGE || highlightType == TileHighlightType.ALREADY_PAINTED_TILE) return;
					
					ObjectPlaceOption po = objectInBucket.getBestPlaceOption(level,								
							tile.tileX * 32, 
							tile.tileY * 32, 
							BuilderItem.this.getObjectInvItem(me),
							perspective, (Line2D) null, true);
					
					if (po != null) {
						String checkResult = objectInBucket.getObject().canPlace(level, po.tileX * 32, po.tileY * 32, po.rotation, true);
						boolean canPlace = !"liquid".equals(checkResult) && !"shore".equals(checkResult);
						
						if (canPlace) {		
							float alpha = 0.5F;						
							po.object.drawMultiTilePreview(level, po.tileX, po.tileY, po.rotation, alpha, perspective, camera);
						}
					}
				}
			};
			highlightDraw.draw(perspective.getLevel().tickManager());
		}
	}

	@Override
	protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
		PacketOpenContainer p = new PacketOpenContainer(ConstructorsMod.BUILDER_CONTAINER,
				ItemInventoryContainer.getContainerContent(this, inventorySlot));
		ContainerRegistry.openAndSendContainer(client, p);
	}
	
	@Override
	public int getInternalInventorySize() {		
		return 1;
	}

	@Override
	public boolean isValidPouchItem(InventoryItem arg0) {			
		return isValidRequestItem(arg0.item);
	}

	@Override
	public boolean isValidRequestItem(Item arg0) {
		return arg0 instanceof ObjectItem;
	}

	@Override
	public boolean isValidRequestType(Item.Type type) {
		return false;
	}
	
	public InventoryItem getObjectInvItem(InventoryItem me) {
		if (hasObject(me)) { 		
			Inventory _me = this.getInternalInventory(me);
			return _me.getItem(0);
		}
		return null;
	}
	
	public boolean hasObject(InventoryItem me) {			
		Inventory _me = this.getInternalInventory(me);
		return (_me.getItemSlot(0) != null) && !_me.isSlotClear(0);		
	}
	
	public ObjectItem getCurrentObject(InventoryItem me) {			
		if (this.hasObject(me)) {
			InventoryItem item = getObjectInvItem(me);
			if (item != null && item.item instanceof ObjectItem) {
				return (ObjectItem) item.item;
			}
		}
		return null;
	}
	
	public int getCurrentObjectAmount(InventoryItem me) {
		if (this.hasObject(me)) {
			Inventory _me = this.getInternalInventory(me);
			return _me.getAmount(0);
		}
		return 0;
	}	
	
	public void setCurrentObjectAmount(InventoryItem me, int newAmount) {
		if (this.hasObject(me)) {			
			Inventory _me = this.getInternalInventory(me);
			_me.setAmount(0, newAmount);
			this.saveInternalInventory(me, _me);
		}
	}
	
	public int addCurrentObjectAmount(InventoryItem me, int addAmount) {
	    if (this.hasObject(me)) {
	        Inventory _me = this.getInternalInventory(me);
	        InventoryItem objectInvItem = getObjectInvItem(me);
	        if (objectInvItem == null) return -1;
	        
	        int limit = _me.getItemStackLimit(0, objectInvItem);
	        int currentAmount = getCurrentObjectAmount(me);

	        int newAmount = Math.min(currentAmount + addAmount, limit);
	        int overTheLimit = Math.max(0, (currentAmount + addAmount) - limit);

	        setCurrentObjectAmount(me, newAmount);
	        return overTheLimit;
	    }
	    return -1; 
	}
	
	private void removeObjectsFromBucket(InventoryItem me, int amount) {
		if (this.hasObject(me)) {
			int now = getCurrentObjectAmount(me);
			if (now <= amount) {
				setCurrentObjectAmount(me, 0);
				return;
			}
			setCurrentObjectAmount(me, now - amount);
		}			
	}
	
	public void setObject(Level level, PlayerMob player, InventoryItem me, InventoryItem newItem) {		
		Inventory _me = this.getInternalInventory(me);
		_me.addItem(level, player, newItem, "give", null);		
		this.saveInternalInventory(me, _me);
	}

	@Override
	protected Ingredient[] getSpecialUpgradeCost(int nextTier) {
		switch (nextTier) {
		case 1: return new Ingredient[]{
				new Ingredient(ObjectRegistry.getObject("woodwall").getObjectItem().getStringID(), 100)
		};
		case 2: return new Ingredient[]{
				new Ingredient(ObjectRegistry.getObject("sandstonerock").getObjectItem().getStringID(), 100),
				new Ingredient(ObjectRegistry.getObject("rock").getObjectItem().getStringID(), 100),
				new Ingredient(ObjectRegistry.getObject("snowrock").getObjectItem().getStringID(), 100),
		};
		case 3: return new Ingredient[]{
				new Ingredient(ObjectRegistry.getObject("deepsandstonerock").getObjectItem().getStringID(), 100),
				new Ingredient(ObjectRegistry.getObject("deepsnowrock").getObjectItem().getStringID(), 100),
				new Ingredient(ObjectRegistry.getObject("deeprock").getObjectItem().getStringID(), 100),
		};
		case 4: return new Ingredient[]{
				new Ingredient(ObjectRegistry.getObject("dungeonwall").getObjectItem().getStringID(), 100)
		};
		case 5: return new Ingredient[]{
				new Ingredient("upgradeshard", 50)
		};
		default: return new Ingredient[]{new Ingredient("upgradeshard", nextTier * 20)};
		}	
	}	
}