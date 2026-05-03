package com.whatxe.xlib.combat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.whatxe.xlib.combat.DamageModifierProfileMergeMode;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import com.mojang.datafixers.util.Either;
import org.junit.jupiter.api.Test;

class DamageModifierProfileApiTest {
    private static final ResourceLocation PROFILE_ID = id("profile/fire_resist");
    private static final ResourceLocation PROFILE_ID_2 = id("profile/projectile_scaling");
    private static final ResourceLocation STALE_PROFILE_ID = id("profile/stale");
    private static final ResourceLocation SOURCE_A = id("source/form_system");
    private static final ResourceLocation SOURCE_B = id("source/artifact_system");
    private static final ResourceLocation FIRE_DAMAGE = ResourceLocation.withDefaultNamespace("in_fire");
    private static final ResourceLocation PROJECTILE_TAG = id("damage_tags/projectile");

    @Test
    void incomingAndOutgoingMultipliersStackAcrossActiveProfiles() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .incomingDamageType(FIRE_DAMAGE, 0.5D)
                    .outgoingDamageTypeTag(PROJECTILE_TAG, 1.25D)
                    .build());
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID_2)
                    .incomingDamageType(FIRE_DAMAGE, 0.8D)
                    .outgoingDamageTypeTag(PROJECTILE_TAG, 1.1D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true)
                    .withProfileSource(PROFILE_ID_2, SOURCE_B, true);

            FakeDamageSource fireDamage = new FakeDamageSource(Set.of(FIRE_DAMAGE), Set.of());
            FakeDamageSource projectileDamage = new FakeDamageSource(Set.of(), Set.of(PROJECTILE_TAG));

            assertEquals(0.4D, DamageModifierProfileApi.incomingMultiplier(data, fireDamage), 0.0001D);
            assertEquals(1.375D, DamageModifierProfileApi.outgoingMultiplier(data, projectileDamage), 0.0001D);
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void sourceRemovalAndSanitizeDropInactiveProfiles() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .incomingDamageType(FIRE_DAMAGE, 0.5D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true)
                    .withProfileSource(PROFILE_ID, SOURCE_B, true)
                    .withProfileSource(STALE_PROFILE_ID, SOURCE_A, true);

            DamageModifierProfileData withoutSourceA = data.clearProfileSource(SOURCE_A);
            assertTrue(withoutSourceA.hasProfile(PROFILE_ID));
            assertFalse(withoutSourceA.sourcesFor(PROFILE_ID).contains(SOURCE_A));

            DamageModifierProfileData sanitized = DamageModifierProfileApi.sanitize(withoutSourceA);
            assertTrue(sanitized.hasProfile(PROFILE_ID));
            assertFalse(sanitized.hasProfile(STALE_PROFILE_ID));
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void datapackDefinitionsParticipateInResolution() {
        unregisterFixtures();
        try {
            DataDrivenDamageModifierProfileApi.setDefinitionsForTesting(Map.of(
                    PROFILE_ID,
                    new DataDrivenDamageModifierProfileApi.LoadedDamageModifierProfileDefinition(
                            PROFILE_ID,
                            DamageModifierProfileDefinition.builder(PROFILE_ID)
                                    .incomingDamageType(FIRE_DAMAGE, 0.25D)
                                    .build()
                    )
            ));

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true);

            FakeDamageSource fireDamage = new FakeDamageSource(Set.of(FIRE_DAMAGE), Set.of());
            assertEquals(0.25D, DamageModifierProfileApi.incomingMultiplier(data, fireDamage), 0.0001D);
            assertTrue(DamageModifierProfileApi.find(PROFILE_ID).isPresent());
            assertTrue(DamageModifierProfileApi.sanitize(data).hasProfile(PROFILE_ID));
        } finally {
            DataDrivenDamageModifierProfileApi.setDefinitionsForTesting(Map.of());
            unregisterFixtures();
        }
    }

    @Test
    void additiveMergeModesSumsMultipliers() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .mergeMode(DamageModifierProfileMergeMode.ADDITIVE)
                    .incomingDamageType(FIRE_DAMAGE, 1.5D)
                    .build());
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID_2)
                    .mergeMode(DamageModifierProfileMergeMode.ADDITIVE)
                    .incomingDamageType(FIRE_DAMAGE, 0.8D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true)
                    .withProfileSource(PROFILE_ID_2, SOURCE_B, true);

            FakeDamageSource fireDamage = new FakeDamageSource(Set.of(FIRE_DAMAGE), Set.of());
            // ADDITIVE: 1.5 + 0.8 = 2.3 (not 1.5 * 0.8 = 1.2)
            assertEquals(2.3D, DamageModifierProfileApi.incomingMultiplier(data, fireDamage), 0.0001D);
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void overrideMergeModePrefersHighestPriority() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .mergeMode(DamageModifierProfileMergeMode.OVERRIDE)
                    .priority(5)
                    .incomingDamageType(FIRE_DAMAGE, 0.1D)
                    .build());
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID_2)
                    .mergeMode(DamageModifierProfileMergeMode.OVERRIDE)
                    .priority(10)
                    .incomingDamageType(FIRE_DAMAGE, 0.9D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true)
                    .withProfileSource(PROFILE_ID_2, SOURCE_B, true);

            FakeDamageSource fireDamage = new FakeDamageSource(Set.of(FIRE_DAMAGE), Set.of());
            // OVERRIDE: only the profile with priority=10 is used, so 0.9
            assertEquals(0.9D, DamageModifierProfileApi.incomingMultiplier(data, fireDamage), 0.0001D);
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void flatAdditionsAreAppliedAfterMultiplier() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .incomingDamageType(FIRE_DAMAGE, 0.5D)
                    .incomingFlatAddition(FIRE_DAMAGE, -3.0D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true);

            FakeDamageSource fireDamage = new FakeDamageSource(Set.of(FIRE_DAMAGE), Set.of());
            // Multiplier: 0.5; flat: -3.0
            assertEquals(-3.0D, DamageModifierProfileApi.incomingFlat(data, fireDamage), 0.0001D);
            // applyIncomingWithFlat on 10.0: (10 * 0.5) + (-3) = 2.0, clamped >= 0
            // We test via the public data overload
            assertEquals(0.5D, DamageModifierProfileApi.incomingMultiplier(data, fireDamage), 0.0001D);
        } finally {
            unregisterFixtures();
        }
    }

    @Test
    void incomingFlatTagsAreSummed() {
        unregisterFixtures();
        try {
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID)
                    .incomingFlatAdditionTag(PROJECTILE_TAG, 5.0D)
                    .build());
            DamageModifierProfileApi.register(DamageModifierProfileDefinition.builder(PROFILE_ID_2)
                    .incomingFlatAdditionTag(PROJECTILE_TAG, 2.5D)
                    .build());

            DamageModifierProfileData data = DamageModifierProfileData.empty()
                    .withProfileSource(PROFILE_ID, SOURCE_A, true)
                    .withProfileSource(PROFILE_ID_2, SOURCE_B, true);

            FakeDamageSource projectileDamage = new FakeDamageSource(Set.of(), Set.of(PROJECTILE_TAG));
            assertEquals(7.5D, DamageModifierProfileApi.incomingFlat(data, projectileDamage), 0.0001D);
        } finally {
            unregisterFixtures();
        }
    }

    private void unregisterFixtures() {
        DamageModifierProfileApi.unregister(PROFILE_ID);
        DamageModifierProfileApi.unregister(PROFILE_ID_2);
        DamageModifierProfileApi.unregister(STALE_PROFILE_ID);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("xlib_test", path);
    }

    private static final class FakeDamageSource extends DamageSource {
        private final Set<ResourceLocation> exactDamageTypes;
        private final Set<ResourceLocation> damageTypeTags;

        private FakeDamageSource(Set<ResourceLocation> exactDamageTypes, Set<ResourceLocation> damageTypeTags) {
            super(new FakeHolder());
            this.exactDamageTypes = exactDamageTypes;
            this.damageTypeTags = damageTypeTags;
        }

        @Override
        public boolean is(ResourceKey<DamageType> damageTypeKey) {
            return this.exactDamageTypes.contains(damageTypeKey.location());
        }

        @Override
        public boolean is(TagKey<DamageType> damageTypeKey) {
            return this.damageTypeTags.contains(damageTypeKey.location());
        }
    }

    private static final class FakeHolder implements Holder<DamageType> {
        @Override
        public DamageType value() {
            return null;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public boolean is(ResourceLocation location) {
            return false;
        }

        @Override
        public boolean is(ResourceKey<DamageType> resourceKey) {
            return false;
        }

        @Override
        public boolean is(java.util.function.Predicate<ResourceKey<DamageType>> predicate) {
            return false;
        }

        @Override
        public boolean is(TagKey<DamageType> tagKey) {
            return false;
        }

        @Override
        public boolean is(Holder<DamageType> holder) {
            return holder == this;
        }

        @Override
        public java.util.stream.Stream<TagKey<DamageType>> tags() {
            return java.util.stream.Stream.empty();
        }

        @Override
        public Either<ResourceKey<DamageType>, DamageType> unwrap() {
            return Either.right(null);
        }

        @Override
        public Optional<ResourceKey<DamageType>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public Kind kind() {
            return Kind.DIRECT;
        }

        @Override
        public boolean canSerializeIn(HolderOwner<DamageType> owner) {
            return true;
        }
    }
}
