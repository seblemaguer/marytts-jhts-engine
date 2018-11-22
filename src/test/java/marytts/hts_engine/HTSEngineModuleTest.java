package marytts.hts_engine;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;

import com.google.common.io.ByteStreams;

import org.junit.Assert;
import org.testng.annotations.Test;

import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import marytts.data.Sequence;
import marytts.data.Utterance;
import marytts.data.item.acoustic.F0List;
import marytts.hts_engine.data.JHTSEngineSupportedSequenceType;

public class HTSEngineModuleTest {

    // @Test
    public void testHTSEngineDurationModellerModuleProcess() throws Exception {
        Utterance utt = new Utterance();

        // Load reference F0
        byte[] bytes = ByteStreams.toByteArray(HTSEngineModuleTest.class.getResourceAsStream("/test.lf0"));
        ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.put(bytes);
        byteBuffer.rewind();

        double[] f0 = new double[byteBuffer.asDoubleBuffer().remaining()];
        byteBuffer.asDoubleBuffer().get(f0);
        F0List f0_item = new F0List(new DenseDoubleMatrix1D(f0));
        Sequence<F0List> f0_seq = new Sequence<F0List>();
        f0_seq.add(f0_item);
        utt.addSequence(JHTSEngineSupportedSequenceType.LF0, f0_seq);

        // // Generate
        // HTSEngineModuleModule jwm = new HTSEngineModuleModule();
        // jwm.setSampleRate(22050);
        // jwm.setFramePeriod(frame_period);
        // Utterance utt_enriched = jwm.process(utt);
    }
}
