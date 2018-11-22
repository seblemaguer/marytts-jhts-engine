package marytts.hts_engine.data;

import marytts.data.SupportedSequenceType;

/**
 *
 *
 * @author <a href="mailto:slemaguer@coli.uni-saarland.de">Sébastien Le Maguer</a>
 */
public class JHTSEngineSupportedSequenceType
{
    public static final String MGC = "MGC";
    public static final String LF0 = "LF0";
    public static final String BAP = "BAP";

    static {
        SupportedSequenceType.addSupportedType(MGC);
        SupportedSequenceType.addSupportedType(LF0);
        SupportedSequenceType.addSupportedType(BAP);
    }
}
