package org.capnproto;

import org.capnproto.amazing.Everything;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class UniverseTest {

    private Everything.Location.Builder loc;

    @Test
    public void test() {
        withLocation();
        assertThat(loc.toString(), is("Location {"
                + " X={203} Y={345} Z={10424} SolarSystem={"
                + "SolarSystem {"
                + " Sun={Sun {"
                + " Meta={"
                + "MetaData {"
                + " Radius={0} Weight={0} Temperature={0}}"
                + "}}} "
                + "Planets="
                + "{Planet { "
                + "Name={Earth} Meta={"
                + "MetaData { Radius={20000} Weight={2424} Temperature={20}}} "
                + "Moons={Moon { Name={Luna} Meta={MetaData { Radius={0} Weight={0} Temperature={0}}}}}}"
                + ","
                + "Planet { Name={Mars} Meta={MetaData { Radius={0} Weight={0} Temperature={0}}}}}}}"
                + "}"));
    }

    private void withLocation() {
        this.loc = new MessageBuilder().initRoot(Everything.Location.factory);
        loc.setX(203L);
        loc.setY(345L);
        loc.setZ(10_424L);
        final StructList.Builder<Everything.Planet.Builder> planets = loc.initSolarSystem().initPlanets(2);
        final Everything.Planet.Builder earth = planets.get(0);
        earth.initMoons(1);
        final Everything.Moon.Builder luna = earth.getMoons().get(0);
        final Everything.Planet.Builder mars = planets.get(1);
        earth.setName("Earth");
        luna.setName("Luna");
        mars.setName("Mars");
        final Everything.MetaData.Builder earthMeta = earth.initMeta();
        earthMeta.setWeight(2424L);
        earthMeta.setTemperature((short) 20);
        earthMeta.setRadius(20_000);
    }
}
