package powercrystals.powerconverters.power.systems.gt5;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import powercrystals.powerconverters.PowerConverterCore;
import powercrystals.powerconverters.gui.PCCreativeTab;
import powercrystals.powerconverters.power.base.BlockPowerConverter;

public class BlockGregTechProducer extends BlockPowerConverter {
    public BlockGregTechProducer() {
        super(18);
        setBlockName("powerconverters.gt.producer");
        setCreativeTab(PCCreativeTab.tab);
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new TileEntityGregTechProducer(metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister ir) {
        String[] voltages = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpmv", "uv"};
        String type = "producer";
        String[] states = {"off", "on"};
        String folderName = getUnlocalizedName().substring("tile.powerconverters.".length());
        folderName = folderName.substring(0, folderName.length() - type.length() - 1); // Strip out type

        int i = 0;
        for (String voltage : voltages) {
            for (String state : states) {
                _icons[i] = ir.registerIcon(String.format("%s:%s/%s/%s_%s", PowerConverterCore.modId, folderName, voltage, type, state));
                i++;
            }
        }
    }

    @Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int par6, float par7, float par8, float par9) {
		TileEntity te = world.getTileEntity(x, y, z);		
		
		if( !world.isRemote && te instanceof TileEntityGregTechProducer) {
			TileEntityGregTechProducer producer = (TileEntityGregTechProducer)te;

			if(player.isSneaking()){
				long newAmperage = producer.incMaxAmperage();			
				player.addChatMessage( new ChatComponentText( StatCollector.translateToLocalFormatted("powerconverters.gt.producer.amperageChanged", newAmperage) ));
			
			}
		} else {
			return super.onBlockActivated(world, x, y, z, player, par6, par7, par8, par9);
		}	
		
		return false;
	}
	
}
