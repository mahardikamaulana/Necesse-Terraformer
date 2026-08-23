package constructors;

import java.util.Collection;

import constructors.container.TerraformerContainer;
import constructors.container.BuilderContainer;
import constructors.drawables.ConstructorTileDrawable;
import constructors.form.BuilderContainerForm;
import constructors.form.TerraformerContainerForm;
import constructors.item.BuilderItem;
import constructors.item.TerraformerItem;
import necesse.engine.input.Control;
import necesse.engine.input.InputID;
import necesse.engine.modLoader.LoadedMod;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.registries.ContainerRegistry;
import necesse.engine.registries.ItemRegistry;
import necesse.gfx.gameTexture.GameTexture;
import necesse.gfx.res.ResourceEncoder;

@ModEntry
public class ConstructorsMod {
		
	public static int TERRAFORMER_CONTAINER;
	public static int BUILDER_CONTAINER;
	public static int TERRAFORMER_ITEM;
	public static int BUILDER_ITEM;
	public static Control SIZE_UP;
	public static Control SIZE_DOWN;

    public void init() {
    	
    	TERRAFORMER_CONTAINER = ContainerRegistry.registerContainer((client, uniqueSeed, content) -> {
			return new TerraformerContainerForm(client,					
					new TerraformerContainer(client.getClient(), uniqueSeed, content));	
		}, (client, uniqueSeed, content, serverObject) -> {
			return new TerraformerContainer(client, uniqueSeed, content);
		});
    	
    	BUILDER_CONTAINER = ContainerRegistry.registerContainer((client, uniqueSeed, content) -> {
			return new BuilderContainerForm(client,					
					new BuilderContainer(client.getClient(), uniqueSeed, content));	
		}, (client, uniqueSeed, content, serverObject) -> {
			return new BuilderContainer(client, uniqueSeed, content);
		});
         
    	TERRAFORMER_ITEM = ItemRegistry.registerItem("terraformer", new TerraformerItem(), 5000, true);
    	BUILDER_ITEM = ItemRegistry.registerItem("builder", new BuilderItem(), 5000, true);

    	necesse.engine.registries.PacketRegistry.registerPacket(constructors.packet.PacketConstructorSize.class);
    	
    	SIZE_UP = Control.addModControl(new Control(InputID.KEY_PERIOD, "terraformersizeup", new necesse.engine.localization.message.LocalMessage("terraformer", "terraformercontrolsizeup")));
    	SIZE_DOWN = Control.addModControl(new Control(InputID.KEY_COMMA,  "terraformersizedown", new necesse.engine.localization.message.LocalMessage("terraformer", "terraformercontrolsizedown")));
    }

    public void initResources() {
    	ConstructorTileDrawable.highlightTexture = GameTexture.fromFile("tiles/tile_highlight");
    	ResourceEncoder.addModResources(LoadedMod.getRunningMod());
    }

    public void postInit() {
    }
}
