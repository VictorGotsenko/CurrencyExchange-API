package currencyexchange.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CurrencyDto {
    private int id;
    private String name;
    private String code;
    private String sign;
}
