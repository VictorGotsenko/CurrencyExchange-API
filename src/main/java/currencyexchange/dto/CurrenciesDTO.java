package currencyexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CurrenciesDTO {
    List<CurrencyDTO> listCurrencyDTO;
}
