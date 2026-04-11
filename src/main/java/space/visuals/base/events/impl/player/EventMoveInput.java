package space.visuals.base.events.impl.player;


import net.minecraft.client.input.Input;
import net.minecraft.util.PlayerInput;
import space.visuals.base.events.callables.EventCancellable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class EventMoveInput extends EventCancellable {
    private PlayerInput input;
    private float forward, strafe;
}