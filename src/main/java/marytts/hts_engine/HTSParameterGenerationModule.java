package marytts.hts_engine;

// Utils
import java.util.ArrayList;

// Exception
import marytts.MaryException;

// Marytts base classes
import marytts.config.MaryConfiguration;
import marytts.data.Relation;
import marytts.data.Sequence;
import marytts.data.SupportedSequenceType;
import marytts.data.Utterance;
import marytts.data.utils.IntegerPair;
import marytts.hts_engine.data.JHTSEngineSupportedSequenceType;
import marytts.data.item.Item;
import marytts.data.item.global.DoubleMatrixItem;
import marytts.data.item.global.DoubleVectorItem;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class HTSParameterGenerationModule extends HTSEngineDurationModellerModule
{
    private ArrayList<String> sequence_names;

    public HTSParameterGenerationModule() throws MaryException {
        super();

        // FIXME: hadcoded
        sequence_names = new ArrayList<String>();
        sequence_names.add(JHTSEngineSupportedSequenceType.MGC);
        sequence_names.add(JHTSEngineSupportedSequenceType.LF0);
        sequence_names.add(JHTSEngineSupportedSequenceType.BAP);
    }

    @Override
    public Utterance process(Utterance utt, MaryConfiguration runtime_configuration) throws MaryException
    {
        try {
            runtime_configuration.applyConfiguration(this);


            this.logger.info(String.format("Generating parameters using voice coming from the following path \"%s\"",
                                           getVoicePath()));

            // Predict the duration and achieve the generation at the same time
            utt = durationPrediction(utt);

            // Retrieve the other part
            for (int i=0; i<sequence_names.size(); i++)
                utt = updateUtteranceWithParameter(utt, sequence_names.get(i), SupportedSequenceType.FEATURES, i);

            // Clear unused memory !
            hts_engine_wrapper.refresh();

            return utt;
        } catch (Exception ex) {
            hts_engine_wrapper.refresh();
            throw new MaryException("Couldn't predict the duration", ex);
        }
    }

    protected Utterance updateUtteranceWithParameter(Utterance utt, String sequence_name, String ref_sequence_name, int stream_index) throws Exception {

        // Get the parameters
        double[][] tmp = hts_engine_wrapper.getGeneratedParameterSequence(stream_index);

        // Generate the sequence
        if (tmp[0].length == 1) { // Vector
            DoubleVectorItem it = new DoubleVectorItem(tmp);
            Sequence<DoubleVectorItem> gen_seq = new Sequence<DoubleVectorItem>();
            gen_seq.add(it);
            utt.addSequence(sequence_name, gen_seq);
        } else {
            DoubleMatrixItem it = new DoubleMatrixItem(tmp);
            Sequence<DoubleMatrixItem> gen_seq = new Sequence<DoubleMatrixItem>();
            gen_seq.add(it);
            utt.addSequence(sequence_name, gen_seq);
        }

        // Generate the relation
        Sequence<? extends Item> ref_sequence = utt.getSequence(ref_sequence_name);
        ArrayList<IntegerPair> alignment = new ArrayList<IntegerPair>();
        for (int i=0; i<ref_sequence.size(); i++) {
            alignment.add(new IntegerPair(i, 0));
        }
        Relation rel = new Relation(ref_sequence, utt.getSequence(sequence_name), alignment);
        utt.setRelation(ref_sequence_name, sequence_name, rel);

        // Return the updated utterance
        return utt;
    }

    protected void setDescription()
    {
        this.description = "Module to call the get the duration using hts-engine";
    }


    public void shutdown() {
        hts_engine_wrapper.clear();
    }


    public ArrayList<String> getSequenceNames() {
        return sequence_names;
    }

    public void setSequenceNames(ArrayList<String> sequence_names) {
        this.sequence_names = sequence_names;
    }
}
