package com.example;

import com.example.api.ElpriserAPI;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriser = new ElpriserAPI();
        ElpriserAPI.Prisklass zon = null;
        LocalDate datum = null;
        boolean sorted = false;
        int chargingDuration = 0;

        // Om argument saknas eller --help
        if (args.length == 0 || (args.length > 0 && args[0].equals("--help"))) {
            System.out.println("Usage: java com.example.Main [OPTIONS]");
            System.out.println("  --zone SE1|SE2|SE3|SE4");
            System.out.println("  --date YYYY-MM-DD");
            System.out.println("  --sorted");
            System.out.println("  --charging 2h|4h|8h");
            System.out.println("  --help");
            return;
        }


        // Parse argument
        int index = 0;
        while (index < args.length) {
            if (args[index].equals("--zone") && index + 1 < args.length) {
                index++;
                if (args[index].equals("SE1")) zon = ElpriserAPI.Prisklass.SE1;
                else if (args[index].equals("SE2")) zon = ElpriserAPI.Prisklass.SE2;
                else if (args[index].equals("SE3")) zon = ElpriserAPI.Prisklass.SE3;
                else if (args[index].equals("SE4")) zon = ElpriserAPI.Prisklass.SE4;
                else {
                    System.out.println("Ogiltig zon: " + args[index]);
                    return;
                }
            }
            else if (args[index].equals("--date") && index + 1 < args.length) {
                index++;
                try {
                    datum = LocalDate.parse(args[index]);
                } catch (Exception e) {
                    System.out.println("Ogiltigt datum");
                    return;
                }
            }
            else if (args[index].equals("--sorted")) {
                sorted = true;
            }
            else if (args[index].equals("--charging") && index + 1 < args.length) {
                index++;
                try {
                    chargingDuration = Integer.parseInt(args[index].replace("h", ""));
                    if (chargingDuration != 2 && chargingDuration != 4 && chargingDuration != 8) {
                        System.out.println("Ogiltig laddningstid (använd 2h, 4h, 8h)");
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("Ogiltig laddningstid (använd 2h, 4h, 8h)");
                    return;
                }
            }
            index++;
        }

        // Om ingen zon angiven
        if (zon == null) {
            System.out.println("Error: --zone is required");
            return;
        }

        // Dagens elpriser
        LocalDate idag = datum != null ? datum : LocalDate.now();
        List<ElpriserAPI.Elpris> priser = elpriser.getPriser(idag, zon);

        if (priser == null || priser.isEmpty()) {
            System.out.println("Ingen data tillgänglig");
            return;
        }

        // Morgondagens elpriser
        LocalDate imorgon = idag.plusDays(1);
        List<ElpriserAPI.Elpris> priserImorgon = elpriser.getPriser(imorgon, zon);
        if (priserImorgon != null && !priserImorgon.isEmpty()) {
            priser.addAll(priserImorgon);
        }

        // Gruppera timpriser
        priser = grupperaTimmar(priser);

        // Skriv ut priser
        printPriser(sorted, priser);

        // Hitta och skriv ut statistik
        visaStatistik(priser);

        // Billigaste laddningsfönstret
        if (chargingDuration > 0) {
            billigasteLaddningsfönster(priser, chargingDuration);
        }
    }


    private static void visaStatistik(List<ElpriserAPI.Elpris> priser) {

        // Hitta och skriv ut: billigast och dyrast timmen samt medelpriset.
        ElpriserAPI.Elpris billigasteTimmen = priser.get(0);
        ElpriserAPI.Elpris dyrasteTimmen = priser.get(0);
        double total = 0.0;

        for (ElpriserAPI.Elpris p : priser) {
            if (p.sekPerKWh() < billigasteTimmen.sekPerKWh()) {
                billigasteTimmen = p;
            }
            if (p.sekPerKWh() > dyrasteTimmen.sekPerKWh()) {
                dyrasteTimmen = p;
            }
            total += p.sekPerKWh();
        }

        System.out.printf(Locale.forLanguageTag("sv-SE"),
                "Lägsta pris: %02d-%02d %.2f öre%n",
                billigasteTimmen.timeStart().getHour(),
                billigasteTimmen.timeEnd().getHour(),
                tillÖre(billigasteTimmen.sekPerKWh()));

        System.out.printf(Locale.forLanguageTag("sv-SE"),
                "Högsta pris: %02d-%02d %.2f öre%n",
                dyrasteTimmen.timeStart().getHour(),
                dyrasteTimmen.timeEnd().getHour(),
                tillÖre(dyrasteTimmen.sekPerKWh()));

        double medel = total / priser.size();
        System.out.printf(Locale.forLanguageTag("sv-SE"),
                "Medelpris: %.2f öre%n", tillÖre(medel));

        System.out.println();
    }

    private static void printPriser(boolean sorted, List<ElpriserAPI.Elpris> priser) {
        LocalDate currentDate = null;
          if (sorted) {
            List<ElpriserAPI.Elpris> sorterad = new ArrayList<>(priser);
            sorterad.sort(Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());

            for (ElpriserAPI.Elpris p : sorterad) {

                LocalDate prisDate = p.timeStart().toLocalDate();
                if (currentDate == null || !prisDate.equals(currentDate)) {
                    System.out.println("\n--- " + prisDate + " ---");
                    currentDate = prisDate;
                }

                int startTimme = p.timeStart().getHour();
                int slutTimme = p.timeEnd().getHour();
                System.out.printf(Locale.forLanguageTag("sv-SE"),
                        "%02d-%02d %.2f öre%n", startTimme, slutTimme, tillÖre(p.sekPerKWh()));
            }
          }
          else {
            for (ElpriserAPI.Elpris p : priser) {

                LocalDate prisDate = p.timeStart().toLocalDate();
                if (currentDate == null || !prisDate.equals(currentDate)) {
                    System.out.println("\n--- " + prisDate + " ---");
                    currentDate = prisDate;
                }

                int startTimme = p.timeStart().getHour();
                int slutTimme = p.timeEnd().getHour();
                System.out.printf(Locale.forLanguageTag("sv-SE"),
                        "%02d-%02d %.2f öre%n", startTimme, slutTimme, tillÖre(p.sekPerKWh()));
            }
        }
    }

    private static double tillÖre(double sek) {
        return sek * 100;
    }

    private static void billigasteLaddningsfönster(List<ElpriserAPI.Elpris> priser, int windowSize) {
        double billigastePris = Double.MAX_VALUE;
        List<ElpriserAPI.Elpris> cheapestWindow = new ArrayList<>();

        for (int i = 0; i <= priser.size() - windowSize; i++) {
            double windowSum = 0;
            for (int j = 0; j < windowSize; j++) {
                windowSum += priser.get(i + j).sekPerKWh();
            }

            if (windowSum < billigastePris) {
                billigastePris = windowSum;
                cheapestWindow.clear();
                for (int j = 0; j < windowSize; j++) {
                    cheapestWindow.add(priser.get(i + j));
                }
            }
        }

        if (!cheapestWindow.isEmpty()) {
            System.out.println("Påbörja laddning kl " +
                    cheapestWindow.get(0).timeStart().toLocalTime());

            double medelPris = billigastePris / windowSize;
            System.out.printf(Locale.forLanguageTag("sv-SE"),
                    "Medelpris för fönster: %.2f öre%n", tillÖre(medelPris));
        }
    }

    private static List<ElpriserAPI.Elpris> grupperaTimmar(List<ElpriserAPI.Elpris> priser) {
        if (priser.isEmpty()) return priser;

        Duration d = Duration.between(priser.get(0).timeStart(), priser.get(0).timeEnd());
        if (d.toHours() >= 1) {
            return priser;
        }

        List<ElpriserAPI.Elpris> hourly = new ArrayList<>();
        List<ElpriserAPI.Elpris> currentHour = new ArrayList<>();
        LocalDateTime lastHour = null;

        for (ElpriserAPI.Elpris p : priser) {
            LocalDateTime hourKey = p.timeStart().truncatedTo(ChronoUnit.HOURS).toLocalDateTime();

            if (lastHour != null && !hourKey.equals(lastHour)) {
                hourly.add(medelvärdeTimme(currentHour));
                currentHour.clear();
            }

            currentHour.add(p);
            lastHour = hourKey;
        }

        if (!currentHour.isEmpty()) {
            hourly.add(medelvärdeTimme(currentHour));
        }

        return hourly;
    }

    private static ElpriserAPI.Elpris medelvärdeTimme(List<ElpriserAPI.Elpris> quarters) {
        double sum = 0;
        for (ElpriserAPI.Elpris p : quarters) {
            sum += p.sekPerKWh();
        }
        double average = sum / quarters.size();

        ElpriserAPI.Elpris first = quarters.get(0);
        ElpriserAPI.Elpris last = quarters.get(quarters.size() - 1);

        return new ElpriserAPI.Elpris(
                average,
                first.eurPerKWh(),
                first.exr(),
                first.timeStart(),
                last.timeEnd()
        );
    }
}
