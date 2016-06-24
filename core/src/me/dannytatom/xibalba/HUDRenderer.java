package me.dannytatom.xibalba;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import me.dannytatom.xibalba.components.AttributesComponent;
import me.dannytatom.xibalba.components.EnemyComponent;
import me.dannytatom.xibalba.components.ItemComponent;
import me.dannytatom.xibalba.map.Cell;

public class HudRenderer {
  private final Main main;

  private final Stage stage;

  private final VerticalGroup actionLog;
  private final VerticalGroup areaDetails;
  private final Label lookDetails;

  /**
   * Renders the HUD.
   *
   * @param main  Instance of Main class
   * @param batch The sprite batch to use (set in PlayScreen)
   */
  public HudRenderer(Main main, SpriteBatch batch) {
    this.main = main;

    Viewport viewport = new FitViewport(960, 540, new OrthographicCamera());
    stage = new Stage(viewport, batch);

    Table topTable = new Table();
    topTable.top().left();
    topTable.setFillParent(true);

    actionLog = new VerticalGroup().left();
    topTable.add(actionLog).pad(10, 10, 10, 10).width(Gdx.graphics.getWidth() / 4 * 3 - 20).top();
    areaDetails = new VerticalGroup().right();
    topTable.add(areaDetails).pad(10, 10, 10, 10).width(Gdx.graphics.getWidth() / 4 - 20).top();

    stage.addActor(topTable);

    Table bottomTable = new Table();
    bottomTable.bottom().left();
    bottomTable.setFillParent(true);

    lookDetails = new Label(null, main.skin);
    bottomTable.add(lookDetails).pad(10, 10, 10, 10);

    stage.addActor(bottomTable);
  }

  /**
   * Do some rendering.
   *
   * @param delta Elapsed time
   */
  public void render(float delta) {
    renderActionLog();
    renderAreaDetails();
    renderLookDetails();

    stage.act(delta);
    stage.draw();
  }

  private void renderActionLog() {
    actionLog.clear();

    for (int i = 0; i < main.log.things.size(); i++) {
      Label label = new Label(main.log.things.get(i), main.skin);
      label.setColor(1f, 1f, 1f, (i == 0 ? 1f : 1f / (i + 1)));
      label.setWrap(true);

      actionLog.addActor(label);
    }
  }

  private void renderAreaDetails() {
    areaDetails.clear();

    // Player area

    AttributesComponent playerAttributes = main.player.getComponent(AttributesComponent.class);

    areaDetails.addActor(new Label(playerAttributes.name, main.skin));

    String playerHealthColor;

    if (playerAttributes.health / playerAttributes.maxHealth <= 0.5f) {
      playerHealthColor = "[RED]";
    } else {
      playerHealthColor = "[WHITE]";
    }

    areaDetails.addActor(
        new Label(
            playerHealthColor + playerAttributes.health
                + "[LIGHT_GRAY]/" + playerAttributes.maxHealth, main.skin
        )
    );

    areaDetails.addActor(new Label("[DARK_GRAY]-----[]", main.skin));

    // Enemies visible in area

    ImmutableArray<Entity> enemies =
        main.engine.getEntitiesFor(Family.all(EnemyComponent.class).get());

    for (Entity enemy : enemies) {
      if (main.entityHelpers.isVisibleToPlayer(enemy, main.getMap())) {
        AttributesComponent enemyAttributes = enemy.getComponent(AttributesComponent.class);

        areaDetails.addActor(new Label(enemyAttributes.name, main.skin));

        String enemyHealthColor;

        if (enemyAttributes.health / enemyAttributes.maxHealth <= 0.5f) {
          enemyHealthColor = "[RED]";
        } else {
          enemyHealthColor = "[WHITE]";
        }

        areaDetails.addActor(
            new Label(
                enemyHealthColor + enemyAttributes.health
                    + "[LIGHT_GRAY]/" + enemyAttributes.maxHealth, main.skin
            )
        );
      }
    }
  }

  private void renderLookDetails() {
    lookDetails.clear();

    if (main.state == Main.State.SEARCHING && main.getMap().target != null) {
      Cell cell = main.getMap().getCell(main.getMap().target);

      if (cell.forgotten) {
        lookDetails.setText("You remember seeing " + cell.description + ".");
      } else {
        String description = "You see " + cell.description + ".";

        Entity itemAtLocation = main.getMap().getItemAt(main.getMap().target);

        if (itemAtLocation != null) {
          String itemDescription = itemAtLocation.getComponent(ItemComponent.class).name;
          description += " A " + itemDescription + " is there.";
        }

        Entity enemyAtLocation = main.getMap().getEnemyAt(main.getMap().target);

        if (enemyAtLocation != null) {
          String enemyDescription = enemyAtLocation.getComponent(AttributesComponent.class).name;
          description += " A " + enemyDescription + " is there.";
        }

        lookDetails.setText(description);
      }
    }
  }
}
