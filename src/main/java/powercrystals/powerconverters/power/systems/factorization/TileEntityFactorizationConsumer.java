package powercrystals.powerconverters.power.systems.factorization;

import factorization.api.Charge;
import factorization.api.Coord;
import factorization.api.IChargeConductor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;
import powercrystals.powerconverters.position.BlockPosition;
import powercrystals.powerconverters.power.PowerSystemManager;
import powercrystals.powerconverters.power.base.TileEntityEnergyConsumer;
import powercrystals.powerconverters.power.systems.PowerFactorization;

public class TileEntityFactorizationConsumer extends TileEntityEnergyConsumer<IChargeConductor> implements IChargeConductor {
    private Charge _charge = new Charge(this);
    private int _chargeLastTick = 0;
    private static final int _maxCG = 1000;
    private boolean neighbourDirty = false;

    public TileEntityFactorizationConsumer() {
        super(PowerSystemManager.getInstance().getPowerSystemByName(PowerFactorization.id), 0, IChargeConductor.class);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        if (worldObj.isRemote) {
            return;
        }

        boolean powered = getWorldObj().getStrongestIndirectPower(xCoord, yCoord, zCoord) > 0;
        if(!powered) {
            if (this._charge.getValue() < _maxCG) {
                this._charge.update();
            }

            if (this._charge.getValue() > 0) {
                int used = _charge.tryTake(_charge.getValue());
                _chargeLastTick = MathHelper.floor_float(used);
                storeEnergy((used * getPowerSystem().getInternalEnergyPerInput(0)), false);
            } else {
                this._chargeLastTick = 0;
            }
        }
    }

    @Override
    public double getInputRate() {
        return this._chargeLastTick;
    }

    @Override
    public Charge getCharge() {
        return this._charge;
    }

    @Override
    public String getInfo() {
        return null;
    }

    @Override
    public Coord getCoord() {
        return new Coord(this);
    }

    @Override
    public void onNeighboorChanged() {
        super.onNeighboorChanged();

        try {
            Class fzNullClass = Class.forName("factorization.shared.TileEntityFzNull");
            for (ForgeDirection d : ForgeDirection.VALID_DIRECTIONS) {
                TileEntity te = BlockPosition.getAdjacentTileEntity(this, d);
                //noinspection unchecked
                if (te != null && fzNullClass.isAssignableFrom(te.getClass())) {
                    neighbourDirty = true;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        if (neighbourDirty) {
            onNeighboorChanged();
            neighbourDirty = false;
        }
        return super.isConnected();
    }
}

