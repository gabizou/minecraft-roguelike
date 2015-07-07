package greymerk.roguelike.dungeon;

import greymerk.roguelike.config.RogueConfig;
import greymerk.roguelike.dungeon.settings.LevelSettings;
import greymerk.roguelike.dungeon.settings.SettingsResolver;
import greymerk.roguelike.dungeon.settings.ISettings;
import greymerk.roguelike.dungeon.towers.Tower;
import greymerk.roguelike.treasure.ITreasureChest;
import greymerk.roguelike.worldgen.Coord;
import greymerk.roguelike.worldgen.WorldGenPrimitive;

import java.util.List;
import java.util.Random;

import net.minecraft.world.World;

public class Dungeon implements IDungeon{
		
	public static final int VERTICAL_SPACING = 10;
	public static final int TOPLEVEL = 50;
	public static SettingsResolver settingsResolver;
	
	static{
		initResolver();
	}
	
	public static void initResolver(){
		settingsResolver = new SettingsResolver();
	}
		
	
	public Dungeon(){
		
	}
	
	public void generateNear(World world, Random rand, int x, int z){
		int attempts = 50;
		
		for(int i = 0;i < attempts;i++){
			Coord location = getNearbyCoord(rand, x, z, 40, 100);
			
			if(!validLocation(world, rand, location.getX(), location.getZ())) continue;
			
			ISettings setting = settingsResolver.getSettings(world, rand, location);
			
			if(setting == null) return;
			
			generate(world, setting, location.getX(), location.getZ());
			return;
		}
	}
	
	public void generate(World world, ISettings settings, int inX, int inZ){
		
		int x = inX;
		int y = TOPLEVEL;
		int z = inZ;
		
		Random rand = getRandom(world, inX, inZ);

		int numLevels = settings.getNumLevels();
		
		Coord start = new Coord(x, y, z);
		
		DungeonNode oldEnd = null;
		
		for (int i = 0; i < numLevels; ++i){
			LevelSettings levelSettings = settings.getLevelSettings(i);
			
			IDungeonLevel level;
			
			rand = getRandom(world, x, z);
			
			level = new DungeonLevel(world, rand, levelSettings, start);
			level.generate(start, oldEnd);
			oldEnd = level.getEnd();
			
			x = oldEnd.getPosition().getX();
			y = y - VERTICAL_SPACING;
			z = oldEnd.getPosition().getZ();
			start = new Coord(x, y, z);
		}
		
		Tower tower = settings.getTower().getTower();
		rand = getRandom(world, inX, inZ);
		Tower.get(tower).generate(world, rand, settings.getTower().getTheme(), inX, TOPLEVEL, inZ);
		
		
	}
	
	public static boolean canSpawnInChunk(int chunkX, int chunkZ, World world){
		
		if(!RogueConfig.getBoolean(RogueConfig.DONATURALSPAWN)){
			return false;
		}
		
		if(!RogueConfig.getIntList(RogueConfig.DIMENSIONWL).contains((Integer)world.provider.getDimensionId())){
			return false;
		}

		
		int frequency = RogueConfig.getInt(RogueConfig.SPAWNFREQUENCY);
		int min = 8 * frequency / 10;
		int max = 32 * frequency / 10;
		
		min = min < 2 ? 2 : min;
		max = max < 8 ? 8 : max;
		
		int tempX = chunkX < 0 ? chunkX - (max - 1) : chunkX;
		int tempZ = chunkZ < 0 ? chunkZ - (max - 1) : chunkZ;

		int m = tempX / max;
		int n = tempZ / max;
		
		Random r = world.setRandomSeed(m, n, 10387312);
		
		m *= max;
		n *= max;
		
		m += r.nextInt(max - min);
		n += r.nextInt(max - min);
		
		if(!(chunkX == m && chunkZ == n)){
			return false;
		}
		
		return true;
	}
	
	public void spawnInChunk(World world, Random rand, int chunkX, int chunkZ) {
		
		if(Dungeon.canSpawnInChunk(chunkX, chunkZ, world)){
			int x = chunkX * 16 + 4;
			int z = chunkZ * 16 + 4;
			
			generateNear(world, rand, x, z);
		}
	}
	
	public static int getLevel(int y){
		
		if (y < 15)	return 4;
		if (y < 25) return 3;
		if (y < 35) return 2;
		if (y < 45) return 1;
		return 0;
	}
	
	public static boolean validLocation(World world, Random rand, int x, int z){
		int upperLimit = RogueConfig.getInt(RogueConfig.UPPERLIMIT);
		int lowerLimit = RogueConfig.getInt(RogueConfig.LOWERLIMIT);
		
		if(!world.isAirBlock(new Coord(x, upperLimit, z).getBlockPos())){
			return false;
		}
		
		int y = upperLimit;
		
		while(!WorldGenPrimitive.getBlock(world, new Coord(x, y, z)).getBlock().getMaterial().isOpaque() && y > lowerLimit){
			--y;
		}
		
		if(y < lowerLimit){
			return false;
		}
		
		List<Coord> above = WorldGenPrimitive.getRectSolid(x - 4, y + 4, z - 4, x + 4, y + 4, z + 4);

		for (Coord c : above){
			if(WorldGenPrimitive.getBlock(world, c).getBlock().getMaterial().isOpaque()){
				return false;
			}
		}
		
		List<Coord> below = WorldGenPrimitive.getRectSolid(x - 4, y - 3, z - 4, x + 4, y - 3, z + 4);
		
		int airCount = 0;
		for (Coord c : below){
			if(!WorldGenPrimitive.getBlock(world, c).getBlock().getMaterial().isOpaque()){
				airCount++;
			}
			if(airCount > 8){
				return false;
			}
		}
		
		return true;
	}
	
	public static Coord getNearbyCoord(Random rand, int x, int z, int min, int max){
		
		int distance = min + rand.nextInt(max - min);
		
		double angle = rand.nextDouble() * 2 * Math.PI;
		
		int xOffset = (int) (Math.cos(angle) * distance);
		int zOffset = (int) (Math.sin(angle) * distance);
		
		Coord nearby = new Coord(x + xOffset, 0, z + zOffset);		
		return nearby;
	}
	
	public static Random getRandom(World world, int x, int z){
		long seed = world.getSeed() * x * z;
		Random rand = new Random();
		rand.setSeed(seed);
		return rand;
	}

	@Override
	public List<DungeonNode> getNodes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<IDungeonLevel> getLevels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ITreasureChest> getChests() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Coord> getChestLocations() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Coord> getSpawnerLocations() {
		// TODO Auto-generated method stub
		return null;
	}
}
