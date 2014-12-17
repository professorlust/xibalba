package me.dannytatom.x2600BC.generators;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import me.dannytatom.x2600BC.Cell;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CaveGenerator {
  public Cell[][] map;
  boolean[][] geometry;
  int width;
  int height;
  TextureAtlas atlas;

  /**
   * Generates a cave.
   *
   * @param atlas  Texture atlas to use when placing tiles
   * @param width  How wide the map should be in cells
   * @param height How long the map should be in cells
   */
  public CaveGenerator(TextureAtlas atlas, int width, int height) {
    this.atlas = atlas;
    this.width = width;
    this.height = height;

    geometry = new boolean[width][height];

    for (boolean[] row : geometry) {
      Arrays.fill(row, true);
    }

    initialize();

    for (int i = 0; i < 4; i++) {
      shapeGeometry();
    }

    for (int i = 0; i < 3; i++) {
      shapeGeometryAgain();
    }

    emptyGeometryEdges();
    createMap();
  }

  // Start off with all ground, then create emptiness randomly
  // (43% chance)
  private void initialize() {
    for (int x = 0; x < geometry.length; x++) {
      for (int y = 0; y < geometry[x].length; y++) {
        if (MathUtils.random() < 0.43f) {
          geometry[x][y] = false;
        }
      }
    }
  }

  // A tile becomes empty if it's already empty and 4 or more of its nine neighbours are empty,
  // or if it is not empty and 5 or more neighbours are or the tile is in open space
  private void shapeGeometry() {
    boolean[][] newGeo = new boolean[width][height];

    for (int x = 0; x < geometry.length; x++) {
      for (int y = 0; y < geometry[x].length; y++) {
        int neighbours1 = emptyNeighbours(1, x, y);
        int neighbours2 = emptyNeighbours(2, x, y);

        if (!geometry[x][y]) {
          newGeo[x][y] = neighbours1 < 4;
        } else {
          newGeo[x][y] = !(neighbours1 >= 5 || neighbours2 <= 2);
        }
      }
    }

    geometry = newGeo;
  }

  // Same as #shapeGeometry, except we don't care about open space
  private void shapeGeometryAgain() {
    boolean[][] newGeo = new boolean[width][height];

    for (int x = 0; x < geometry.length; x++) {
      for (int y = 0; y < geometry[x].length; y++) {
        int neighbours = emptyNeighbours(1, x, y);

        if (!geometry[x][y]) {
          newGeo[x][y] = neighbours < 4;
        } else {
          newGeo[x][y] = neighbours < 5;
        }
      }
    }

    geometry = newGeo;
  }

  // Edge of the geometry should always be inaccessible
  private void emptyGeometryEdges() {
    for (int x = 0; x < geometry.length; x++) {
      for (int y = 0; y < geometry[x].length; y++) {
        if (x == 0 || y == 0) {
          geometry[x][y] = false;
        }

        if (x == geometry.length - 1 || y == geometry[x].length - 1) {
          geometry[x][y] = false;
        }
      }
    }
  }

  // I'M GONNA PAINT THE TOWN RED
  private void createMap() {
    map = new Cell[width][height];

    for (int x = 0; x < geometry.length; x++) {
      for (int y = 0; y < geometry[x].length; y++) {
        if (geometry[x][y]) {
          if (MathUtils.random() <= .8) {
            map[x][y] = new Cell(atlas.createSprite("caveFloor-"
                + MathUtils.random(10, 16)), false);
          } else {
            map[x][y] = new Cell(atlas.createSprite("caveFloor-"
                + MathUtils.random(1, 9)), false);
          }
        } else {
          int neighbours = groundNeighbours(x, y);

          if (neighbours > 0) {
            map[x][y] = new Cell(atlas.createSprite("caveWallBack-1"), true);
          } else {
            map[x][y] = new Cell(atlas.createSprite("nothing"), true);
          }
        }
      }
    }
  }

  /**
   * Returns number of empty neighbors around cell within
   * the amount of space given.
   *
   * @param amount How many neighboring cells to check
   * @param cellX  x of cell to search from
   * @param cellY  y of cell to search from
   * @return number of empty neighbors
   */
  private int emptyNeighbours(int amount, int cellX, int cellY) {
    int count = 0;

    for (int i = -amount; i < amount + 1; i++) {
      for (int j = -amount; j < amount + 1; j++) {
        int nx = cellX + i;
        int ny = cellY + j;

        if (i != 0 || j != 0) {
          if (nx < 0 || ny < 0 || nx >= geometry.length || ny >= geometry[0].length) {
            count += 1;
          } else if (!geometry[nx][ny]) {
            count += 1;
          }
        }
      }
    }

    return count;
  }

  /**
   * Get number of ground cells around a cell.
   *
   * @param cellX x of cell to search from
   * @param cellY y of cell to search from
   * @return number of ground neighbors
   */
  private int groundNeighbours(int cellX, int cellY) {
    int count = 0;

    for (int i = -1; i < 2; i++) {
      for (int j = -1; j < 2; j++) {
        int nx = cellX + i;
        int ny = cellY + j;

        if (i != 0 || j != 0) {
          if (nx >= 0 && ny >= 0 && nx < geometry.length && ny < geometry[0].length) {
            if (geometry[nx][ny]) {
              count += 1;
            }
          }
        }
      }
    }

    return count;
  }

  /**
   * Find a good starting area for player.
   *
   * @return x & y position of cell
   */
  public Map<String, Integer> findPlayerStart() {
    Map<String, Integer> space = new HashMap<>();

    search:
    for (int x = 0; x < map.length; x++) {
      for (int y = 0; y < map[x].length; y++) {
        if (!map[x][y].blocksMovement) {
          space.put("x", x);
          space.put("y", y);

          break search;
        }
      }
    }

    return space;
  }

  /**
   * Find a good spot for a mob.
   *
   * @return x & y position of cell
   */
  public Map<String, Integer> findMobStart() {
    Map<String, Integer> space = new HashMap<>();
    int cellX;
    int cellY;

    do {
      cellX = MathUtils.random(0, map.length - 1);
      cellY = MathUtils.random(0, map[cellX].length - 1);
    } while (map[cellX][cellY].blocksMovement);

    space.put("x", cellX);
    space.put("y", cellY);

    return space;
  }
}
