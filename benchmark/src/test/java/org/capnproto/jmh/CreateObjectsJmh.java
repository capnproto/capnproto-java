package org.capnproto.jmh;

import java.util.concurrent.TimeUnit;
import org.capnproto.AllocatingArena;
import org.capnproto.BuilderArena;
import org.capnproto.MessageBuilder;
import org.capnproto.benchmark.CarSalesSchema;
import org.capnproto.benchmark.DataSchema;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * To run this test execute maven with -P benchmark or just run the main of this
 * file in your IDE.
 */
// only fork 1 JVM per benchmark
@Fork(1)
// 5 times 2 second warmup per benchmark
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
// 5 times 2 second measurment per benchmark
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
// score is duration of one call
@BenchmarkMode(Mode.AverageTime)
// in micros
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class CreateObjectsJmh {

    // call is too fast for ms
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void createCar(Blackhole hole) {
        MessageBuilder builder = new MessageBuilder();
        final CarSalesSchema.Car.Builder car = builder.initRoot(CarSalesSchema.Car.factory);
        createCar(car);

        // consume the object in a way, the JVM doesn't know it's unused
        hole.consume(builder);
    }

    private void createCar(final CarSalesSchema.Car.Builder car) {
        final CarSalesSchema.Engine.Builder engine = car.initEngine();
        engine.setCc(34000);
        engine.setHorsepower((short) 45);
        engine.setUsesGas(true);
        engine.setUsesElectric(true);
        car.setMake("Heya");
        car.setModel("Ola");
        car.setColor(CarSalesSchema.Color.BLACK);
        car.setDoors((byte) 4);
        car.setFuelCapacity((float) 4.5);
        car.setHasCruiseControl(true);
        car.setHasNavSystem(false);
        car.setHasPowerSteering(true);
        car.setHasPowerWindows(true);
        car.setHeight((short) 4);
        car.setLength((short) 5);
        car.setSeats((byte) 10);
        car.setFuelLevel((float) 0.4);
    }

    @Benchmark
    public void createParkingLotWith1000CarsWithStream(Blackhole hole) {
        MessageBuilder builder = new MessageBuilder();
        final CarSalesSchema.ParkingLot.Builder parkingLot = builder.initRoot(CarSalesSchema.ParkingLot.factory);
        parkingLot.initCars(1000);
        parkingLot.getCars().stream().forEach(this::createCar);

        // consume the object in a way, the JVM doesn't know it's unused
        hole.consume(builder);
    }

    @Benchmark
    public void createParkingLotWith1000Cars(Blackhole hole) {
        MessageBuilder builder = new MessageBuilder();
        final CarSalesSchema.ParkingLot.Builder parkingLot = builder.initRoot(CarSalesSchema.ParkingLot.factory);
        parkingLot.initCars(1000);
        for (int i = 0; i < 1000; i++) {
            createCar(parkingLot.getCars().get(i));
        }

        // consume the object in a way, the JVM doesn't know it's unused
        hole.consume(builder);
    }

    @Benchmark
    public void create100kData(Blackhole hole) {
        MessageBuilder builder = new MessageBuilder();
        final DataSchema.Message.Builder initRoot = builder.initRoot(DataSchema.Message.factory);
        initRoot.initLeft().setValue(new byte[100_000]);
        initRoot.getLeft();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CreateObjectsJmh.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }
}
