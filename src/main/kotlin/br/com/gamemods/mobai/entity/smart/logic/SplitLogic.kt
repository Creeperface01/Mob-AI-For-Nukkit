package br.com.gamemods.mobai.entity.smart.logic

import br.com.gamemods.mobai.delegators.custom.customField
import br.com.gamemods.mobai.delegators.reflection.intField
import br.com.gamemods.mobai.entity.smart.EntityAI
import br.com.gamemods.mobai.entity.smart.EntityProperties
import br.com.gamemods.mobai.entity.smart.SmartEntity
import cn.nukkit.entity.Attribute
import cn.nukkit.entity.Entity
import cn.nukkit.entity.EntityType
import cn.nukkit.entity.impl.BaseEntity
import cn.nukkit.level.Level
import cn.nukkit.math.Vector3f
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface SplitLogic: EntityProperties {
    val ai: EntityAI<*>

    fun setMaxHealth(maxHealth: Int) {
        this.maxHealth = maxHealth.toFloat()
    }

    fun updateAttribute(id: Int): Attribute?

    fun randomParticlePos(
        xScale: Double = 1.0,
        yScale: Double = random.nextDouble(),
        zScale: Double = 1.0,
        xOffset: Double = 0.0,
        yOffset: Double = 0.0,
        zOffset: Double = 0.0
    ): Vector3f {
        base {
            val random = random
            val width = width
            return Vector3f(
                x + xOffset + width * (2.0 * random.nextDouble() - 1.0) * xScale,
                y + yOffset + height * yScale,
                z + zOffset + width * (2.0 * random.nextDouble() - 1.0) * zScale
            )
        }
    }

    fun attribute(id: Int) = attributes.computeIfAbsent(id, Attribute::getAttribute)
}

inline val SplitLogic.entity get() = this as Entity
inline val SplitLogic.base get() = this as BaseEntity
inline val SplitLogic.smart get() = this as SmartEntity

inline val SplitLogic.level: Level get() = base.level
inline val SplitLogic.type: EntityType<*> get() = entity.type

private var BaseEntity.maxHealthField by intField<BaseEntity>("maxHealth")

var SplitLogic.maxHealth by customField(20F, transforming = { thisRef, _, _, newValue ->
        thisRef.smart {
            ifNotOnInit {
                val updated = recalculateAttribute(maxHealthAttribute)
                if (updated != newValue) {
                    base.maxHealthField = updated.toInt()
                    return@customField updated
                }
            }
            base.maxHealthField = newValue.toInt()
        }
        newValue
    })

inline fun SplitLogic.smart(operation: SmartEntity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    smart.apply(operation)
}

inline fun SplitLogic.base(operation: BaseEntity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    base.apply(operation)
}

inline fun SplitLogic.entity(operation: Entity.() -> Unit) {
    contract {
        callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
    }
    entity.apply(operation)
}

inline fun <R> SplitLogic.runSmart(operation: SmartEntity.() -> R) = smart.run(operation)
inline fun <R> SplitLogic.runBase(operation: BaseEntity.() -> R) = base.run(operation)
inline fun <R> SplitLogic.runEntity(operation: Entity.() -> R) = entity.run(operation)


// The way Nukkit designed entities makes this get called before this object is fully setup,
// causing NPE on instantiation
inline fun SplitLogic.ifNotOnInit(action: () -> Unit) {
    try {
        definitions.hashCode()
    } catch (_: NullPointerException) {
        return
    }

    action()
}

inline fun SplitLogic.ifOnInit(action: () -> Unit) {
    try {
        definitions.hashCode()
        return
    } catch (_: NullPointerException) {
    }

    action()
}
