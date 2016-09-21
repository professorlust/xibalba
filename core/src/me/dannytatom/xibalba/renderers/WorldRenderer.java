package me.dannytatom.xibalba.renderers;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import me.dannytatom.xibalba.Main;
import me.dannytatom.xibalba.components.DecorationComponent;
import me.dannytatom.xibalba.components.EnemyComponent;
import me.dannytatom.xibalba.components.EntranceComponent;
import me.dannytatom.xibalba.components.ExitComponent;
import me.dannytatom.xibalba.components.ItemComponent;
import me.dannytatom.xibalba.components.PlayerComponent;
import me.dannytatom.xibalba.components.PositionComponent;
import me.dannytatom.xibalba.utils.ComponentMappers;
import me.dannytatom.xibalba.world.Map;
import me.dannytatom.xibalba.world.MapCell;
import me.dannytatom.xibalba.world.WorldManager;
import org.apache.commons.lang3.ArrayUtils;
import org.xguzm.pathfinding.grid.GridCell;

public class WorldRenderer {
  private final SpriteBatch batch;
  private final Viewport viewport;
  private final OrthographicCamera worldCamera;

  private final PlayerComponent playerDetails;

  // These get reused a ton
  private final Sprite target;
  private final Sprite targetPath;
  private final Sprite shadow;

  /**
   * Setup world renderer.
   *
   * @param worldCamera Instance of camera
   * @param batch       Instance of sprite batch
   */
  public WorldRenderer(OrthographicCamera worldCamera, SpriteBatch batch) {
    this.worldCamera = worldCamera;
    this.batch = batch;

    viewport = new FitViewport(960, 540, worldCamera);

    playerDetails = ComponentMappers.player.get(WorldManager.player);

    target = Main.asciiAtlas.createSprite("0915");
    targetPath = Main.asciiAtlas.createSprite("0915");
    targetPath.setColor(Colors.get("YELLOW"));
    shadow = Main.asciiAtlas.createSprite("1113");
    shadow.setColor(Colors.get("caveBackground"));
  }

  /**
   * Render shit.
   */
  public void render(float delta) {
    // Get playerDetails position
    PositionComponent playerPosition = ComponentMappers.position.get(WorldManager.player);

    // Handle screen shake
    if (Main.cameraShake.time > 0) {
      Main.cameraShake.update(delta, worldCamera, playerPosition.pos);
    } else {
      // Set worldCamera to follow player
      worldCamera.position.set(
          playerPosition.pos.x * Main.SPRITE_WIDTH,
          playerPosition.pos.y * Main.SPRITE_HEIGHT, 0
      );
    }

    worldCamera.update();

    batch.setProjectionMatrix(worldCamera.combined);
    batch.begin();

    Map map = WorldManager.world.getCurrentMap();

    for (int x = 0; x < map.width - 1; x++) {
      for (int y = 0; y < map.height - 1; y++) {
        MapCell cell = WorldManager.mapHelpers.getCell(x, y);

        if (map.lightMap[x][y] > 0) {
          cell.hidden = false;
        }

        if (!cell.hidden) {
          cell.forgotten = map.lightMap[x][y] <= 0;

          if (WorldManager.entityHelpers.getEntitiesAt(new Vector2(x, y)).size() == 0) {
            cell.sprite.draw(batch);
          }
        }
      }
    }

    if (playerDetails.path != null && playerDetails.target != null) {
      for (int i = 0; i < playerDetails.path.size(); i++) {
        boolean isLast = i == (playerDetails.path.size() - 1);

        GridCell cell = playerDetails.path.get(i);

        batch.draw(
            isLast ? target : targetPath,
            cell.x * Main.SPRITE_WIDTH, cell.y * Main.SPRITE_HEIGHT
        );
      }
    }

    renderStairs();
    renderDecorations();
    renderItems();
    renderEnemies();
    renderPlayer();
    renderShadows();

    batch.end();
  }

  private void renderStairs() {
    ImmutableArray<Entity> entrances =
        WorldManager.engine.getEntitiesFor(Family.all(EntranceComponent.class).get());
    ImmutableArray<Entity> exits =
        WorldManager.engine.getEntitiesFor(Family.all(ExitComponent.class).get());

    Object[] entities = ArrayUtils.addAll(entrances.toArray(), exits.toArray());

    for (Object e : entities) {
      Entity entity = (Entity) e;

      if (WorldManager.entityHelpers.isVisible(entity)) {
        ComponentMappers.visual.get(entity).sprite.draw(batch);
      }
    }
  }

  private void renderDecorations() {
    ImmutableArray<Entity> entities =
        WorldManager.engine.getEntitiesFor(Family.all(DecorationComponent.class).get());

    for (Entity entity : entities) {
      if (WorldManager.entityHelpers.isVisible(entity)) {
        ComponentMappers.visual.get(entity).sprite.draw(batch);
      }
    }
  }

  private void renderItems() {
    ImmutableArray<Entity> entities =
        WorldManager.engine.getEntitiesFor(
            Family.all(ItemComponent.class).get()
        );

    for (Entity entity : entities) {
      if (WorldManager.entityHelpers.isVisible(entity)) {
        ComponentMappers.visual.get(entity).sprite.draw(batch);
      }
    }
  }

  private void renderEnemies() {
    ImmutableArray<Entity> entities =
        WorldManager.engine.getEntitiesFor(Family.all(EnemyComponent.class).get());

    for (Entity entity : entities) {
      if (WorldManager.entityHelpers.isVisible(entity)) {
        ComponentMappers.visual.get(entity).sprite.draw(batch);
      }
    }
  }

  private void renderPlayer() {
    ImmutableArray<Entity> entities =
        WorldManager.engine.getEntitiesFor(Family.all(PlayerComponent.class).get());

    ComponentMappers.visual.get(entities.first()).sprite.draw(batch);
  }

  private void renderShadows() {
    Map map = WorldManager.world.getCurrentMap();

    for (int x = 0; x < map.lightMap.length; x++) {
      for (int y = 0; y < map.lightMap[0].length; y++) {
        float alpha = map.lightMap[x][y] <= 0.15f ? 0.15f : map.lightMap[x][y];

        shadow.setAlpha(-alpha);
        shadow.setPosition(x * Main.SPRITE_WIDTH, y * Main.SPRITE_HEIGHT);

        shadow.draw(batch);
      }
    }
  }

  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }
}
