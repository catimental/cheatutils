package com.zergatul.cheatutils.scripting.api.modules;

import com.zergatul.cheatutils.common.Registries;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.EntityTracerConfig;
import com.zergatul.cheatutils.scripting.api.ApiVisibility;
import com.zergatul.cheatutils.scripting.api.ApiType;
import com.zergatul.cheatutils.scripting.api.HelpText;
import com.zergatul.cheatutils.scripting.types.Position3d;
import com.zergatul.cheatutils.utils.ColorUtils;
import com.zergatul.cheatutils.utils.EntityUtils;
import com.zergatul.cheatutils.wrappers.ClassRemapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTagVisitor;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class EntitiesApi {

    private static final Minecraft mc = Minecraft.getInstance();
    
    public boolean isEnabled(String className) {
        var config = getConfig(className);
        if (config == null) {
            return false;
        }
        return config.enabled;
    }

    @ApiVisibility(ApiType.UPDATE)
    public void toggle(String className) {
        var config = getConfig(className);
        if (config == null) {
            return;
        }
        config.enabled = !config.enabled;
        ConfigStore.instance.requestWrite();
    }

    @SuppressWarnings("unchecked")
    public int getCount(String className) {
        EntityUtils.EntityInfo info = EntityUtils.getEntityClass(ClassRemapper.toObf(className));
        if (info == null) {
            return Integer.MIN_VALUE;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }

        int count = 0;
        for (Entity entity: level.entitiesForRendering()) {
            if (info.clazz.isAssignableFrom(entity.getClass())) {
                count++;
            }
        }

        return count;
    }

    public int getCountById(String id) {
        ResourceLocation location = new ResourceLocation(id);
        EntityType<?> type = Registries.ENTITY_TYPES.getValue(location);
        if (type == null) {
            return Integer.MIN_VALUE;
        }

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return 0;
        }

        int count = 0;
        for (Entity entity: level.entitiesForRendering()) {
            if (entity.getType() == type) {
                count++;
            }
        }

        return count;
    }

    private EntityTracerConfig getConfig(String className) {
        var list = ConfigStore.instance.getConfig().entities.configs;
        return list.stream()
                .filter(c -> c.clazz.getName().equals(ClassRemapper.toObf(className)))
                .findFirst()
                .orElse(null);
    }

    @HelpText("""
                Returns integer entity id
                """)
    public int findClosestEntityById(String id) {
        if (mc.level == null || mc.player == null) {
            return Integer.MIN_VALUE;
        }

        EntityType<?> type = Registries.ENTITY_TYPES.getValue(new ResourceLocation(id));
        if (type == null) {
            return Integer.MIN_VALUE;
        }

        Entity target = null;
        double min = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) {
                continue;
            }
            if (entity.getType() == type) {
                double dist = mc.player.distanceToSqr(entity);
                if (dist < min) {
                    min = dist;
                    target = entity;
                }
            }
        }

        return target == null ? Integer.MIN_VALUE : target.getId();
    }

    @HelpText("""
                Returns integer entity id
                """)
    public int findClosestEntityByClass(String className) {
        if (mc.level == null || mc.player == null) {
            return Integer.MIN_VALUE;
        }

        EntityUtils.EntityInfo info = EntityUtils.getEntityClass(ClassRemapper.toObf(className));
        if (info == null) {
            return Integer.MIN_VALUE;
        }

        Entity target = null;
        double min = Double.MAX_VALUE;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player) {
                continue;
            }
            if (info.clazz.isAssignableFrom(entity.getClass())) {
                double dist = mc.player.distanceToSqr(entity);
                if (dist < min) {
                    min = dist;
                    target = entity;
                }
            }
        }

        return target == null ? Integer.MIN_VALUE : target.getId();
    }

    public Position3d getPosition(int entityId) {
        return getValue(
                entityId,
                entity -> new Position3d(entity.getX(), entity.getY(), entity.getZ()),
                () -> new Position3d(0, 0, 0));
    }

    public double getX(int entityId) {
        return getDoubleValue(entityId, Entity::getX);
    }

    public double getY(int entityId) {
        return getDoubleValue(entityId, Entity::getY);
    }

    public double getZ(int entityId) {
        return getDoubleValue(entityId, Entity::getZ);
    }

    public double getXRot(int entityId) {
        return getDoubleValue(entityId, entity -> (double) entity.getXRot());
    }

    public double getYRot(int entityId) {
        return getDoubleValue(entityId, entity -> (double) entity.getYRot());
    }

    public boolean isAlive(int entityId) {
        return getBooleanValue(entityId, Entity::isAlive);
    }

    @HelpText("""
                Returns Minecraft id of entity, or empty string is entity does not exist
                """)
    public String getType(int entityId) {
        return getStringValue(entityId, entity -> {
            EntityType<?> type = entity.getType();
            return Registries.ENTITY_TYPES.getKey(type).toString();
        });
    }

    public boolean hasCustomName(int entityId) {
        return getBooleanValue(entityId, Entity::hasCustomName);
    }

    @HelpText("""
                Gets display name of an entity
                """)
    public String getDisplayName(int entityId) {
        return getStringValue(entityId, entity -> {
            Component name = entity.getDisplayName();
            if (name == null) {
                return "";
            } else {
                return name.getString();
            }
        });
    }

    @HelpText("""
                Gets name of an entity
                """)
    public String getName(int entityId) {
        return getStringValue(entityId, entity -> {
            Component name = entity.getName();
            if (name == null) {
                return "";
            } else {
                return name.getString();
            }
        });
    }

    public boolean isInstanceOf(int entityId, String className) {
        EntityUtils.EntityInfo info = EntityUtils.getEntityClass(ClassRemapper.toObf(className));
        if (info == null) {
            return false;
        }

        if (mc.level == null) {
            return false;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        return info.clazz.isAssignableFrom(entity.getClass());
    }

    public double getHorseMovementSpeed(int entityId) {
        return getDoubleValue(entityId, entity -> {
            if (entity instanceof LivingEntity living) {
                AttributeInstance attribute = living.getAttribute(Attributes.MOVEMENT_SPEED);
                return attribute != null ? attribute.getValue() * 42.16 : Double.NaN;
            } else {
                return Double.NaN;
            }
        });
    }

    public double getHorseJumpHeight(int entityId) {
        return getDoubleValue(entityId, entity -> {
            if (entity instanceof LivingEntity living) {
                AttributeInstance attribute = living.getAttribute(Attributes.JUMP_STRENGTH);
                return attribute != null ? jumpStrengthToHeight(attribute.getValue()) : Double.NaN;
            } else {
                return Double.NaN;
            }
        });
    }

    public boolean isBaby(int entityId) {
        return getBooleanValue(entityId, entity -> {
            if (entity instanceof LivingEntity living) {
                return living.isBaby();
            } else {
                return false;
            }
        });
    }

    public boolean isPassenger(int entityId) {
        return getBooleanValue(entityId, Entity::isPassenger);
    }

    public int getVehicle(int entityId) {
        return getIntegerValue(entityId, entity -> {
            Entity vehicle = entity.getVehicle();
            return vehicle != null ? vehicle.getId() : Integer.MIN_VALUE;
        });
    }

    public int[] getPassengers(int entityId) {
        return getIntegerArrayValue(entityId, entity -> entity.getPassengers().stream().mapToInt(Entity::getId).toArray());
    }

//
//    public ItemStackWrapper getEquippedHeadItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.HEAD), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }
//
//    public ItemStackWrapper getEquippedChestItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.CHEST), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }
//
//    public ItemStackWrapper getEquippedLegsItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.LEGS), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }
//
//    public ItemStackWrapper getEquippedFeetItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.FEET), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }
//
//    public ItemStackWrapper getEquippedMainHandItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.MAINHAND), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }
//
//    public ItemStackWrapper getEquippedOffHandItem(int entityId) {
//        return getValue(entityId, getEquippedItem(EquipmentSlot.OFFHAND), () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }

    public int getHealth(int entityId) {
        return getIntegerValue(entityId, entity -> {
            if (entity instanceof LivingEntity living) {
                return (int) living.getHealth();
            } else {
                return Integer.MIN_VALUE;
            }
        });
    }

//    @HelpText("""
//                Returns ItemStack if entity is ItemEntity
//                """)
//    public ItemStackWrapper getItemStack(int entityId) {
//        return getValue(entityId, (entity, factory) -> {
//            if (entity instanceof ItemEntity itemEntity) {
//                return new ItemStackWrapper(itemEntity.getItem());
//            } else {
//                return factory.get();
//            }
//        }, () -> new ItemStackWrapper(ItemStack.EMPTY));
//    }

//    public String getNbt(int entityId) {
//        return getStringValue(entityId, entity -> {
//            CompoundTag compound = new CompoundTag();
//            entity.saveWithoutId(compound);
//            StringTagVisitor visitor = new StringTagVisitor();
//            compound.accept(visitor);
//            return visitor.build();
//        });
//    }

//    public int getIntTag(int entityId, String tag) {
//        return getIntegerValue(entityId, entity -> {
//            if (entity instanceof LivingEntity living) {
//                CompoundTag compound = new CompoundTag();
//                living.addAdditionalSaveData(compound);
//                Tag item = compound.get(tag);
//                if (item instanceof NumericTag numeric) {
//                    return numeric.intValue();
//                } else {
//                    return Integer.MIN_VALUE;
//                }
//            } else {
//                return Integer.MIN_VALUE;
//            }
//        });
//    }

//    @HelpText("""
//                Checks few simple spawn rules for mob type at specified coordinates.
//                This method doesn't check:
//                 - light conditions
//                 - structures bounds (we do not have them on client)
//                 - more complex rules like animals can spawn on grass
//                """)
//    public boolean canSpawnAt(String id, int x, int y, int z) {
//        if (mc.level == null) {
//            return false;
//        }
//        EntityType<?> type = Registries.ENTITY_TYPES.safeParse(id);
//        if (type == null) {
//            return false;
//        }
//        BlockPos pos = new BlockPos(x, y, z);
//        if (!SpawnPlacements.isSpawnPositionOk(type, mc.level, pos)) {
//            return false;
//        }
//        return mc.level.noBlockCollision(null, type.getSpawnAABB(x + 0.5, y, z + 0.5));
//    }

    @HelpText("""
                Returns bounding box (or hitbox in other words) for specified entity
                """)
    public BoundingBox getBoundingBox(int entityId) {
        return getValue(
                entityId,
                entity -> new BoundingBox(entity.blockPosition()), //TODO: Test
                () -> new BoundingBox(0, 0, 0, 0, 0, 0));
    }

//    private Function<Entity, ItemStackWrapper> getEquippedItem(EquipmentSlot slot) {
//        return entity -> {
//            if (entity instanceof LivingEntity living) {
//                ItemStack stack = living.getItemBySlot(slot);
//                return new ItemStackWrapper(stack);
//            } else {
//                return new ItemStackWrapper(ItemStack.EMPTY);
//            }
//        };
//    }

    private boolean getBooleanValue(int entityId, Function<Entity, Boolean> getter) {
        if (mc.level == null) {
            return false;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return false;
        }

        return getter.apply(entity);
    }

    private int getIntegerValue(int entityId, Function<Entity, Integer> getter) {
        if (mc.level == null) {
            return Integer.MIN_VALUE;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return Integer.MIN_VALUE;
        }

        return getter.apply(entity);
    }

    private double getDoubleValue(int entityId, Function<Entity, Double> getter) {
        if (mc.level == null) {
            return Double.NaN;
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return Double.NaN;
        }

        return getter.apply(entity);
    }

    private String getStringValue(int entityId, Function<Entity, String> getter) {
        if (mc.level == null) {
            return "";
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return "";
        }

        return getter.apply(entity);
    }

    private int[] getIntegerArrayValue(int entityId, Function<Entity, int[]> getter) {
        if (mc.level == null) {
            return new int[0];
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return new int[0];
        }

        return getter.apply(entity);
    }

    private <T> T getValue(int entityId, Function<Entity, T> getter, Supplier<T> defaultSupplier) {
        if (mc.level == null) {
            return defaultSupplier.get();
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return defaultSupplier.get();
        }

        return getter.apply(entity);
    }

    private <T> T getValue(int entityId, BiFunction<Entity, Supplier<T>, T> getter, Supplier<T> defaultSupplier) {
        if (mc.level == null) {
            return defaultSupplier.get();
        }

        Entity entity = mc.level.getEntity(entityId);
        if (entity == null) {
            return defaultSupplier.get();
        }

        return getter.apply(entity, defaultSupplier);
    }

    private static double jumpStrengthToHeight(double s) {
        // based on cubic interpolation from minecraft wiki data
        // {0.4, 1.1093}, {0.5, 1.6248}, {0.6, 2.2216}, {0.7, 2.8933}, {0.8, 3.6339}, {0.9, 4.4379}, {1.0, 5.29997}
        return -0.964722 * s * s * s + 5.48621 * s * s + 0.808726 * s - 0.0303267;
    }
}