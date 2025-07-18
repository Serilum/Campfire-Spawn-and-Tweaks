package com.natamus.campfirespawnandtweaks.forge.events;

import com.natamus.campfirespawnandtweaks.events.CampfireEvent;
import com.natamus.collective.functions.WorldFunctions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;

import java.lang.invoke.MethodHandles;

public class ForgeCampfireEvent {
	public static void registerEventsInBus() {
		BusGroup.DEFAULT.register(MethodHandles.lookup(), ForgeCampfireEvent.class);
	}

	@SubscribeEvent
	public static void onWorldLoad(LevelEvent.Load e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		CampfireEvent.onWorldLoad((ServerLevel)level);
	}
	
	@SubscribeEvent
	public static void onWorldTick(LevelTickEvent e) {
		Level level = e.level;
		if (level.isClientSide) {
			return;
		}

		CampfireEvent.onWorldTick((ServerLevel)level);
	}
	
	@SubscribeEvent
	public static void onEntityBlockPlace(EntityPlaceEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		Entity entity = e.getEntity();
		if (!(entity instanceof LivingEntity)) {
			return;
		}

		CampfireEvent.onEntityBlockPlace(level, e.getPos(), e.getPlacedBlock(), (LivingEntity)entity, null);
	}
	
	@SubscribeEvent
	public static boolean onRightClickCampfireBlock(PlayerInteractEvent.RightClickBlock e) {
		if (!CampfireEvent.onRightClickCampfireBlock(e.getLevel(), e.getEntity(), e.getHand(), e.getPos(), e.getHitVec())) {
			return true;
		}
		return false;
	}
	
	@SubscribeEvent
	public static void onCampfireBreak(BlockEvent.BreakEvent e) {
		Level level = WorldFunctions.getWorldIfInstanceOfAndNotRemote(e.getLevel());
		if (level == null) {
			return;
		}

		CampfireEvent.onCampfireBreak(level, e.getPlayer(), e.getPos(), e.getState(), null);
	}
	
	@SubscribeEvent
	public static void onPlayerRespawn(PlayerRespawnEvent e) {
		Player player = e.getEntity();
		Level level = player.level();
		if (level.isClientSide) {
			return;
		}

		CampfireEvent.onPlayerRespawn(null, (ServerPlayer)player, true);
	}
}