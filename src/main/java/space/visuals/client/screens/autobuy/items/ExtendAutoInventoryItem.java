package space.visuals.client.screens.autobuy.items;

import space.visuals.base.autobuy.item.ItemBuy;
import space.visuals.client.screens.menu.settings.api.MenuSetting;

import java.util.List;

public abstract class ExtendAutoInventoryItem extends AutoInventoryItem {
    public ExtendAutoInventoryItem(ItemBuy itemBuy) {
        super(itemBuy);
    }
    public abstract List<MenuSetting> getEnchants();

}
