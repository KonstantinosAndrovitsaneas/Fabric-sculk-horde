package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkPhantomModel;
import com.github.sculkhorde.common.entity.SculkPhantomEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkPhantomRenderer extends GeoEntityRenderer<SculkPhantomEntity> {


    public SculkPhantomRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkPhantomModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
