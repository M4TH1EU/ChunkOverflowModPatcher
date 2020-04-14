package ch.m4th1eu.chunkpatcher.mixins;

import io.netty.buffer.Unpooled;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenBook;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.io.IOException;

@Mixin(GuiScreenBook.class)
public class MixinChunkPatcher extends GuiScreen {

    @Shadow
    @Final
    private boolean bookIsUnsigned;

    @Shadow
    private boolean bookIsModified;

    @Shadow
    private NBTTagList bookPages;

    @Shadow
    @Final
    private ItemStack book;

    @Shadow
    @Final
    private EntityPlayer editingPlayer;

    @Shadow
    private String bookTitle;

    /**
     * @author M4TH1EU_#0001
     * @reason Fix the chunk overflow glitches.
     */
    @Overwrite
    private void sendBookToServer(boolean publish) throws IOException {
        if (bookIsUnsigned && this.bookIsModified) {
            if (this.bookPages != null) {
                if (isChinese(bookPages.toString())) {
                    editingPlayer.sendMessage(new TextComponentString(TextFormatting.RED + "Vous ne pouvez pas écrire des caractères spéciaux dans les livres."));
                    return;
                }

                while (this.bookPages.tagCount() > 1) {
                    String s = this.bookPages.getStringTagAt(this.bookPages.tagCount() - 1);
                    if (!s.isEmpty()) {
                        break;
                    }

                    this.bookPages.removeTag(this.bookPages.tagCount() - 1);
                }

                if (this.book.hasTagCompound()) {
                    NBTTagCompound nbttagcompound = this.book.getTagCompound();
                    nbttagcompound.setTag("pages", this.bookPages);
                } else {
                    this.book.setTagInfo("pages", this.bookPages);
                }

                String s1 = "MC|BEdit";

                if (publish) {
                    s1 = "MC|BSign";
                    this.book.setTagInfo("author", new NBTTagString(this.editingPlayer.getName()));
                    this.book.setTagInfo("title", new NBTTagString(this.bookTitle.trim()));
                }

                PacketBuffer packetbuffer = new PacketBuffer(Unpooled.buffer());
                packetbuffer.writeItemStack(this.book);
                this.mc.getConnection().sendPacket(new CPacketCustomPayload(s1, packetbuffer));
            }
        }
    }

    private boolean isChinese(final String text) {
        /*final CharsetEncoder asciiEncoder = StandardCharsets.US_ASCII.newEncoder();
        final CharsetEncoder isoEncoder = StandardCharsets.ISO_8859_1.newEncoder();
        return asciiEncoder.canEncode(text) || isoEncoder.canEncode(text);*/

        return text.codePoints().anyMatch(
                codepoint ->
                        Character.UnicodeScript.of(codepoint) == Character.UnicodeScript.HAN);
    }
}
