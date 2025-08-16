package com.zergatul.cheatutils.modules.tacz;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import com.zergatul.cheatutils.common.Events;
import com.zergatul.cheatutils.scripting.api.Root;
import com.zergatul.cheatutils.scripting.api.modules.PlayerApi;
import com.zergatul.cheatutils.utils.TaczUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tacz 총알 명중 시 엔티티 체력 변화를 추적하는 시스템
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class TaczEntityTracker {
    
    private static final Logger logger = LogManager.getLogger(TaczEntityTracker.class);
    private static final Minecraft mc = Minecraft.getInstance();
    
    // 엔티티 ID와 예상 체력을 매핑하는 맵
    private static final Map<Integer, Float> entityHealthMap = new ConcurrentHashMap<>();

    // 체력이 0이 된 엔티티들의 ID 집합
    private static final Set<Integer> deadEntities = ConcurrentHashMap.newKeySet();

    // 엔티티 예측 시간을 저장하는 맵 (자동 정리용)
    private static final Map<Integer, Long> entityPredictionTime = new ConcurrentHashMap<>();

    // 예측 유효 시간 (밀리초) - 3초 후 자동 정리
    private static final long PREDICTION_TIMEOUT = 500;
    
    // 싱글톤 인스턴스
    public static final TaczEntityTracker instance = new TaczEntityTracker();
    
    private TaczEntityTracker() {
        // 플레이어 로그인 시 데이터 초기화
        Events.ClientPlayerLoggingIn.add(connection -> clearAllData());
        // 차원 변경 시 데이터 초기화
        Events.DimensionChange.add(this::clearAllData);
        // 월드 언로드 시 데이터 초기화
        Events.WorldUnload.add(this::clearAllData);
    }
    
    /**
     * 총 발사 이벤트 처리 - 맞을 예정인 엔티티를 예측하고 트래킹
     */
    @SubscribeEvent
    public static void onGunShoot(GunShootEvent event) {
        try {
            if (mc.player == null || mc.level == null) {
                return;
            }

            // 발사한 플레이어가 현재 플레이어인지 확인
            if (!event.getShooter().equals(mc.player)) {
                return;
            }

            ItemStack gunStack = event.getGunItemStack();
            if (gunStack.isEmpty() || !(gunStack.getItem() instanceof IGun gun)) {
                return;
            }

            logger.debug("Gun shot detected: {}", gunStack.getDisplayName().getString());

            // 총알이 맞을 예정인 엔티티 예측
            Entity targetEntity = predictTargetEntity();
            if (targetEntity instanceof LivingEntity livingEntity) {
                // 총의 데미지 계산
                double damage = calculateTotalDamage(gunStack, gun, livingEntity);
                float currentHealth = livingEntity.getHealth();
                float expectedHealth = Math.max(0, currentHealth - (float)damage);

                int entityId = targetEntity.getId();
                entityHealthMap.put(entityId, expectedHealth);

                logger.debug("Predicted target: {} (ID: {}), Current Health: {}, Expected Damage: {}, Expected Health: {}",
                        targetEntity.getName().getString(), entityId, currentHealth, damage, expectedHealth);

                // 예상 체력이 0이 되면 죽을 예정인 엔티티로 표시
                if (expectedHealth <= 0) {
                    deadEntities.add(entityId);
                    entityPredictionTime.put(entityId, System.currentTimeMillis());
                    logger.debug("Entity {} predicted to die from shot (expected health: {})",
                            targetEntity.getName().getString(), expectedHealth);
                }
            }

        } catch (Exception e) {
            logger.error("Error in onGunShoot: ", e);
        }
    }
    
    /**
     * 엔티티가 총에 맞았을 때 이벤트 처리 - 예측과 실제 결과 검증
     */
    @SubscribeEvent
    public static void onEntityHurtByGun(EntityHurtByGunEvent event) {
        try {
            if (mc.player == null || mc.level == null) {
                return;
            }

            Entity hurtEntity = event.getHurtEntity();
            if (!(hurtEntity instanceof LivingEntity livingEntity)) {
                return;
            }

            // 발사한 플레이어가 현재 플레이어인지 확인
            if (!event.getAttacker().equals(mc.player)) {
                return;
            }

            float actualDamage = event.getAmount();
            int entityId = hurtEntity.getId();
            float currentHealth = livingEntity.getHealth();
            float actualExpectedHealth = Math.max(0, currentHealth - actualDamage);

            logger.debug("Entity {} actually hurt by gun. Actual Damage: {}, Current Health: {}, Actual Expected Health: {}",
                    hurtEntity.getName().getString(), actualDamage, currentHealth, actualExpectedHealth);

            // 예측된 체력과 실제 결과 비교 검증
            Float predictedHealth = entityHealthMap.get(entityId);
            if (predictedHealth != null) {
                float healthDifference = Math.abs(predictedHealth - actualExpectedHealth);
                if (healthDifference > 0.1f) { // 0.1 체력 차이 허용
                    logger.warn("Health prediction mismatch for entity {}: predicted={}, actual={}, difference={}",
                            hurtEntity.getName().getString(), predictedHealth, actualExpectedHealth, healthDifference);
                } else {
                    logger.debug("Health prediction accurate for entity {}: predicted={}, actual={}",
                            hurtEntity.getName().getString(), predictedHealth, actualExpectedHealth);
                }
            }

            // 실제 결과로 업데이트
            entityHealthMap.put(entityId, actualExpectedHealth);

            // 실제로 체력이 0이 되었다면 죽은 엔티티 목록에 추가
            if (actualExpectedHealth <= 0) {
                deadEntities.add(entityId);
                logger.debug("Entity {} confirmed dead (actual expected health: {})",
                        hurtEntity.getName().getString(), actualExpectedHealth);
            } else {
                // 예측에서는 죽을 예정이었지만 실제로는 살아있다면 제거
                deadEntities.remove(entityId);
            }

        } catch (Exception e) {
            logger.error("Error in onEntityHurtByGun: ", e);
        }
    }
    
    /**
     * 엔티티가 죽었는지 확인
     */
    public static boolean isEntityDead(int entityId) {
        return deadEntities.contains(entityId);
    }
    
    /**
     * 엔티티의 예상 체력 반환
     */
    public static float getExpectedHealth(int entityId) {
        return entityHealthMap.getOrDefault(entityId, -1f);
    }
    
    /**
     * 엔티티가 살아있는지 확인 (실제 체력과 예상 체력을 모두 고려)
     */
    public static boolean isEntityAlive(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }

        if (!(entity instanceof LivingEntity livingEntity)) {
            return entity.isAlive();
        }

        int entityId = entity.getId();

        // 예측 시간이 만료된 엔티티들 정리
        cleanupExpiredPredictions();

        // 죽은 엔티티 목록에 있는지 확인
        if (deadEntities.contains(entityId)) {
            // 실제 체력을 다시 확인하여 살아있다면 목록에서 제거
            if (livingEntity.getHealth() > 0) {
                logger.debug("Entity {} was predicted to die but is still alive (health: {}), removing from dead list",
                        entity.getName().getString(), livingEntity.getHealth());
                removeEntity(entityId);
                return true;
            }
            return false;
        }

        // 실제 체력이 0 이하라면 죽은 것으로 간주
        if (livingEntity.getHealth() <= 0) {
            deadEntities.add(entityId);
            return false;
        }

        return true;
    }
    
    /**
     * 특정 엔티티의 추적 정보 제거
     */
    public static void removeEntity(int entityId) {
        entityHealthMap.remove(entityId);
        deadEntities.remove(entityId);
        entityPredictionTime.remove(entityId);
    }
    
    /**
     * 모든 추적 데이터 초기화
     */
    public void clearAllData() {
        entityHealthMap.clear();
        deadEntities.clear();
        entityPredictionTime.clear();
        logger.debug("Cleared all entity tracking data");
    }
    
    /**
     * 현재 추적 중인 엔티티 수 반환
     */
    public static int getTrackedEntityCount() {
        return entityHealthMap.size();
    }
    
    /**
     * 죽은 엔티티 수 반환
     */
    public static int getDeadEntityCount() {
        return deadEntities.size();
    }

    /**
     * 만료된 예측들을 정리
     */
    private static void cleanupExpiredPredictions() {
        long currentTime = System.currentTimeMillis();
        Set<Integer> expiredEntities = new HashSet<>();

        for (Map.Entry<Integer, Long> entry : entityPredictionTime.entrySet()) {
            if (currentTime - entry.getValue() > PREDICTION_TIMEOUT) {
                expiredEntities.add(entry.getKey());
            }
        }

        for (Integer entityId : expiredEntities) {
            logger.debug("Removing expired prediction for entity ID: {}", entityId);
            removeEntity(entityId);
        }
    }

    /**
     * 수동으로 만료된 예측 정리 (외부에서 호출 가능)
     */
    public static void cleanupExpiredPredictionsManual() {
        cleanupExpiredPredictions();
    }

    /**
     * 총알이 맞을 예정인 타겟 엔티티를 예측
     */
    private static Entity predictTargetEntity() {
        if (mc.player == null || mc.level == null) {
            return null;
        }

        try {

            // PlayerApi를 사용하여 커서에 가장 가까운 엔티티 찾기
            final var playerApi = Root.player;

            int entityId = playerApi.target.getEntityInNearestCursor(100, 45, false); // 100블록, 45도 각도

            if (entityId == Integer.MIN_VALUE) {
                return null;
            }

            Entity entity = mc.level.getEntity(entityId);
            if (entity instanceof LivingEntity && entity.isAlive()) {
                return entity;
            }

        } catch (Exception e) {
            logger.error("Error predicting target entity: ", e);
        }

        return null;
    }

    /**
     * 총의 총 데미지를 계산 (헤드샷, 방어구 관통 등 고려)
     */
    private static double calculateTotalDamage(ItemStack gunStack, IGun gun, LivingEntity target) {
        try {
            // 기본 데미지
            double baseDamage = TaczUtils.calculateGunDamage(gunStack, gun);

            // 헤드샷 배율 (실제 헤드샷 여부는 알 수 없으므로 기본값 사용)
            double headshotMultiplier = TaczUtils.getHeadshotMultiplier(gunStack, gun);

            // 방어구 관통력
            double armorIgnore = TaczUtils.getArmorIgnore(gunStack, gun);

            // 폭발 데미지
            double explosionDamage = TaczUtils.getExplosionDamage(gunStack, gun);

            // 기본적으로는 헤드샷이 아닌 것으로 가정하고 계산
            double totalDamage = baseDamage + explosionDamage;

            // 방어구 관통력 적용 (간단한 계산)
            if (armorIgnore > 0) {
                totalDamage *= (1.0 + armorIgnore);
            }

            logger.debug("Calculated total damage: {} (base: {}, explosion: {}, armor ignore: {})",
                    totalDamage, baseDamage, explosionDamage, armorIgnore);

            return totalDamage;

        } catch (Exception e) {
            logger.error("Error calculating total damage: ", e);
            return 0.0;
        }
    }
}
