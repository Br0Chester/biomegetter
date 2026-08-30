package com.idk.biomegetter.fluid;

import com.idk.biomegetter.BiomeGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

/**
 * Базовая реализация физической жидкости — Фаза 1: течёт и растекается ТОЧНО как вода
 * (getSpread/getDropOff/getSlopeFindDistance переопределять не нужно — берём их из
 * FlowingFluid по умолчанию, они уже реализуют "нормальную" гравитацию). Fазе 2 (gravity_mode
 * "zero"/"anti") понадобится переопределить {@link #getFlow} — оставлено TODO ниже.
 * <p>
 * Одна пара классов ({@link Source}/{@link Flowing}) обслуживает ВСЕ зарегистрированные
 * жидкости — конкретную жидкость отличает {@link #configId}, поведенческие числа читаются из
 * {@link ModFluidConfigLoader#get(Identifier)} заново на каждый вызов.
 */
public abstract class ConfigurableFluid extends FlowingFluid {

    protected final Identifier configId;
    private final Supplier<? extends Fluid> stillSupplier;
    private final Supplier<? extends Fluid> flowingSupplier;
    private final Supplier<? extends ItemLike> bucketSupplier;
    private final Supplier<? extends net.minecraft.world.level.block.Block> blockSupplier;

    protected ConfigurableFluid(
            Identifier configId,
            Supplier<? extends Fluid> stillSupplier,
            Supplier<? extends Fluid> flowingSupplier,
            Supplier<? extends ItemLike> bucketSupplier,
            Supplier<? extends net.minecraft.world.level.block.Block> blockSupplier
    ) {
        this.configId = configId;
        this.stillSupplier = stillSupplier;
        this.flowingSupplier = flowingSupplier;
        this.bucketSupplier = bucketSupplier;
        this.blockSupplier = blockSupplier;
    }

    private FluidConfig config() {
        FluidConfig cfg = ModFluidConfigLoader.get(configId);
        if (cfg == null) {
            BiomeGetter.LOGGER.warn("ConfigurableFluid {} has no fluid_config json loaded — using fallback water-like defaults", configId);
        }
        return cfg;
    }

    // ---- Фаза 2: гравитация (gravity_mode) ----

    /**
     * Единая точка входа физики растекания — читает gravity_mode ИЗ КОНФИГА заново на каждый
     * тик (та же идея, что и везде в проекте: одна Java-реализация, поведение переключается
     * JSON-полем, без пересборки мода на смену режима). "normal" (или отсутствие/битый конфиг)
     * — полностью ванильное поведение через super.spread(...). "zero"/"anti" — собственная,
     * упрощённая реализация ниже (см. javadoc canFlowInto про известные упрощения).
     */
    @Override
    protected void spread(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState state, FluidState fluidState) {
        if (fluidState.isEmpty()) return;
        FluidConfig cfg = config();
        String mode = cfg != null ? cfg.gravityMode() : "normal";
        switch (mode) {
            case "zero" -> spreadZero(level, pos, fluidState);
            case "anti" -> { /* anti целиком обрабатывается в tickAnti() — spread() для него не вызывается */ }
            default -> super.spread(level, pos, state, fluidState);
        }
    }

    /**
     * gravity_mode "zero": растекается ТОЛЬКО по горизонтали (своей Y-плоскости) — вертикальное
     * течение (DOWN) полностью пропускается, отсутствие опоры снизу не проверяется и не влияет
     * (жидкость "не замечает" обрыв и продолжает висеть/течь по воздуху, как и договаривались).
     * Убывание силы по горизонтали — тот же getDropOff(), что и у обычной воды (~7-8 клеток).
     */
    private void spreadZero(net.minecraft.server.level.ServerLevel level, BlockPos pos, FluidState fluidState) {
        int amount = fluidState.getAmount() - getDropOff(level);
        if (fluidState.getValue(FALLING)) {
            amount = 7; // ствол (падающий/растущий столб) всегда даёт максимальную боковую силу — как у воды
        }
        if (amount <= 0) return;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos neighborPos = pos.relative(direction);
            FluidState candidate = getFlowing(amount, false);
            if (canFlowInto(level, neighborPos, candidate)) {
                spreadTo(level, neighborPos, level.getBlockState(neighborPos), direction, candidate);
            }
        }
    }


    /**
     * Упрощённая (не 1-в-1 с ванилью) проверка "может ли жидкость течь в эту клетку" —
     * используется ТОЛЬКО режимами zero/anti (обычный "normal" идёt через оригинальный
     * super.spread(), не через этот метод, и ничем не рискует). Не воспроизводит все ванильные
     * частные случаи (двери, лестницы, LiquidBlockContainer и т.п.) — пропускает клетку, если
     * блок там физически не блокирует движение (blocksMotion()==false) и если там ещё нет
     * такой же жидкости РАВНОЙ ИЛИ БОЛЬШЕЙ силы. Если на практике найдётся блок, который
     * ведёт себя неожиданно — разберём точечно.
     */
    private boolean canFlowInto(LevelReader level, BlockPos pos, FluidState newState) {
        BlockState state = level.getBlockState(pos);
        if (state.blocksMotion()) return false;
        FluidState existing = level.getFluidState(pos);
        if (existing.getType().isSame(this) && existing.getAmount() >= newState.getAmount()) return false;
        return true;
    }

    /**
     * Для gravity_mode "zero"/"anti" ПОЛНОСТЬЮ ПЕРЕХВАТЫВАЕМ tick — родительский
     * FlowingFluid.tick() перед вызовом spread() сам пересчитывает содержимое клетки через
     * getNewLiquid(...), а этот метод жёстко заточен под ПАДЕНИЕ ВНИЗ (проверяет клетку сверху,
     * горизонтальных соседей) — для anti (растёт вверх) это работает в буквально обратную
     * сторону и постоянно "переоткрывает" уже устоявшийся ствол, из-за чего боковая лужа
     * рождается заново на каждом уровне (отсюда ёлочка/пена). Bypass'им этот пересчёт целиком
     * для zero/anti — единственный источник истины тогда наш собственный spread().
     */
    @Override
    public void tick(net.minecraft.server.level.ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        FluidConfig cfg = config();
        String mode = cfg != null ? cfg.gravityMode() : "normal";
        if (!mode.equals("zero") && !mode.equals("anti")) {
            super.tick(level, pos, blockState, fluidState);
            return;
        }
        if (fluidState.isEmpty()) return;

        if (mode.equals("anti")) {
            tickAnti(level, pos, fluidState, cfg);
            return;
        }

        // zero — как раньше, без изменений (уже подтверждено рабочим)
        if (!fluidState.isSource()) {
            FluidState recalculated = recalcZero(level, pos);
            if (recalculated.isEmpty()) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                return;
            } else if (!recalculated.equals(fluidState)) {
                blockState = recalculated.createLegacyBlock();
                level.setBlock(pos, blockState, 3);
                level.scheduleTick(pos, recalculated.getType(), getTickDelay(level));
                fluidState = recalculated;
            }
        }
        spread(level, pos, blockState, fluidState);
    }

    /**
     * KISS-реализация anti-гравитации: одно-единственное правило — РАССТОЯНИЕ ОТ ИСТОЧНИКА ПО
     * ПУТИ ЖИДКОСТИ (BFS, не по прямой) не может превышать max_up_distance. Никакого понятия
     * "ствол/лужа" больше нет — просто каждая клетка каждый тик спрашивает "как далеко я от
     * настоящего источника?" и либо растёт дальше (расстояние+1 ≤ лимита), либо исчезает
     * (путь до источника не найден в пределах лимита — это и чинит баг с оторванной каплей:
     * раньше проверялся только сосед снизу, теперь — реальная связность до источника).
     */
    // TODO Фаза 2 (отложено): anti-гравитация нестабильна — при выращивании столба
    // получается бесконечно растущая "ёлочка" вместо чистого ▼. BFS-подход из последней
    // попытки чинит зависшие оторванные капли, но не решает переразрастание вширь. Решение
    // отложено — normal и zero полностью рабочие и не затронуты этим кодом. Если понадобится
    // вернуться: искать в паре с этим TODO tickAnti/bfsDistanceToSource ниже.
    private void tickAnti(net.minecraft.server.level.ServerLevel level, BlockPos pos, FluidState fluidState, @Nullable FluidConfig cfg) {
        int maxUp = cfg != null ? cfg.maxUpDistance() : 10;

        int distance;
        if (fluidState.isSource()) {
            distance = 0;
        } else {
            distance = bfsDistanceToSource(level, pos, maxUp);
            if (distance < 0) {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                return; // не нашли путь до источника в пределах лимита — клетка-сирота, исчезает
            }
        }

        if (distance + 1 > maxUp) return; // дальше расти нельзя, лимит исчерпан

        FluidState candidate = getFlowing(8, false);

        BlockPos abovePos = pos.above();
        if (canFlowInto(level, abovePos, candidate)) {
            spreadTo(level, abovePos, level.getBlockState(abovePos), Direction.UP, candidate);
        } else {
            // Путь вверх закрыт препятствием — "обтекаем": пробуем все свободные боковые
            // направления, каждое тоже на 1 шаг дальше от источника.
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos sidePos = pos.relative(direction);
                if (canFlowInto(level, sidePos, candidate)) {
                    spreadTo(level, sidePos, level.getBlockState(sidePos), direction, candidate);
                }
            }
        }

        level.scheduleTick(pos, this, getTickDelay(level));
    }

    /**
     * Кратчайшее расстояние (в шагах, через уже существующие клетки ЭТОЙ ЖЕ жидкости) от
     * {@code start} до ближайшего источника — обычный BFS, ограниченный глубиной
     * {@code maxDepth} (= max_up_distance). Если источник не найден в пределах лимита —
     * возвращает -1 (клетка считается оторванной и должна исчезнуть).
     */
    private int bfsDistanceToSource(LevelReader level, BlockPos start, int maxDepth) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Map<Long, Integer> distances = new HashMap<>();
        queue.add(start);
        distances.put(start.asLong(), 0);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            int currentDistance = distances.get(current.asLong());
            if (currentDistance >= maxDepth) continue;

            for (Direction direction : Direction.values()) {
                BlockPos next = current.relative(direction);
                long key = next.asLong();
                if (distances.containsKey(key)) continue;

                FluidState neighborState = level.getFluidState(next);
                if (!neighborState.getType().isSame(this)) continue;

                int nextDistance = currentDistance + 1;
                if (neighborState.isSource()) return nextDistance;

                distances.put(key, nextDistance);
                queue.add(next);
            }
        }
        return -1;
    }

    /**
     * Decay для zero — чисто горизонтальный пересчёт (без вертикали вообще, как и весь режим):
     * сила клетки = максимум среди горизонтальных same-type соседей минус getDropOff(). Если
     * никто из соседей больше не подпитывает — клетка исчезает.
     */
    private FluidState recalcZero(LevelReader level, BlockPos pos) {
        int highest = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            FluidState neighbor = level.getFluidState(pos.relative(direction));
            if (neighbor.getType().isSame(this)) highest = Math.max(highest, neighbor.getAmount());
        }
        int amount = highest - getDropOff(level);
        return amount <= 0 ? net.minecraft.world.level.material.Fluids.EMPTY.defaultFluidState() : getFlowing(amount, false);
    }

    public Identifier getConfigId() {
        return configId;
    }

    @Override
    public Fluid getFlowing() {
        return flowingSupplier.get();
    }

    @Override
    public Fluid getSource() {
        return stillSupplier.get();
    }

    @Override
    public boolean isSame(Fluid other) {
        // КРИТИЧНО: без этого переопределения Source и Flowing считают друг друга РАЗНЫМИ
        // жидкостями (дефолт — сравнение по ссылке this == other). Вся физика растекания
        // (getNewLiquid, affectsFlow, isSourceBlockOfThisType и т.д.) опирается именно на
        // isSame, чтобы распознать "это тот же поток" — без фикса расчёт на каждом тике не
        // может договориться сам с собой, отсюда и мерцание/распад на несвязанные блоки.
        // Мирроит WaterFluid.isSame(other), который сравнивает с Fluids.WATER/FLOWING_WATER.
        return other == this.stillSupplier.get() || other == this.flowingSupplier.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        // ВАЖНО: обязательно кодируем LiquidBlock.LEVEL через getLegacyLevel(fluidState) —
        // это и есть "сила потока", убывающая с расстоянием от источника (0 = источник/полный,
        // 1-7 = поток, тем слабее, чем дальше). Без явного .setValue(...) свойство LEVEL по
        // умолчанию равно 0 — то есть КАЖДАЯ клетка потока физически ставилась как НОВЫЙ
        // источник, отсюда и неограниченное распространение (каждый поток сам плодил новые
        // источники, а не затухающие потоки).
        return this.blockSupplier.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.LiquidBlock.LEVEL, getLegacyLevel(fluidState));
    }

    @Override
    protected boolean canConvertToSource(net.minecraft.server.level.ServerLevel level) {
        return false; // как лава: не самовосстанавливается в источник — простой и безопасный дефолт Фазы 1
    }

    @Override
    protected void beforeDestroyingBlock(net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockState state) {
        // ничего не роняем при разрушении жидкости — совпадает с поведением воды/лавы по умолчанию
    }

    @Override
    public int getSlopeFindDistance(LevelReader level) {
        return 4; // как у воды
    }


    @Override
    public int getDropOff(LevelReader level) {
        FluidConfig cfg = config();
        return cfg != null ? Math.max(1, cfg.spreadDropoff()) : 1; // читаем заново каждый вызов, как и весь остальной конфиг
    }

    @Override
    public Item getBucket() {
        return bucketSupplier.get().asItem();
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, Fluid fluid, net.minecraft.core.Direction direction) {
        // Мирроит WaterFluid: разрешаем замещение сверху вниз, если это не та же самая жидкость
        // (иначе поток не сможет literally течь вниз в клетку, где уже что-то от него же есть).
        return direction == net.minecraft.core.Direction.DOWN && !isSame(fluid);
    }

    @Override
    public int getTickDelay(LevelReader level) {
        FluidConfig cfg = config();
        if (cfg == null || cfg.viscosity() <= 0f) return 5; // как у воды — дефолт/защита от деления на 0
        // Ниже viscosity (тяжелее плавать) -> больше задержка -> медленнее растекается.
        // viscosity=1.0 (как воздух) -> 5 тиков (без изменений); viscosity=0.8 (как вода) -> ~6;
        // viscosity=0.4 (густая жидкость) -> ~13 (заметно медленнее).
        int delay = Math.round(5f / cfg.viscosity());
        return net.minecraft.util.Mth.clamp(delay, 1, 100);
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(net.minecraft.sounds.SoundEvents.BUCKET_FILL);
    }

    // TODO Фаза 2: переопределить getFlow(...) в зависимости от config().gravityMode()
    //   ("zero" — течь только по горизонтали текущего слоя, "anti" — вверх не более 10 блоков).
    //   Пока gravity_mode игнорируется, поведение всегда как у воды (getSpread у FlowingFluid).

    public static class Source extends ConfigurableFluid {
        public Source(Identifier configId, Supplier<? extends Fluid> stillSupplier, Supplier<? extends Fluid> flowingSupplier,
                      Supplier<? extends ItemLike> bucketSupplier, Supplier<? extends net.minecraft.world.level.block.Block> blockSupplier) {
            super(configId, stillSupplier, flowingSupplier, bucketSupplier, blockSupplier);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends ConfigurableFluid {
        public Flowing(Identifier configId, Supplier<? extends Fluid> stillSupplier, Supplier<? extends Fluid> flowingSupplier,
                       Supplier<? extends ItemLike> bucketSupplier, Supplier<? extends net.minecraft.world.level.block.Block> blockSupplier) {
            super(configId, stillSupplier, flowingSupplier, bucketSupplier, blockSupplier);
        }

        @Override
        protected void createFluidStateDefinition(
                net.minecraft.world.level.block.state.StateDefinition.Builder<net.minecraft.world.level.material.Fluid, net.minecraft.world.level.material.FluidState> builder
        ) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}