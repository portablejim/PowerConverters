package powercrystals.powerconverters.common;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import powercrystals.powerconverters.power.PowerSystemManager;
import powercrystals.powerconverters.power.base.TileEntityEnergyProducer;
import powercrystals.powerconverters.power.systems.PowerIndustrialcraft;

import java.util.ArrayList;
import java.util.List;

public class TileEntityCharger extends TileEntityEnergyProducer<IInventory> {
    private static List<IChargeHandler> _chargeHandlers = new ArrayList<IChargeHandler>();
    private EntityPlayer _player;

    public static void registerChargeHandler(IChargeHandler handler) {
        _chargeHandlers.add(handler);
    }

    public TileEntityCharger() {
        super(PowerSystemManager.getInstance().getPowerSystemByName(PowerIndustrialcraft.id), 0, IInventory.class);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (_player != null) {
            int pX = (int) Math.floor(_player.posX);
            int pY = (int) Math.floor(_player.posY - _player.getYOffset());
            int pZ = (int) Math.floor(_player.posZ);
            if (pX != xCoord || pY - 1 != yCoord || pZ != zCoord) {
                setPlayer(null);
            }
        }
    }

    @Override
    public double produceEnergy(double energy) {
        if (energy == 0)
            return 0;
        int energyRemaining = (int) energy;

        boolean powered = getWorldObj().getStrongestIndirectPower(xCoord, yCoord, zCoord) > 0;
        if(!powered) {
            if (_player != null)
                energyRemaining = chargeInventory(_player.inventory, energyRemaining);

            for (IInventory inv : getTiles().values())
                energyRemaining = chargeInventory(inv, energyRemaining);
        }

        return energyRemaining;
    }

    private int chargeInventory(IInventory inventory, int energy) {
        int energyRemaining = energy;

        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            for (IChargeHandler chargeHandler : _chargeHandlers) {
                ItemStack s = inventory.getStackInSlot(i);
                if (s == null)
                    continue;

                if (chargeHandler.canHandle(s)) {
                    energyRemaining = chargeHandler.charge(s, energyRemaining);
                    if (energyRemaining < energy) {
                        _powerSystem = chargeHandler.getPowerSystem();
                        energy = energyRemaining;
                    }
                }
            }
        }
        return energyRemaining;
    }

    public void setPlayer(EntityPlayer player) {
        if (_player != player) {
            worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
            _player = player;
        }
    }

    @Override
    public boolean isConnected() {
        return super.isConnected() || _player != null;
    }

    @Override
    public boolean isSideConnected(int side) {
        return side == 1 && _player != null || super.isSideConnected(side);
    }

    @Override
    public boolean isSideConnectedClient(int side) {
        return side == 1 && _player != null || super.isSideConnectedClient(side);
    }
}
