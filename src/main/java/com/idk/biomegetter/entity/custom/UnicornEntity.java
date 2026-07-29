package com.idk.biomegetter.entity.custom;

import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.effect.SummonEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.*;

// на вики extend PathfinderMob
// Но Animals уже водержит его в себе
public class UnicornEntity extends Animal {

    //    Почему мы повторились дважды?
    //  super отказывается эту строку принимать(
    public UnicornEntity(Level level) {
        this(ModEntities.UNICORN, level);
    }

    private final Map<UUID, Integer> minionTimers = new HashMap<>();
    private int summonCooldown = 0;
    private static final int MINION_LIFETIME_TICKS = 200; // 10 секунд × 20 тиков

    protected SimpleContainer inventory;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;


    public UnicornEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }


    @Override
    protected void registerGoals() {
//        this.goalSelector.addGoal(0, new Goal());
        this.goalSelector.addGoal(0, new TemptGoal(this, 0.35, Ingredient.of(Items.DIAMOND), false));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.35));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 0.35));
//        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.goalSelector.addGoal(3, new UnicornSummonUndeadGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                LivingEntity.class,
                10,           // chance
                true,         // mustSee
                false,        // mustReach
                (entity, serverLevel) -> !(entity instanceof UnicornEntity) // ← 2 параметра!
        ));
    }

    static class UnicornSummonUndeadGoal extends Goal {
        private final UnicornEntity unicorn;

        public UnicornSummonUndeadGoal(UnicornEntity unicorn) {
            this.unicorn = unicorn;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.unicorn.getTarget() != null && this.unicorn.summonCooldown <= 0;
        }

        @Override
        public void start() {
            this.unicorn.summonCooldown = 80 + this.unicorn.getRandom().nextInt(40); // 4–6 секунд

            if (this.unicorn.level() instanceof ServerLevel serverLevel) {
                SummonEffects.playLightningCast(serverLevel, this.unicorn.getX(), this.unicorn.getY(), this.unicorn.getZ());
            }

            for (int i = 0; i < 5; i++) {
                this.summonUndead();
            }
        }

        @Override
        public void tick() {
            if (this.unicorn.summonCooldown > 0) {
                --this.unicorn.summonCooldown;
            }
        }

        private void summonUndead() {
            if (!(this.unicorn.level() instanceof ServerLevel serverLevel)) return;

            LivingEntity target = this.unicorn.getTarget();
            if (target == null) return;

            // Случайный тип нежити — используем свои "союзные" подтипы,
            // а не ванильные, чтобы иметь контроль над таргетингом/лутом/визуалом
            EntityType<? extends Mob> type = switch (this.unicorn.getRandom().nextInt(3)) {
                case 0 -> ModEntities.ALLY_ZOMBIE;
                case 1 -> ModEntities.ALLY_SKELETON;
                default -> ModEntities.ALLY_WITHER_SKELETON;
            };

            // Создание сущности (новый API 1.21+)
            Mob undead = type.create(serverLevel, EntitySpawnReason.MOB_SUMMONED);
            if (undead == null) return;

            // Позиция рядом с единорогом
            double x = this.unicorn.getX() + (this.unicorn.getRandom().nextDouble() - 0.5) * 3;
            double y = this.unicorn.getY();
            double z = this.unicorn.getZ() + (this.unicorn.getRandom().nextDouble() - 0.5) * 3;

            // === 1. Меньше здоровья ===
            undead.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0); // обычный зомби = 20
            undead.setHealth(10.0F);

            // === 2. Не горит на солнце ===
            undead.setPersistenceRequired();           // не деспавнится сам
            // Самый надёжный способ — поставить броню на голову
            undead.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));

            undead.setPos(x, y, z);

            // Сразу даём цель
            undead.setTarget(target);

            // Добавляем в мир

            serverLevel.addFreshEntity(undead);

            // Звук
            serverLevel.playSound(null, this.unicorn.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.0F);
        }


    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.TEMPT_RANGE, 10)
                .add(Attributes.SCALE, 2.5f)
                .add(Attributes.MOVEMENT_SPEED, 1f);
    }


    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        } else {
            this.tickSummonCooldown();
            this.tickMinionLifetimes();
        }
    }

    private void tickSummonCooldown() {
        if (this.summonCooldown > 0) {
            --this.summonCooldown;
        }
    }

    private void tickMinionLifetimes() {
        if (this.minionTimers.isEmpty() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Iterator<Map.Entry<UUID, Integer>> iterator = this.minionTimers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticksLeft = entry.getValue() - 1;

            if (ticksLeft <= 0) {
                Entity minion = serverLevel.getEntity(entry.getKey());
                if (minion != null) {
                    minion.discard(); // discard(), а не kill() — без смерти/лута/звука смерти
                }
                iterator.remove();
            } else {
                entry.setValue(ticksLeft);
            }
        }
    }

    private void setupAnimationStates() {
        // Idle: проигрываем случайный "простойный" клип с паузами
        if (this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = this.random.nextInt(40) + 80;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }

        // Walk: включена, пока сущность физически движется
        if (this.walkAnimation.isMoving()) {
            this.walkAnimationState.startIfStopped(this.tickCount);
        } else {
            this.walkAnimationState.stop();
        }
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    //  Я думаю, эти методы можно сразу унаследовать
    public final int getInventorySize() {
        return AbstractMountInventoryMenu.getInventorySize(this.getInventoryColumns());
    }

    public int getInventoryColumns() {
        return 0;
    }

    protected void createInventory() {
        SimpleContainer old = this.inventory;
        this.inventory = new SimpleContainer(this.getInventorySize());
        if (old != null) {
            int max = Math.min(old.getContainerSize(), this.inventory.getContainerSize());

            for (int slot = 0; slot < max; slot++) {
                ItemStack itemStack = old.getItem(slot);
                if (!itemStack.isEmpty()) {
                    this.inventory.setItem(slot, itemStack.copy());
                }
            }
        }
    }
}
