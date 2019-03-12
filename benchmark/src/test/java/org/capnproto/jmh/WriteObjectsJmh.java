package org.capnproto.jmh;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;
import org.capnproto.ArrayOutputStream;
import org.capnproto.MessageBuilder;
import org.capnproto.Serialize;
import org.capnproto.benchmark.CarSalesSchema;
import org.capnproto.benchmark.DataSchema;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 *
 * @author developer
 */
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
public class WriteObjectsJmh {

    @State(Scope.Benchmark)
    public static class DataProvider {

        private MessageBuilder car;
        private MessageBuilder data;
        private MessageBuilder lot;

        @Setup
        public void init() {
            {
                MessageBuilder builder = new MessageBuilder();
                final CarSalesSchema.Car.Builder car = builder.initRoot(CarSalesSchema.Car.factory);
                createCar(car);
                this.car = builder;
            }
            {
                MessageBuilder builder = new MessageBuilder();
                final DataSchema.Message.Builder initRoot = builder.initRoot(DataSchema.Message.factory);
                initRoot.initLeft().setValue(new byte[100_000]);
                this.data = builder;
            }
            {
                MessageBuilder builder = new MessageBuilder();
                final CarSalesSchema.ParkingLot.Builder parkingLot = builder.initRoot(CarSalesSchema.ParkingLot.factory);
                parkingLot.initCars(1000);
                parkingLot.getCars().stream().forEach(this::createCar);
                this.lot = builder;
            }
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
    }

    // call is too fast for ms
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void writeChannelCar(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.car, hole);
    }

    @Benchmark
    public void writeChhannelParkingLotWith1000Cars(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.lot, hole);
    }

    @Benchmark
    public void writeChannel100kData(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.data, hole);
    }

        // call is too fast for ms
    @OutputTimeUnit(TimeUnit.NANOSECONDS)
    @Benchmark
    public void writeArrayOutputStreamCar(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.car, hole);
    }

    @Benchmark
    public void writeArrayOutputStreamParkingLotWith1000Cars(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.lot, hole);
    }

    @Benchmark
    public void writeArrayOutputStream100kData(Blackhole hole, DataProvider data) throws IOException {
        writeChannel(data.data, hole);
    }

    private void writeChannel(MessageBuilder builder, Blackhole hole) throws IOException {
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Serialize.write(Channels.newChannel(byteArrayOutputStream), builder);
        hole.consume(byteArrayOutputStream);
    }

    private void writeBuff(MessageBuilder builder, Blackhole hole) throws IOException {
        final ArrayOutputStream byteArrayOutputStream = new ArrayOutputStream(ByteBuffer.allocate(1000_000));
        Serialize.write(byteArrayOutputStream, builder);
        hole.consume(byteArrayOutputStream);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(WriteObjectsJmh.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
