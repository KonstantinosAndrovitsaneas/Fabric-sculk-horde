package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkGhastModel;
import com.github.sculkhorde.client.model.enitity.SculkPhantomModel;
import com.github.sculkhorde.common.entity.SculkGhastEntity;
import com.github.sculkhorde.common.entity.SculkPhantomEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class SculkGhastRenderer extends GeoEntityRenderer<SculkGhastEntity> {


    public SculkGhastRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkGhastModel());
        //if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
