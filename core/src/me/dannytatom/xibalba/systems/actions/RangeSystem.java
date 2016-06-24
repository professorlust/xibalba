package me.dannytatom.xibalba.systems.actions;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;
import me.dannytatom.xibalba.Main;
import me.dannytatom.xibalba.components.AttributesComponent;
import me.dannytatom.xibalba.components.ItemComponent;
import me.dannytatom.xibalba.components.actions.RangeComponent;
import me.dannytatom.xibalba.systems.ActionSystem;
import me.dannytatom.xibalba.utils.ComponentMappers;

import java.util.Objects;

public class RangeSystem extends ActionSystem {
  private final Main main;

  /**
   * Handles range combat.
   *
   * @param main Instance of the main class, needed for helpers*
   */
  public RangeSystem(Main main) {
    super(Family.all(RangeComponent.class).get());

    this.main = main;
  }

  @Override
  protected void processEntity(Entity entity, float deltaTime) {
    RangeComponent range = ComponentMappers.range.get(entity);
    AttributesComponent attributes = ComponentMappers.attributes.get(entity);

    if (range.item != null) {
      Entity enemy = main.getMap().getEnemyAt(range.target);

      if (enemy != null) {
        main.combatHelpers.range(entity, enemy, range.item, range.skill);
      }

      if (Objects.equals(range.skill, "throwing")) {
        range.item.getComponent(ItemComponent.class).throwing = false;
        main.inventoryHelpers.dropItem(entity, range.item, range.target);
      } else {
        main.inventoryHelpers.removeItem(entity, range.item);
      }

      attributes.energy -= RangeComponent.COST;
    }

    entity.remove(RangeComponent.class);
  }
}
