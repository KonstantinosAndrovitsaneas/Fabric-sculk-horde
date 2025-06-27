package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkSpitterModel;
import com.github.sculkhorde.common.entity.SculkSpitterEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class SculkSpitterRenderer extends GeoEntityRenderer<SculkSpitterEntity> {

    public SculkSpitterRenderer(EntityRendererProvider.Context renderManager)
    {
        super(renderManager, new SculkSpitterModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
