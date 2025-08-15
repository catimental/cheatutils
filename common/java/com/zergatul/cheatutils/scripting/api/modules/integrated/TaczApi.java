package com.zergatul.cheatutils.scripting.api.modules.integrated;


import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.compat.kubejs.events.GunKubeJSEvents;
import com.tacz.guns.compat.kubejs.events.TimelessCommonEvents;
import com.zergatul.cheatutils.configs.ConfigStore;
import com.zergatul.cheatutils.configs.TaczConfig;
import com.zergatul.cheatutils.scripting.api.modules.ModuleApi;


public class TaczApi  extends ModuleApi<TaczConfig> {


    @Override
    protected TaczConfig getConfig() {
        return ConfigStore.instance.getConfig().taczConfig;
    }

}
