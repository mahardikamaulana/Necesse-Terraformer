package constructors.item;

import java.util.ArrayList;

import constructors.ConstructorsMod;
import constructors.drawables.ConstructorTileDrawable;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.network.packet.PacketOpenContainer;
import necesse.engine.network.server.ServerClient;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.TileRegistry;
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
import necesse.inventory.item.placeableItem.tileItem.TileItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.gameTile.GameTile;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;

public class TerraformerItem extends ConstructorItem {
		
	public static final boolean SR_NO_MODIFY = true;	
	
	public TerraformerItem() {
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
		tooltips.add(Localization.translate("terraformer", "terraformertip1"));
		tooltips.add(Localization.translate("terraformer", "terraformertip2"));
		tooltips.add(Localization.translate("terraformer", "terraformertip3"));
		tooltips.add(Localization.translate("terraformer", "terraformertip4"));
		tooltips.add(Localization.translate("terraformer", "terraformertip5"));
		tooltips.add(Localization.translate("terraformer", "terraformertip6"));
		tooltips.add(Localization.translate("terraformer", "terraformertip7"));
		tooltips.add(super.getPreEnchantmentTooltips(item, perspective, blackboard));
		return tooltips;
	}
			
	@Override
	public InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent) {		
		
		if (!(attackerMob instanceof PlayerMob)) return me;
		PlayerMob p = (PlayerMob) attackerMob;
		
		ArrayList<TileItem> replacedTiles = new ArrayList<TileItem>();	
		LevelTile[][] targetTiles = this.getTargetTiles(me, p, new TilePosition(level, x / 32, y / 32));
		if (targetTiles != null && targetTiles.length > 0) {
			LevelTile[][] cloneTiles = targetTiles.clone();	
			this.clearOutOfRangeTiles(cloneTiles, p, this.getMaxPlacementRange(me));
			int tilesExpended = 0;
			boolean modifiedLiquidOrShore = false;

			for (int i = 0; i < cloneTiles.length; i++) {
				for (int j = 0; j < cloneTiles[i].length; j++) {
					LevelTile targetTile = cloneTiles[i][j];
					if (targetTile == null) {
						continue;
					}
					
					TileItem highlightedTileItem = targetTile.tile.getTileItem();						
					TileItem tileInBucket = getCurrentTile(me);
					if (tileInBucket != null) {		
						int numTilesInBucket = getCurrentTileAmount(me);
						if (numTilesInBucket >= tilesExpended + 1) {
							if (highlightedTileItem == null || (highlightedTileItem.getID() != tileInBucket.getID())) {	
								if (level.isServer()) {
									if (highlightedTileItem != null && highlightedTileItem.getID() != TileRegistry.dirtID) {
										replacedTiles.add(highlightedTileItem);											
									}											
									tilesExpended += 1;		
									
									if (targetTile.tile.isLiquid || TileRegistry.getTile(tileInBucket.tileID).isLiquid || level.isShore(targetTile.tileX, targetTile.tileY)) {
										modifiedLiquidOrShore = true;
									}
									level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, tileInBucket.tileID);
									level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);					
								}		
							}
						}	
					}				
				}
			}
			
			if (level.isServer()) {
				if (tilesExpended > 0) {
					this.removeTilesFromBucket(me, tilesExpended);
				}
				if (modifiedLiquidOrShore && level.liquidManager != null) {
					level.liquidManager.calculateShores();
				}
				if (replacedTiles.size() > 0) {
					for (TileItem tile : replacedTiles) {
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
			int addTiles = 0;
			boolean modifiedLiquidOrShore = false;

			for (int i = 0; i < cloneTiles.length; i++) {
				for (int j = 0; j < cloneTiles[i].length; j++) {
					LevelTile targetTile = cloneTiles[i][j];
					if (targetTile == null) {
						continue;
					}			
					if (targetTile.tile.getID() == TileRegistry.dirtID) {
						continue;
					}		
					if (targetTile.tile.isLiquid || level.isShore(targetTile.tileX, targetTile.tileY)) {
						modifiedLiquidOrShore = true;
					}
					if (!hasTile(me)) {
						if (level.isServer()) {
							if (targetTile.tile.getTileItem() != null) {
								InventoryItem newItem = new InventoryItem(targetTile.tile.getTileItem());
								newItem.setAmount(1);
								setTile(level, p, me, newItem);		
								
								level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());
								level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);
							}
						}											
					} else if (getCurrentTile(me) != null && targetTile.tile.getID() == getCurrentTile(me).tileID) {		
						if (level.isServer()) { 
							addTiles += 1;
							level.sendTileChangePacket(level.getServer(), targetTile.tileX, targetTile.tileY, targetTile.tile.getDestroyedTile());
							level.tileLayer.setIsPlayerPlaced(targetTile.tileX, targetTile.tileY, true);								
						}
					}		
				}
			}		
			if (level.isServer()) {
				if (addTiles > 0) {
					int remainder = addCurrentTileAmount(me, addTiles);
					if (remainder > 0 && this.getCurrentTile(me) != null) {
						InventoryItem newItem = new InventoryItem(this.getCurrentTile(me));
						newItem.setAmount(remainder);
						p.getInv().addItem(newItem, true, "give", (InventoryAddConsumer) null);
					}
				}
				if (modifiedLiquidOrShore && level.liquidManager != null) {
					level.liquidManager.calculateShores();
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
			TileItem tileInBucket = null;
			int tileID = -1;
			if (hasTile(me)) {
				tileInBucket = getCurrentTile(me);
				if (tileInBucket != null) {
					tileID = tileInBucket.tileID;
				}
			}
		
			ConstructorTileDrawable<GameTile> highlightDraw = new ConstructorTileDrawable<GameTile>(
					perspective.getLevel(),
					perspective,
					camera,
					targetTiles,
					tileID,
					this.getMaxPlacementRange(me),
					(lvTile) -> lvTile.tile,
					(lvTile, tgTileID) -> lvTile.getID() == tgTileID
			);	
			
			highlightDraw.draw(perspective.getLevel().tickManager());
		}
	}

	@Override
	protected void openContainer(ServerClient client, PlayerInventorySlot inventorySlot) {
		PacketOpenContainer p = new PacketOpenContainer(ConstructorsMod.TERRAFORMER_CONTAINER,
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
		return arg0 instanceof TileItem;
	}

	@Override
	public boolean isValidRequestType(Item.Type type) {
		return false;
	}
	
	public InventoryItem getTileInvItem(InventoryItem me) {
		if (hasTile(me)) { 		
			Inventory _me = this.getInternalInventory(me);
			return _me.getItem(0);
		}
		return null;
	}
	
	public boolean hasTile(InventoryItem me) {			
		Inventory _me = this.getInternalInventory(me);
		return (_me.getItemSlot(0) != null) && !_me.isSlotClear(0);		
	}
	
	public TileItem getCurrentTile(InventoryItem me) {			
		if (this.hasTile(me)) {
			InventoryItem item = getTileInvItem(me);
			if (item != null && item.item instanceof TileItem) {
				return (TileItem) item.item;
			}
		}
		return null;
	}
	
	public int getCurrentTileAmount(InventoryItem me) {
		if (this.hasTile(me)) {
			Inventory _me = this.getInternalInventory(me);
			return _me.getAmount(0);
		}
		return 0;
	}	
	
	public void setCurrentTileAmount(InventoryItem me, int newAmount) {
		if (this.hasTile(me)) {			
			Inventory _me = this.getInternalInventory(me);
			_me.setAmount(0, newAmount);
			this.saveInternalInventory(me, _me);
		}
	}
	
	public int addCurrentTileAmount(InventoryItem me, int addAmount) {
	    if (this.hasTile(me)) {
	        Inventory _me = this.getInternalInventory(me);
	        InventoryItem tileInvItem = getTileInvItem(me);
	        if (tileInvItem == null) return -1;
	        
	        int limit = _me.getItemStackLimit(0, tileInvItem);
	        int currentAmount = getCurrentTileAmount(me);

	        int newAmount = Math.min(currentAmount + addAmount, limit);
	        int overTheLimit = Math.max(0, (currentAmount + addAmount) - limit);

	        setCurrentTileAmount(me, newAmount);
	        return overTheLimit;
	    }
	    return -1; 
	}
	
	private void removeTilesFromBucket(InventoryItem me, int tilesExpended) {
		if (this.hasTile(me)) {
			int now = getCurrentTileAmount(me);
			if (now <= tilesExpended) {
				setCurrentTileAmount(me, 0);
				return;
			}
			setCurrentTileAmount(me, now - tilesExpended);
		}			
	}
	
	public void setTile(Level level, PlayerMob player, InventoryItem me, InventoryItem newItem) {		
		Inventory _me = this.getInternalInventory(me);
		_me.addItem(level, player, newItem, "give", null);		
		this.saveInternalInventory(me, _me);
	}

	@Override
	protected Ingredient[] getSpecialUpgradeCost(int nextTier) {
		switch (nextTier) {
			case 1: return new Ingredient[]{
					new Ingredient(TileRegistry.getTile(TileRegistry.grassID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.sandID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.snowID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.swampGrassID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.plainsGrassID).getTileItem().getStringID(), 100)
			};
			case 2: return new Ingredient[]{
					new Ingredient(TileRegistry.getTile(TileRegistry.rockID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.sandstoneID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.swampRockID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.graniteRockID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.snowRockID).getTileItem().getStringID(), 100)
			};
			case 3: return new Ingredient[]{
					new Ingredient(TileRegistry.getTile(TileRegistry.deepRockID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.deepSandstoneID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.deepSwampRockID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.deepStoneFloorID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.deepSnowRockID).getTileItem().getStringID(), 100)
			};
			case 4: return new Ingredient[]{
					new Ingredient(TileRegistry.getTile(TileRegistry.dungeonFloorID).getTileItem().getStringID(), 100),
					new Ingredient(TileRegistry.getTile(TileRegistry.lavaID).getTileItem().getStringID(), 50)
			};
			case 5: return new Ingredient[]{
					new Ingredient("upgradeshard", 50)
			};
			default: return new Ingredient[]{new Ingredient("upgradeshard", nextTier * 20)};
		}		
	}	
}
