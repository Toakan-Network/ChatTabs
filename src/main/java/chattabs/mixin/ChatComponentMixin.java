package chattabs.mixin;

import chattabs.ChatTabs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Draws only the open tab's messages. Every message still goes into vanilla's history ({@code allMessages}), so
 * switching tabs rebuilds the drawn lines from it and other chat mods keep working on the full history.
 */
@Mixin(ChatComponent.class)
abstract class ChatComponentMixin {
    @WrapOperation(method = "addMessage", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"))
    private void chattabs$addToOpenTab(ChatComponent chat, GuiMessage message, Operation<Void> original) {
        if (ChatTabs.onAdded(message)) original.call(chat, message);
    }

    @WrapOperation(method = "refreshTrimmedMessages", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"))
    private void chattabs$refreshOpenTab(ChatComponent chat, GuiMessage message, Operation<Void> original) {
        if (ChatTabs.visible(message)) original.call(chat, message);
    }
}
