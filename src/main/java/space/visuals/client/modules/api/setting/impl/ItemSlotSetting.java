package space.visuals.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import space.visuals.client.modules.api.setting.Setting;

import java.util.function.Supplier;

/**
 * Настройка-слот: хранит один ItemStack, выбранный из инвентаря.
 * В меню рендерится как кнопка с иконкой предмета.
 * Клик → открывается инвентарь → выбираешь предмет → возвращаешься в меню.
 */
public class ItemSlotSetting extends Setting {

    @Getter
    private ItemStack stack = ItemStack.EMPTY;

    /** Callback вызывается когда предмет выбран из инвентаря */
    private Runnable onPick;

    public ItemSlotSetting(String name) {
        super(name);
    }

    public ItemSlotSetting(String name, Supplier<Boolean> visible) {
        super(name);
        setVisible(visible);
    }

    public void setStack(ItemStack stack) {
        this.stack = stack != null ? stack.copy() : ItemStack.EMPTY;
        if (onPick != null) onPick.run();
    }

    public void setOnPick(Runnable onPick) {
        this.onPick = onPick;
    }

    public boolean isEmpty() {
        return stack == null || stack.isEmpty();
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        if (!isEmpty()) {
            String id = Registries.ITEM.getId(stack.getItem()).toString();
            propertiesObject.addProperty(name, id);
        } else {
            propertiesObject.addProperty(name, "");
        }
    }

    @Override
    public void load(JsonObject propertiesObject) {
        if (!propertiesObject.has(name)) return;
        String id = propertiesObject.get(name).getAsString();
        if (id.isEmpty()) { stack = ItemStack.EMPTY; return; }
        try {
            var item = Registries.ITEM.get(Identifier.of(id));
            stack = new ItemStack(item);
        } catch (Exception ignored) {
            stack = ItemStack.EMPTY;
        }
    }
}
