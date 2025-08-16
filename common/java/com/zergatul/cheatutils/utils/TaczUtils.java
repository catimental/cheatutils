package com.zergatul.cheatutils.utils;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;

public class TaczUtils {

    private static final Logger logger = LogManager.getLogger(TaczUtils.class);

    /**
     * 플레이어가 들고 있는 총의 기본 공격 데미지를 계산합니다.
     */
    public static double getAttackDamage(Player player) {
        if(player == null) {
            return 0;
        }

        ItemStack heldItem = player.getMainHandItem();
        if(heldItem.isEmpty()) {
            return 0;
        }

        if(heldItem.getItem() instanceof IGun heldGun) {
            return calculateGunDamage(heldItem, heldGun);
        }

        return 0;
    }

    /**
     * 총의 데미지를 계산합니다 (어태치먼트 포함)
     */
    public static double calculateGunDamage(ItemStack gunStack, IGun gun) {
        try {
            ResourceLocation gunId = gun.getGunId(gunStack);
            if (gunId == null) {
                return 0;
            }

            Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOpt.isEmpty()) {
                return 0;
            }

            CommonGunIndex gunIndex = gunIndexOpt.get();
            GunData gunData = gunIndex.getGunData();

            // 어태치먼트를 고려한 데미지 계산
            double damage = AttachmentDataUtils.getDamageWithAttachment(gunStack, gunData);

            // 총알 개수 고려
            BulletData bulletData = gunData.getBulletData();
            int bulletAmount = bulletData.getBulletAmount();

            // 폭발 데미지 추가 계산
            if (bulletData.getExplosionData() != null &&
                (AttachmentDataUtils.isExplodeEnabled(gunStack, gunData) ||
                 bulletData.getExplosionData().isExplode())) {
                // 폭발 데미지는 별도로 계산되므로 여기서는 기본 데미지만 반환
                logger.debug("Gun has explosion damage: {}", bulletData.getExplosionData().getDamage());
            }

            logger.debug("Calculated gun damage: {} (bullets: {})", damage, bulletAmount);
            return damage;

        } catch (Exception e) {
            logger.error("Error calculating gun damage: ", e);
            return 0;
        }
    }

    /**
     * 총의 헤드샷 배율을 계산합니다
     */
    public static double getHeadshotMultiplier(ItemStack gunStack, IGun gun) {
        try {
            ResourceLocation gunId = gun.getGunId(gunStack);
            if (gunId == null) {
                return 1.0;
            }

            Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOpt.isEmpty()) {
                return 1.0;
            }

            CommonGunIndex gunIndex = gunIndexOpt.get();
            GunData gunData = gunIndex.getGunData();

            return AttachmentDataUtils.getHeadshotMultiplier(gunStack, gunData);

        } catch (Exception e) {
            logger.error("Error calculating headshot multiplier: ", e);
            return 1.0;
        }
    }

    /**
     * 총의 방어구 관통력을 계산합니다
     */
    public static double getArmorIgnore(ItemStack gunStack, IGun gun) {
        try {
            ResourceLocation gunId = gun.getGunId(gunStack);
            if (gunId == null) {
                return 0.0;
            }

            Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOpt.isEmpty()) {
                return 0.0;
            }

            CommonGunIndex gunIndex = gunIndexOpt.get();
            GunData gunData = gunIndex.getGunData();

            return AttachmentDataUtils.getArmorIgnoreWithAttachment(gunStack, gunData);

        } catch (Exception e) {
            logger.error("Error calculating armor ignore: ", e);
            return 0.0;
        }
    }

    /**
     * 총의 폭발 데미지를 계산합니다
     */
    public static double getExplosionDamage(ItemStack gunStack, IGun gun) {
        try {
            ResourceLocation gunId = gun.getGunId(gunStack);
            if (gunId == null) {
                return 0.0;
            }

            Optional<CommonGunIndex> gunIndexOpt = TimelessAPI.getCommonGunIndex(gunId);
            if (gunIndexOpt.isEmpty()) {
                return 0.0;
            }

            CommonGunIndex gunIndex = gunIndexOpt.get();
            GunData gunData = gunIndex.getGunData();
            BulletData bulletData = gunData.getBulletData();

            if (bulletData.getExplosionData() != null &&
                (AttachmentDataUtils.isExplodeEnabled(gunStack, gunData) ||
                 bulletData.getExplosionData().isExplode())) {
                return bulletData.getExplosionData().getDamage();
            }

            return 0.0;

        } catch (Exception e) {
            logger.error("Error calculating explosion damage: ", e);
            return 0.0;
        }
    }
}
