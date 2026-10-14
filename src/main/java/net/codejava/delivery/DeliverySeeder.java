package net.codejava.delivery;

import net.codejava.common.seed.SeedFileReader;
import net.codejava.delivery.model.DeliveryArea;
import net.codejava.delivery.model.Driver;
import net.codejava.delivery.repository.DeliveryAreaRepository;
import net.codejava.delivery.repository.DriverRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Loads delivery areas and drivers from seed files the first time the app runs. */
@Component
public class DeliverySeeder implements CommandLineRunner {

    private final DeliveryAreaRepository areaRepository;
    private final DriverRepository driverRepository;
    private final SeedFileReader reader;

    public DeliverySeeder(DeliveryAreaRepository areaRepository, DriverRepository driverRepository,
                          SeedFileReader reader) {
        this.areaRepository = areaRepository;
        this.driverRepository = driverRepository;
        this.reader = reader;
    }

    @Override
    public void run(String... args) {
        if (areaRepository.count() == 0) {
            // columns: name | latitude | longitude
            for (String[] row : reader.read("areas.psv")) {
                areaRepository.save(new DeliveryArea(row[0], Double.parseDouble(row[1]), Double.parseDouble(row[2])));
            }
        }
        if (driverRepository.count() == 0) {
            // columns: name | phone | vehicle
            for (String[] row : reader.read("drivers.psv")) {
                driverRepository.save(new Driver(row[0], row[1], row[2]));
            }
        }
    }
}
