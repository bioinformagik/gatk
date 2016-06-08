package org.broadinstitute.hellbender.transformers;

import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.utils.baq.BAQ;
import org.broadinstitute.hellbender.utils.read.GATKRead;

/**
 * Transform base qualities to BAQ (per-Base Alignment Qualities), computing on the fly if necessary if the tag is not found.
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class BAQReadTransformer implements ReadTransformer {
    private static final long serialVersionUID = 1L;

    private final BAQ baq;
    private final ReferenceDataSource reference;

    /**
     * Construct a BAQ transformer using the reference source
     */
    public BAQReadTransformer(final ReferenceDataSource reference) {
        this.baq = new BAQ();
        this.reference = reference;
    }

    @Override
    public GATKRead apply(GATKRead gatkRead) {
        baq.baqRead(gatkRead, reference, BAQ.CalculationMode.CALCULATE_AS_NECESSARY, BAQ.QualityMode.OVERWRITE_QUALS);
        return gatkRead;
    }
}
