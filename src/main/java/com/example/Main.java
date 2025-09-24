package com.example;

import com.example.api.ElpriserAPI;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        ElpriserAPI elpriser = new ElpriserAPI();
        LocalDate idag = LocalDate.now();
        ElpriserAPI.Prisklass zon = ElpriserAPI.Prisklass.SE1;

        List<ElpriserAPI.Elpris> priser = elpriser.getPriser(idag, zon);

        ElpriserAPI.Elpris billigast = priser.get(0); // startvärde
        double total = 0.0;


        // Loop för att skriva ut timpriser
        for (ElpriserAPI.Elpris p : priser) {

            if (p.sekPerKWh() < billigast.sekPerKWh()) {
                billigast = p;
            }


            total += p.sekPerKWh();


            System.out.printf(p.timeStart().toLocalTime() + " - %.2f öre/kWh",
                    p.sekPerKWh());
            System.out.print(" || ");
            System.out.print(p.timeStart().toLocalTime() + " - " + p.eurPerKWh() + " euro/kWh");
            System.out.println();
        }

        System.out.println();
        System.out.printf("Billigaste timmen: %s → %.2f öre/kWh%n",
                billigast.timeStart().toLocalTime(),
                billigast.sekPerKWh());

        double medel = total/ priser.size();
        System.out.printf("Medelpriset är %.2f öre/kWh%n",
                medel);

    }
}
