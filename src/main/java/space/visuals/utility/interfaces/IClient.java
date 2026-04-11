package space.visuals.utility.interfaces;

import space.visuals.Zenith;
import space.visuals.base.rotation.AimManager;
import space.visuals.base.rotation.RotationManager;
import space.visuals.base.rotation.deeplearnig.DeepLearningManager;

public interface IClient extends IWindow{
    Zenith zenith = Zenith.getInstance();
    DeepLearningManager deepLearningManager = Zenith.getInstance().getDeepLearningManager();
    RotationManager rotationManager = Zenith.getInstance().getRotationManager();
    AimManager aimManager = rotationManager.getAimManager();

}
