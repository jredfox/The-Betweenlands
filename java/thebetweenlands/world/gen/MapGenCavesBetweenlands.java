package thebetweenlands.world.gen;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;
import thebetweenlands.blocks.BLBlockRegistry;
import thebetweenlands.utils.MathUtils;
import thebetweenlands.world.WorldProviderBetweenlands;
import thebetweenlands.world.biomes.base.BLBiomeRegistry;
import thebetweenlands.world.biomes.base.BiomeGenBaseBetweenlands;

import com.google.common.collect.Lists;

public class MapGenCavesBetweenlands extends MapGenBase {
	private OpenSimplexNoise cave;

	private OpenSimplexNoise seaLevelBreak;

	private FractalOpenSimplexNoise form;

	private List<BiomeGenBaseBetweenlands> noBreakBiomes;

	public MapGenCavesBetweenlands(long seed) {
		cave = new OpenSimplexNoise(seed);
		seaLevelBreak = new OpenSimplexNoise(seed + 1);
		form = new FractalOpenSimplexNoise(seed + 2, 4, 0.1);
		noBreakBiomes = Lists.newArrayList(
			BLBiomeRegistry.deepWater,
			BLBiomeRegistry.coarseIslands,
			BLBiomeRegistry.marsh1,
			BLBiomeRegistry.marsh2,
			BLBiomeRegistry.patchyIslands
		);
	}

	@Override
	public void func_151539_a(IChunkProvider chunkProvider, World world, int chunkX, int chunkZ, Block[] blocks) {
		int cx = chunkX * 16;
		int cz = chunkZ * 16;
		int slice = blocks.length / 256;
		for (int bx = 0; bx < 16; bx++) {
			for (int bz = 0; bz < 16; bz++) {
				int x = cx + bx, z = cz + bz;
				BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
				boolean shouldntBreak = noBreakBiomes.contains(biome);
				int xzIndex = (bx * 16 + bz) * slice;
				int level = 0;
				while (!(blocks[xzIndex + level] != null && blocks[xzIndex + level++].getMaterial().isLiquid()) && level <= WorldProviderBetweenlands.LAYER_HEIGHT);
				boolean brokeSurface = false;
				for (int y = 0; y <= level; y++) {
					double noise = cave.eval(x * 0.08, y * 0.15, z * 0.08) + form.eval(x * 0.5, y * 0.3, z * 0.5) * 0.4;
					double limit = -0.3;
					if (y <= 10) {
						limit = (limit + 1) / 10 * y - 1;
					}
					int surfaceDist = level - y;
					int lead = 20;
					if (surfaceDist <= lead) {
						double s = (shouldntBreak ? 2.5F : (seaLevelBreak.eval(x * 0.05, z * 0.05) * 0.5 + 0.5)) * 0.5 * (1 - surfaceDist / (float) lead);
						noise += s;
					}
					if (noise < limit) {
						if (y == level) {
							brokeSurface = true;
						}
						int index = xzIndex + y;
						if (blocks[index] == null || blocks[index].getMaterial().isLiquid()) {
							continue;
						}
						blocks[index] = y > WorldProviderBetweenlands.WATER_HEIGHT ? Blocks.air : BLBlockRegistry.swampWater;
					} else if (y == level && noise < limit + 0.5) {
						double h = MathUtils.linearTransformd(noise, limit, limit + 0.5, 0, 1);
						if (h < 0.5) {
							brokeSurface = true;
						}
						// https://www.desmos.com/calculator/nhbhw6jwk6
						double f1 = Math.sin(2 * Math.PI * (Math.pow(1 - h, 2) - 0.25)) * 0.5 + 0.5;
						double f2 = Math.sqrt(Math.pow(0.5, 2) - Math.pow(h - 0.5, 2));
						int height = (int) ((f1 + f2) / 1.4576 * 3);
						for (int dy = -1; dy < height; dy++) {
							blocks[xzIndex + y + dy] = dy < height - 1 ? BLBlockRegistry.swampDirt : BLBlockRegistry.swampGrass;
						}
					}
				}
				if (brokeSurface) {
					int newlevel = WorldProviderBetweenlands.LAYER_HEIGHT;
					while (newlevel > 0) {
						if (blocks[xzIndex + newlevel] != null) {
							Material material = blocks[xzIndex + newlevel].getMaterial();
							if (blocks[xzIndex + newlevel] != BLBlockRegistry.algae && material != Material.air && !material.isLiquid()) {
								break;
							}
						}
						newlevel--;
					}
					if (newlevel > 0) {
						for (int y = newlevel; y < WorldProviderBetweenlands.LAYER_HEIGHT + 2; y++) {
							blocks[xzIndex + y] = Blocks.air;
						}
					}
				}
			}
		}
	}
}