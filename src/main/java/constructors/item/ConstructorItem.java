package constructors.item;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

import constructors.ConstructorsMod;
import necesse.engine.GameState;
import necesse.engine.localization.Localization;
import necesse.engine.network.gameNetworkData.GNDItemMap;
import necesse.engine.util.GameBlackboard;
import necesse.engine.world.GameClock;
import necesse.engine.world.WorldSettings;
import necesse.entity.Entity;
import necesse.entity.TileEntity;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.entity.mobs.itemAttacker.ItemAttackSlot;
import necesse.entity.mobs.itemAttacker.ItemAttackerMob;
import necesse.gfx.GameColor;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.gameTexture.GameSprite;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.gameTooltips.ListGameTooltips;
import necesse.inventory.Inventory;
import necesse.inventory.InventoryItem;
import necesse.inventory.item.ItemInteractAction;
import necesse.inventory.item.ItemStatTip;
import necesse.inventory.item.ItemStatTipList;
import necesse.inventory.item.LocalMessageDoubleItemStatTip;
import necesse.inventory.item.miscItem.PouchItem;
import necesse.inventory.item.upgradeUtils.IntUpgradeValue;
import necesse.inventory.item.upgradeUtils.UpgradableItem;
import necesse.inventory.item.upgradeUtils.UpgradedItem;
import necesse.inventory.recipe.Ingredient;
import necesse.level.maps.Level;
import necesse.level.maps.LevelTile;
import necesse.level.maps.TilePosition;

public abstract class ConstructorItem extends PouchItem implements UpgradableItem, ItemInteractAction {

	public static enum Shape {
		SQUARE,
		CHECKERBOARD,
		LINE,
		CIRCLE,
		RING,
		LINE_BOX
	}
	
	public static enum LineDirection {
		HORIZONTAL, 
		VERTICAL,
		DIAGONAL_TL_BR,
		DIAGONAL_TR_BL
	}
	
	public static final int MAX_UPGRADE_TIER = 5;
	
	public final Map<Shape, ShapeSelection> shapes = new HashMap<Shape, ShapeSelection>();
	protected boolean shapes_initialized = false;
	
	public IntUpgradeValue maxPlacementRange;
	public IntUpgradeValue maxShapeSize;
	public int minShapeSize = 1;
	
	public ConstructorItem() {
		super();	
		initializeShapes();
		
		this.maxPlacementRange = new IntUpgradeValue();
		this.maxPlacementRange.setBaseValue(12);
		this.maxPlacementRange.setUpgradedValue(1.0F, 16);
		this.maxPlacementRange.setUpgradedValue(2.0F, 20);
		this.maxPlacementRange.setUpgradedValue(3.0F, 26);
		this.maxPlacementRange.setUpgradedValue(4.0F, 32);
		this.maxPlacementRange.setUpgradedValue(5.0F, 40);

		this.maxShapeSize = new IntUpgradeValue();
		this.maxShapeSize.setBaseValue(5);
		this.maxShapeSize.setUpgradedValue(1.0F, 7);
		this.maxShapeSize.setUpgradedValue(2.0F, 9);
		this.maxShapeSize.setUpgradedValue(3.0F, 11);
		this.maxShapeSize.setUpgradedValue(4.0F, 13);
		this.maxShapeSize.setUpgradedValue(5.0F, 15);
		
		this.stackSize = 1;			
		this.attackCooldownTime = new IntUpgradeValue().setBaseValue(100);			
		this.rarity = Rarity.UNIQUE;
	}

	protected GameTexture[] tierTextures;

	@Override
	protected void loadItemTextures() {
		super.loadItemTextures();
		this.tierTextures = new GameTexture[MAX_UPGRADE_TIER + 1];
		this.tierTextures[0] = this.itemTexture;
		for (int i = 1; i <= MAX_UPGRADE_TIER; i++) {
			this.tierTextures[i] = GameTexture.fromFile("items/" + this.getStringID() + "_tier" + i, this.itemTexture);
		}
	}

	@Override
	public GameSprite getItemSprite(InventoryItem item, PlayerMob player) {
		if (item != null && this.tierTextures != null) {
			int tier = Math.max(0, Math.min(MAX_UPGRADE_TIER, (int) this.getUpgradeTier(item)));
			if (tier < this.tierTextures.length && this.tierTextures[tier] != null) {
				return new GameSprite(this.tierTextures[tier]);
			}
		}
		return super.getItemSprite(item, player);
	}

	@Override
	public GameSprite getWorldItemSprite(InventoryItem item, PlayerMob player) {
		if (item != null && this.tierTextures != null) {
			int tier = Math.max(0, Math.min(MAX_UPGRADE_TIER, (int) this.getUpgradeTier(item)));
			if (tier < this.tierTextures.length && this.tierTextures[tier] != null) {
				return new GameSprite(this.tierTextures[tier]);
			}
		}
		return super.getWorldItemSprite(item, player);
	}

	public Shape getShape(InventoryItem item) {
		if (item == null) return Shape.SQUARE;
		String shapeName = item.getGndData().getString("shape", Shape.SQUARE.name());
		try {
			return Shape.valueOf(shapeName);
		} catch (Exception e) {
			return Shape.SQUARE;
		}
	}

	public void setShape(InventoryItem item, Shape shape) {
		if (item != null && shape != null) {
			item.getGndData().setString("shape", shape.name());
		}
	}

	public ShapeSelection getShapeSelection(InventoryItem item) {
		Shape shape = getShape(item);
		ShapeSelection selection = shapes.get(shape);
		return selection != null ? selection : shapes.get(Shape.SQUARE);
	}

	public LevelTile[][] getTargetTiles(InventoryItem item, PlayerMob player, TilePosition pos) {
		if (item == null || player == null || pos == null) return new LevelTile[0][0];
		ShapeSelection selection = getShapeSelection(item);
		int size = getShapeSize(item);
		return selection.getTilesAround(player, pos, size);
	}

	public int getShapeSize(InventoryItem item) {
		if (item == null) return this.minShapeSize;
		int maxSize = this.maxShapeSize.getValue(this.getUpgradeTier(item));
		int savedSize = item.getGndData().getInt("shapeSize", 3);
		return Math.max(this.minShapeSize, Math.min(maxSize, savedSize));
	}

	public void setShapeSize(InventoryItem item, int size) {
		if (item != null) {
			int maxSize = this.maxShapeSize.getValue(this.getUpgradeTier(item));
			int clamped = Math.max(this.minShapeSize, Math.min(maxSize, size));
			item.getGndData().setInt("shapeSize", clamped);
		}
	}

	public void modShapeSize(int mod, InventoryItem item) {
		if (item != null) {
			int current = getShapeSize(item);
			setShapeSize(item, current + mod);
		}
	}

	public int getMaxPlacementRange(InventoryItem item) {
		if (item == null) return this.maxPlacementRange.getValue(0.0F);
		return this.maxPlacementRange.getValue(this.getUpgradeTier(item));
	}

	public int getMaxShapeSize(InventoryItem item) {
		if (item == null) return this.maxShapeSize.getValue(0.0F);
		return this.maxShapeSize.getValue(this.getUpgradeTier(item));
	}

	public ListGameTooltips getPreEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
			GameBlackboard blackboard) {
		ListGameTooltips tooltips = new ListGameTooltips();
		ItemAttackerMob equippedMob = (ItemAttackerMob) blackboard.get(ItemAttackerMob.class, "equippedMob",
				perspective);
		if (equippedMob == null) {
			equippedMob = (ItemAttackerMob) blackboard.get(ItemAttackerMob.class, "perspective", perspective);
		}
		if (equippedMob == null) {
			equippedMob = perspective;
		}
		tooltips.add(new necesse.gfx.gameTooltips.SpacerGameTooltip(12));
		this.addStatTooltips(tooltips, item, (InventoryItem) blackboard.get(InventoryItem.class, "compareItem"),
				blackboard.getBoolean("showDifference"), blackboard.getBoolean("forceAdd"),
				equippedMob);
		if (this.getUpgradeTier(item) < (float) MAX_UPGRADE_TIER) {
			tooltips.add(new necesse.engine.localization.message.LocalMessage("constructor.ui", "itemupgradeable"));
		}
		return tooltips;
	}

	public ListGameTooltips getPostEnchantmentTooltips(InventoryItem item, PlayerMob perspective,
			GameBlackboard blackboard) {
		return new ListGameTooltips();
	}

	@Override
	public final ListGameTooltips getTooltips(InventoryItem item, PlayerMob perspective, GameBlackboard blackboard) {
		ListGameTooltips tooltips = super.getTooltips(item, perspective, blackboard);
		tooltips.add(this.getPreEnchantmentTooltips(item, perspective, blackboard));
		tooltips.add(this.getPostEnchantmentTooltips(item, perspective, blackboard));
		return tooltips;
	}
	
	@Override
	public String getCanBeUpgradedError(InventoryItem item) {			
		return this.getUpgradeTier(item) >= (float) MAX_UPGRADE_TIER
				? Localization.translate("constructor.ui", "itemupgradelimit")
				: null;
	}

	@Override
	public void addUpgradeStatTips(ItemStatTipList list, InventoryItem lastItem, InventoryItem upgradedItem,
			ItemAttackerMob perspective, ItemAttackerMob statPerspective) {
		float tier = this.getUpgradeTier(upgradedItem);
		float lastTier = lastItem == null ? tier : this.getUpgradeTier(lastItem);
		
		ItemStatTip tierTip = new LocalMessageDoubleItemStatTip("itemtooltip", "tooltier", "value", (double) tier, 0)
				.setCompareValue((double) lastTier);
		list.add(Integer.MIN_VALUE, tierTip);
		this.addStatTooltips(list, upgradedItem, lastItem, perspective, true);
	}
	
	public final void addStatTooltips(ListGameTooltips tooltips, InventoryItem currentItem, InventoryItem lastItem,
			boolean showDifference, boolean forceAdd, ItemAttackerMob perspective) {
		ItemStatTipList list = new ItemStatTipList();
		this.addStatTooltips(list, currentItem, lastItem, perspective, forceAdd);
		Iterator<ItemStatTip> it = list.iterator();
		while (it.hasNext()) {
			ItemStatTip itemStatTip = it.next();
			tooltips.add(itemStatTip.toTooltip((Color) GameColor.GREEN.color.get(), (Color) GameColor.RED.color.get(),
					(Color) GameColor.YELLOW.color.get(), showDifference));
		}
	}
	
	public void addMaxRangeTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			Mob perspective, boolean forceAdd) {
		int currentMaxRange = this.maxPlacementRange.getValue(this.getUpgradeTier(currentItem));
		LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("constructor.itemtooltip", "rangetip", "value",
			(double) currentMaxRange, 0);
		
		if (lastItem != null) {
			int lastMaxRange = this.maxPlacementRange.getValue(this.getUpgradeTier(lastItem));
			tip.setCompareValue((double) lastMaxRange);
		}

		list.add(250, tip);
	}
	
	public void addMaxSizeTip(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			Mob perspective, boolean forceAdd) {
		int currentMaxSize = this.maxShapeSize.getValue(this.getUpgradeTier(currentItem));
		LocalMessageDoubleItemStatTip tip = new LocalMessageDoubleItemStatTip("constructor.itemtooltip", "sizetip", "value",
				(double) currentMaxSize, 0);
		
		if (lastItem != null) {
			int lastMaxSize = this.maxShapeSize.getValue(this.getUpgradeTier(lastItem));
			tip.setCompareValue((double) lastMaxSize);
		}

		list.add(260, tip);
	}
	
	public void addStatTooltips(ItemStatTipList list, InventoryItem currentItem, InventoryItem lastItem,
			ItemAttackerMob perspective, boolean forceAdd) {
		this.addMaxRangeTip(list, currentItem, lastItem, perspective, forceAdd);
		this.addMaxSizeTip(list, currentItem, lastItem, perspective, forceAdd);
	}
	
	protected int getNextUpgradeTier(InventoryItem item) {
		int currentTier = (int) this.getUpgradeTier(item);
		return Math.min(MAX_UPGRADE_TIER, currentTier + 1);
	}
	
	@Override
	public UpgradedItem getUpgradedItem(InventoryItem item) {
		int nextTier = this.getNextUpgradeTier(item);
		InventoryItem upgradedItem = item.copy();
		this.setUpgradeTier(upgradedItem, (float) nextTier);
		return new UpgradedItem(item, upgradedItem, this.getSpecialUpgradeCost(nextTier));
	}
	
	protected abstract Ingredient[] getSpecialUpgradeCost(int nextTier);

	public abstract void initializeShapes();
	
	protected void clearOutOfRangeTiles(LevelTile[][] cloneTiles, PlayerMob perspective, int range) {
		int rangeSq = range * range;
		int playerTileX = perspective.getTileX();
		int playerTileY = perspective.getTileY();

		for (int i = 0; i < cloneTiles.length; i++) {
			for (int j = 0; j < cloneTiles[i].length; j++) {
				LevelTile targetTile = cloneTiles[i][j];
				if (targetTile == null) continue;
				int dx = targetTile.tileX - playerTileX;
				int dy = targetTile.tileY - playerTileY;
				if ((dx * dx + dy * dy) > rangeSq) {
					cloneTiles[i][j] = null;
				}
			}
		}
	}
	
	@Override
	public abstract InventoryItem onAttack(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int animAttack, int seed, GNDItemMap mapContent);		

	@Override
	public abstract InventoryItem onLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, int attackHeight,
			InventoryItem me, ItemAttackSlot slot, int seed, GNDItemMap mapContent);
	
	@Override
	public boolean canLevelInteract(Level level, int x, int y, ItemAttackerMob attackerMob, InventoryItem item) {
		return true;
	}
	
	@Override
	public abstract void onMouseHoverTile(InventoryItem me, GameCamera camera, PlayerMob perspective, int mouseX, int mouseY,
			TilePosition pos, boolean isDebug);
	
	@Override
	public void tick(Inventory arg0, int arg1, InventoryItem me, GameClock arg3, GameState arg4, Entity arg5,
	                 TileEntity arg6, WorldSettings arg7, Consumer<InventoryItem> arg8) {
	    super.tick(arg0, arg1, me, arg3, arg4, arg5, arg6, arg7, arg8);
	    
	    if (!(arg5 instanceof PlayerMob)) return;
	   
	    PlayerMob p = (PlayerMob) arg5;
	    // Only the local client controlling this player, and ONLY the actively selected item slot should process hotkeys
	    if (p.isClient() && p.isClientClient() && p.getSelectedItem() == me) {
	        necesse.engine.network.client.Client client = p.getClientClient() != null ? p.getClientClient().getClient() : (p.getLevel() != null ? p.getLevel().getClient() : null);
	        if (client != null && !client.hasFocusForm()) {
	            boolean sizeUp = ConstructorsMod.SIZE_UP != null && ConstructorsMod.SIZE_UP.isPressed();
	            boolean sizeDown = ConstructorsMod.SIZE_DOWN != null && ConstructorsMod.SIZE_DOWN.isPressed();
	            
	            if (sizeUp || sizeDown) {
	                int delta = sizeUp ? 1 : -1;
	                modShapeSize(delta, me);
	                int actualSize = getShapeSize(me);
	                int slot = p.getSelectedSlot();
	                client.network.sendPacket(new constructors.packet.PacketConstructorSize(slot, actualSize));
	                
	                if (p.getLevel() != null && p.getLevel().hudManager != null) {
	                    String sizeText = Localization.translate("terraformer", "shapeadjustment") + " " + actualSize;
	                    p.getLevel().hudManager.addElement(new necesse.level.maps.hudManager.floatText.FloatTextFade((int) p.x, (int) p.y - 30, sizeText, new necesse.gfx.gameFont.FontOptions(16).color(Color.WHITE)));
	                }
	            }
	        }
	    }
	}
	
	public static abstract class ShapeSelection {			
		public final Shape shapeID;
		public final String shapeName; 

		protected ConstructorItem item;
		public ShapeSelection(ConstructorItem item, Shape shapeID, String shapeName) {
			this.shapeID = shapeID;
			this.shapeName = shapeName;
			this.item = item;
		}
		
		public int shapeSize(InventoryItem me) {
			return item != null ? item.getShapeSize(me) : 3;
		}
		
		public int maxSize(InventoryItem me) {
			return item != null ? item.maxShapeSize.getValue(item.getUpgradeTier(me)) : 5;
		}
		
		public int minSize() {
			return item != null ? item.minShapeSize : 1;
		}
			
		public abstract LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize);
	}
	
	public static class ShapeSelectionSquare extends ShapeSelection {		
		public ShapeSelectionSquare(ConstructorItem item) {
			super(item, Shape.SQUARE, "square");
		}

		@Override
		public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
		    if (currentSize <= 0 || player == null || p == null) return new LevelTile[0][0];
		    
		    Level l = player.getLevel();
		    LevelTile[][] shape = new LevelTile[currentSize][currentSize];

		    int startX = p.tileX - (currentSize / 2);
		    int startY = p.tileY - (currentSize / 2);
		    int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
		    int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);

		    int _x = 0;
		    for (int x = startX; x <= endX; x++) { 
		        int _y = 0;
		        for (int y = startY; y <= endY; y++) { 
		            shape[_x][_y] = l.getLevelTile(x, y);
		            _y++;
		        }
		        _x++;
		    }
		    return shape;
		}
	}
	
	public static class ShapeSelectionLineBox extends ShapeSelection {        
	    public ShapeSelectionLineBox(ConstructorItem item) {        
	        super(item, Shape.LINE_BOX, "linebox");    
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
	        if (currentSize <= 0 || player == null || p == null) return new LevelTile[0][0];
	        
	        Level l = player.getLevel();
	        LevelTile[][] shape = new LevelTile[currentSize][currentSize];

	        int startX = p.tileX - (currentSize / 2);
	        int startY = p.tileY - (currentSize / 2);
	        int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);

	        int _x = 0;
	        for (int x = startX; x <= endX; x++) { 
	            int _y = 0;
	            for (int y = startY; y <= endY; y++) { 
	                if (x == startX || x == endX || y == startY || y == endY) {
	                    shape[_x][_y] = l.getLevelTile(x, y);
	                } else {
	                    shape[_x][_y] = null;
	                }
	                _y++;
	            }
	            _x++;
	        }
	        return shape;
	    }
	}

	public static class ShapeSelectionCheckerboard extends ShapeSelection {
	    public ShapeSelectionCheckerboard(ConstructorItem item) {
	        super(item, Shape.CHECKERBOARD, "checkerboard");
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
	        if (currentSize <= 0 || player == null || p == null) return new LevelTile[0][0];
	        
	        Level l = player.getLevel();
	        LevelTile[][] shape = new LevelTile[currentSize][currentSize];
	        
	        int startX = p.tileX - (currentSize / 2);
	        int startY = p.tileY - (currentSize / 2);
	        int endX = p.tileX + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        int endY = p.tileY + (currentSize / 2) + (currentSize % 2 == 0 ? -1 : 0);
	        
	        int _x = 0;
	        for (int x = startX; x <= endX; x++) { 
	            int _y = 0;
	            for (int y = startY; y <= endY; y++) { 
	                if ((_x + _y) % 2 == 0) {
	                    shape[_x][_y] = l.getLevelTile(x, y);
	                } else {
	                    shape[_x][_y] = null;
	                }
	                _y++;
	            }
	            _x++;
	        }
	        return shape;
	    }
	}

	public static class ShapeSelectionCircle extends ShapeSelection {
	    public ShapeSelectionCircle(ConstructorItem item) {
	        super(item, Shape.CIRCLE, "circle");	  
	    }
	    
	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
	    	if (currentSize < 3 || player == null || p == null) return new LevelTile[0][0];	        
	        
		    Level l = player.getLevel();
	        int radius = currentSize / 2;
	        int diameter = radius * 2 + 1;
	        int radiusSq = radius * radius;

	        LevelTile[][] shape = new LevelTile[diameter][diameter];

	        int centerX = p.tileX;
	        int centerY = p.tileY;

	        for (int x = -radius; x <= radius; x++) {
	            for (int y = -radius; y <= radius; y++) {
	                int distSq = x * x + y * y;
	                if (distSq <= radiusSq) {
	                    int _x = x + radius;
	                    int _y = y + radius;

	                    if (_x >= 0 && _x < diameter && _y >= 0 && _y < diameter) {
	                        shape[_x][_y] = l.getLevelTile(centerX + x, centerY + y);
	                    }
	                }
	            }
	        }

	        return shape;
	    }
	}
	
	public static class ShapeSelectionRing extends ShapeSelection {
	    public ShapeSelectionRing(ConstructorItem item) {
	        super(item, Shape.RING, "ring");
	    }
	    
	    @Override
	    public int minSize() {
	    	return this.item != null ? Math.max(3, this.item.minShapeSize) : 3;
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
	    	if (currentSize < 3 || player == null || p == null) return new LevelTile[0][0];
	        Level l = player.getLevel();

	        int radius = currentSize / 2;
	        int innerRadius = Math.max(1, radius - 1);
	        int diameter = radius * 2 + 1;
	        int radiusSq = radius * radius;
	        int innerRadiusSq = innerRadius * innerRadius;

	        LevelTile[][] shape = new LevelTile[diameter][diameter];

	        int centerX = p.tileX;
	        int centerY = p.tileY;

	        for (int x = -radius; x <= radius; x++) {
	            for (int y = -radius; y <= radius; y++) {
	                int distSq = x * x + y * y;
	                if (distSq <= radiusSq && distSq >= innerRadiusSq) {
	                    int _x = x + radius;
	                    int _y = y + radius;

	                    if (_x >= 0 && _x < diameter && _y >= 0 && _y < diameter) {
	                        shape[_x][_y] = l.getLevelTile(centerX + x, centerY + y);
	                    }
	                }
	            }
	        }

	        return shape;
	    }
	}
	
	public static class ShapeSelectionLine extends ShapeSelection {		  
	    public ShapeSelectionLine(ConstructorItem item) {
	        super(item, Shape.LINE, "line");
	    }

	    @Override
	    public LevelTile[][] getTilesAround(PlayerMob player, TilePosition p, int currentSize) {
	    	if (currentSize <= 0 || player == null || p == null) return new LevelTile[0][0];	        
	        Level l = player.getLevel();
	        
	        boolean vertical = (player.getDir() == 0 || player.getDir() == 2);
	        LevelTile[][] shape = vertical ? new LevelTile[currentSize][1] : new LevelTile[1][currentSize];

	        int startX = p.tileX;
	        int startY = p.tileY;

	        for (int i = 0; i < currentSize; i++) {
	            if (vertical) {
	                int y = startY - (currentSize / 2) + i;
	                shape[i][0] = l.getLevelTile(startX, y);
	            } else {
	                int x = startX - (currentSize / 2) + i;
	                shape[0][i] = l.getLevelTile(x, startY);
	            }
	        }

	        return shape;
	    }
	}
}
