package space.visuals.base.events.impl.input;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import space.visuals.base.events.callables.EventCancellable;

@Getter
@AllArgsConstructor
public final class EventChatSend extends EventCancellable {
    @Setter
    private String message;
}