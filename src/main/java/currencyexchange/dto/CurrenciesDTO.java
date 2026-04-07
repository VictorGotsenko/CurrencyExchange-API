package currencyexchange.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class CurrenciesDTO {
    /*
    здесь располагается список валют
    который затем сереализуется в JSON
     */

    List<CurrencyDTO> listCurrencyDTO;
}
