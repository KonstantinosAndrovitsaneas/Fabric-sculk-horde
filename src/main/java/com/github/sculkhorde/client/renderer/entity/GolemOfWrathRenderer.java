package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.GolemOfWrathModel;
import com.github.sculkhorde.common.entity.GolemOfWrathEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class GolemOfWrathRenderer extends GeoEntityRenderer<GolemOfWrathEntity> {


    public GolemOfWrathRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new GolemOfWrathModel());
        //if(!ModConfig.SERVER.enable_gpu_compatibility_mode.get()) {this.addRenderLayer(new AutoGlowingGeoLayer(this));}
    }

}
