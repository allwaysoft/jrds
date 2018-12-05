package jrds;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import jrds.mockobjects.MokeProbe;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestTranslateProbe {
    static final private Logger logger = Logger.getLogger(TestTranslateProbe.class);
    static ProbeDesc<String> pd;

    @SuppressWarnings("unchecked")
    @BeforeClass
    static public void configure() throws SecurityException, IllegalArgumentException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, Exception {
        Tools.configure();
        Tools.prepareXml();
        Tools.setLevel(logger, Level.ERROR, "jrds.Probe");
        pd = (ProbeDesc<String>) jrds.configuration.GeneratorHelper.getProbeDesc(Tools.parseRessource("fulldesc.xml"));

    }

    @Test
    public void constructAndCheckImmediateHigh() {
        Probe<String, Number> p = new MokeProbe<String, Number>();
        p.setPd(pd);

        Map<String, String> nameMap = p.getCollectMapping();
        Map<String, Number> filteredSamples = new HashMap<String, Number>();
        Map<String, Number> resultSample = new HashMap<String, Number>();

        for(Map.Entry<String, Number> e: filteredSamples.entrySet()) {
            String dsName = nameMap.get(e.getKey());
            double value = e.getValue().doubleValue();
            if(dsName != null) {
                resultSample.put(dsName, value);
            } else {
                logger.debug("Dropped entry: " + e.getKey());
            }
        }

    }
}
