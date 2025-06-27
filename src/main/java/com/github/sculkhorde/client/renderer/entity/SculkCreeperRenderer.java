package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkCreeperModel;
import com.github.sculkhorde.common.entity.SculkCreeperEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkCreeperRenderer extends GeoEntityRenderer<SculkCreeperEntity> {


    public SculkCreeperRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkCreeperModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
