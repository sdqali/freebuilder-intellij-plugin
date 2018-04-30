package in.sdqali.intellij.freebuilder;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;

public class Notifier {
  public void info(String title, String message) {
    Notifications.Bus.notify(new Notification("FreeBuilder Plugin", title,
        message, NotificationType.INFORMATION));
  }

  public void warn(String title, String message) {
    Notifications.Bus.notify(new Notification("FreeBuilder Plugin", title,
        message, NotificationType.WARNING));
  }
}
