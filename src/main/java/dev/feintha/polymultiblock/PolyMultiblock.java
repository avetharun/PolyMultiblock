package dev.feintha.polymultiblock;

import eu.pb4.polymer.core.api.block.PolymerBlock;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerLightingProvider;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;

public abstract class PolyMultiblock extends Block implements PolymerBlock {

    public PolyMultiblock(Settings settings) {
        super(settings);
        this.setDefaultState(overrideDefaultState(getDefaultState()));
    }
    public @Nullable Direction getRotationDirection(BlockPos pos, BlockState state) {
        return Direction.SOUTH;
    };
    public abstract Pattern createPattern();
    protected Pattern _pattern;
    public Pattern getPattern() {return _pattern;}
    public BlockState processMultiblock(ServerWorld world, BlockPos center, BlockState patternState, int x, int y, int z) {return patternState;}
    public boolean isValid(BlockState state, BlockPos pos, WorldView world) {
        var p = getPattern();
        return state.isOf(this) && getPattern().blocks.get(pos.subtract(new BlockPos(state.get(p.x), state.get(p.y), state.get(p.z)))) != null;
    }
    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos centerPos) {
        AtomicBoolean canPlace = new AtomicBoolean(true);

        getPattern().iterateBlocksFrom(centerPos, (p, s) ->{
            if (!world.getBlockState(p).isReplaceable()) {
                canPlace.set(false);
            }
        });

        return super.canPlaceAt(state, world, centerPos) && canPlace.get();
    }
    public boolean isBlockPartOfMultiblock(BlockPos pos, BlockState state) {
        return state.isOf(this);
    }

    public Vec3i getCenter(BlockPos pos, BlockState state) {
        if (!state.isOf(this)) throw new RuntimeException("Block at %s isn't a PolyMultiblock!".formatted(pos));
        var p = getPattern();
        var z = new BlockPos(state.get(p.x), state.get(p.y), state.get(p.z));
        return pos.add(new BlockPos(z.getX(), z.getY(), z.getZ()));
    }
    public boolean isCenter(BlockPos pos, BlockState state) {
        return getCenter(pos, state).equals(pos);
    }
    public boolean isCenter(BlockPos selfPos, ServerWorld world, BlockHitResult result) {
        return selfPos.equals(result.getBlockPos()) && isCenter(result.getBlockPos(), world.getBlockState(result.getBlockPos())) && isCenter(selfPos, world.getBlockState(selfPos));
    }

    /// Places the pattern as instances of the pattern's actual BlockStates
    public boolean placePatternAt(ServerWorld world, BlockPos centerPos, @Nullable ItemPlacementContext unused, @Nullable Direction facing) {
        this.getPattern().copyFacing(facing == null ? Direction.SOUTH : facing).iterateBlocksFrom(centerPos, world::setBlockState);
        return true;
    }
    /// Places the pattern as instances of the current PolyMultiblock's internal state
    public boolean placeThisAt(ServerWorld world, BlockPos centerPos, @Nullable ItemPlacementContext context, @Nullable Direction facing) {
        var p = getPattern();
        this.getPattern().copyRotated(facing == null ? 0 : facing.getHorizontalQuarterTurns()).iterateBlocks((s, _s)->{
//            System.out.println(getDefaultState());
            var state = getDefaultState()
                .with(p.x, -s.getX())
                .with(p.y, -s.getY())
                .with(p.z, -s.getZ())
                .with(Properties.HORIZONTAL_FACING, facing)
            ;
            world.setBlockState(centerPos.add(s),state);
            processMultiblock(world, centerPos.add(s.getX(), s.getY(), s.getZ()), state, s.getX(), s.getY(), s.getZ());
        });
        return true;
    }

    public ActionResult interactMultiblock(ItemStack stack, BlockState state, ServerWorld world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit){return ActionResult.PASS;}
    public ActionResult interactCenter(ItemStack stack, BlockState state, ServerWorld world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit){return ActionResult.PASS;}
    @Override
    protected final ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {

        Direction d = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : null;
        int r = d != null ? d.getHorizontalQuarterTurns() : 0;
        final ActionResult[] result = {ActionResult.PASS};
        var ps = new BlockPos(getCenter(pos, state));
        var pv = ps.toCenterPos();
        ((ServerWorld)world).spawnParticles(ParticleTypes.BUBBLE_POP, pv.x, pv.y, pv.z, 1, 0, 0,0,0);
        ((ServerWorld)world).spawnParticles(ParticleTypes.FLASH, pv.x, pv.y, pv.z, 1, 0, 0,0,0);
        getPattern().iterateBlocksFrom(r,ps, (_p,unused)->{
            var p = _p;
//            System.out.println();
            var s = world.getBlockState(_p);
//            System.out.printf("%d %s, %s, %s%n",r, _p, p, s);
            var r1 = interactMultiblock(player.getStackInHand(player.getActiveHand()), s, (ServerWorld) world, p, player, player.getActiveHand(), hit);
            result[0] = r1 != ActionResult.PASS ? r1 : result[0];

        });
        return result[0];
    }
    BlockState overrideDefaultState(BlockState state) {
        return state;
    }
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        Direction d = state.contains(Properties.HORIZONTAL_FACING) ? state.get(Properties.HORIZONTAL_FACING) : null;
        int r = d != null ? d.getHorizontalQuarterTurns() : 0;
        getPattern().iterateBlocksFrom(r,new BlockPos(getCenter(pos, state)), (p,s)->{
            world.breakBlock(p, false, player);
//            System.out.println(p);
        });
        return super.onBreak(world, pos, state, player);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction facing = null;
        if (this.getDefaultState().contains(Properties.HORIZONTAL_FACING)) {
            facing = ctx.getHorizontalPlayerFacing();
        }
        System.out.printf("Facing (PS) %s%n",facing);
        if (facing != null) {
            System.out.printf("Turns (PS) %d%n", facing.getHorizontalQuarterTurns());
        }
        placeThisAt((ServerWorld) ctx.getWorld(), ctx.getBlockPos(), ctx, facing);
        return super.getPlacementState(ctx);
    }

    @Override @MustBeInvokedByOverriders
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        _pattern = createPattern();
        if (_pattern.centerPos == null) throw new RuntimeException("Pattern Center cannot be null!");
        super.appendProperties(getPattern().appendProperties(builder));
    }
    @Override
    public BlockState getPolymerBlockState(BlockState blockState, PacketContext packetContext) {
        return Blocks.STONE.getDefaultState();
    }

    public static final class SignedIntProperty extends Property<Integer> {
        private final IntImmutableList values;
        private final int min;
        private final int max;

        private SignedIntProperty(String name, int min, int max) {
            super(name, Integer.class);
            if (max <= min) {
                throw new IllegalArgumentException("Max value of " + name + " must be greater than min (" + min + ")");
            } else {
                this.min = min;
                this.max = max;
                this.values = IntImmutableList.toList(IntStream.range(min, max + 1));
            }
        }

        public List<Integer> getValues() {
            return this.values;
        }

        public boolean equals(Object object) {
            if (this == object) {
                return true;
            } else {
                if (object instanceof SignedIntProperty) {
                    SignedIntProperty intProperty = (SignedIntProperty) object;
                    if (super.equals(object)) {
                        return this.values.equals(intProperty.values);
                    }
                }

                return false;
            }
        }

        public int computeHashCode() {
            return 31 * super.computeHashCode() + this.values.hashCode();
        }

        public static SignedIntProperty of(String name, int min, int max) {
            return new SignedIntProperty(name, min, max);
        }

        public Optional<Integer> parse(String name) {
            try {
                int i = Integer.parseInt(name);
                return Optional.of(i);
            } catch (NumberFormatException var3) {
                return Optional.empty();
            }
        }

        public String name(Integer integer) {
            return integer.toString();
        }

        public int ordinal(Integer integer) {
            return integer <= this.max ? integer - this.min : -1;
        }
    }


    /// Base pattern class. Patterns are represented as getRotatedDirection->NORTH
    public static class Pattern {
        SignedIntProperty x = null;
        SignedIntProperty y = null;
        SignedIntProperty z = null;
        public SignedIntProperty xProp() {return x;}
        public SignedIntProperty yProp() {return y;}
        public SignedIntProperty zProp() {return z;}
        StateManager.Builder<Block, BlockState> appendProperties(StateManager.Builder<Block, BlockState> builder) {
            x = SignedIntProperty.of("px", -width, width);
            y = SignedIntProperty.of("py", -height, height);
            z = SignedIntProperty.of("pz", -length, length);
            builder.add(x,y,z);
            return builder;
        }
        public static Pattern ofBlock(BlockState state) {
            return create().withBlock(BlockPos.ORIGIN, state);
        }
        public void iterateBlocksFrom(BlockPos center, BiConsumer<BlockPos, BlockState> iterator) {
            copy().offset(center).iterateBlocks(iterator);
        }
        /// rotate first then offset
        public void iterateBlocksFrom(int quarter_steps, BlockPos center, BiConsumer<BlockPos, BlockState> iterator) {
            copyRotated(quarter_steps).offset(center).iterateBlocks(iterator);
        }
        /// offset first then rotate
        public void iterateBlocksFrom(BlockPos center, int quarter_steps, BiConsumer<BlockPos, BlockState> iterator) {
            copy().offset(center).rotated(quarter_steps).iterateBlocks(iterator);
        }
        public Pattern offset(Vec3i offset) {
            return this.offset(offset.getX(), offset.getY(), offset.getZ());
        }
        public Pattern offset(int x, int y, int z) {
            HashMap<BlockPos, BlockState> blocks1 = new HashMap<>();
            iterateBlocks((p, s) -> {
                blocks1.put(p.add(x,y,z), s);
            });
            return setBlocks(blocks1);
        }

        public void iterateBlocks(BiConsumer<BlockPos, BlockState> iterator) {
            blocks.forEach(iterator);
        }
        public void iterateBlocks(int quarter_steps, BiConsumer<BlockPos, BlockState> iterator) {
            copyRotated(quarter_steps).iterateBlocks(iterator);
        }
        public Pattern rotated(int quarter_steps) {
//            System.out.println(quarter_steps);
            var blocks1 = new HashMap<BlockPos, BlockState>();
            blocks.forEach((blockPos, blockState) -> {
                for (int i = 0; i < quarter_steps; i++) {
                    blockPos = blockPos.rotate(BlockRotation.CLOCKWISE_90);
                }
                blocks1.put(blockPos, blockState);
            });
            BlockPos size = new BlockPos(width, height, length);

            for (int i = 0; i < quarter_steps; i++) {
                size = size.rotate(BlockRotation.CLOCKWISE_90);
            }
            return this.setBlocks(blocks1)
                    .withWidth(size.getX())
                    .withHeight(size.getY())
                    .withLength(size.getZ())
                    ;
        }
        public Pattern rotated(Direction direction) {
            return rotated(direction.getHorizontalQuarterTurns());
        }
        /// Rotates from the current facing direction (Assumed North by default)
        public Pattern copyRotated(int quarter_steps) {
            return copy().rotated(quarter_steps);
        }
        public Pattern copyRotated(Direction direction) {
            return copyRotated(direction.getHorizontalQuarterTurns());
        }
        public Pattern copyFacing(Direction direction) {
            return copyRotated(direction.getHorizontalQuarterTurns());
        }
        HashMap<BlockPos, BlockState> blocks = new HashMap<>();
        public static Pattern create() {return new Pattern();}
        int length=1, width=1, height=1;
        BlockPos centerPos = BlockPos.ORIGIN;
        public Pattern withCenter(BlockPos pos) {this.centerPos = pos; return this;}
        public Pattern withLength(int length) {this.length = length; return this;}
        public Pattern withWidth(int width) {this.width = width; return this;}
        public Pattern withHeight(int height) {this.height = height; return this;}
        public Pattern withBlock(BlockPos pos, BlockState state) {
            blocks.put(pos, state);
            return this;
        }
        public Pattern fillBlocks(BlockState state, BlockPos start, BlockPos end) {
            for (BlockPos blockPos : BlockPos.iterate(start, end)) {
                this.blocks.put(blockPos, state);
            }
            return this;
        }
        public Pattern withBlocks(List<Map.Entry<BlockPos, BlockState>> blocks) {
            for (Map.Entry<BlockPos, BlockState> block : blocks) {
                this.blocks.put(block.getKey(), block.getValue());
            }
            return this;
        }
        public Pattern withBlocks(Map<BlockPos, BlockState> blocks1) {
            this.blocks.putAll(blocks1);
            return this;
        }
        public Pattern setBlocks(Map<BlockPos, BlockState> blocks1) {
            this.blocks = new HashMap<>(blocks1);
            return this;
        }
        public Pattern copy() {
            return new Pattern()
                    .setBlocks(blocks)
                    .withWidth(width)
                    .withHeight(height)
                    .withLength(length)
                    ;
        }
    }
}
