package greymerk.roguelike.dungeon;

import java.util.List;

import greymerk.roguelike.dungeon.settings.LevelSettings;
import greymerk.roguelike.worldgen.Coord;

public interface IDungeonLevel {

	public void generate(Coord start, DungeonNode oldEnd);
	
	public DungeonNode getEnd();
	
	public LevelSettings getSettings();
	
	public List<DungeonNode> getNodes();
	
	public List<DungeonTunnel> getTunnels();

	public boolean inRange(Coord pos);
	
	boolean hasNearbyNode(Coord pos);
	
}
