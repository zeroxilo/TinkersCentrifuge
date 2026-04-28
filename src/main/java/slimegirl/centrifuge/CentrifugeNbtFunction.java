package slimegirl.centrifuge;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemConditionalFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class CentrifugeNbtFunction extends LootItemConditionalFunction {
    protected CentrifugeNbtFunction(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ItemStack run(ItemStack stack, LootContext context) {
        BlockEntity be = context.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        if (be instanceof MultiFluidTankBlockEntify tank) {
            CompoundTag nbt = stack.getOrCreateTag();
            ListTag allTanks = tank.tanks.writeToNBT(new CompoundTag()).getList("tanks", ListTag.TAG_COMPOUND);
            if (!allTanks.isEmpty()) {
                nbt.put("tank", allTanks.get(0));
                if (allTanks.size() > 1) {
                    allTanks.remove(0);
                    nbt.put("tanks", allTanks);
                }
            }
            stack.setTag(nbt);
        }
        return stack;
    }

    @Override
    public LootItemFunctionType getType() {
        return TinkersCentrifuge.CENTRIFUGE_NBT_FUNCTION.get();
    }

    // 一个简单的 Serializer 内部类
    public static class CentrifugeSerializer extends LootItemConditionalFunction.Serializer<CentrifugeNbtFunction> {
        @Override
        public CentrifugeNbtFunction deserialize(JsonObject json, JsonDeserializationContext context, LootItemCondition[] conditions) {
            return new CentrifugeNbtFunction(conditions);
        }
    }
}
