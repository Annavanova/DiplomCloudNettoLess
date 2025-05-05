package DiplomCloud.DiplomCloud;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;


@RestController
@RequestMapping("/test")
public class TestController {
    @Value("${jwt.secret}")
    private String secret;

    @GetMapping
    public String testConfig() {
        return "JWT Secret: " + secret;
    }
}
