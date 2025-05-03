package com.zergatul.cheatutils.scripting.api.modules;

import com.zergatul.cheatutils.controllers.DisconnectController;
import com.zergatul.cheatutils.controllers.SpeedCounterController;
import com.zergatul.cheatutils.mixins.common.accessors.MultiPlayerGameModeAccessor;
import com.zergatul.cheatutils.scripting.api.ApiVisibility;
import com.zergatul.cheatutils.scripting.api.ApiType;
import com.zergatul.cheatutils.scripting.api.HelpText;
import com.zergatul.cheatutils.scripting.types.BlockPosWrapper;
import com.zergatul.cheatutils.scripting.types.Position3d;
import com.zergatul.cheatutils.utils.NearbyBlockEnumerator;
import com.zergatul.cheatutils.utils.Rotation;
import com.zergatul.cheatutils.utils.RotationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;


//partially done
public class PlayerApi {

    private final static Minecraft mc = Minecraft.getInstance();
//    private final static Minecraft mc = Minecraft.getInstance();

//    public TargetApi target = new TargetApi();
//    public EffectsApi effects = new EffectsApi();
//    public InteractionsApi interactions = new InteractionsApi();

    @HelpText("""
            Sends text to ingame chat. To send commands use player.command(...) method.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void chat(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendChat(text);
        }
    }

    @HelpText("""
            Sends ingame command to the server
            """)
    @ApiVisibility(ApiType.ACTION)
    public void command(String text) {
        if (text != null && text.startsWith("/")) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.connection.sendCommand(text.substring(1));
            }
        }
    }

    public Position3d getPosition() {
        Entity entity = mc.getCameraEntity();
        if (entity == null) {
            return new Position3d(0, 0, 0);
        }
        return new Position3d(entity.getX(), entity.getY(), entity.getZ());
    }

    @HelpText("""
            Returns formatted X/Y/Z player coordinates
            """)
    public String getCoordinatesFormatted() {
        if (mc.getCameraEntity() != null) {
            return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX(), mc.getCameraEntity().getY(), mc.getCameraEntity().getZ());
        } else {
            return "";
        }
    }

    @HelpText("If you are in the Overworld, returns calculated coordinates in the Nether")
    public String getCalculatedNetherCoordinates() {
        if (mc.level == null || mc.level.dimension() == Level.NETHER || mc.getCameraEntity() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX() / 8, mc.getCameraEntity().getY(), mc.getCameraEntity().getZ() / 8);
    }

    @HelpText("If you are in the Nether, returns calculated coordinates in the Overworld")
    public String getCalculatedOverworldCoordinates() {
        if (mc.level == null || mc.level.dimension() == Level.OVERWORLD || mc.getCameraEntity() == null) {
            return "";
        }
        return String.format(Locale.ROOT, "%.3f / %.5f / %.3f", mc.getCameraEntity().getX() * 8, mc.getCameraEntity().getY(), mc.getCameraEntity().getZ() * 8);
    }

    public String getBlockCoordinatesFormatted() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        return String.format(Locale.ROOT, "%d %d %d [%d %d]",
                blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                blockPos.getX() & 15, blockPos.getZ() & 15);
    }

    public String getChunkCoordinatesFormatted() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        ChunkPos chunkPos = new ChunkPos(blockPos);
        return String.format(Locale.ROOT, "%d %d", chunkPos.x, chunkPos.z);
    }

    public String getDirection() {
        if (mc.getCameraEntity() == null) {
            return "";
        }
        Direction direction = mc.getCameraEntity().getDirection();
        return direction.getName();
    }

    public String getBiome() {
        if (mc.level == null || mc.getCameraEntity() == null) {
            return "";
        }
        BlockPos blockPos = mc.getCameraEntity().blockPosition();
        Holder<Biome> holder = mc.level.getBiome(blockPos);
        return holder.unwrap().map(id -> id.location().toString(), biome -> "[unregistered " + biome + "]");
    }

    @HelpText("Measured in 0.5 sec window.")
    public String getHorizontalSpeed() {
        return String.format(Locale.ROOT, "%.3f", SpeedCounterController.instance.getHorizontalSpeed());
    }

    @HelpText("Measured in 0.5 sec window.")
    public String getSpeed() {
        return String.format(Locale.ROOT, "%.3f", SpeedCounterController.instance.getSpeed());
    }

    public double getX() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getX();
    }

    public double getY() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getY();
    }

    public double getZ() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getZ();
    }

    public double getXRot() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getXRot();
    }

    public double getYRot() {
        if (mc.player == null) {
            return 0;
        }
        return mc.player.getYRot();
    }

    @ApiVisibility(ApiType.ACTION)
    public void setXRot(double value) {
        if (mc.player == null) {
            return;
        }
        mc.player.setXRot((float)value);
    }

    @ApiVisibility(ApiType.ACTION)
    public void setYRot(double value) {
        if (mc.player == null) {
            return;
        }
        mc.player.setYRot((float)value);
    }

    public int getHealth() {
        if (mc.player == null) {
            return 0;
        }

        return (int) mc.player.getHealth();
    }

    public int getFood() {
        if (mc.player == null) {
            return 0;
        }

        return mc.player.getFoodData().getFoodLevel();
    }

    public boolean isUnderwater() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isUnderWater();
    }

    public boolean isElytraFlying() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isFallFlying();
    }

    public boolean isOnGround() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.onGround();
    }

    public boolean isPassenger() {
        if (mc.player == null) {
            return false;
        }

        return mc.player.isPassenger();
    }

    public boolean isDestroyingBlock() {
        if (mc.gameMode == null) {
            return false;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU();
    }

    public int getDestroyingBlockX() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getX() : Integer.MIN_VALUE;
    }

    public int getDestroyingBlockY() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getY() : Integer.MIN_VALUE;
    }

    public int getDestroyingBlockZ() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyBlockPos_CU().getZ() : Integer.MIN_VALUE;
    }

    public double getDestroyingBlockProgress() {
        if (mc.gameMode == null) {
            return Integer.MIN_VALUE;
        }
        MultiPlayerGameModeAccessor mode = (MultiPlayerGameModeAccessor) mc.gameMode;
        return mode.getIsDestroying_CU() ? mode.getDestroyProgress_CU() : Double.NaN;
    }

    @ApiVisibility(ApiType.ACTION)
    public void lookAt(double x, double y, double z) {
        if (mc.player == null) {
            return;
        }
        Rotation rotation = RotationUtils.getRotation(mc.player.getEyePosition(), new Vec3(x, y, z));
        mc.player.setXRot(rotation.xRot());
        mc.player.setYRot(rotation.yRot());
    }

    @HelpText("""
            Allowed disconnect types: "self-attack", "invalid-chars". Anything else (for example "") - normal disconnect.
            """)
    @ApiVisibility(ApiType.ACTION)
    public void disconnect(String type) {
        switch (type) {
            case "self-attack" -> DisconnectController.instance.selfAttack(null);
            case "invalid-chars" -> DisconnectController.instance.invalidChars(null);
            default -> DisconnectController.instance.disconnect(null);
        }
    }

    @HelpText("""
            Allowed disconnect types: "self-attack", "invalid-chars". Anything else (for example "") - normal disconnect.
            You can specify custom message to be displayed at the disconnect screen
            """)
    @ApiVisibility(ApiType.ACTION)
    public void disconnect(String type, String message) {
        switch (type) {
            case "self-attack" -> DisconnectController.instance.selfAttack(message);
            case "invalid-chars" -> DisconnectController.instance.invalidChars(message);
            default -> DisconnectController.instance.disconnect(message);
        }
    }

    @ApiVisibility(ApiType.ACTION)
    public boolean attack(int entityId) {
        if (mc.level == null) {
            return false;
        }
        if (mc.player == null) {
            return false;
        }
        if (mc.gameMode == null) {
            return false;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        mc.gameMode.attack(mc.player, entity);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    @HelpText("Returns [0..1]. 0 means attack will do full damage.")
    public double getAttackCooldown() {
        if (mc.player == null) {
            return Double.NaN;
        }
        return 1d - mc.player.getAttackStrengthScale(0);
    }

    @HelpText("""
            Returns distance to entity.
            Uses the same algorithm as vanilla Minecraft when checking if entity is close enough for interaction.
            This is distance from player eyes to entity bounding box.
            """)
    public double getInteractionDistanceTo(int entityId) {
        if (mc.player == null || mc.level == null) {
            return Double.NaN;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return Double.NaN;
        }

        // copy from Player.canInteractWithEntity
        return Math.sqrt(entity.getBoundingBox().distanceToSqr(mc.player.getEyePosition()));
    }

    @HelpText("""
            Returns list of nearby blocks coordinates.
            Uses distance from player eyes to blocks center.
            Blocks are sorted in ascending order by the distance from the eyes.
            Allowed range values: [0..100]
            """)
    public BlockPosWrapper[] enumerateNearbyBlocks(double range) {
        if (mc.player == null) {
            return new BlockPosWrapper[0];
        }
        if (range < 0 || range > 100) {
            return new BlockPosWrapper[0];
        }
        return NearbyBlockEnumerator.getPositions(mc.player.getEyePosition(), range)
                .stream()
                .map(BlockPosWrapper::new)
                .toArray(BlockPosWrapper[]::new);
    }
}