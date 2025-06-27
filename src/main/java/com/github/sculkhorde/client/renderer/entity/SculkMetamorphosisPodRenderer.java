package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkMetamorphosisPodModel;
import com.github.sculkhorde.common.entity.SculkMetamorphosisPodEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class SculkMetamorphosisPodRenderer extends GeoEntityRenderer<SculkMetamorphosisPodEntity> {


    public SculkMetamorphosisPodRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkMetamorphosisPodModel());
        //if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
