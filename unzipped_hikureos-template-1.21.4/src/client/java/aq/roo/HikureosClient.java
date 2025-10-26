package aq.roo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.MathHelper;

/**
 * Client entrypoint. Adds a simple echolocation helper which plays a note-block sound
 * at the block/entity the player is pointing at. Pitch and volume change based on distance.
 */
public class HikureosClient implements ClientModInitializer {
	// play every N ticks to avoid spamming every frame
	private static final int TICK_INTERVAL = 8; // ~0.4s at 20 TPS
	private static final double MAX_RANGE = 32.0D; // max distance to map to pitch range
	private static int tickCounter = 0;

	@Override
	public void onInitializeClient() {
		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
	}

	private void onEndClientTick(MinecraftClient client) {
		if (client == null) return;
		if (client.player == null || client.world == null) return;

		tickCounter = (tickCounter + 1) % TICK_INTERVAL;
		if (tickCounter != 0) return; // only run on interval

		HitResult hit = client.crosshairTarget;
		if (hit == null || hit.getType() == HitResult.Type.MISS) return;

		Vec3d targetPos = null;
		if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult) {
			BlockPos bp = ((BlockHitResult) hit).getBlockPos();
			// center of block
			targetPos = Vec3d.ofCenter(bp);
		} else if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult) {
			targetPos = ((EntityHitResult) hit).getEntity().getPos();
		}

		if (targetPos == null) return;

		Vec3d playerPos = client.player.getPos();
		double distance = playerPos.distanceTo(targetPos);

		// Normalize distance to [0,1] where 0 is far (MAX_RANGE or more), 1 is very close (0)
		double clamped = MathHelper.clamp(1.0D - (distance / MAX_RANGE), 0.0D, 1.0D);

		// Map normalized value to pitch and volume ranges
		// pitch: 0.5 (low) .. 2.0 (high)
		float pitch = (float) (0.5D + clamped * 1.5D);
		// volume: 0.35 .. 1.0
		float volume = (float) (0.35D + clamped * 0.65D);

		// Play the note block sound at the target position on the client world (local-only)
	// Use the world.playSound overload that accepts a player + registry entry for the sound.
	// This plays the sound at the target position for nearby clients (client-side this will still play).
	client.world.playSound(client.player, targetPos.x, targetPos.y, targetPos.z,
		SoundEvents.BLOCK_NOTE_BLOCK_HARP,
		SoundCategory.PLAYERS,
		volume,
		pitch);
	}
}