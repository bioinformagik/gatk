package org.broadinstitute.hellbender.cmdline.argumentcollections;

import com.google.api.services.genomics.model.Read;
import org.broadinstitute.hellbender.cmdline.Argument;
import org.broadinstitute.hellbender.cmdline.ArgumentCollectionDefinition;
import org.broadinstitute.hellbender.engine.ReferenceDataSource;
import org.broadinstitute.hellbender.exceptions.UserException;
import org.broadinstitute.hellbender.transformers.BAQReadTransformer;
import org.broadinstitute.hellbender.transformers.MisencodedBaseQualityReadTransformer;
import org.broadinstitute.hellbender.transformers.ReadTransformer;

/**
 * Argument collection for optional read transformations
 *
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadTransformerArgumentCollection implements ArgumentCollectionDefinition {

    @Argument(fullName = "baq", doc= "Override base qualities with BAQ (per-Base Alignment Quality) scores.", common = false, optional = true)
    public boolean baq = false;

    @Argument(fullName = "fix_misencoded_quality_scores", doc= "Override base qualities with BAQ (per-Base Alignment Quality) scores.", common = false, optional = true)
    public boolean fix_misencoded_qualities = false;

    /**
     * Returns the read transformer based on the arguments
     * @param reference reference data source for BAQ recalibration; may be null if not provided
     * @return non-null read transformer
     */
    public ReadTransformer makeReadTransformer(final ReferenceDataSource reference) {
        ReadTransformer readTransformer = null;
        if(fix_misencoded_qualities) {
            readTransformer = new MisencodedBaseQualityReadTransformer();
        }
        if(baq) {
            if(reference == null) {
                throw new UserException.MissingReference("Reference should be provided if BAQ recalibration is used");
            }
            readTransformer = (readTransformer == null) ? new BAQReadTransformer(reference) : readTransformer.andThen(new BAQReadTransformer(reference));
        }
        return (readTransformer == null) ? ReadTransformer.identity() : readTransformer;
    }


}
