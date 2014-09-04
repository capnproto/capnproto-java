package org.capnproto.benchmark;

import org.capnproto.StructList;
import org.capnproto.Text;
import org.capnproto.benchmark.CarSalesSchema.*;

public class CarSales
    extends TestCase<ParkingLot.Factory, ParkingLot.Builder, ParkingLot.Reader,
    TotalValue.Factory, TotalValue.Builder, TotalValue.Reader, Long> {

    static final long carValue(Car.Reader car) {
        long result = 0;
        result += car.getSeats() * 200;
        result += car.getDoors() * 350;

        for (Wheel.Reader wheel : car.getWheels()) {
            result += ((long)wheel.getDiameter() * (long)wheel.getDiameter());
            result += wheel.getSnowTires() ? 100 : 0;
        }

        result += (long)car.getLength() * (long) car.getWidth() * (long) car.getHeight() / 50;

        Engine.Reader engine = car.getEngine();
        result += (long)engine.getHorsepower() * 40;
        if (engine.getUsesElectric()) {
            if (engine.getUsesGas()) {
                result += 5000;
            } else {
                result += 3000;
            }
        }

        result += car.getHasPowerWindows() ? 100 : 0;
        result += car.getHasPowerSteering() ? 200 : 0;
        result += car.getHasCruiseControl() ? 400 : 0;
        result += car.getHasNavSystem() ? 2000 : 0;

        result += (long)car.getCupHolders() * 25;

        return result;
    }

    static Text.Reader[] MAKES = {new Text.Reader("Toyota"), new Text.Reader("GM"),
                                  new Text.Reader("Ford"), new Text.Reader("Honda"),
                                  new Text.Reader("Tesla")};


    static Text.Reader[] MODELS = {new Text.Reader("Camry"), new Text.Reader("Prius"),
                                   new Text.Reader("Volt"), new Text.Reader("Accord"),
                                   new Text.Reader("Leaf"), new Text.Reader("Model S")};

    static final void randomCar(Common.FastRand rng, Car.Builder car) {
        car.setMake(MAKES[rng.nextLessThan(MAKES.length)]);
        car.setModel(MODELS[rng.nextLessThan(MODELS.length)]);

        car.setColor(Color.values()[rng.nextLessThan(Color.values().length)]);
        car.setSeats((byte)(2 + rng.nextLessThan(6)));
        car.setDoors((byte)(2 + rng.nextLessThan(3)));

        StructList.Builder<Wheel.Builder> wheels = car.initWheels(4);
        for (int i = 0; i < wheels.size(); ++i) {
            Wheel.Builder wheel = wheels.get(i);
            wheel.setDiameter((short)(25 + rng.nextLessThan(15)));
            wheel.setAirPressure((float)(30.0 + rng.nextDouble(20.0)));
            wheel.setSnowTires(rng.nextLessThan(16) == 0);
        }

        car.setLength((short)(170 + rng.nextLessThan(150)));
        car.setWidth((short)(48 + rng.nextLessThan(36)));
        car.setHeight((short)(54 + rng.nextLessThan(48)));
        car.setWeight((int)car.getLength() * (int)car.getWidth() * (int) car.getHeight() / 200);

        Engine.Builder engine = car.initEngine();
        engine.setHorsepower((short)(100 * rng.nextLessThan(400)));
        engine.setCylinders((byte)(4  + 2 * rng.nextLessThan(3)));
        engine.setCc(800 + rng.nextLessThan(10000));
        engine.setUsesGas(true);
        engine.setUsesElectric(rng.nextLessThan(2) == 1);

        car.setFuelCapacity((float)(10.0 + rng.nextDouble(30.0)));
        car.setFuelLevel((float)(rng.nextDouble(car.getFuelCapacity())));
        car.setHasPowerWindows(rng.nextLessThan(2) == 1);
        car.setHasPowerSteering(rng.nextLessThan(2) == 1);
        car.setHasCruiseControl(rng.nextLessThan(2) == 1);
        car.setCupHolders((byte)rng.nextLessThan(12));
        car.setHasNavSystem(rng.nextLessThan(2) == 1);
    }


    public final Long setupRequest(Common.FastRand rng, ParkingLot.Builder request) {
        long result = 0;
        StructList.Builder<Car.Builder> cars = request.initCars(rng.nextLessThan(200));
        for (int i = 0; i < cars.size(); ++i) {
            Car.Builder car = cars.get(i);
            randomCar(rng, car);
            result += carValue(car.asReader());
        }
        return result;
    }


    public final void handleRequest(ParkingLot.Reader request, TotalValue.Builder response) {
        long result = 0;
        for (Car.Reader car : request.getCars()) {
            result += carValue(car);
        }
        response.setAmount(result);
    }

    public final boolean checkResponse(TotalValue.Reader response, Long expected) {
        return response.getAmount() == expected;
    }


    public static void main(String[] args) {
        CarSales testCase = new CarSales();
        testCase.execute(args, ParkingLot.factory, TotalValue.factory);
    }

}
