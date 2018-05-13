package powercrystals.powerconverters.crafting.mods;

import cpw.mods.fml.common.registry.GameRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.common.config.Configuration;
import powercrystals.powerconverters.PowerConverterCore;
import powercrystals.powerconverters.crafting.RecipeProvider;
import powercrystals.powerconverters.power.PowerSystem;
import powercrystals.powerconverters.power.PowerSystemManager;
import powercrystals.powerconverters.power.systems.PowerRedstoneFlux;
import powercrystals.powerconverters.power.systems.PowerSteam;

/**
 * Add recipes for Buildcraft
 */
public class RecipeBuildCraft extends RecipeProvider {
    private boolean enableRecipes = true;
    public static final String RECIPE_BUILDCRAFT_CATEGORY = RECIPE_CATEGORY + ".bc";

    @Override
    public void registerRecipes() {
        try {
            ItemStack redstoneBlock = new ItemStack(GameRegistry.findBlock("minecraft", "redstone_block"), 1);

            Item cable = GameRegistry.findItem("BuildCraft|Transport", "item.buildcraftPipe.pipepowergold");
            Item struct = GameRegistry.findItem("BuildCraft|Transport", "item.buildcraftPipe.pipestructurecobblestone");
            Item conduit = GameRegistry.findItem("BuildCraft|Transport", "item.buildcraftPipe.pipepowerdiamond");
            Item fluid = GameRegistry.findItem("BuildCraft|Transport", "item.buildcraftPipe.pipefluidsgold");

            Block chest = GameRegistry.findBlock("BuildCraft|Transport", "filteredBufferBlock");
            Block engine = GameRegistry.findBlock("BuildCraft|Core", "engineBlock");
            Block pump = GameRegistry.findBlock("BuildCraft|Factory", "pumpBlock");

            if (enableRecipes) {
                if(redstoneBlock.getItem() == null
                        || cable == null
                        || conduit == null
                        || engine == null
                        || chest == null
                        || OreDictionary.getOres("gearGold").size() == 0) {
                    PowerConverterCore.instance.logger.error("Buildcraft recipe is missing items, not adding Power Converters converter recipes.");
                }
                
                GameRegistry.addRecipe(new ItemStack(PowerConverterCore.converterBlockCommon, 1, 0),
                        "PPP",
                        "PBP",
                        "PPP",
                        'B', redstoneBlock,
                        'P', conduit
                );
                GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(PowerConverterCore.converterBlockCommon, 1, 2), true, new Object[] {
                        "TGT",
                        "#S#",
                        "T#T",
                        'T', cable,
                        'S', chest,
                        '#', engine,
                        'G', "gearGold"
                }));
                PowerSystem steam = PowerSystemManager.getInstance().getPowerSystemByName(PowerSteam.id);
                if (steam != null) {
                    GameRegistry.addRecipe(new ItemStack(steam.block, 1, 0),
                            "GSG",
                            "SES",
                            "GSG",
                            'G', fluid,
                            'S', struct,
                            'E', pump
                    );
                }
            }

            PowerSystem rf = PowerSystemManager.getInstance().getPowerSystemByName(PowerRedstoneFlux.id);
            if(rf != null) {
                Block converterBlock = rf.block;
                if(cable == null
                        || struct == null) {
                    PowerConverterCore.instance.logger.error("Buildcraft recipe is missing items, not adding Power Converters recipe.");
                    return;
                }
                GameRegistry.addRecipe(new ShapedOreRecipe(new ItemStack(converterBlock, 1, 0), true, new Object[] {
                        "GSG",
                        "SES",
                        "GSG",
                        'G', cable,
                        'S', struct,
                        'E', "gearGold"
                }));
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    @Override
    public void loadConfig(Configuration c) {
        enableRecipes = c.get(RECIPE_BUILDCRAFT_CATEGORY, "enableRecipes", true).getBoolean(true);
    }

    @Override
    public void saveConfig(Configuration c) {
        c.getCategory(RECIPE_BUILDCRAFT_CATEGORY).get("enableRecipes").set(enableRecipes);
    }
}
