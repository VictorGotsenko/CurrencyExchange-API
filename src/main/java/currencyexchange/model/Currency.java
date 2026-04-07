package currencyexchange.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Currency extends BaseModel{

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
}
