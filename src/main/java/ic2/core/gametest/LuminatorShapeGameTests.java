package ic2.core.gametest;

import ic2.core.block.tileentity.Ic2TileEntityBlock;
import ic2.core.ref.Ic2Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ic2")
@PrefixGameTestTemplate(false)
public final class LuminatorShapeGameTests {
  @GameTest(template = "gametest/empty3x3x3")
  public static void fullMountingFaceCanBeTargetedInEveryOrientation(GameTestHelper helper) {
    var relative = new BlockPos(1, 1, 1);
    var pos = helper.absolutePos(relative);
    var level = helper.getLevel();
    for (var side : Direction.values()) helper.setBlock(relative.relative(side), Blocks.STONE);
    for (var facing : Direction.values()) {
      for (boolean active : new boolean[] {false, true}) {
        var state =
            Ic2Blocks.LUMINATOR_FLAT
                .defaultBlockState()
                .setValue(Ic2TileEntityBlock.anyFacingProperty, facing)
                .setValue(Ic2TileEntityBlock.ACTIVE, active);
        helper.setBlock(relative, state);
        var shape = state.getShape(level, pos);
        var bounds = shape.bounds();
        helper.assertTrue(
            bounds.minX >= 0
                && bounds.minY >= 0
                && bounds.minZ >= 0
                && bounds.maxX <= 1
                && bounds.maxY <= 1
                && bounds.maxZ <= 1,
            "lamp outline stays inside its block: " + facing);
        for (var axis : Direction.Axis.values())
          Ic2GameTestAssertions.assertNear(
              helper,
              axis.choose(bounds.getXsize(), bounds.getYsize(), bounds.getZsize()),
              axis == facing.getAxis() ? 1.0 / 16 : 1.0,
              "lamp is one texel thick and covers the complete mounting face: " + facing);
        helper.assertTrue(
            state.getCollisionShape(level, pos).isEmpty(),
            "the existing non-solid lamp collision remains unchanged");
        for (double u : new double[] {.05, .95}) {
          for (double v : new double[] {.05, .95}) {
            var target =
                Vec3.atLowerCornerOf(pos)
                    .add(
                        switch (facing.getAxis()) {
                          case X -> new Vec3(.5, u, v);
                          case Y -> new Vec3(u, .5, v);
                          case Z -> new Vec3(u, v, .5);
                        });
            var normal = Vec3.atLowerCornerOf(facing.getNormal());
            var hit =
                shape.clip(target.add(normal.scale(1.5)), target.add(normal.scale(-1.5)), pos);
            helper.assertTrue(
                hit != null && hit.getDirection() == facing,
                "every visible corner of the lamp can be targeted: " + facing);
            double face =
                facing.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? 1.0 / 16
                    : 15.0 / 16;
            Ic2GameTestAssertions.assertNear(
                helper,
                hit.getLocation().subtract(Vec3.atLowerCornerOf(pos)).get(facing.getAxis()),
                face,
                "ray hits the visible lamp face rather than the supporting block");
          }
        }
      }
    }
    helper.succeed();
  }
}
