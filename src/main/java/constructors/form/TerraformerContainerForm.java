package constructors.form;

import constructors.container.TerraformerContainer;
import necesse.engine.network.client.Client;

public class TerraformerContainerForm extends ConstructorContainerForm<TerraformerContainer> {
	public TerraformerContainerForm(Client client, final TerraformerContainer container) {
		super(client, "Terraformer Settings", container);
	}
}
