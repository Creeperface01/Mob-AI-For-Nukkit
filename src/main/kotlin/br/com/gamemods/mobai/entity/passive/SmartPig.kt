package br.com.gamemods.mobai.entity.passive

import br.com.gamemods.mobai.ai.goal.*
import br.com.gamemods.mobai.entity.Flag
import br.com.gamemods.mobai.entity.lootingLevel
import br.com.gamemods.mobai.entity.smart.EntityAI
import br.com.gamemods.mobai.entity.smart.EntityProperties
import br.com.gamemods.mobai.entity.smart.EntityPropertyStorage
import br.com.gamemods.mobai.entity.smart.SmartAnimal
import br.com.gamemods.mobai.entity.smart.logic.Breedable
import br.com.gamemods.mobai.entity.smart.logic.ifOnInit
import br.com.gamemods.mobai.level.SimpleSound
import cn.nukkit.entity.Entity
import cn.nukkit.entity.EntityType
import cn.nukkit.entity.EntityTypes
import cn.nukkit.entity.data.EntityFlag
import cn.nukkit.entity.impl.passive.EntityPig
import cn.nukkit.entity.misc.LightningBolt
import cn.nukkit.entity.passive.Pig
import cn.nukkit.event.entity.EntityDamageEvent
import cn.nukkit.item.Item
import cn.nukkit.item.ItemIds.*
import cn.nukkit.level.Sound
import cn.nukkit.level.chunk.Chunk
import cn.nukkit.math.Vector3f
import cn.nukkit.nbt.tag.CompoundTag
import cn.nukkit.network.protocol.LevelSoundEventPacket
import cn.nukkit.player.Player
import cn.nukkit.registry.EntityRegistry

class SmartPig(type: EntityType<Pig>, chunk: Chunk, tag: CompoundTag)
    : EntityPig(type, chunk, tag), SmartAnimal, Breedable,
    EntityProperties by EntityPropertyStorage(tag,
        expDrop = 1..3,
        simpleStepSound = SimpleSound(Sound.MOB_PIG_STEP)
    ) {

    override val ai = EntityAI(this).apply {
        goalSelector.add(0, SwimGoal(this))
        goalSelector.add(1, EscapeDangerGoal(this, 1.25))
        goalSelector.add(3, AnimalMateGoal(this, 1.0))
        goalSelector.add(4, TemptGoal(this, 1.2, CARROT_ON_A_STICK))
        goalSelector.add(4, TemptGoal(this, 1.2, ::isBreedingItem))
        goalSelector.add(5, FollowParentGoal(this, 1.1))
        goalSelector.add(6, WanderAroundFarGoal(this, 1.0))
        goalSelector.add(7, LookAtEntityGoal(this, Player::class, 6.0))
        goalSelector.add(8, LookAroundGoal(this))
    }

    var isSaddled by Flag(EntityFlag.SADDLED)

    init { init() }

    override fun onStruckByLightning(lightningBolt: LightningBolt?) {
        val zombie = EntityRegistry.get().newEntity(EntityTypes.ZOMBIE_PIGMAN, chunk,
            Entity.getDefaultNBT(position, motion, yaw.toFloat(), pitch.toFloat())
        ) ?: return super.onStruckByLightning(lightningBolt)
        if (isNameTagVisible) {
            zombie.setFlag(EntityFlag.CAN_SHOW_NAMETAG, true)
        }
        if (isNameTagAlwaysVisible) {
            zombie.setFlag(EntityFlag.ALWAYS_SHOW_NAMETAG, true)
        }
        if (hasCustomName()) {
            zombie.nameTag = nameTag
        }
        close()
        zombie.spawnToAll()
    }

    override fun isBreedingItem(item: Item) = super<EntityPig>.isBreedingItem(item)

    override fun getDrops(): Array<Item> {
        ifOnInit { return emptyArray() }
        val random = random
        val looting = attacker?.lootingLevel ?: 0
        val drops = mutableListOf<Item>(Item.get(
            if (isOnFire) COOKED_PORKCHOP else PORKCHOP,
            0,
            1 + random.nextInt(3 + looting)
        ))
        if (isSaddled) {
            drops += Item.get(SADDLE)
        }
        return drops.toTypedArray()
    }

    override fun onInteract(player: Player, item: Item, clickedPos: Vector3f): Boolean {
        if (item.id == SADDLE) {
            if (!isSaddled) {
                isSaddled = true
                item.decrementCount()
                level.addLevelSoundEvent(position, LevelSoundEventPacket.SOUND_SADDLE, -1, type)
                return true
            }
            return false
        }
        return super<Breedable>.onInteract(player, item, clickedPos)
                || super<EntityPig>.onInteract(player, item, clickedPos)
    }

    override fun setMaxHealth(maxHealth: Int) = super<SmartAnimal>.setMaxHealth(maxHealth)
    override fun updateMovement() = super<SmartAnimal>.updateMovement()
    override fun onUpdate(currentTick: Int) = super<SmartAnimal>.onUpdate(currentTick)
    override fun attack(source: EntityDamageEvent) = super<EntityPig>.attack(source) && super<SmartAnimal>.attack(source)

    override fun saveNBT() {
        super<EntityPig>.saveNBT()
        super<SmartAnimal>.saveNBT()
    }

    override fun kill() {
        super<EntityPig>.kill()
        super<SmartAnimal>.kill()
    }
}
