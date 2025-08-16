package com.zergatul.cheatutils.scripting.api.modules.integrated;


import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.compat.kubejs.events.GunKubeJSEvents;
import com.tacz.guns.compat.kubejs.events.TimelessCommonEvents;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.TaczConfig;
import com.zergatul.cheatutils.scripting.api.ApiType;
import com.zergatul.cheatutils.scripting.api.ApiVisibility;
import com.zergatul.cheatutils.scripting.api.modules.ModuleApi;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.LogicalSide;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class TaczApi  extends ModuleApi<TaczConfig> {

    private final static Minecraft mc = Minecraft.getInstance();
    private static final Logger logger = LogManager.getLogger(TaczApi.class);
    @Override
    protected TaczConfig getConfig() {
        return ConfigStore.instance.getConfig().taczConfig;
    }

    /**
     * Tacz 총을 발사합니다.
     * @return
     */
    @ApiVisibility(ApiType.ACTION)
    public boolean doShoot() {
        if(mc.player == null) {
            return false ;
        }

        var heldItem = mc.player.getMainHandItem();
        if(heldItem.isEmpty()) {
            return false;
        }

//        if(heldItem.getItem() instanceof AbstractGunItem heldGun) {
        if(heldItem.getItem() instanceof AbstractGunItem IGun) {
//            heldGun.use()
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);
            operator.shoot();
            return true;
        }
        return false;
    }

    /**
     * Tacz 총을 발사할 준비가 되었는지 확인합니다.
     * @return
     */
    @ApiVisibility(ApiType.ACTION)
    public boolean isAimingShootReady() {
        if(mc.player == null) {
            return false;
        }

        if(mc.player.getMainHandItem().getItem() instanceof AbstractGunItem heldGun) {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);
            return operator.getDataHolder().clientAimingProgress >= 0.9;
//            logger.info("");
//            return !operator.getDataHolder().clientStateLock;
        }
        return false;
    }

    /**
     * Tacz 총을 장전합니다.
     * @return
     */
    @ApiVisibility(ApiType.ACTION)
    public boolean doReload() {
        if(mc.player == null) {
            return false;
        }

        if(mc.player.getMainHandItem().getItem() instanceof AbstractGunItem heldGun) {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);
            operator.reload();
            return true;
        }
        return false;
    }

    /**
     * Tacz 총을 장전할 수 있는지 컨디션 체크
     */
    @ApiVisibility(ApiType.ACTION)
    public boolean isReloadReady() {
        if(mc.player == null) {
            return false;
        }

        final var heldItem = mc.player.getMainHandItem();
        if(mc.player.getMainHandItem().getItem() instanceof AbstractGunItem heldGun) {
            IClientPlayerGunOperator operator = IClientPlayerGunOperator.fromLocalPlayer(mc.player);

            return heldGun.getCurrentAmmoCount(heldItem) == 0 && heldGun.hasBulletInBarrel(heldItem)
                    && heldGun.canReload(mc.player, heldItem);

                    //already reloading check
        }
        return false;
    }

}
