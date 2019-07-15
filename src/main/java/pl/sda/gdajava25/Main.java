package pl.sda.gdajava25;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Zadanie NBP API:
 * <p>
 * Stwórz main'a w którym pytasz użytkownika o 4 parametry, są nimi:
 * - kod waluty
 * - data początku zakresu
 * - data końca zakresu  (zweryfikuj że data końca jest
 * późniejsza niż początku zakresu)
 * - rodzaj tabeli
 * - jeśli użytkownik wybierze ASK/BID, chodzi o tabelę C
 * - jeśli użytkownik wybierze MID, chodzi o tabelę A/B
 * (możemy przyjąć że będzie to zawsze tabela A, przy wybraniu
 * drugiej opcji).
 * <p>
 * <p>
 * Jako wynik aplikacji wypisz System.out.println() zapytanie które
 * należy wywołać na API by otrzymać wynik zgodny z danymi które
 * wprowadził użytkownik.
 * <p>
 * Przetestuj działanie aplikacji - sprawdź czy zapytanie (skopiuj je do
 * przeglądarki) zwraca poprawne wyniki.
 **/

public class Main {
    private final static DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Witaj w konsolowej aplikacji do pobierania kursów walut z API NBP");

        CurrencyCode currencyCodeEnum = loadCurrencyCodeFromUser(scanner);

        LocalDate pD = loadDateFromUser(scanner);
        LocalDate kD;
        do {
            kD = loadDateFromUser(scanner);
        } while (kD.isBefore(pD));

        String table = getTableFromUser(scanner);

        System.out.println(generateHTTP(currencyCodeEnum,pD,kD,table));
    }

    private static String getTableFromUser(Scanner scanner) {
        String table;
        do {
            System.out.println("Podaj typ tabeli: ASK/BID = C, MID - A/B");
            table = scanner.nextLine();

            if (!table.equalsIgnoreCase("C") && !table.equalsIgnoreCase("A") && !table.equalsIgnoreCase("B")) {
                table = null;
                System.err.println("Niepoprawny typ tabeli. Wpisz ponownie typ tabeli.");
            }
        } while (table == null);
        return table;
    }


    private static String generateHTTP(CurrencyCode kodWaluty, LocalDate poczatkowyZakresDaty, LocalDate koncowyZakresDaty, String tabelaString) {
        return "http://api.nbp.pl/api/exchangerates/rates/" + tabelaString + "/" + kodWaluty.toString() + "/" + poczatkowyZakresDaty.format(DATE_TIME_FORMATTER) + "/" + koncowyZakresDaty.format(DATE_TIME_FORMATTER) + "/";
    }


    private static CurrencyCode loadCurrencyCodeFromUser(Scanner scanner) {
        CurrencyCode currencyCodeEnum = null;
        do {
            try {
                System.out.println("Podaj kod waluty " + Arrays.toString(CurrencyCode.values()) + "?");
                String currencyCode = scanner.nextLine();
                currencyCodeEnum = CurrencyCode.valueOf(currencyCode);
            } catch (IllegalArgumentException iae) {
                System.err.println("Niepoprawny kurs waluty, podaj go ponownie.");
            }
        } while (currencyCodeEnum == null);
        return currencyCodeEnum;
    }

    private static LocalDate loadDateFromUser(Scanner scanner) {
        LocalDate dateFromUser = null;
        do {
            try {
                System.out.println("Podaj datę [MM-dd]: ");
                String date = "2018-"+scanner.nextLine();

                dateFromUser = LocalDate.parse(date, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException dtpe) {
                System.err.println("Niepoprawny format daty, podaj ją ponownie.");
            }
        } while (dateFromUser == null);

        return dateFromUser;
    }
}
