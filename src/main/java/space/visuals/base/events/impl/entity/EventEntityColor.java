package space.visuals.base.events.impl.entity;

import lombok.*;
import space.visuals.base.events.callables.EventCancellable;
@Getter
@Setter
@AllArgsConstructor
public class EventEntityColor extends EventCancellable {
    private int color;
}
