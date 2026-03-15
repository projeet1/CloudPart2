package acp.cw2.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AcpConfig {

    @Value("${acp.sid}")
    private String sid;

    public String getSid() {
        return sid;
    }
}