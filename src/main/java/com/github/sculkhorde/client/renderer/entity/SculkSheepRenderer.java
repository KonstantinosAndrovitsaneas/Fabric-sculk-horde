package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkSheepModel;
import com.github.sculkhorde.common.entity.SculkSheepEntity;
import com.github.sculkhorde.core.ModConfig;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;


public class SculkSheepRenderer extends GeoEntityRenderer<SculkSheepEntity> {


    public SculkSheepRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkSheepModel());
        if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
