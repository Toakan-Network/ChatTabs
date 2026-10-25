package chattabs.mixin;

import chattabs.ChatFrame;
import chattabs.ChatTabs;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

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

    /**
     * Records where chat is, for the tab bar, open or closed. Vanilla's one updatePose call scales chat; any mod that
     * moves chat has changed the pose before it, so the pose after it maps chat's own coordinates to the screen.
     */
    @WrapOperation(method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;updatePose(Ljava/util/function/Consumer;)V"))
    private void chattabs$recordFrame(ChatComponent.ChatGraphicsAccess graphics, Consumer<Matrix3x2f> updater,
                                      Operation<Void> original, @Local(argsOnly = true, ordinal = 0) int screenHeight) {
        original.call(graphics, (Consumer<Matrix3x2f>) pose -> {
            updater.accept(pose);
            ChatFrame.record(pose, screenHeight);
        });
    }
}
