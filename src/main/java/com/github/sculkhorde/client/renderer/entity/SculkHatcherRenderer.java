package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkHatcherModel;
import com.github.sculkhorde.common.entity.SculkHatcherEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SculkHatcherRenderer extends GeoEntityRenderer<SculkHatcherEntity> {

    public SculkHatcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkHatcherModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }
}
