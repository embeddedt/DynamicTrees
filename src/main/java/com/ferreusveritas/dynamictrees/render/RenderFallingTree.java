package com.ferreusveritas.dynamictrees.render;

import java.util.Map;

import com.ferreusveritas.dynamictrees.entities.EntityFallingTree;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderFallingTree extends Render<EntityFallingTree>{
	
	protected RenderFallingTree(RenderManager renderManager) {
		super(renderManager);
	}
	
	@Override
	protected ResourceLocation getEntityTexture(EntityFallingTree entity) {
		return null;
	}
	
	@Override
	public void doRender(EntityFallingTree entity, double x, double y, double z, float entityYaw, float partialTicks) {
		super.doRender(entity, x, y, z, entityYaw, partialTicks);

		BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
		BlockPos cutPos = entity.getCutPos();
		
		GlStateManager.disableLighting();
		
		for( Map.Entry<BlockPos, IExtendedBlockState> entry : entity.getStateMap().entrySet()) {
			BlockPos pos = entry.getKey(); //Get the relative position of the block
			IExtendedBlockState exState = entry.getValue();
			IBakedModel model = dispatcher.getModelForState(exState.getClean());
			
			World world = entity.getEntityWorld();
			
			this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
			GlStateManager.pushMatrix();
			
			Tessellator tessellator = Tessellator.getInstance();
			BufferBuilder bufferbuilder = tessellator.getBuffer();
			bufferbuilder.begin(7, DefaultVertexFormats.BLOCK);
			
			Vec3d gc = entity.getMassCenter();
			
			GlStateManager.translate(x, y, z);
			GlStateManager.translate(gc.x, gc.y, gc.z);
			GlStateManager.rotate(entityYaw, 0, 1, 0);
			GlStateManager.translate(-gc.x, -gc.y, -gc.z);
			GlStateManager.translate(-x, -y, -z);
			
			double dx = x - cutPos.getX() + pos.getX() - 0.5;
			double dy = y - cutPos.getY() + pos.getY();
			double dz = z - cutPos.getZ() + pos.getZ() - 0.5;
			GlStateManager.translate(dx, dy, dz);

			dispatcher.getBlockModelRenderer().renderModel(world, model, exState, cutPos, bufferbuilder, false, 0);
			tessellator.draw();
				
			GlStateManager.popMatrix();
		}
		
		GlStateManager.enableLighting();
	}
	
	public static class Factory implements IRenderFactory<EntityFallingTree> {
		
		@Override
		public Render<EntityFallingTree> createRenderFor(RenderManager manager) {
			return new RenderFallingTree(manager);
		}
		
	}
	
}

