package powercrystals.powerconverters.common;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import powercrystals.powerconverters.PowerConverterCore;
import powercrystals.powerconverters.position.BlockPosition;
import powercrystals.powerconverters.position.INeighboorUpdateTile;
import powercrystals.powerconverters.power.PowerSystem;
import powercrystals.powerconverters.power.util.ICustomHandler;
import powercrystals.powerconverters.power.base.TileEntityBridgeComponent;
import powercrystals.powerconverters.power.base.TileEntityEnergyConsumer;
import powercrystals.powerconverters.power.base.TileEntityEnergyProducer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class TileEntityEnergyBridge extends TileEntity implements INeighboorUpdateTile {
    private int _energyStored;
    private int _energyStoredMax = PowerConverterCore.bridgeBufferSize;
    private int _energyScaledClient;

    private int _energyStoredLast;
    private boolean _isInputLimited;

    private Map<ForgeDirection, TileEntityEnergyProducer<?>> _producerTiles;
    private Map<ForgeDirection, BridgeSideData> _clientSideData;
    private Map<ForgeDirection, Integer> _producerOutputRates;

    private boolean _initialized;

    public TileEntityEnergyBridge() {
        _producerTiles = new HashMap<ForgeDirection, TileEntityEnergyProducer<?>>();
        _clientSideData = new HashMap<ForgeDirection, BridgeSideData>();
        _producerOutputRates = new HashMap<ForgeDirection, Integer>();
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            _clientSideData.put(d, new BridgeSideData());
            _producerOutputRates.put(d, 0);
        }
    }

    /**
     * @return power in buffer (must be converted to the power system units)
     */
    public int getEnergyStored() {
        return _energyStored;
    }

    /**
     * @return maximum buffer size (must be converted to the power system units)
     */
    public int getEnergyStoredMax() {
        return _energyStoredMax;
    }

    /**
     * Stores energy in the internal buffer
     *
     * @param energy   how much current to store
     * @param simulate whether to actually store the power
     * @return how much energy was stored
     */
    public double storeEnergy(double energy, boolean simulate) {
        double toStore = Math.min(energy, _energyStoredMax - _energyStored);
        if (!simulate)
            _energyStored += toStore;
        return energy - toStore;
    }

    /**
     * Uses energy from the internal buffer
     *
     * @param energy   how much current to draw
     * @param simulate whether to actually draw the power
     * @return how much energy was used
     */
    public double useEnergy(double energy, boolean simulate) {
        double toUse = Math.max(0, Math.min(_energyStored, energy));
        if (!simulate) {
            _energyStored -= toUse;
            _isInputLimited = !((_energyStored == _energyStoredLast && _energyStored == _energyStoredMax) || _energyStored > _energyStoredLast);
            _energyStoredLast = _energyStored;
        }
        return toUse;
    }

    /**
     * Updates the producer information that will be displayed inside the GUI
     *
     * @param dir    direction that the producer is in of the energy bridge
     * @param output converted output units
     */
    public void updateProducerInfo(ForgeDirection dir, int output) {
        _producerOutputRates.put(dir, Math.max(0, output));
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (!_initialized) {
            onNeighboorChanged();
            _initialized = true;
        }

        if (!worldObj.isRemote) {
            int energyRemaining = Math.min(_energyStored, _energyStoredMax);
            int energyNotProduced;
            for (Entry<ForgeDirection, TileEntityEnergyProducer<?>> prod : _producerTiles.entrySet()) {
                if (prod.getValue() instanceof ICustomHandler && ((ICustomHandler) prod.getValue()).shouldHandle()) {
                    int temp = (int) ((ICustomHandler) prod.getValue()).getOutputRate();
                    updateProducerInfo(prod.getKey(), temp);
                    continue;
                }
                if (energyRemaining > 0) {
                    energyNotProduced = (int) prod.getValue().produceEnergy(energyRemaining);
                    if (energyNotProduced > energyRemaining) {
                        energyNotProduced = energyRemaining;
                    }
                    PowerSystem producerPowerSystem= prod.getValue().getPowerSystem();
                    if(producerPowerSystem != null) {
                        updateProducerInfo(prod.getKey(), (int) ((energyRemaining - energyNotProduced) / producerPowerSystem.getInternalEnergyPerOutput(prod.getValue().getSubtype() + 1)));
                    }
                    energyRemaining = energyNotProduced;
                } else {
                    prod.getValue().produceEnergy(0);
                    updateProducerInfo(prod.getKey(), 0);
                }
            }
            _energyStored = Math.max(0, energyRemaining);
            _isInputLimited = !((_energyStored == _energyStoredLast && _energyStored == _energyStoredMax) || _energyStored > _energyStoredLast);
            _energyStoredLast = _energyStored;
        }
    }

    @Override
    public void onNeighboorChanged() {
        Map<ForgeDirection, TileEntityEnergyProducer<?>> producerTiles = new HashMap<ForgeDirection, TileEntityEnergyProducer<?>>();
        for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
            BlockPosition p = new BlockPosition(this);
            p.orientation = d;
            p.moveForwards(1);
            TileEntity te = worldObj.getTileEntity(p.x, p.y, p.z);
            if (te != null && te instanceof TileEntityEnergyProducer) {
                producerTiles.put(d, (TileEntityEnergyProducer<?>) te);
            }
        }
        _producerTiles = producerTiles;
    }

    public BridgeSideData getDataForSide(ForgeDirection dir) {
        if (!worldObj.isRemote) {
            BridgeSideData d = new BridgeSideData();
            BlockPosition p = new BlockPosition(this);
            p.orientation = dir;
            p.moveForwards(1);

            TileEntity te = worldObj.getTileEntity(p.x, p.y, p.z);
            if (te != null && te instanceof TileEntityBridgeComponent) {
                if (te instanceof TileEntityEnergyConsumer) {
                    d.isConsumer = true;
                    d.outputRate = ((TileEntityEnergyConsumer<?>) te).getInputRate();
                }
                if (te instanceof TileEntityEnergyProducer) {
                    d.isProducer = true;
                    d.outputRate = _producerOutputRates.get(dir);
                }
                TileEntityBridgeComponent<?> c = (TileEntityBridgeComponent<?>) te;
                d.powerSystem = c.getPowerSystem();
                d.subtype = c.getSubtype();
                d.isConnected = c.isConnected();
                d.side = dir;
                d.voltageNameIndex = c.getVoltageIndex();
            }

            return d;
        } else {
            return _clientSideData.get(dir);
        }
    }

    public boolean isInputLimited() {
        return _isInputLimited;
    }

    @SideOnly(Side.CLIENT)
    public void setIsInputLimited(boolean isInputLimited) {
        _isInputLimited = isInputLimited;
    }

    public int getEnergyScaled() {
        if (worldObj.isRemote) {
            return _energyScaledClient;
        } else {
            return (int) (120 * ((double) _energyStored / (double) _energyStoredMax));
        }
    }

    public void setEnergyScaled(int scaled) {
        _energyScaledClient = scaled;
    }

    public void setEnergyStored(int stored) {
        _energyStored = stored;
    }

    @Override
    public void writeToNBT(NBTTagCompound par1nbtTagCompound) {
        super.writeToNBT(par1nbtTagCompound);
        par1nbtTagCompound.setInteger("energyStored", _energyStored);
    }

    @Override
    public void readFromNBT(NBTTagCompound par1nbtTagCompound) {
        super.readFromNBT(par1nbtTagCompound);
        _energyStored = par1nbtTagCompound.getInteger("energyStored");
    }
}
