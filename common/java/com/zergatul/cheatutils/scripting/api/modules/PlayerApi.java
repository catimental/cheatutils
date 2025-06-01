package com.zergatul.cheatutils.scripting.api.modules;

import com.zergatul.cheatutils.common.Registries;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

import java.util.*;
import java.util.function.Predicate;


//partially done
public class PlayerApi {

    private final static Minecraft mc = Minecraft.getInstance();
    public TargetApi target = new TargetApi();
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

    @ApiVisibility(ApiType.ACTION)
    public boolean lookAtEntityHead(int entityId) {
        if (mc.player == null) {
            return false;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        Vec3 headPos = getEntityHeadPosition(entity);
//        Rotation rotation = RotationUtils.getRotation(mc.player.getEyePosition(), headPos);
//        mc.player.setXRot(rotation.xRot());
//        mc.player.setYRot(rotation.yRot());
        lookAt(headPos.x, headPos.y, headPos.z);
        return true;
    }

    @ApiVisibility(ApiType.ACTION)
    public boolean lookAtEntityCenter(int entityId) {
        if (mc.player == null) {
            return false;
        }
        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        Vec3 centerPos = getEntityCenterPosition(entity);
        lookAt(centerPos.x, centerPos.y, centerPos.z);
        return true;
    }

    @ApiVisibility(ApiType.ACTION)
    public void lookAtWithRotation(double x, double y, double z, double xRot, double yRot) {
        if (mc.player == null) {
            return;
        }
        Rotation rotation = RotationUtils.getRotation(mc.player.getEyePosition(), new Vec3(x, y, z));
        mc.player.setXRot((float) xRot);
        mc.player.setYRot((float) yRot);
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

    public static class TargetApi {

        public boolean hasBlock() {
            if (mc.hitResult == null) {
                return false;
            }

            return mc.hitResult.getType() == HitResult.Type.BLOCK;
        }

        public BlockPosWrapper getBlockPos() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return new BlockPosWrapper(hitResult.getBlockPos());
            } else {
                return new BlockPosWrapper(0, 0, 0);
            }
        }

        public int getBlockX() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getX();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public int getBlockY() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getY();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public int getBlockZ() {
            if (mc.hitResult instanceof BlockHitResult hitResult) {
                return hitResult.getBlockPos().getZ();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public boolean hasEntity() {
            if (mc.hitResult == null) {
                return false;
            }

            return mc.hitResult.getType() == HitResult.Type.ENTITY;
        }

        public int getEntityId() {
            if (mc.hitResult instanceof EntityHitResult hitResult) {
                return hitResult.getEntity().getId();
            } else {
                return Integer.MIN_VALUE;
            }
        }

        public String getBlockCoordinatesFormatted() {
            if (mc.level == null) {
                return "";
            }

            Entity entity = mc.getCameraEntity();
            if (entity == null) {
                return "";
            }

            HitResult result = entity.pick(20.0D, 0.0F, false);
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
                return blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ();
            } else {
                return "";
            }
        }

        public String getBlockName() {
            if (mc.level == null) {
                return "";
            }

            Entity entity = mc.getCameraEntity();
            if (entity == null) {
                return "";
            }

            HitResult result = entity.pick(20.0D, 0.0F, false);
            if (result.getType() == HitResult.Type.BLOCK) {
                BlockPos blockPos = ((BlockHitResult) result).getBlockPos();
                BlockState blockState = mc.level.getBlockState(blockPos);
                return com.zergatul.cheatutils.common.Registries.BLOCKS.getKey(blockState.getBlock()).toString();
            } else {
                return "";
            }
        }

        public BlockPosWrapper getBlockPosInRange(double maxRange, boolean captureFluids) {
            if (mc.level == null || mc.player == null) {
                return new BlockPosWrapper(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }

            HitResult result = mc.player.pick(maxRange, 0, captureFluids);
            if (result instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                return new BlockPosWrapper(hitResult.getBlockPos());
            } else {
                return new BlockPosWrapper(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
            }
        }

        public int getEntityInNearestCursor() {
            return getEntityInNearestCursor(1000, 100);
        }

        private static List<String> IGNORE_ENTITY_LIST = List.of(
                "spore:scent"
        );
        @HelpText("""
                카메라의 시점에서 가장 가까운 엔티티id를 반환합니다.
                """)
        public int getEntityInNearestCursor(double maxDistance, float maxAngle) {
            if (mc.level == null || mc.player == null) {
                return Integer.MIN_VALUE;
            }

            // Get player eye position and look vector
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 lookVec = mc.player.getViewVector(1.0F).normalize();


            // Get entities within range
            AABB searchBox = new AABB(
                    eyePos.x - maxDistance, eyePos.y - maxDistance, eyePos.z - maxDistance,
                    eyePos.x + maxDistance, eyePos.y + maxDistance, eyePos.z + maxDistance);

            List<Entity> entities = mc.level.getEntities(mc.player, searchBox,
                    entity -> entity instanceof LivingEntity && entity != mc.player && entity.isAlive());
            return entities.stream()
                    //simple filter
                    .filter(entity -> {
                        // check if entity is in ignore list
                        final String type = Registries.ENTITY_TYPES.getKey(entity.getType()).toString();
                        for (String ignore : IGNORE_ENTITY_LIST) {
                            if (type.contains(ignore)) {
                                return false;
                            }
                        }
                        return true;
                    })
                    //block collision check
                    .filter(entity -> {
                        // get entity center
                        Vec3 entityCenter = entity.getBoundingBox().getCenter();

                        // laycasting
                        Vec3 toEntity = entityCenter.subtract(eyePos);
                        double distance = toEntity.length();

                        // is colliding with a block?
                        BlockHitResult blockHit = mc.level.clip(new ClipContext(
                                eyePos,
                                entityCenter,
                                ClipContext.Block.COLLIDER,
                                ClipContext.Fluid.NONE,
                                mc.player));

                        // check if the block hit is not the entity itself
                        return blockHit.getType() == HitResult.Type.MISS ||
                                blockHit.getLocation().distanceTo(eyePos) > distance;
                    })
                    .filter(entity -> {
                        // calculate vector to entity center
                        Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eyePos).normalize();

                        // calculate angle between look vector and entity vector
                        double dot = lookVec.dot(toEntity);
                        float angle = (float) Math.toDegrees(Math.acos(dot));

                        // filter by angle
                        return angle <= maxAngle;
                    })
                    .min(Comparator.<Entity>comparingDouble(entity -> {
                        // Calculate angle
                        Vec3 toEntity = entity.getBoundingBox().getCenter().subtract(eyePos).normalize();
                        double dot = lookVec.dot(toEntity);
                        float angle = (float) Math.toDegrees(Math.acos(dot));

                        // calculate distance
                        double distance = entity.distanceTo(mc.player);

                        // prioritize angle over distance with a weighting factor
                        return angle * 100 + distance;
                    }))
                    .map(Entity::getId)
                    .orElse(Integer.MIN_VALUE);
        }

        /**
         * 플레이어의 크로스헤어(커서)에 있는 엔티티의 ID를 반환합니다.
         * 블록과의 충돌을 고려하며, 특정 블록은 무시할 수 있습니다.
         *
         * @param maxDistance 최대 확인 거리
         * @return 커서에 있는 엔티티의 ID, 없으면 Integer.MIN_VALUE
         */
        @ApiVisibility(ApiType.ACTION)
        public int getEntityUnderCursor(double maxDistance) {
            return getEntityUnderCursor(maxDistance, Blocks.GRASS, Blocks.TALL_GRASS);
        }

        /**
         * 플레이어의 크로스헤어(커서)에 있는 엔티티의 ID를 반환합니다.
         * 블록과의 충돌을 고려하며, 특정 블록은 무시할 수 있습니다.
         *
         * @param maxDistance 최대 확인 거리
         * @param ignoredBlocks 무시할 블록 목록 (선택사항)
         * @return 커서에 있는 엔티티의 ID, 없으면 Integer.MIN_VALUE
         */
        @ApiVisibility(ApiType.ACTION)
        public int getEntityUnderCursor(double maxDistance, Block... ignoredBlocks) {
            if (mc.level == null || mc.player == null) {
                return Integer.MIN_VALUE;
            }

            // 무시할 블록 셋 생성
            Set<Block> ignoredBlockSet = new HashSet<>();
            if (ignoredBlocks != null) {
                Collections.addAll(ignoredBlockSet, ignoredBlocks);
            }

            // 플레이어 시선 정보 가져오기
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 lookVec = mc.player.getViewVector(1.0F);
            Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

            // 엔티티 충돌 확인용 레이 생성
            EntityHitResult entityHit = null;
            BlockHitResult blockHit = null;

            // 블록 충돌 확인
            blockHit = mc.level.clip(new ClipContext(
                    eyePos,
                    endPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    mc.player));

            // 블록 충돌이 있고, 그 블록이 무시 목록에 없다면
            boolean blockHitValid = false;
            if (blockHit.getType() != HitResult.Type.MISS) {
                BlockPos blockPos = blockHit.getBlockPos();
                Block hitBlock = mc.level.getBlockState(blockPos).getBlock();

                // 블록이 무시 목록에 없으면 유효한 충돌로 간주
                blockHitValid = !ignoredBlockSet.contains(hitBlock);
            }

            // 블록 충돌 거리 계산 (충돌이 없거나 무시된 경우 최대 거리로 설정)
            double blockHitDistance = blockHitValid ?
                    blockHit.getLocation().distanceTo(eyePos) : maxDistance;

            // 엔티티 충돌 확인
            // 모든 엔티티에 대해 레이 충돌 검사
            List<Entity> entities = mc.level.getEntities(mc.player,
                    mc.player.getBoundingBox().inflate(maxDistance),
                    entity -> entity instanceof LivingEntity && entity != mc.player && entity.isAlive());

            Entity closestEntity = null;
            double closestDistance = Double.MAX_VALUE;

            for (Entity entity : entities) {
                final String type = Registries.ENTITY_TYPES.getKey(entity.getType()).toString();
                if(IGNORE_ENTITY_LIST.contains(type)) {
                    continue;
                }
                // 엔티티의 히트박스와 레이의 충돌 확인
                AABB box = entity.getBoundingBox();
                Optional<Vec3> hitOptional = box.clip(eyePos, endPos);

                if (hitOptional.isPresent()) {
                    Vec3 hitPos = hitOptional.get();
                    double distance = eyePos.distanceTo(hitPos);

                    // 블록 충돌보다 가까워야 함
                    if (distance < closestDistance && distance < blockHitDistance) {
                        closestEntity = entity;
                        closestDistance = distance;
                    }
                }
            }

            return closestEntity != null ? closestEntity.getId() : Integer.MIN_VALUE;
        }

        /**
         * 플레이어의 크로스헤어(커서)에 있는 엔티티의 ID를 반환합니다.
         * 블록과의 충돌을 고려하며, 특정 블록 타입은 무시할 수 있습니다.
         *
         * @param maxDistance 최대 확인 거리
         * @return 커서에 있는 엔티티의 ID, 없으면 Integer.MIN_VALUE
         */
        @ApiVisibility(ApiType.ACTION)
        public int getEntityUnderCursorWithPredicate(double maxDistance) {
            return getEntityUnderCursorWithPredicate(maxDistance, blockState -> false);
        }

        /**
         * 플레이어의 크로스헤어(커서)에 있는 엔티티의 ID를 반환합니다.
         * 블록과의 충돌을 고려하며, 특정 블록 타입은 무시할 수 있습니다.
         *
         * @param maxDistance 최대 확인 거리
         * @param ignoredBlockTypes 무시할 블록 타입 목록 (BlockState Predicate)
         * @return 커서에 있는 엔티티의 ID, 없으면 Integer.MIN_VALUE
         */
        @ApiVisibility(ApiType.ACTION)
        public int getEntityUnderCursorWithPredicate(double maxDistance, Predicate<BlockState> ignoredBlockTypes) {
            if (mc.level == null || mc.player == null || ignoredBlockTypes == null) {
                return Integer.MIN_VALUE;
            }

            // 플레이어 시선 정보 가져오기
            Vec3 eyePos = mc.player.getEyePosition();
            Vec3 lookVec = mc.player.getViewVector(1.0F);
            Vec3 endPos = eyePos.add(lookVec.scale(maxDistance));

            // 블록 충돌 확인 (커스텀 블록 타입 필터링을 위한 클래스)
            BlockHitResult blockHit = mc.level.clip(
                    new ClipContext(
                            eyePos,
                            endPos,
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE,
                            mc.player
                    )
            );

            // 블록 충돌이 있고, 그 블록이 무시 목록에 있는지 확인
            boolean blockHitValid = false;
            if (blockHit.getType() != HitResult.Type.MISS) {
                BlockPos blockPos = blockHit.getBlockPos();
                BlockState hitBlockState = mc.level.getBlockState(blockPos);

                // 블록이 무시 목록에 있으면 충돌을 무시
                blockHitValid = !ignoredBlockTypes.test(hitBlockState);
            }

            // 블록 충돌 거리 계산 (충돌이 없거나 무시된 경우 최대 거리로 설정)
            double blockHitDistance = blockHitValid ?
                    blockHit.getLocation().distanceTo(eyePos) : maxDistance;

            // 엔티티 충돌 확인
            List<Entity> entities = mc.level.getEntities(mc.player,
                    mc.player.getBoundingBox().inflate(maxDistance),
                    entity -> entity instanceof LivingEntity && entity != mc.player && entity.isAlive());

            Entity closestEntity = null;
            double closestDistance = Double.MAX_VALUE;

            for (Entity entity : entities) {
                // 엔티티의 히트박스와 레이의 충돌 확인
                AABB box = entity.getBoundingBox();
                Optional<Vec3> hitOptional = box.clip(eyePos, endPos);

                if (hitOptional.isPresent()) {
                    Vec3 hitPos = hitOptional.get();
                    double distance = eyePos.distanceTo(hitPos);

                    // 블록 충돌보다 가까워야 함
                    if (distance < closestDistance && distance < blockHitDistance) {
                        closestEntity = entity;
                        closestDistance = distance;
                    }
                }
            }

            return closestEntity != null ? closestEntity.getId() : Integer.MIN_VALUE;
        }

    }

    private static Vec3 getEntityHeadPosition(Entity entity) {
        if (entity == null) return null;

        //return eyeposition if entity is LivingEntity
        if (entity instanceof LivingEntity) {
            return ((LivingEntity) entity).getEyePosition();
        }

        // else box center
        AABB box = entity.getBoundingBox();
        return new Vec3(
                (box.minX + box.maxX) / 2,
                box.maxY,
                (box.minZ + box.maxZ) / 2
        );
    }

    private static Vec3 getEntityCenterPosition(Entity entity) {
        if (entity == null) return null;

        return entity.getBoundingBox().getCenter();
    }

}