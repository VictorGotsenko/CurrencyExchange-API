package currencyexchange.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class Currency {

    // LONG для ID подходит лучше. Но в SQLite нет long
    // for details https://www.sqlite.org/datatype3.html#storage_classes_and_datatypes
    private int id;
    private String name;
    private String code;
    private String sign;


    public Currency(String name, String code, String sign) {
        this.name = name;
        this.code = code;
        this.sign = sign;
    }
}
