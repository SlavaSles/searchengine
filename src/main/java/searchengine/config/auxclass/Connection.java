package searchengine.config.auxclass;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "connection-settings")
public class Connection {
    ArrayList<Agent> userAgents;
    String referrer;
}
