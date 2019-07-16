package pl.sda.gdajava25.NBP_API;

import pl.sda.gdajava25.NBP_API.exceptions.WrongDataFormatException;
import pl.sda.gdajava25.NBP_API.model.ExchangeRatesSeries;
import pl.sda.gdajava25.NBP_API.model.Rate;

import javax.xml.bind.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

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

        String table = loadTableFromUser(scanner);

        DataFormat dataFormat = loadDataFormatFromUser(scanner);

        String requestURL = null;
        ExchangeRatesSeries exchangeRatesSeries = null;

        Period period = Period.between(pD, kD);
        if (period.getDays() > 93) {
            int temporary = period.getDays();
            int iloscPetli = temporary / 93;
            int ostatnieDni = temporary % 93;
            LocalDate newPD = pD;
            LocalDate newKD = newPD.plusDays(93);

            for (int i = 0; i < iloscPetli; i++) {
                try {
                    requestURL = generateHTTP(currencyCodeEnum, newPD, newKD, table, dataFormat);
                    exchangeRatesSeries = unmarshalExchangeRatesSeries(requestURL);
                } catch (WrongDataFormatException e) {
                    e.getMessage();
                } catch (UnmarshalException e) {
                    System.err.println("Pusty rekord");
                }
                if (i == iloscPetli - 1) {
                    newPD = newKD;
                    newKD = newPD.plusDays(ostatnieDni);
                } else {
                    newPD = newKD;
                    newKD = newKD.plusDays(93);
                }
            }
        } else {

            try {
                requestURL = generateHTTP(currencyCodeEnum, pD, kD, table, dataFormat);
            } catch (WrongDataFormatException e) {
                e.getMessage();
            }

            try {
                exchangeRatesSeries = unmarshalExchangeRatesSeries(requestURL);
            } catch (UnmarshalException e) {
                System.err.println("Pusty rekord!");
            }
        }
        if (exchangeRatesSeries != null)
            printOperation(exchangeRatesSeries, scanner);


    }

    private static ExchangeRatesSeries unmarshalExchangeRatesSeries(String requestURL) throws UnmarshalException {
        ExchangeRatesSeries exchangeRatesSeries = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ExchangeRatesSeries.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            exchangeRatesSeries = (ExchangeRatesSeries) unmarshaller.unmarshal(new URL(requestURL));
        } catch (JAXBException | MalformedURLException e) {
            e.printStackTrace();
        }
        return exchangeRatesSeries;
    }

    private static void printOperation(ExchangeRatesSeries exchangeRatesSeries, Scanner scanner) {
        String coRobic = loadingOperationTypeFromUser(scanner);
        String table = exchangeRatesSeries.getTable();
        switch (coRobic) {
            case "A":
                if (table.equalsIgnoreCase("A"))
                    printAverageExchangeRateForTableA(exchangeRatesSeries);
                if (table.equalsIgnoreCase("C"))
                    printAverageExchangeRateForTableC(exchangeRatesSeries);
                break;
            case "B":
                if (table.equalsIgnoreCase("A"))
                    printExtremaForTableA(exchangeRatesSeries);
                if (table.equalsIgnoreCase("C"))
                    printExtremaForTableC(exchangeRatesSeries);
                break;
            case "C":
                if (table.equalsIgnoreCase("A"))
                    printMaxAndMinForTableA(exchangeRatesSeries);
                if (table.equalsIgnoreCase("C"))
                    printMaxAndMinForTableC(exchangeRatesSeries);
                break;
        }
    }

    private static String loadingOperationTypeFromUser(Scanner scanner) {
        String coRobic;
        do {
            System.out.println("Co chcesz obliczyc ? " +
                    "\na) Średni kurs." +
                    "\nb) Odchyleń maksymalnych." +
                    "\nc) Kurs maksymalny i minimalny.");

            coRobic = scanner.nextLine().toUpperCase();

            if (!coRobic.equalsIgnoreCase("C") && !coRobic.equalsIgnoreCase("A") && !coRobic.equalsIgnoreCase("B")) {
                coRobic = null;
                System.err.println("Niepoprawny typ obliczeń. Wpisz ponownie typ obliczeń.");
            }
        } while (coRobic == null);
        return coRobic;
    }

    private static String loadContenFromUrl(String requestURL) {
        String apiContent = null;
        try {
            URL url = new URL(requestURL);

            InputStream inputStream = url.openStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();
            String linia;
            while ((linia = bufferedReader.readLine()) != null) {
                builder.append(linia);
            }
            bufferedReader.close();
            apiContent = builder.toString();

        } catch (IOException e) {
            e.printStackTrace();
        }
        return apiContent;

    }

    private static DataFormat loadDataFormatFromUser(Scanner scanner) {
        DataFormat dataFormat = null;
        do {
            try {
                System.out.println("Podaj format " + Arrays.toString(DataFormat.values()) + "?");
                String dataFormatString = scanner.nextLine().toUpperCase();
                dataFormat = DataFormat.valueOf(dataFormatString);
            } catch (IllegalArgumentException iae) {
                System.err.println("Niepoprawny format, wpisz ponownie");
            }
        } while (dataFormat == null);
        return dataFormat;
    }

    private static String loadTableFromUser(Scanner scanner) {
        String table;
        do {
            System.out.println("Podaj typ tabeli: ASK/BID = C, MID - A");
            table = scanner.nextLine();

            if (!table.equalsIgnoreCase("C") && !table.equalsIgnoreCase("A")) {
                table = null;
                System.err.println("Niepoprawny typ tabeli. Wpisz ponownie typ tabeli.");
            }
        } while (table == null);
        return table;
    }

    private static String generateHTTP(CurrencyCode kodWaluty, LocalDate poczatkowyZakresDaty, LocalDate koncowyZakresDaty, String tabelaString, DataFormat dataFormat) throws WrongDataFormatException {
        if (dataFormat == DataFormat.JSON) {
            throw new WrongDataFormatException("Format daty nie obsługiwany");
        }
        return "http://api.nbp.pl/api/exchangerates/rates/" + tabelaString + "/" + kodWaluty.toString() + "/" + poczatkowyZakresDaty.format(DATE_TIME_FORMATTER) + "/" + koncowyZakresDaty.format(DATE_TIME_FORMATTER) + "/?format=" + dataFormat.toString();
    }

    private static CurrencyCode loadCurrencyCodeFromUser(Scanner scanner) {
        CurrencyCode currencyCodeEnum = null;
        do {
            try {
                System.out.println("Podaj kod waluty " + Arrays.toString(CurrencyCode.values()) + "?");
                String currencyCode = scanner.nextLine().toUpperCase();
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
                String date = "2018-" + scanner.nextLine();

                dateFromUser = LocalDate.parse(date, DATE_TIME_FORMATTER);
            } catch (DateTimeParseException dtpe) {
                System.err.println("Niepoprawny format daty, podaj ją ponownie.");
            }
        } while (dateFromUser == null);

        return dateFromUser;
    }

    private static void printAverageExchangeRateForTableA(ExchangeRatesSeries exchangeRatesSeries) {
        double avrMid = finAvrgForMid(exchangeRatesSeries);

        System.out.println("Średni kurs waluty "
                + exchangeRatesSeries.getCode()
                + " : " + avrMid + " zł.");
    }

    private static void printAverageExchangeRateForTableC(ExchangeRatesSeries exchangeRatesSeries) {
        double avrBid = finAvrgForBid(exchangeRatesSeries);
        double avrAsk = finAvrgForAsk(exchangeRatesSeries);

        System.out.println("Średnie kursy waluty "
                + exchangeRatesSeries.getCode()
                + ".\nŚrednia sprzedaży : " + avrBid + " zł."
                + "\nŚrednia kupna : " + avrAsk + " zł.");
    }

    private static void printExtremaForTableA(ExchangeRatesSeries exchangeRatesSeries) {
        Double max = findMaxForMid(exchangeRatesSeries);
        Double min = findMinForMid(exchangeRatesSeries);
        System.out.println("Odchylenia waluty " +
                exchangeRatesSeries.getCode() +
                " wynosi : " + (max - min) + " zł.");
    }

    private static void printExtremaForTableC(ExchangeRatesSeries exchangeRatesSeries) {
        Double maxAsk = findMaxForAsk(exchangeRatesSeries);
        Double minAsk = findMinForAsk(exchangeRatesSeries);
        double extremaAsk = maxAsk - minAsk;
        Double maxBid = findMaxForBid(exchangeRatesSeries);
        Double minBid = findMinForBid(exchangeRatesSeries);
        double extremaBid = maxBid - minBid;
        System.out.println("Odchylenia waluty "
                + exchangeRatesSeries.getCode()
                + ".\nSprzedaży : " + extremaBid + " zł."
                + "\nKupna : " + extremaAsk + " zł.");
    }

    private static void printMaxAndMinForTableA(ExchangeRatesSeries exchangeRatesSeries) {
        Double maxMid = findMaxForMid(exchangeRatesSeries);
        Double minMid = findMinForMid(exchangeRatesSeries);
        System.out.println("Wartości waluty " + exchangeRatesSeries.getCode()
                + "\n Max : " + maxMid + " zł."
                + "\n Min : " + minMid + " zł.");

    }

    private static void printMaxAndMinForTableC(ExchangeRatesSeries exchangeRatesSeries) {
        Double maxAsk = findMaxForAsk(exchangeRatesSeries);
        Double minAsk = findMinForAsk(exchangeRatesSeries);
        Double maxBid = findMaxForBid(exchangeRatesSeries);
        Double minBid = findMinForBid(exchangeRatesSeries);
        System.out.println("Wartości waluty " + exchangeRatesSeries.getCode()
                + "\nSPRZEDAŻ"
                + "\n Max : " + maxBid + " zł."
                + "\n Min : " + minBid + " zł."
                + "\nKUPNO"
                + "\n Max : " + maxAsk + " zł."
                + "\n Min : " + minAsk + " zł.");
    }

    private static Double findMaxForBid(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> maxRate = exchangeRatesSeries.getRates()
                .stream().max(Comparator.comparingDouble(
                        Rate::getBid
                ));
        return maxRate.map(Rate::getBid).orElse(null);
    }

    private static Double findMaxForAsk(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> maxRate = exchangeRatesSeries.getRates()
                .stream().max(Comparator.comparingDouble(
                        Rate::getAsk
                ));
        return maxRate.map(Rate::getAsk).orElse(null);
    }

    private static Double findMaxForMid(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> maxRate = exchangeRatesSeries.getRates()
                .stream().max(Comparator.comparingDouble(
                        Rate::getMid
                ));
        return maxRate.map(Rate::getMid).orElse(null);
    }

    private static Double findMinForBid(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> minRate = exchangeRatesSeries.getRates()
                .stream().min(Comparator.comparingDouble(
                        Rate::getBid
                ));
        return minRate.map(Rate::getBid).orElse(null);
    }

    private static Double findMinForAsk(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> minRate = exchangeRatesSeries.getRates()
                .stream().min(Comparator.comparingDouble(
                        Rate::getAsk
                ));
        return minRate.map(Rate::getAsk).orElse(null);
    }

    private static Double findMinForMid(ExchangeRatesSeries exchangeRatesSeries) {
        Optional<Rate> minRate = exchangeRatesSeries.getRates()
                .stream().min(Comparator.comparingDouble(
                        Rate::getMid
                ));
        return minRate.map(Rate::getMid).orElse(null);
    }

    private static Double finAvrgForBid(ExchangeRatesSeries exchangeRatesSeries) {
        OptionalDouble
                avrBid = exchangeRatesSeries.getRates()
                .stream()
                .mapToDouble(Rate::getBid)
                .average();
        if (avrBid.isPresent())
            return avrBid.getAsDouble();

        return null;
    }

    private static Double finAvrgForAsk(ExchangeRatesSeries exchangeRatesSeries) {
        OptionalDouble avrAsk = exchangeRatesSeries.getRates()
                .stream()
                .mapToDouble(Rate::getAsk)
                .average();
        if (avrAsk.isPresent())
            return avrAsk.getAsDouble();

        return null;
    }

    private static Double finAvrgForMid(ExchangeRatesSeries exchangeRatesSeries) {
        OptionalDouble
                avrMid = exchangeRatesSeries.getRates()
                .stream()
                .mapToDouble(Rate::getMid)
                .average();
        if (avrMid.isPresent())
            return avrMid.getAsDouble();
        return null;
    }
}
