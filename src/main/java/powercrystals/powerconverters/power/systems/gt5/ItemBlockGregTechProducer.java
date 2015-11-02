package powercrystals.powerconverters.power.systems.gt5;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import powercrystals.powerconverters.power.systems.PowerGregTech5;

import java.util.List;

public class ItemBlockGregTechProducer extends ItemBlock {
    public ItemBlockGregTechProducer(Block block) {
        super(block);
        setHasSubtypes(true);
        setMaxDamage(0);
    }

    @Override
    public int getMetadata(int i) {
        return i;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        //noinspection unchecked
        par3List.add(EnumChatFormatting.BLUE + "Max EU out: " + PowerGregTech5.VOLTAGE_VALUES[par1ItemStack.getItemDamage()]);
    }

    @Override
    public String getUnlocalizedName(ItemStack itemstack) {
        int md = itemstack.getItemDamage();
        if (md == 0)
            return "powerconverters.gt.ulv.producer";
        else if (md == 1)
            return "powerconverters.gt.lv.producer";
        else if (md == 2)
            return "powerconverters.gt.mv.producer";
        else if (md == 3)
            return "powerconverters.gt.hv.producer";
        else if (md == 4)
            return "powerconverters.gt.ev.producer";
        else if (md == 5)
            return "powerconverters.gt.iv.producer";
        else if (md == 6)
            return "powerconverters.gt.luv.producer";
        else if (md == 7)
            return "powerconverters.gt.zpmv.producer";
        else if (md == 8)
            return "powerconverters.gt.uv.producer";

        return "unknown";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public void getSubItems(Item item, CreativeTabs creativeTab, List subTypes) {
        for (int i = 0; i < 9; i++) {
            subTypes.add(new ItemStack(item, 1, i));
        }
    }
}
