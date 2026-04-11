package space.visuals.base.events.impl.server;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.component.Component;
import net.minecraft.text.Text;
import space.visuals.base.events.callables.EventCancellable;
import space.visuals.utility.render.display.base.CustomDrawContext;


@Getter
@Setter
@AllArgsConstructor
public class EventChatReceive extends EventCancellable {

    private Text message;


}