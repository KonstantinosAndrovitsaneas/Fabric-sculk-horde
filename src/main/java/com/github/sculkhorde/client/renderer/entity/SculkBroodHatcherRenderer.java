package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkBroodHatcherModel;
import com.github.sculkhorde.common.entity.SculkBroodHatcherEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkBroodHatcherRenderer extends GeoEntityRenderer<SculkBroodHatcherEntity> {


    public SculkBroodHatcherRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkBroodHatcherModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
