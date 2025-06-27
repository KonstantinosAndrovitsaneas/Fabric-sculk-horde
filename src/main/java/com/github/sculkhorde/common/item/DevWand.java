package com.github.sculkhorde.common.item;

import com.github.sculkhorde.core.ModSavedData;
import com.github.sculkhorde.core.SculkHorde;
import com.github.sculkhorde.systems.path_builder_system.PathBuilderRequest;
import com.github.sculkhorde.util.StructureUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeItem;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

public class DevWand extends Item implements IForgeItem {
	/* NOTE:
	 * Learned from https://www.youtube.com/watch?v=0vLbG-KrQy4 "Advanced Items - Minecraft Forge 1.16.4 Modding Tutorial"
	 * and learned from https://www.youtube.com/watch?v=itVLuEcJRPQ "Add CUSTOM TOOLS to Minecraft 1.16.5 with Forge"
	 * Also this is just an example item, I don't intend for this to be used
	*/

	StructureUtil.StructurePlacer structurePlacer;

	/**
	 * The Constructor that takes in properties
	 * @param properties The Properties
	 */
	public DevWand(Properties properties) {
		super(properties);
		
	}

	/**
	 * A simpler constructor that does not take in properties.<br>
	 * I made this so that registering items in ItemRegistry.java can look cleaner
	 */
	public DevWand() {this(getProperties());}

	/**
	 * Determines the properties of an item.<br>
	 * I made this in order to be able to establish a item's properties from within the item class and not in the ItemRegistry.java
	 * @return The Properties of the item
	 */
	public static Properties getProperties()
	{
		return new Item.Properties()
				.rarity(Rarity.EPIC)
				.fireResistant();

	}

	@Override
	public Rarity getRarity(ItemStack itemStack) {
		return Rarity.EPIC;
	}

	public void announceToAllPlayers(Component message)
	{
		ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach((player) -> player.displayClientMessage(message, false));
	}



	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player playerIn, InteractionHand handIn)
	{
		ItemStack itemstack = playerIn.getItemInHand(handIn);

		//If item is not on cool down
		if(level.isClientSide())
		{
			return InteractionResultHolder.fail(itemstack);
		}

		ServerLevel serverLevel = (ServerLevel) level;

		ClipContext rayTrace = new ClipContext(playerIn.getEyePosition(1.0F), playerIn.getEyePosition(1.0F).add(playerIn.getLookAngle().scale(5)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, playerIn);
		Vec3 spawnPosition = rayTrace.getTo();

		if(!level.canSeeSky(BlockPos.containing(spawnPosition)))
		{
			playerIn.sendSystemMessage(Component.literal("Error: Cannot See Sky."));
			return InteractionResultHolder.fail(itemstack);
		}

		Optional<ModSavedData.NodeEntry> closestNode = ModSavedData.getSaveData().getClosestNodeEntry(serverLevel, playerIn.blockPosition());

		if(closestNode.isEmpty())
		{
			playerIn.sendSystemMessage(Component.literal("Error: No Nearby Node."));
			return InteractionResultHolder.fail(itemstack);
		}

		PathBuilderRequest request = new PathBuilderRequest(serverLevel, closestNode.get().getPosition(), BlockPos.containing(playerIn.getEyePosition()), 10, null, null);
		SculkHorde.pathBuilderSystem.addPathBuilderRequest(request);

		return InteractionResultHolder.pass(itemstack);
	}


}
