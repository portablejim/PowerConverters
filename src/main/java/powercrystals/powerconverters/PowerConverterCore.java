package powercrystals.powerconverters;

import com.google.common.base.Throwables;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.Logger;
import powercrystals.powerconverters.common.BlockPowerConverterCommon;
import powercrystals.powerconverters.common.ItemBlockPowerConverterCommon;
import powercrystals.powerconverters.common.TileEntityCharger;
import powercrystals.powerconverters.common.TileEntityEnergyBridge;
import powercrystals.powerconverters.crafting.RecipeProvider;
import powercrystals.powerconverters.crafting.mods.RecipeGregTech5;
import powercrystals.powerconverters.crafting.mods.RecipeBuildCraft;
import powercrystals.powerconverters.crafting.mods.RecipeEnderIO;
import powercrystals.powerconverters.crafting.mods.RecipeFactorization;
import powercrystals.powerconverters.crafting.mods.RecipeForestry;
import powercrystals.powerconverters.crafting.mods.RecipeIndustrialCraft;
import powercrystals.powerconverters.crafting.mods.RecipeRailcraft;
import powercrystals.powerconverters.crafting.mods.RecipeThermalExpansion;
import powercrystals.powerconverters.crafting.mods.RecipeVanilla;
import powercrystals.powerconverters.gui.PCGUIHandler;
import powercrystals.powerconverters.network.PacketClientSync;
import powercrystals.powerconverters.power.PowerSystemManager;
import powercrystals.powerconverters.power.systems.PowerFactorization;
import powercrystals.powerconverters.power.systems.PowerIndustrialcraft;
import powercrystals.powerconverters.power.systems.PowerRedstoneFlux;
import powercrystals.powerconverters.power.systems.PowerSteam;
import powercrystals.powerconverters.power.systems.PowerGregTech5;

import java.io.File;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

@Mod(modid = PowerConverterCore.modId, name = PowerConverterCore.modName, dependencies = "after:BuildCraft|Energy;after:factorization;after:IC2;after:Railcraft;after:ThermalExpansion;after:gregtech")
public final class PowerConverterCore {
    public static final String modId = "PowerConverters";
    public static final String modName = "Power Converters";
    public static final String version;

    static {
        Properties prop = new Properties();

        try {
            InputStream stream = PowerConverterCore.class.getClassLoader().getResourceAsStream("version.properties");
            prop.load(stream);
            stream.close();
        } catch (Exception e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            Throwables.propagate(e);
        }

        version = String.format("%s_build-%s", prop.getProperty("version"), prop.getProperty("build_number"));
    }

    public static final String guiFolder = modId + ":" + "textures/gui/";

    public static Block converterBlockCommon;

    @Mod.Instance(modId)
    public static PowerConverterCore instance;

    public static int bridgeBufferSize;

    private Set<RecipeProvider> enabledRecipes;

    public SimpleNetworkWrapper networkWrapper;

    public Logger logger;

    @SuppressWarnings("UnusedDeclaration")
    @EventHandler
    public void preInit(FMLPreInitializationEvent evt) {
        logger = evt.getModLog();
        evt.getModMetadata().version = PowerConverterCore.version;

        FMLCommonHandler.instance().bus().register(this);

        networkWrapper = NetworkRegistry.INSTANCE.newSimpleChannel("powerconverters");
        networkWrapper.registerMessage(PacketClientSync.Handler.class, PacketClientSync.class, 0, Side.CLIENT);

        registerPowerSystems();

        File dir = evt.getModConfigurationDirectory();
        loadConfig(dir);

        converterBlockCommon = new BlockPowerConverterCommon();
        GameRegistry.registerBlock(converterBlockCommon, ItemBlockPowerConverterCommon.class, "converter.common");
        GameRegistry.registerTileEntity(TileEntityEnergyBridge.class, "powerConverterEnergyBridge");
        GameRegistry.registerTileEntity(TileEntityCharger.class, "powerConverterUniversalCharger");
    }

    @SuppressWarnings("UnusedParameters")
    @EventHandler
    public void init(FMLInitializationEvent evt) throws Exception {
        PowerSystemManager.getInstance().registerBlocks();
    }

    @SuppressWarnings("UnusedDeclaration")
    @EventHandler
    public void postInit(FMLPostInitializationEvent unused) throws Exception {
        logger.info("+++++++++++++++++++++++++[PowerConverters][NOTICE]+++++++++++++++++++++++++");
        logger.info("Default power ratios are based on FTB standards and not all mods follow these");
        logger.info("If you find conflicting ratios in your pack, please adjust the config acoordingly or remove PC");
        logger.info("These conflicts are not the fault of any mod author. It is the nature on minecraft");
        logger.info("-------------------------[PowerConverters][STEAM]-------------------------");
        logger.info("Default steam ratios, while based on standards, will create infinite energy loops");
        logger.info("To prevent over powered infinite energy, use a steam throttle values of less than 5");
        logger.info("+++++++++++++++++++++++++[PowerConverters][NOTICE]+++++++++++++++++++++++++");

        PowerSystemManager.getInstance().registerCommonRecipes();
        for(RecipeProvider provider : enabledRecipes) {
            provider.registerRecipes();
        }

        NetworkRegistry.INSTANCE.registerGuiHandler(instance, new PCGUIHandler());
        MinecraftForge.EVENT_BUS.register(instance);
    }

    private void registerPowerSystems(){
        PowerSystemManager manager = PowerSystemManager.getInstance();
        enabledRecipes = new HashSet<RecipeProvider>();

        enabledRecipes.add(new RecipeVanilla());
        if(FluidRegistry.isFluidRegistered("steam")) {
            if(manager.getPowerSystemByName(PowerSteam.id) == null) {
                manager.registerPowerSystem(new PowerSteam());
            }
            ((PowerSteam) manager.getPowerSystemByName(PowerSteam.id)).addSteamType("steam", "Steam", 500, 500);
        }

        if(Loader.isModLoaded("BuildCraft|Energy")) {
            enabledRecipes.add(new RecipeBuildCraft());
            if(manager.getPowerSystemByName(PowerRedstoneFlux.id) == null) {
                manager.registerPowerSystem(new PowerRedstoneFlux());
            }
        }
        if(Loader.isModLoaded("EnderIO")) {
            enabledRecipes.add(new RecipeEnderIO());
            if(manager.getPowerSystemByName(PowerRedstoneFlux.id) == null) {
                manager.registerPowerSystem(new PowerRedstoneFlux());
            }
        }
        if(Loader.isModLoaded("factorization")) {
            enabledRecipes.add(new RecipeFactorization());
            if(manager.getPowerSystemByName(PowerFactorization.id) == null) {
                manager.registerPowerSystem(new PowerFactorization());
            }
        }
        if(Loader.isModLoaded("IC2")) {
            enabledRecipes.add(new RecipeIndustrialCraft());
            if(manager.getPowerSystemByName(PowerIndustrialcraft.id) == null) {
                manager.registerPowerSystem(new PowerIndustrialcraft());
            }
            if(manager.getPowerSystemByName(PowerSteam.id) == null) {
                manager.registerPowerSystem(new PowerSteam());
            }
            ((PowerSteam)manager.getPowerSystemByName(PowerSteam.id)).addSteamType("ic2steam", "IC2 Steam", 4000, 4000);
            ((PowerSteam)manager.getPowerSystemByName(PowerSteam.id)).addSteamType("ic2superheatedsteam", "IC2 Superheated Steam", 8000, 8000);
        }
        if(Loader.isModLoaded("Railcraft")) {
            enabledRecipes.add(new RecipeRailcraft());
            if(manager.getPowerSystemByName(PowerSteam.id) == null) {
                manager.registerPowerSystem(new PowerSteam());
            }
        }
        if(Loader.isModLoaded("Forestry")) {
            enabledRecipes.add(new RecipeForestry());
            if(manager.getPowerSystemByName(PowerRedstoneFlux.id) == null) {
                manager.registerPowerSystem(new PowerRedstoneFlux());
            }
        }
        if(Loader.isModLoaded("ThermalExpansion")) {
            enabledRecipes.add(new RecipeThermalExpansion());
            if(manager.getPowerSystemByName(PowerRedstoneFlux.id) == null) {
                manager.registerPowerSystem(new PowerRedstoneFlux());
            }
        }
        if(Loader.isModLoaded("gregtech")){
            ModContainer modContainer = Loader.instance().getIndexedModList().get("gregtech");
            // "MC1710" = GT5 reboot.
            // "GT6-MC1710" = GT6
            if("MC1710".equals(modContainer.getVersion())) {
                enabledRecipes.add(new RecipeGregTech5());
                if (manager.getPowerSystemByName(PowerGregTech5.id) == null) {
                    manager.registerPowerSystem(new PowerGregTech5());
                }
            }
        }        
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void serverStarted(FMLServerStartingEvent event) {
        if(event.getSide().isServer()) {
            logger.debug("Server: initialising powersystem data.");
            PowerSystemManager.getInstance().setServerSystemIds();
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void clientConnected(PlayerEvent.PlayerLoggedInEvent event) {
        if(event.player == null || event.player.worldObj == null ||
                event.player.worldObj.isRemote || !(event.player instanceof EntityPlayerMP)) {
            return;
        }
        NBTTagCompound energyNBT = PowerSystemManager.getInstance().writePowerData();
        PacketClientSync packetClientSync = new PacketClientSync(energyNBT);
        networkWrapper.sendTo(packetClientSync, (EntityPlayerMP) event.player);
        logger.debug("Sending sync packet to player %s", event.player.toString());
    }

    public static Object tryOreDict(String name, ItemStack itemStack) {
        for (ItemStack ore : OreDictionary.getOres(name)) {
            if (ore.isItemEqual(itemStack)) {
                return name;
            }
        }
        return itemStack;
    }

    private void loadConfig(File dir) {
        dir = new File(new File(dir, modId.toLowerCase()), "common.cfg");
        Configuration c = new Configuration(dir);

        bridgeBufferSize = c.get(Configuration.CATEGORY_GENERAL, "bridgeBufferSize", 160000000).getInt();
        for(RecipeProvider provider : enabledRecipes) {
            provider.loadConfig(c);
        }
        PowerSystemManager.getInstance().loadConfigs(c);

        c.save();
    }
}
