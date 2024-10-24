package com.natamus.campfirespawnandtweaks.events;

import com.mojang.datafixers.util.Pair;
import com.natamus.campfirespawnandtweaks.config.ConfigHandler;
import com.natamus.campfirespawnandtweaks.util.Util;
import com.natamus.collective.functions.*;
import com.natamus.collective.services.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class CampfireEvent {
	public static HashMap<String, Pair<Level, BlockPos>> playercampfires = new HashMap<String, Pair<Level, BlockPos>>();
	
	public static HashMap<Level, List<Pair<Player, BlockPos>>> playerstorespawn = new HashMap<Level, List<Pair<Player, BlockPos>>>();
	private static final HashMap<Level, List<BlockPos>> firestoextinguish = new HashMap<Level, List<BlockPos>>();
	
	private static final List<Block> extinguishblocks = new ArrayList<Block>(Arrays.asList(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.SAND, Blocks.RED_SAND, Blocks.SOUL_SAND));
	
	public static void onWorldLoad(ServerLevel level) {
		Util.loadCampfireSpawnsFromWorld(level);
	}
	
	public static void onWorldTick(ServerLevel level) {
		if (HashMapFunctions.computeIfAbsent(firestoextinguish, level, k -> new ArrayList<BlockPos>()).size() > 0) {
			BlockPos campfirepos = firestoextinguish.get(level).get(0);
			BlockState state = level.getBlockState(campfirepos);
			if (state.getBlock() instanceof CampfireBlock) {
				level.setBlockAndUpdate(campfirepos, state.setValue(CampfireBlock.LIT, false).setValue(CampfireBlock.WATERLOGGED, false));
			}
			
			firestoextinguish.get(level).remove(0);
		}
		if (HashMapFunctions.computeIfAbsent(playerstorespawn, level, k -> new ArrayList<Pair<Player, BlockPos>>()).size() > 0) {
			Pair<Player, BlockPos> pair = playerstorespawn.get(level).get(0);
			Player player = pair.getFirst();
			BlockPos respawnpos = pair.getSecond();
			
			if (player instanceof ServerPlayer) {
				if (level.getBlockState(respawnpos).getBlock() instanceof CampfireBlock) {
					ServerPlayer serverplayer = ((ServerPlayer)player);
					
					Vec3 ts;
					
					int fireresistancems = ConfigHandler.fireResitanceDurationOnRespawnInMs;
					if (fireresistancems > 0) {
						ts = new Vec3(respawnpos.getX()+0.5, respawnpos.getY()+0.5, respawnpos.getZ()+0.5);
						EntityFunctions.addPotionEffect(player, MobEffects.FIRE_RESISTANCE, fireresistancems);
						
					}
					else {
						ts = new Vec3(respawnpos.getX()+1.5, respawnpos.getY(), respawnpos.getZ()+0.5);
					}
					
					if (ConfigHandler.createAirPocketIfBlocksAboveCampfire) {
						BlockPos tsbp = BlockPos.containing(ts.x, ts.y, ts.z);
						Iterator<BlockPos> posaround = BlockPos.betweenClosedStream(tsbp.getX(), tsbp.getY(), tsbp.getZ(), tsbp.getX(), tsbp.getY()+1, tsbp.getZ()).iterator();
						while (posaround.hasNext()) {
							BlockPos around = posaround.next();
							Block block = level.getBlockState(around).getBlock();
							if (block.equals(Blocks.AIR) || block instanceof CampfireBlock) {
								continue;
							}
							
							BlockFunctions.dropBlock(level, around);
						}
						
					}
					
					serverplayer.teleportTo(level, ts.x, ts.y, ts.z, Relative.ALL, player.getYRot(), player.getXRot(), true);
				}
				else {
					String playername = player.getName().toString();
					playercampfires.remove(playername.toLowerCase());
					if (ConfigHandler.sendMessageOnCampfireSpawnMissing) {
						MessageFunctions.sendMessage(player, "Campfire spawn point missing.", ChatFormatting.DARK_GRAY);
					}
				}
			}
			
			playerstorespawn.get(level).remove(0);
		}
	}
	
	public static boolean onEntityBlockPlace(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack itemStack) {
		if (level.isClientSide) {
			return true;
		}
		
		if (!(entity instanceof Player)) {
			return true;
		}
		
		Block block = state.getBlock();
		if (block instanceof CampfireBlock) {
			Player player = (Player)entity;
			if (Services.TOOLFUNCTIONS.isFlintAndSteel(player.getMainHandItem()) || Services.TOOLFUNCTIONS.isFlintAndSteel(player.getOffhandItem())) {
				return true;
			}
			
			if (ConfigHandler.campfiresStartUnlit) {
				level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
				return false;
			}
		}
		
		return true;
	}
	
	public static boolean onRightClickCampfireBlock(Level level, Player player, InteractionHand hand, BlockPos pos, BlockHitResult hitVec) {
		if (level.isClientSide) {
			return true;
		}
		
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();
		
		boolean allowAction = true;
		if (block instanceof CampfireBlock) {
			String playername = player.getName().getString();
			if (player.isShiftKeyDown()) {
				if (ConfigHandler.sneakRightClickCampfireToUnset) {
					if (Util.checkForCampfireSpawnRemoval(level, playername, pos)) {
						if (ConfigHandler.sendMessageOnNewCampfireSpawnSet) {
							MessageFunctions.sendMessage(player, "Campfire spawn point removed.", ChatFormatting.DARK_GRAY);
						}
					}
					return true;
				}
			}
			
			ItemStack itemstack = player.getItemInHand(hand);
			Item item = itemstack.getItem();
			
			boolean holdinglighter = false;
			if (Services.TOOLFUNCTIONS.isFlintAndSteel(player.getMainHandItem()) || Services.TOOLFUNCTIONS.isFlintAndSteel(player.getOffhandItem())) {
				holdinglighter = true;
				if (state.getValue(CampfireBlock.LIT)) {
					allowAction = false;
				}
			}
			
			boolean removed = false;
			if (state.getValue(CampfireBlock.LIT) || holdinglighter) {				
				boolean iswaterbucket = item.equals(Items.WATER_BUCKET);
				Block itemblock = Block.byItem(item);
				if (extinguishblocks.contains(itemblock) || iswaterbucket && !holdinglighter) {
					if (!player.isCreative() && !iswaterbucket) {
						itemstack.shrink(1);
					}
					
					allowAction = false;
					level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.LIT, false));
					
					if (iswaterbucket) {
						HashMapFunctions.computeIfAbsent(firestoextinguish, level, k -> new ArrayList<BlockPos>()).add(pos);
					}
					
					if (Util.checkForCampfireSpawnRemoval(level, playername, pos)) {
						if (ConfigHandler.sendMessageOnNewCampfireSpawnSet) {
							MessageFunctions.sendMessage(player, "Campfire spawn point removed.", ChatFormatting.DARK_GRAY);
						}
					}
					removed = true;
				}
				

				if (!removed && hand.equals(InteractionHand.MAIN_HAND) && (holdinglighter || itemstack.isEmpty())) {
					boolean replaced = playercampfires.containsKey(playername.toLowerCase());
					BlockPos oldpos = null;
					if (replaced) {
						oldpos = playercampfires.get(playername.toLowerCase()).getSecond().immutable();
					}
					
					if (Util.setCampfireSpawn(level, playername, pos)) {
						if (ConfigHandler.sendMessageOnNewCampfireSpawnSet) {
							if (holdinglighter) {
								level.setBlockAndUpdate(pos, state.setValue(CampfireBlock.WATERLOGGED, false));
								player.swing(hand);
							}
							
							if (replaced) {
								if (oldpos.equals(pos)) {
									MessageFunctions.sendMessage(player, "Campfire spawn point remains the same.", ChatFormatting.DARK_GRAY);
									return true;
								}
								MessageFunctions.sendMessage(player, "Campfire spawn point replaced.", ChatFormatting.DARK_GRAY);
								return true;
							}
							
							MessageFunctions.sendMessage(player, "Campfire spawn point set.", ChatFormatting.DARK_GRAY);
						}
					}
				}
			}
		}
		else if (block instanceof BedBlock) {
			if (player.isShiftKeyDown()) {
				if (!ConfigHandler.bedsOverrideCampfireSpawnOnSneakRightClick) {
					return true;
				}
				
				String playername = player.getName().getString().toLowerCase();
				if (playercampfires.containsKey(playername)) {
					BlockPos newspawn = pos.immutable();

					Pair<Level, BlockPos> pair = playercampfires.get(playername);
					Level oldlevel = pair.getFirst();
					BlockPos oldpos = pair.getSecond();
					
					if (WorldFunctions.getWorldDimensionName(level).equals(WorldFunctions.getWorldDimensionName(oldlevel))) {
						if (newspawn.equals(oldpos)) {
							return true;
						}
					}
					
					if (Util.checkForCampfireSpawnRemoval(level, playername, oldpos)) {
						if (ConfigHandler.sendMessageOnCampfireSpawnOverride) {
							MessageFunctions.sendMessage(player, "Campfire spawn point unset.", ChatFormatting.DARK_GRAY);
						}
					}
				}
			}
		}
		
		return allowAction;
	}
	
	public static void onCampfireBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
		if (level.isClientSide) {
			return;
		}
		
		if (level.getBlockState(pos).getBlock() instanceof CampfireBlock) {
			String playername = player.getName().getString().toLowerCase();

			if (Util.checkForCampfireSpawnRemoval(level, playername, pos)) {
				if (ConfigHandler.sendMessageOnNewCampfireSpawnSet) {
					MessageFunctions.sendMessage(player, "Campfire spawn point removed.", ChatFormatting.DARK_GRAY);
				}
			}
		}
		
	}
	
	public static void onPlayerRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		Level level = newPlayer.level();
		if (level.isClientSide) {
			return;
		}
		
		String playername = newPlayer.getName().getString().toLowerCase();
		if (!playercampfires.containsKey(playername)) {
			return;
		}
		
		Pair<Level, BlockPos> pair = playercampfires.get(playername);
		HashMapFunctions.computeIfAbsent(playerstorespawn, pair.getFirst(), k -> new ArrayList<Pair<Player, BlockPos>>()).add(new Pair<Player, BlockPos>(newPlayer, pair.getSecond().immutable()));
	}
}