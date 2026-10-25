package chattabs.mixin;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The chat input box, so the tab bar can step aside while a command is typed. */
@Mixin(ChatScreen.class)
public interface ChatScreenAccessor {
    @Accessor("input")
    EditBox chattabs$input();
}
