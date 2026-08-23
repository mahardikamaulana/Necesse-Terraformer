package constructors.form;

import constructors.container.BuilderContainer;
import necesse.engine.network.client.Client;

public class BuilderContainerForm extends ConstructorContainerForm<BuilderContainer> {
	public BuilderContainerForm(Client client, final BuilderContainer container) {
		super(client, "Builder Settings", container);
	}
}
