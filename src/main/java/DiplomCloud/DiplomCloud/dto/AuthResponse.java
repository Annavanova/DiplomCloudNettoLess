package DiplomCloud.DiplomCloud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public class AuthResponse {
    @JsonProperty("auth-token")
    private String authToken;

    public AuthResponse(String token) {
    }

    public String getAuthToken() {
        return authToken;
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}
