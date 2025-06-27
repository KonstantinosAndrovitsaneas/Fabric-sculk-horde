package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkWitchModel;
import com.github.sculkhorde.common.entity.SculkWitchEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkWitchRenderer extends GeoEntityRenderer<SculkWitchEntity> {


    public SculkWitchRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkWitchModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
