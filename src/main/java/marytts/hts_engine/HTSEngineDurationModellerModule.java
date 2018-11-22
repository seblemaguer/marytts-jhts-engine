package marytts.hts_engine;

// Utils
import java.util.ArrayList;

// Exception
import marytts.MaryException;
import marytts.exceptions.MaryConfigurationException;

// Marytts base classes
import marytts.config.MaryConfiguration;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.item.phonology.Phoneme;
import marytts.data.utils.IntegerPair;
import marytts.data.item.acoustic.Segment;
import marytts.modules.MaryModule;

// Marytts Serializer
import marytts.io.serializer.label.DefaultHTSLabelSerializer;
import marytts.io.serializer.Serializer;

// Java hts engine wrapper classes
import jhts_engine.JHTSEngineWrapper;
import jhts_engine.FilledLabel;
/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class HTSEngineDurationModellerModule extends MaryModule
{
    private String voice_path;

    /** The label serializer */
    private Serializer label_serializer;

    /** The HTS engine wrapper */
    protected JHTSEngineWrapper hts_engine_wrapper;

    public HTSEngineDurationModellerModule() throws MaryException {
        super("acoustic");
        hts_engine_wrapper = new JHTSEngineWrapper();
        setLabelSerializer(new DefaultHTSLabelSerializer());
    }


    public Serializer getLabelSerializer() {
        return label_serializer;
    }

    public void setLabelSerializer(String label_serializer_class_name) {
        this.label_serializer = label_serializer;
    }

    public void setLabelSerializer(Serializer label_serializer) {

        this.label_serializer = label_serializer;
    }

    protected String getVoicePath() {
        return voice_path;
    }

    public void setVoicePath(String voice_path) throws MaryException {
        try {
            this.voice_path = voice_path;
            hts_engine_wrapper.setVoice(voice_path);
        } catch (Exception ex) {
            throw new MaryException("Can't define the voice for the duration modeller", ex);
        }
    }


    public void checkStartup() throws MaryConfigurationException {
    }

    public void checkInput(Utterance utt) throws MaryException {

        // if ((! utt.hasSequence(SupportedSequenceType.FEATURES)) ||
        //     (! utt.hasSequence(SupportedSequenceType.LABELS))) {

        if (! utt.hasSequence(SupportedSequenceType.FEATURES)) {
            throw new MaryException(String.format("The utterance doesn't contain a %s sequence",
                                                  SupportedSequenceType.FEATURES));
        }

        if (! utt.hasSequence(SupportedSequenceType.PHONE)) {
            throw new MaryException(String.format("The utterance doesn't contains the %s sequence",
                                                  SupportedSequenceType.PHONE));
        }
    }

    public Utterance process(Utterance utt, MaryConfiguration runtime_configuration) throws MaryException
    {
        try {
            runtime_configuration.applyConfiguration(this);

            // Predict the duration
            utt = durationPrediction(utt);

            // Clear unused memory !
            hts_engine_wrapper.refresh();

            return utt;
        } catch (Exception ex) {
            hts_engine_wrapper.refresh();
            throw new MaryException("Couldn't predict the duration", ex);
        }
    }

    protected Utterance durationPrediction(Utterance utt) throws Exception
    {
        // Generate the parameters through HTS engine
        String input_features = getLabelSerializer().export(utt).toString();
        hts_engine_wrapper.generateAcousticParameters(input_features);

        // Get duration information
        Sequence<Phoneme> seq_ph = (Sequence<Phoneme>) utt.getSequence(SupportedSequenceType.PHONE);
        ArrayList<FilledLabel> labels = hts_engine_wrapper.getDurations();
        if (labels.size() != seq_ph.size()) {
            throw new MaryException(String.format("The number of segments (%d) produced by HTS engine doesn't correspond to the given number of segments (%d)",
                                                  labels.size(), seq_ph.size()));
        }

        // Fill sequence
        Sequence<Segment> seq_segment = new Sequence<Segment>();
        for (int i=0; i<labels.size(); i++) {
            double start = labels.get(i).getStart() / FilledLabel.MS_TO_HTK;
            double duration = labels.get(i).getDuration() / FilledLabel.MS_TO_HTK;
            Segment cur_seg = new Segment(start, duration);
            seq_segment.add(cur_seg);
        }

        utt.addSequence(SupportedSequenceType.SEGMENT, seq_segment);


        ArrayList<IntegerPair> alignment_phone_seg = new ArrayList<IntegerPair>();
        for (int i = 0; i < seq_ph.size(); i++) {
            alignment_phone_seg.add(new IntegerPair(i, i));
        }

        Relation rel = new Relation(seq_ph, seq_segment, alignment_phone_seg);
        utt.setRelation(SupportedSequenceType.PHONE, SupportedSequenceType.SEGMENT, rel);

        return utt;
    }

    protected void setDescription()
    {
        this.description = "Module to call the get the duration using hts-engine";
    }


    public void shutdown() {
        this.logger.info("Clear wrapper");
        hts_engine_wrapper.clear();
    }
}
