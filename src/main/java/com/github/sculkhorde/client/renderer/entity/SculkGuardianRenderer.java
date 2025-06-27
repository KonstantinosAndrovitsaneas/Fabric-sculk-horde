package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkGuardianModel;
import com.github.sculkhorde.common.entity.SculkGuardianEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkGuardianRenderer extends GeoEntityRenderer<SculkGuardianEntity> {


    public SculkGuardianRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkGuardianModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
