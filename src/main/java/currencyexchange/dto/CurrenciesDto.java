package currencyexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CurrenciesDto {
    List<CurrencyDto> currencyDtos;
}
