package thebetweenlands.tileentities;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;

public class TileEntitySpikeTrap extends TileEntity {

	public int animationTicks;
	public boolean active;

	@Override
	public boolean canUpdate() {
		return true;
	}

	@Override
	public void updateEntity() {
		if (!worldObj.isRemote) {
			if (!worldObj.isAirBlock(xCoord, yCoord + 1, zCoord)) {
				worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 3);
				setActive(true);
				Block block = worldObj.getBlock(xCoord, yCoord + 1, zCoord);
				worldObj.playAuxSFXAtEntity(null, 2001, xCoord, yCoord + 1, zCoord, Block.getIdFromBlock(worldObj.getBlock(xCoord, yCoord + 1, zCoord)));
				block.dropBlockAsItem(worldObj, xCoord, yCoord + 1, zCoord, worldObj.getBlockMetadata(xCoord, yCoord + 1, zCoord), 0);
				worldObj.setBlockToAir(xCoord, yCoord + 1, zCoord);
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
			if (worldObj.rand.nextInt(50) == 0) {
				if (worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 0 && !active && animationTicks == 0)
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 0, 3);
				else if (isBlockOccupied() == null)
					worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, 1, 3);
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
			
			if (isBlockOccupied() != null && worldObj.getBlockMetadata(xCoord, yCoord, zCoord) != 0) {
				if(!active && animationTicks == 0)
					setActive(true);
				worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
			}
		}
		if (active) {
			activateBlock();
			if (animationTicks == 0)
				worldObj.playSoundEffect(xCoord, yCoord, zCoord, "tile.piston.out", 1.25F, 1.0F);
			if (animationTicks <= 20)
				animationTicks++;
			if (animationTicks == 20)
				setActive(false);
		}
		if (!active)
			if (animationTicks >= 1)
				animationTicks--;
	}

	public void setActive(boolean isActive) {
		active = isActive;
	}

	@SuppressWarnings("unchecked")
	protected Entity activateBlock() {
		float y = 1F / 16 * animationTicks;
		List<EntityLivingBase> list = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1D, yCoord + 1D + y, zCoord + 1D));
		if (animationTicks >= 1)
			for (int i = 0; i < list.size(); i++) {
				Entity entity = list.get(i);
				if (entity != null)
					if (entity instanceof EntityPlayer)
						((EntityLivingBase) entity).attackEntityFrom(DamageSource.generic, 2);
			}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected Entity isBlockOccupied() {
		List<EntityLivingBase> list = worldObj.getEntitiesWithinAABB(EntityLivingBase.class, AxisAlignedBB.getBoundingBox(xCoord, yCoord, zCoord, xCoord + 1D, yCoord + 2D, zCoord + 1D));
		for (int i = 0; i < list.size(); i++) {
			Entity entity = list.get(i);
			if (entity != null)
				if (entity instanceof EntityPlayer)
					return entity;
		}
		return null;
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("animationTicks", animationTicks);
		nbt.setBoolean("active", active);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		animationTicks = nbt.getInteger("animationTicks");
		active = nbt.getBoolean("active");
	}

	@Override
	public Packet getDescriptionPacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setInteger("animationTicks", animationTicks);
		nbt.setBoolean("active", active);
		return new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, nbt);
	}

	@Override
	public void onDataPacket(NetworkManager net,
			S35PacketUpdateTileEntity packet) {
		animationTicks = packet.func_148857_g().getInteger("animationTicks");
		active = packet.func_148857_g().getBoolean("active");
	}
}