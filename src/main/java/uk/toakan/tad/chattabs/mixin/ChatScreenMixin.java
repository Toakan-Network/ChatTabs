package uk.toakan.tad.chattabs.mixin;

import uk.toakan.tad.chattabs.ChatTabs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sends what the player typed to the open tab's channel ("/gc hi" from the Guild tab). Only chat typed and sent
 * with Enter passes here; commands and other mods' messages are left alone.
 */
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @WrapOperation(method = "handleChatInput", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;sendChat(Ljava/lang/String;)V"))
    private void chattabs$sendToOpenTab(ClientPacketListener connection, String message, Operation<Void> original) {
        String command = ChatTabs.commandFor(message);
        if (command != null) connection.sendCommand(command);
        else original.call(connection, message);
    }
}
