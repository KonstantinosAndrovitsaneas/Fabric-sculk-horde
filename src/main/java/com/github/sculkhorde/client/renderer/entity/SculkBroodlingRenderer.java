package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.SculkBroodlingModel;
import com.github.sculkhorde.common.entity.SculkBroodlingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;


public class SculkBroodlingRenderer extends GeoEntityRenderer<SculkBroodlingEntity> {


    public SculkBroodlingRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SculkBroodlingModel());
    }

}
