package com.idk.biomegetter.block.custom;

import com.mojang.serialization.MapCodec;

/**
 * Неулучшенный котёл — весь функционал полностью наследуется от ModCauldronBlock (который
 * концептуально является "Upgraded", без ограничений), собственного поведения не добавляет.
 * Единственная роль — служить маркером для instanceof-проверки в
 * ModCauldronBlockEntity#isLiquidAllowed: некоторые жидкости (лава, в будущем — с тегом
 * "unstable_magic", см. CauldronUpgradeBlacklistLoader) можно налить только в котёл,
 * НЕ являющийся экземпляром этого класса.
 */
public class ModCauldronBlockBasic extends ModCauldronBlock {

    private static final MapCodec<ModCauldronBlockBasic> CODEC = simpleCodec(ModCauldronBlockBasic::new);

    public ModCauldronBlockBasic(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<? extends ModCauldronBlock> codec() {
        return CODEC;
    }
}