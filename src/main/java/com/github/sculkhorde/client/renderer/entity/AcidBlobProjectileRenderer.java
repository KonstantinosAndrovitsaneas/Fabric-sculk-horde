package com.github.sculkhorde.client.renderer.entity;

import com.github.sculkhorde.client.model.enitity.AcidBlobProjectileModel;
import com.github.sculkhorde.common.entity.projectile.AcidBlobProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AcidBlobProjectileRenderer extends GeoEntityRenderer<AcidBlobProjectileEntity> {
    public AcidBlobProjectileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AcidBlobProjectileModel());
    }
}