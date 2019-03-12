package org.capnproto.jmh;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.util.concurrent.TimeUnit;
import org.capnproto.ArrayInputStream;
import org.capnproto.ArrayOutputStream;
import org.capnproto.MessageBuilder;
import org.capnproto.ReaderOptions;
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
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ReadObjectsJmh {

    private void read(byte[] bb, Blackhole hole) throws IOException {
        hole.consume(Serialize.read(ByteBuffer.wrap(bb), ReaderOptions.DEFAULT_READER_OPTIONS));
    }

    private void readChannel(byte[] data, Blackhole hole) throws IOException {
        hole.consume(Serialize.read(Channels.newChannel(new ByteArrayInputStream(data))));
    }

    private void readArrayInputStream(byte[] data, Blackhole hole) throws IOException {
        hole.consume(Serialize.read(new ArrayInputStream(ByteBuffer.wrap(data))));
    }

    @State(Scope.Benchmark)
    public static class DataProvider {

        private byte[] car;
        private byte[] data;
        private byte[] lot;

        @Setup
        public void init() throws IOException {
            {
                MessageBuilder builder = new MessageBuilder();
                final CarSalesSchema.Car.Builder car = builder.initRoot(CarSalesSchema.Car.factory);
                createCar(car);
                this.car = write(builder);
            }
            {
                MessageBuilder builder = new MessageBuilder();
                final DataSchema.Message.Builder initRoot = builder.initRoot(DataSchema.Message.factory);
                initRoot.initLeft().setValue(new byte[100_000]);
                this.data = write(builder);
            }
            {
                MessageBuilder builder = new MessageBuilder();
                final CarSalesSchema.ParkingLot.Builder parkingLot = builder.initRoot(CarSalesSchema.ParkingLot.factory);
                parkingLot.initCars(1000);
                parkingLot.getCars().stream().forEach(this::createCar);
                this.lot = write(builder);
            }
        }

        private byte[] write(MessageBuilder builder) throws IOException {
            final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            Serialize.write(Channels.newChannel(byteArrayOutputStream), builder);
            return byteArrayOutputStream.toByteArray();
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

    @Benchmark
    public void readByteBufferCar(Blackhole hole, DataProvider data) throws IOException {
        read(data.car, hole);
    }

    @Benchmark
    public void readByteBufferParkingLotWith1000Cars(Blackhole hole, DataProvider data) throws IOException {
        read(data.lot, hole);
    }

    @Benchmark
    public void readByteBuffer100kData(Blackhole hole, DataProvider data) throws IOException {
        read(data.data, hole);
    }

    @Benchmark
    public void readChannelCar(Blackhole hole, DataProvider data) throws IOException {
        readChannel(data.car, hole);
    }

    @Benchmark
    public void readChannelParkingLotWith1000Cars(Blackhole hole, DataProvider data) throws IOException {
        readChannel(data.lot, hole);
    }

    @Benchmark
    public void readChannel100kData(Blackhole hole, DataProvider data) throws IOException {
        readChannel(data.data, hole);
    }

    @Benchmark
    public void readArrayInputStreamCar(Blackhole hole, DataProvider data) throws IOException {
        readArrayInputStream(data.car, hole);
    }

    @Benchmark
    public void readArrayInputStreamParkingLotWith1000Cars(Blackhole hole, DataProvider data) throws IOException {
        readArrayInputStream(data.lot, hole);
    }

    @Benchmark
    public void readArrayInputStream100kData(Blackhole hole, DataProvider data) throws IOException {
        readArrayInputStream(data.data, hole);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(ReadObjectsJmh.class
                        .getSimpleName())
                .build();

        new Runner(opt).run();
    }

}
