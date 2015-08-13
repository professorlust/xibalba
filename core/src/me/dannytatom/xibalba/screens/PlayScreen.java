package me.dannytatom.xibalba.screens;

import com.badlogic.ashley.core.Engine;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import me.dannytatom.xibalba.*;
import me.dannytatom.xibalba.components.AttributesComponent;
import me.dannytatom.xibalba.map.CaveGenerator;
import me.dannytatom.xibalba.map.Map;
import me.dannytatom.xibalba.systems.AttributesSystem;
import me.dannytatom.xibalba.systems.EffectSystem;
import me.dannytatom.xibalba.systems.actions.MeleeSystem;
import me.dannytatom.xibalba.systems.actions.MovementSystem;
import me.dannytatom.xibalba.systems.actions.RangeSystem;
import me.dannytatom.xibalba.systems.ai.BrainSystem;
import me.dannytatom.xibalba.systems.ai.TargetSystem;
import me.dannytatom.xibalba.systems.ai.WanderSystem;
import me.dannytatom.xibalba.utils.CombatHelpers;
import me.dannytatom.xibalba.utils.EntityHelpers;
import me.dannytatom.xibalba.utils.InventoryHelpers;
import me.dannytatom.xibalba.utils.SkillHelpers;

class PlayScreen implements Screen {
  private final Main main;
  private final WorldRenderer worldRenderer;
  private final InterfaceRenderer interfaceRenderer;
  private final SpriteBatch batch;
  private final Engine engine;
  private Map map;

  /**
   * Play Screen.
   *
   * @param main Instance of Main class
   */
  public PlayScreen(Main main) {
    this.main = main;

    engine = new Engine();
    batch = new SpriteBatch();

    // Setup helpers
    main.entityHelpers = new EntityHelpers(main, engine);
    main.inventoryHelpers = new InventoryHelpers(main);
    main.skillHelpers = new SkillHelpers(main);
    main.combatHelpers = new CombatHelpers(main, engine);

    // Setup action log
    main.log = new ActionLog();

    // Generate cave & initialize map
    CaveGenerator cave = new CaveGenerator(
        main.assets.get("sprites/cave.atlas"),
        MathUtils.random(50, 80), MathUtils.random(30, 60)
    );
    map = new Map(engine, main.entityHelpers, cave.map);

    // Add player entity
    main.entityHelpers.spawnPlayer(main.player, map.findPlayerStart());
    engine.addEntity(main.player);

    // Spawn some spider monkeys
    for (int i = 0; i < 5; i++) {
      engine.addEntity(main.entityHelpers.spawnEnemy("spiderMonkey", map.getRandomOpenPosition()));
    }

    for (int i = 0; i < 2; i++) {
      engine.addEntity(main.entityHelpers.spawnItem("chippedFlint", map.getRandomOpenPosition()));
    }

    for (int i = 0; i < 3; i++) {
      engine.addEntity(main.entityHelpers.spawnItem("bomb", map.getRandomOpenPosition()));
    }

    // Setup engine (they're run in order added)
    engine.addSystem(new AttributesSystem());
    engine.addSystem(new BrainSystem(main.entityHelpers, map));
    engine.addSystem(new WanderSystem(map));
    engine.addSystem(new TargetSystem(map));
    engine.addSystem(new MeleeSystem(main.combatHelpers));
    engine.addSystem(new RangeSystem(main, engine, map));
    engine.addSystem(new EffectSystem(engine, map, main.combatHelpers));
    engine.addSystem(new MovementSystem(map));

    // Setup renderers
    worldRenderer = new WorldRenderer(main, engine, batch, map);
    interfaceRenderer = new InterfaceRenderer(main);
  }

  @Override
  public void render(float delta) {
    if (main.executeTurn) {
      engine.update(delta);

      main.executeTurn = false;
    }

    worldRenderer.render(delta);
    interfaceRenderer.render(delta);

    if (main.player.getComponent(AttributesComponent.class).health <= 0) {
      main.setScreen(new MainMenuScreen(main));
    }
  }

  @Override
  public void resize(int width, int height) {
    interfaceRenderer.resize(width, height);
  }

  @Override
  public void show() {
    Gdx.input.setInputProcessor(new PlayerInput(main, map));
  }

  @Override
  public void hide() {

  }

  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }

  @Override
  public void dispose() {
    batch.dispose();
    interfaceRenderer.dispose();
  }
}
