package powercrystals.powerconverters.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

/**
 * Handler to redirect message processing.
 * Used to easily unify 1.7 and 1.8 code in a thread-safe way.
 */
public abstract class GenericHandler<REQ extends IMessage> implements IMessageHandler<REQ, IMessage> {
    @Override
    public IMessage onMessage(REQ message, MessageContext ctx) {
        processMessage(message, ctx);
        return null;
    }

    public abstract void processMessage(REQ message, MessageContext context);
}
