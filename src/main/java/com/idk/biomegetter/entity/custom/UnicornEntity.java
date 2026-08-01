package com.idk.biomegetter.entity.custom;

import com.idk.biomegetter.entity.ModAttributes;
import com.idk.biomegetter.entity.ModEntities;
import com.idk.biomegetter.entity.custom.ally.AllyMobs;
import com.idk.biomegetter.entity.custom.ally.SummonedAlly;
import com.idk.biomegetter.entity.custom.mana.ManaPool;
import com.idk.biomegetter.entity.custom.projectile.UnicornBoltEntity;
import com.idk.biomegetter.entity.custom.state.UnicornCombatState;
import com.idk.biomegetter.entity.custom.util.CombatUtils;
import com.idk.biomegetter.entity.effect.SummonEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.*;

// на вики extend PathfinderMob
// Но Animals уже содержит его в себе
public class UnicornEntity extends Animal {

    //    Почему мы повторились дважды?
    //  super отказывается эту строку принимать(
    public UnicornEntity(Level level) {
        this(ModEntities.UNICORN, level);
    }

    private final ManaPool manaPool = new ManaPool(this);

    private int explosiveShotCooldown = 0;

    private int dashCooldown = 0;
    boolean isDashing = false; // package-private: читается голом, влияет на fall damage

    private static final String NBT_SUMMON_COOLDOWN = "SummonCooldown";

    private final Set<UUID> minions = new HashSet<>();
    private int summonCooldown = 0;
    private static final int MINION_LIFETIME_TICKS = 200; // 10 секунд × 20 тиков

    private UnicornCombatState combatState = UnicornCombatState.NEUTRAL;
    private Player alertPlayer;
    private int warningStrikeCooldown = 0;

    private static final double ALERT_RADIUS = 10.0;
    private static final double STRIKE_RADIUS = 2.0;

    protected SimpleContainer inventory;

    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState walkAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;


    public UnicornEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }


    @Override
    protected void registerGoals() {
        // Боевые голы — таргетинг для них полностью управляется updateCombatState(),
        // отдельные targetSelector-голы больше не нужны (единый источник истины).
        this.goalSelector.addGoal(0, new TemptGoal(this, 0.35, Ingredient.of(Items.DIAMOND), false));
        this.goalSelector.addGoal(1, new UnicornSummonUndeadGoal(this));
        this.goalSelector.addGoal(1, new UnicornExplosiveShotGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
        this.goalSelector.addGoal(2, new UnicornWarningStrikeGoal(this));
        this.goalSelector.addGoal(3, new UnicornDashGoal(this));
        this.goalSelector.addGoal(4, new BreedGoal(this, 0.35));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 0.35));
        this.goalSelector.addGoal(6, new EatBlockGoal(this));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(8, new UnicornWatchThreatGoal(this));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
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

            if (undead instanceof SummonedAlly ally) {
                ally.setLifetimeTicks(UnicornEntity.MINION_LIFETIME_TICKS);
            }
            this.unicorn.minions.add(undead.getUUID());


            // Звук
            serverLevel.playSound(null, this.unicorn.blockPosition(),
                    SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.0F, 1.0F);
        }


    }

    private static class UnicornWatchThreatGoal extends Goal {
        private final UnicornEntity unicorn;

        UnicornWatchThreatGoal(UnicornEntity unicorn) {
            this.unicorn = unicorn;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.unicorn.combatState == UnicornCombatState.ALERT && this.unicorn.alertPlayer != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void tick() {
            if (this.unicorn.alertPlayer != null) {
                this.unicorn.getLookControl().setLookAt(this.unicorn.alertPlayer, 30.0F, 30.0F);
            }
        }
    }

    private static class UnicornWarningStrikeGoal extends Goal {
        private static final int STRIKE_COOLDOWN_TICKS = 20; // 1 секунда между предупредительными ударами

        private final UnicornEntity unicorn;

        UnicornWarningStrikeGoal(UnicornEntity unicorn) {
            this.unicorn = unicorn;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player player = this.unicorn.alertPlayer;
            return this.unicorn.combatState == UnicornCombatState.ALERT
                    && this.unicorn.warningStrikeCooldown <= 0
                    && player != null && player.isAlive()
                    && this.unicorn.distanceToSqr(player) <= STRIKE_RADIUS * STRIKE_RADIUS;
        }

        @Override
        public boolean canContinueToUse() {
            return false; // разовое действие за один тик, не удерживаем гол
        }

        @Override
        public void start() {
            Player player = this.unicorn.alertPlayer;
            if (player == null || !(this.unicorn.level() instanceof ServerLevel serverLevel)) {
                return;
            }

            player.hurtServer(serverLevel, this.unicorn.damageSources().mobAttack(this.unicorn), 4.0F);

            Vec3 knockback = player.position().subtract(this.unicorn.position()).normalize().scale(1.6);
            player.setDeltaMovement(player.getDeltaMovement().add(knockback.x, 0.5, knockback.z));
            player.hurtMarked = true;

            this.unicorn.warningStrikeCooldown = STRIKE_COOLDOWN_TICKS;
        }
    }

    private static class UnicornDashGoal extends Goal {
        private static final double MIN_RANGE = 3.0;
        private static final double MAX_RANGE = 7.0;
        private static final int WINDUP_TICKS = 15;     // ~0.75 сек предупреждения
        private static final int COOLDOWN_TICKS = 100;  // 5 сек
        private static final double MANA_COST = 20.0;
        private static final double DASH_SPEED_PER_TICK = 0.9;

        private final UnicornEntity unicorn;
        private int windupTicksLeft;
        private int dashTicksLeft;
        private Vec3 dashDirection = Vec3.ZERO;

        UnicornDashGoal(UnicornEntity unicorn) {
            this.unicorn = unicorn;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.unicorn.getTarget();
            if (this.unicorn.combatState != UnicornCombatState.COMBAT || target == null || !target.isAlive()) {
                return false;
            }
            if (this.unicorn.dashCooldown > 0 || !this.unicorn.manaPool.canAfford(MANA_COST)) {
                return false;
            }
            double distSqr = this.unicorn.distanceToSqr(target);
            return distSqr >= MIN_RANGE * MIN_RANGE && distSqr <= MAX_RANGE * MAX_RANGE;
        }

        @Override
        public boolean canContinueToUse() {
            return this.windupTicksLeft > 0 || this.dashTicksLeft > 0;
        }

        @Override
        public void start() {
            this.windupTicksLeft = WINDUP_TICKS;
            this.dashTicksLeft = 0;
            if (this.unicorn.level() instanceof ServerLevel serverLevel) {
                SummonEffects.playLightningCast(serverLevel, this.unicorn.getX(), this.unicorn.getY(), this.unicorn.getZ());
            }
        }

        @Override
        public void stop() {
            this.unicorn.isDashing = false;
            this.unicorn.setDeltaMovement(Vec3.ZERO);
            this.unicorn.resetFallDistance();
        }

        @Override
        public void tick() {
            LivingEntity target = this.unicorn.getTarget();

            if (this.windupTicksLeft > 0) {
                if (target != null) {
                    this.unicorn.getLookControl().setLookAt(target);
                }
                --this.windupTicksLeft;
                if (this.windupTicksLeft == 0) {
                    this.beginDash(target);
                }
                return;
            }

            if (this.dashTicksLeft > 0) {
                this.unicorn.isDashing = true;
                this.unicorn.setDeltaMovement(this.dashDirection);
                this.unicorn.hurtMarked = true;
                this.unicorn.resetFallDistance();
                --this.dashTicksLeft;

                for (LivingEntity nearby : this.unicorn.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.unicorn.getBoundingBox().inflate(0.6),
                        entity -> entity != this.unicorn && AllyMobs.isValidTarget(entity)
                )) {
                    this.hitTarget(nearby);
                    this.dashTicksLeft = 0;
                    break;
                }

                if (this.dashTicksLeft == 0) {
                    this.unicorn.isDashing = false;
                    this.unicorn.setDeltaMovement(Vec3.ZERO);
                    this.unicorn.dashCooldown = COOLDOWN_TICKS;
                }
            }
        }

        private void beginDash(LivingEntity target) {
            if (target == null || !this.unicorn.manaPool.trySpend(MANA_COST)) {
                this.dashTicksLeft = 0;
                this.unicorn.dashCooldown = COOLDOWN_TICKS;
                return;
            }

            Vec3 diff = target.position().subtract(this.unicorn.position());
            Vec3 direction = new Vec3(diff.x, 0.0, diff.z).normalize();
            this.dashDirection = direction.scale(DASH_SPEED_PER_TICK);

            double distance = Math.min(MAX_RANGE, this.unicorn.distanceTo(target));
            this.dashTicksLeft = (int) Math.ceil(distance / DASH_SPEED_PER_TICK);
        }

        private void hitTarget(LivingEntity target) {
            if (this.unicorn.level() instanceof ServerLevel serverLevel) {
                target.hurtServer(serverLevel, this.unicorn.damageSources().mobAttack(this.unicorn), 8.0F);
            }
            Vec3 knockback = target.position().subtract(this.unicorn.position()).normalize().scale(2.2);
            target.setDeltaMovement(target.getDeltaMovement().add(knockback.x, 0.6, knockback.z));
            target.hurtMarked = true;
        }
    }

    private static class UnicornExplosiveShotGoal extends Goal {
        private static final double MIN_RANGE = 7.0;
        private static final double MAX_RANGE = 10.0;
        private static final double SAFETY_RADIUS = 4.0; // радиус взрыва (3) + запас
        private static final int WINDUP_TICKS = 20; // 1 секунда
        private static final int COOLDOWN_TICKS = 100;
        private static final double MANA_COST = 25.0;

        private final UnicornEntity unicorn;
        private int windupTicksLeft;
        private BlockPos targetBlock;

        UnicornExplosiveShotGoal(UnicornEntity unicorn) {
            this.unicorn = unicorn;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity target = this.unicorn.getTarget();
            if (this.unicorn.combatState != UnicornCombatState.COMBAT || target == null || !target.isAlive()) {
                return false;
            }
            if (this.unicorn.explosiveShotCooldown > 0 || !this.unicorn.manaPool.canAfford(MANA_COST)) {
                return false;
            }
            double distSqr = this.unicorn.distanceToSqr(target);
            if (distSqr < MIN_RANGE * MIN_RANGE || distSqr > MAX_RANGE * MAX_RANGE) {
                return false;
            }
            return this.isSafeToFire(target.blockPosition());
        }

        private boolean isSafeToFire(BlockPos target) {
            if (!(this.unicorn.level() instanceof ServerLevel serverLevel)) {
                return false;
            }
            AABB dangerZone = new AABB(target).inflate(SAFETY_RADIUS);
            return serverLevel.getEntitiesOfClass(UnicornEntity.class, dangerZone).isEmpty();
        }

        @Override
        public boolean canContinueToUse() {
            return this.windupTicksLeft > 0;
        }

        @Override
        public void start() {
            LivingEntity target = this.unicorn.getTarget();
            this.targetBlock = target != null ? target.blockPosition() : null;
            this.windupTicksLeft = WINDUP_TICKS;
            if (this.unicorn.level() instanceof ServerLevel serverLevel) {
                SummonEffects.playLightningCast(serverLevel, this.unicorn.getX(), this.unicorn.getY(), this.unicorn.getZ());
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.unicorn.getTarget();
            if (target != null) {
                this.unicorn.getLookControl().setLookAt(target);
            }

            --this.windupTicksLeft;
            if (this.windupTicksLeft <= 0) {
                this.fire();
            }
        }

        private void fire() {
            this.unicorn.explosiveShotCooldown = COOLDOWN_TICKS;

            if (this.targetBlock == null || !(this.unicorn.level() instanceof ServerLevel serverLevel)
                    || !this.isSafeToFire(this.targetBlock) || !this.unicorn.manaPool.trySpend(MANA_COST)) {
                return; // условия изменились за время задержки — тихо отменяем
            }

            UnicornBoltEntity bolt = new UnicornBoltEntity(ModEntities.UNICORN_BOLT, serverLevel);
            bolt.setOwner(this.unicorn);
            bolt.setPos(this.unicorn.getX(), this.unicorn.getEyeY(), this.unicorn.getZ());
            bolt.shootTowards(Vec3.atCenterOf(this.targetBlock));

            serverLevel.addFreshEntity(bolt);
        }
    }


    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.TEMPT_RANGE, 10)
                .add(Attributes.SCALE, 2.5f)
                .add(Attributes.MOVEMENT_SPEED, 1f)
                .add(Attributes.ATTACK_DAMAGE, 6.0)
                .add(ModAttributes.MAX_MANA, 100.0)
                .add(ModAttributes.MANA_REGENERATION, 5.0);
    }


    //    Функции сохранения при выходе из мира
    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.manaPool.save(output);
        output.putInt(NBT_SUMMON_COOLDOWN, this.summonCooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.manaPool.load(input);
        this.summonCooldown = input.getIntOr(NBT_SUMMON_COOLDOWN, 0);
    }

    /**
     * Центральная логика боевого состояния юникорна.
     * Единственное место, где вызывается setTarget() — targetSelector-голы
     * намеренно не используются, чтобы не было двух источников истины.
     */
    private void updateCombatState() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity combatThreat = this.findCombatThreat(serverLevel);
        if (combatThreat != null) {
            this.enterCombat(combatThreat);
            return;
        }

        Player threateningPlayer = this.findAlertPlayer(serverLevel);
        if (threateningPlayer != null) {
            this.enterAlert(threateningPlayer);
            return;
        }

        this.enterNeutral();
    }

    @Nullable
    private LivingEntity findCombatThreat(ServerLevel serverLevel) {
        // 1. Враждебный моб в радиусе (кроме собственных приспешников) → бой
        Monster hostileMob = serverLevel.getNearestEntity(
                Monster.class,
                TargetingConditions.forCombat()
                        .range(ALERT_RADIUS)
                        .selector((entity, level) -> !(entity instanceof SummonedAlly)),
                this, this.getX(), this.getY(), this.getZ(),
                this.getBoundingBox().inflate(ALERT_RADIUS)
        );
        if (hostileMob != null) {
            return hostileMob;
        }

        // 2. Уже в бою, текущая цель жива и в радиусе → остаёмся в бою (не сбрасываем цель зря)
        if (this.combatState == UnicornCombatState.COMBAT) {
            LivingEntity currentTarget = this.getTarget();
            if (currentTarget != null && currentTarget.isAlive()
                    && this.distanceToSqr(currentTarget) <= ALERT_RADIUS * ALERT_RADIUS) {
                return currentTarget;
            }
        }

        // 3. Игрок недавно ударил юникорна и всё ещё рядом → бой
        if (this.getLastHurtByMob() instanceof Player attacker && attacker.isAlive()
                && this.distanceToSqr(attacker) <= ALERT_RADIUS * ALERT_RADIUS) {
            return attacker;
        }

        return null;
    }

    @Nullable
    private Player findAlertPlayer(ServerLevel serverLevel) {
        return serverLevel.getNearestPlayer(
                TargetingConditions.forCombat()
                        .range(ALERT_RADIUS)
                        .selector((entity, level) -> entity instanceof Player player
                                && !player.isCrouching()
                                && CombatUtils.isHoldingWeapon(player)),
                this
        );
    }

    private void enterCombat(LivingEntity threat) {
        this.combatState = UnicornCombatState.COMBAT;
        this.alertPlayer = null;
        if (this.getTarget() != threat) {
            this.setTarget(threat);
        }
    }

    private void enterAlert(Player player) {
        if (this.combatState != UnicornCombatState.ALERT) {
            if (this.level() instanceof ServerLevel serverLevel) {
                SummonEffects.playLightningCast(serverLevel, this.getX(), this.getY(), this.getZ());
            }
        }
        this.combatState = UnicornCombatState.ALERT;
        this.alertPlayer = player;
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
    }

    private void enterNeutral() {
        this.combatState = UnicornCombatState.NEUTRAL;
        this.alertPlayer = null;
        if (this.getTarget() != null) {
            this.setTarget(null);
        }
    }


    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
    }

    /**
     * ГЛАВНЫЙ ТИК СУЩНОСТИ (не гола!). Вызывается движком каждый тик для самого юникорна.
     * Все "глобальные" механики юникорна (кулдауны, регенерация маны, состояние боя)
     * подключаются именно сюда, а не в tick() отдельных Goal-классов ниже по файлу —
     * у Goal.tick() своя, другая роль: он выполняется только пока конкретный гол активен
     * (см. UnicornSummonUndeadGoal.tick() ниже — тот декрементирует кулдаун только пока
     * гол "запущен", что вообще другая история и другой смысл).
     */
    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.setupAnimationStates();
        } else {
            this.tickSummonCooldown();
            this.tickWarningStrikeCooldown();
            this.tickDashCooldown();
            this.tickExplosiveShotCooldown();
            this.manaPool.tick();
            this.updateCombatState();
            this.syncMinionTargets();
        }
    }

    private void tickDashCooldown() {
        if (this.dashCooldown > 0) {
            --this.dashCooldown;
        }
    }

    private void tickExplosiveShotCooldown() {
        if (this.explosiveShotCooldown > 0) {
            --this.explosiveShotCooldown;
        }
    }

    private void tickWarningStrikeCooldown() {
        if (this.warningStrikeCooldown > 0) {
            --this.warningStrikeCooldown;
        }
    }

    /**
     * Каждый тик приводим цель всех живых приспешников в соответствие
     * с текущей целью юникорна — если юникорн сменил врага или потерял его,
     * приспешники узнают об этом немедленно, а не по "снимку" на момент призыва.
     */
    private void syncMinionTargets() {
        if (this.minions.isEmpty() || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        LivingEntity currentEnemy = this.getTarget();
        Iterator<UUID> iterator = this.minions.iterator();

        while (iterator.hasNext()) {
            Entity minion = serverLevel.getEntity(iterator.next());
            if (minion == null || !minion.isAlive()) {
                iterator.remove(); // приспешник погиб/исчез — больше не отслеживаем
                continue;
            }
            if (minion instanceof Mob minionMob) {
                minionMob.setTarget(currentEnemy);
            }
        }
    }

    private void tickSummonCooldown() {
        if (this.summonCooldown > 0) {
            --this.summonCooldown;
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
        return ModEntities.UNICORN.create(level, EntitySpawnReason.BREEDING);
    }

    @Override
    public float getAgeScale() {
        return this.isBaby() ? 0.4F : 1.0F;
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
