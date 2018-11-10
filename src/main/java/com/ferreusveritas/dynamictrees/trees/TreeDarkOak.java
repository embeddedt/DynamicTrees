package com.ferreusveritas.dynamictrees.trees;

import java.util.List;
import java.util.Random;
import java.util.function.BiFunction;

import com.ferreusveritas.dynamictrees.ModBlocks;
import com.ferreusveritas.dynamictrees.ModConfigs;
import com.ferreusveritas.dynamictrees.api.TreeHelper;
import com.ferreusveritas.dynamictrees.blocks.BlockBranch;
import com.ferreusveritas.dynamictrees.blocks.BlockSurfaceRoot;
import com.ferreusveritas.dynamictrees.systems.GrowSignal;
import com.ferreusveritas.dynamictrees.systems.dropcreators.DropCreatorApple;
import com.ferreusveritas.dynamictrees.systems.featuregen.FeatureGenHugeMushrooms;
import com.ferreusveritas.dynamictrees.systems.featuregen.FeatureGenMound;
import com.ferreusveritas.dynamictrees.systems.featuregen.FeatureGenRoots;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.worldgen.JoCode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockNewLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary.Type;

public class TreeDarkOak extends TreeFamilyVanilla {
	
	public class SpeciesDarkOak extends Species {
		
		protected FeatureGenHugeMushrooms underGen;
		protected FeatureGenRoots rootGen;
		protected FeatureGenMound moundGen;
		protected int mountCutoffRadius = 5;
		
		SpeciesDarkOak(TreeFamily treeFamily) {
			super(treeFamily.getName(), treeFamily, ModBlocks.darkOakLeavesProperties);
			
			//Dark Oak Trees are tall, slowly growing, thick trees
			setBasicGrowingParameters(0.30f, 18.0f, 4, 6, 0.8f);
			
			setSoilLongevity(14);//Grows for a long long time
			
			envFactor(Type.COLD, 0.75f);
			envFactor(Type.HOT, 0.50f);
			envFactor(Type.DRY, 0.25f);
			envFactor(Type.MUSHROOM, 1.25f);
			
			if(ModConfigs.worldGen && !ModConfigs.enableAppleTrees) {
				addDropCreator(new DropCreatorApple());
			}
			
			setupStandardSeedDropping();
			underGen = new FeatureGenHugeMushrooms(this);
			rootGen = new FeatureGenRoots(this, 13).setScaler(getRootScaler());
			moundGen = new FeatureGenMound(this);
		}
		
		@Override
		public BlockPos preGeneration(World world, BlockPos rootPos, int radius, EnumFacing facing, SafeChunkBounds safeBounds, JoCode joCode) {
			
			//Erase a volume of blocks that could potentially get in the way
			for(MutableBlockPos pos : BlockPos.getAllInBoxMutable(rootPos.add(new Vec3i(-1,  1, -1)), rootPos.add(new Vec3i(1, 6, 1)))) {
				world.setBlockToAir(pos);
			}
			
			if(safeBounds != SafeChunkBounds.ANY) {//worldgen
				if(radius >= mountCutoffRadius) {
					rootPos = moundGen.preGeneration(world, rootPos, radius, facing, safeBounds, joCode);
				}
			}
			
			return rootPos;
		}
		
		@Override
		public void postGeneration(World world, BlockPos rootPos, Biome biome, int radius, List<BlockPos> endPoints, SafeChunkBounds safeBounds, IBlockState initialDirtState) {
			super.postGeneration(world, rootPos, biome, radius, endPoints, safeBounds, initialDirtState);
			
			int trunkRadius = flareBottom(world, rootPos, 1);
						
			if(safeBounds != SafeChunkBounds.ANY) {//worldgen
				if(radius < mountCutoffRadius) {//A mound was already generated in preGen if the radius is 5 or greater
					moundGen.postGeneration(world, rootPos, biome, radius, endPoints, safeBounds, initialDirtState);
				}
				
				//Generate huge mushroom undergrowth
				underGen.postGeneration(world, rootPos, biome, radius, endPoints, safeBounds, initialDirtState);
			}
			
			rootGen.setTrunkRadius(trunkRadius).postGeneration(world, rootPos, biome, trunkRadius, endPoints, safeBounds, initialDirtState);			
		}
		
		protected BiFunction<Integer, Integer, Integer> getRootScaler() {
			return (inRadius, trunkRadius) -> {
				float scale = MathHelper.clamp(trunkRadius >= 13 ? (trunkRadius / 24f) : 0, 0, 1);
				return (int) (inRadius * scale);
			};
		}
		
		/**
		 * Put a cute little flare on the bottom of the dark oaks
		 * 
		 * @param world The world
		 * @param rootPos The position of the rooty dirt block of the tree
		 * @return The radius of the bottom trunk section after operation
		 */
		public int flareBottom(World world, BlockPos rootPos, int soilLife) {
			
			if(soilLife > 0) {
				//Put a cute little flare on the bottom of the dark oaks
				int radius3 = TreeHelper.getRadius(world, rootPos.up(3));
				
				if(radius3 > 6) {
					getDynamicBranch().setRadius(world, rootPos.up(2), radius3 + 1, EnumFacing.UP);
					return getDynamicBranch().setRadius(world, rootPos.up(1), radius3 + 2, EnumFacing.UP);
				}
			}
			
			return TreeHelper.getRadius(world, rootPos.up(1));
		}
		
		@Override
		public boolean isBiomePerfect(Biome biome) {
			return isOneOfBiomes(biome, Biomes.ROOFED_FOREST);
		};
		
		@Override
		public int getLowestBranchHeight(World world, BlockPos pos) {
			return (int)(super.getLowestBranchHeight(world, pos) * biomeSuitability(world, pos));
		}
		
		@Override
		public float getEnergy(World world, BlockPos pos) {
			return super.getEnergy(world, pos) * biomeSuitability(world, pos);
		}
		
		@Override
		public float getGrowthRate(World world, BlockPos pos) {
			return super.getGrowthRate(world, pos) * biomeSuitability(world, pos);
		}
		
		@Override
		public boolean postGrow(World world, BlockPos rootPos, BlockPos treePos, int soilLife, boolean natural) {
			int trunkRadius = flareBottom(world, rootPos, soilLife);
			rootGen.setTrunkRadius(trunkRadius).postGrow(world, rootPos, treePos, soilLife, natural);
			return super.postGrow(world, rootPos, treePos, soilLife, natural);
		}
		
		@Override
		protected int[] customDirectionManipulation(World world, BlockPos pos, int radius, GrowSignal signal, int probMap[]) {
			
			probMap[EnumFacing.UP.getIndex()] = 4;
			
			//Disallow up/down turns after having turned out of the trunk once.
			if(!signal.isInTrunk()) {
				probMap[EnumFacing.UP.getIndex()] = 0;
				probMap[EnumFacing.DOWN.getIndex()] = 0;
				probMap[signal.dir.ordinal()] *= 0.35;//Promotes the zag of the horizontal branches
			}
			
			//Amplify cardinal directions to encourage spread the higher we get
			float energyRatio = signal.delta.getY() / getEnergy(world, pos);
			float spreadPush = energyRatio * 2;
			spreadPush = spreadPush < 1.0f ? 1.0f : spreadPush;
			for(EnumFacing dir: EnumFacing.HORIZONTALS) {
				probMap[dir.ordinal()] *= spreadPush;
			}
			
			//Ensure that the branch gets out of the trunk at least two blocks so it won't interfere with new side branches at the same level 
			if(signal.numTurns == 1 && signal.delta.distanceSq(0, signal.delta.getY(), 0) == 1.0 ) {
				for(EnumFacing dir: EnumFacing.HORIZONTALS) {
					if(signal.dir != dir) {
						probMap[dir.ordinal()] = 0;
					}
				}
			}
			
			//If the side branches are too swole then give some other branches a chance
			if(signal.isInTrunk()) {
				for(EnumFacing dir: EnumFacing.HORIZONTALS) {
					if(probMap[dir.ordinal()] >= 7) {
						probMap[dir.ordinal()] = 2;
					}
				}
				if(signal.delta.getY() > getLowestBranchHeight() + 5) {
					probMap[EnumFacing.UP.ordinal()] = 0;
					signal.energy = 2;
				}
			}
			
			return probMap;
		}
		
		@Override
		public boolean rot(World world, BlockPos pos, int neighborCount, int radius, Random random, boolean rapid) {
			if(super.rot(world, pos, neighborCount, radius, random, rapid)) {
				if(radius > 2 && TreeHelper.isRooty(world.getBlockState(pos.down())) && world.getLightFor(EnumSkyBlock.SKY, pos) < 6) {
					world.setBlockState(pos, ModBlocks.blockStates.redMushroom);//Change branch to a red mushroom
					world.setBlockState(pos.down(), ModBlocks.blockStates.podzol);//Change rooty dirt to Podzol
				}
				return true;
			}
			
			return false;
		}
	}
	
	BlockSurfaceRoot surfaceRootBlock;
	
	public TreeDarkOak() {
		super(BlockPlanks.EnumType.DARK_OAK);
		ModBlocks.darkOakLeavesProperties.setTree(this);
		
		surfaceRootBlock = new BlockSurfaceRoot(Material.WOOD, getName() + "root");
		
		addConnectableVanillaLeaves((state) -> { return state.getBlock() instanceof BlockNewLeaf && (state.getValue(BlockNewLeaf.VARIANT) == BlockPlanks.EnumType.DARK_OAK); });
	}
	
	@Override
	public void createSpecies() {
		setCommonSpecies(new SpeciesDarkOak(this));
	}
	
	@Override
	public boolean isThick() {
		return true;
	}
	
	@Override
	public int getRadiusForCellKit(IBlockAccess blockAccess, BlockPos pos, IBlockState blockState, EnumFacing dir, BlockBranch branch) {
		int radius = branch.getRadius(blockState);
		if(radius == 1) {
			if(blockAccess.getBlockState(pos.down()).getBlock() == branch) {
				return 128;
			}
		}
		return radius;
	}
	
	@Override
	public List<Block> getRegisterableBlocks(List<Block> blockList) {
		blockList = super.getRegisterableBlocks(blockList);
		blockList.add(surfaceRootBlock);
		return blockList;
	}
	
	@Override
	public List<Item> getRegisterableItems(List<Item> itemList) {
		itemList = super.getRegisterableItems(itemList);
		itemList.add(new ItemBlock(surfaceRootBlock).setRegistryName(surfaceRootBlock.getRegistryName()));
		return itemList;
	}
	
	@Override
	public BlockSurfaceRoot getSurfaceRoots() {
		return surfaceRootBlock;
	}
	
}
