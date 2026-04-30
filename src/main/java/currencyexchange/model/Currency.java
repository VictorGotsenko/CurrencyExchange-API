package currencyexchange.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Currency extends BaseModel {

    private int id;
    private String name;
    private String code;
    private String sign;
    private LocalDateTime createdAt;

    public Currency(String name, String code, String sign) {
        this.name = name;
        this.code = code;
        this.sign = sign;
    }

    @Override
    public String toString() {
        // work
        return "{\"id\":" + id + ",\"name\":\"" + name + "\",\"code\":\"" + code + "\",\"sign\":\"" + sign + "\"}";
    }
}
