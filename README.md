# CurrencyExchange-API
### REST API for describing currencies and exchange rates
 - Allows you to view and edit lists of currencies and exchange rates,
and calculate the conversion of arbitrary amounts from one currency to another.

### API Endpoints:
#### Currencies
- GET /currencies — _list of all currencies_   
- GET /currency/{code} — _receiving currency by code_
- POST /currencies — _create new currency_

#### Exchange rates
- GET /exchangeRates — _list of all exchange rates_
- GET /exchangeRate/{pair} — _getting the exchange rate for a currency pair_ 
- POST /exchangeRates — _create a new exchange rate_
- PATCH /exchangeRate/{pair} — _exchange rate update_

#### Conversion
- GET /exchange?from={code}&to={code}&amount=10 — _Calculation of the conversion of an amount from one currency to another_

### How to use:
 * System requirements: 
   Java v.21
   Gradle v.8.5 or above
   Apache Tomcat v.11 
 * Clone the project locally and run:
```shell
make build-run
```
  * Open http://localhost:8080

Specification is [avalable](https://zhukovsd.github.io/java-backend-learning-course/projects/currency-exchange/)

About [Java-Backend-Learning-course](https://zhukovsd.github.io/java-backend-learning-course/) by Sergey Zhukov
