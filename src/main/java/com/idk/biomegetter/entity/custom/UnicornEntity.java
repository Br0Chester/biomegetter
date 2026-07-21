package com.idk.biomegetter.entity.custom;

import com.idk.biomegetter.entity.ModEntities;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractMountInventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

// на вики extend PathfinderMob
// Но Animals уже водержит его в себе
public class UnicornEntity extends Animal {

    //    Почему мы повторились дважды?
    //  super отказывается эту строку принимать(
    public UnicornEntity(Level level) {
        this(ModEntities.UNICORN, level);
    }


    protected SimpleContainer inventory;

    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;


    public UnicornEntity(EntityType<? extends Animal> type, Level level) {
        super(type, level);
    }


    @Override
    protected void registerGoals() {
//        this.goalSelector.addGoal(0, new Goal());
        this.goalSelector.addGoal(0, new TemptGoal(this, 1, Ingredient.of(Items.DIAMOND), false));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.0));
//        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createCubeAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 50)
                .add(Attributes.TEMPT_RANGE, 10)
                .add(Attributes.MOVEMENT_SPEED, 1f);
    }

    @Override
    public boolean isFood(ItemStack itemStack) {
        return false;
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
