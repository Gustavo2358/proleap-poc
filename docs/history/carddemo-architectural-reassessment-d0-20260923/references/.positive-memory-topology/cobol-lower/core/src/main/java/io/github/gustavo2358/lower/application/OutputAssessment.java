package io.github.gustavo2358.lower.application;

import io.github.gustavo2358.air.model.Publication;
import io.github.gustavo2358.air.validation.AirValidator;
import io.github.gustavo2358.air.validation.ValidationOptions;
import io.github.gustavo2358.air.validation.ValidationResult;
import java.util.Optional;

/** Classifies the real validator's result without repairing or dropping target facts. */
public record OutputAssessment(LoweringResult.Status status, Optional<Publication> publication, ValidationResult validation) {
    public static OutputAssessment assess(Publication publication, ValidationOptions options) { return assess(publication,options,false); }
    public static OutputAssessment assessForPartialAnalysis(Publication publication, ValidationOptions options) { return assess(publication,options,true); }
    private static OutputAssessment assess(Publication publication, ValidationOptions options,boolean partial) {
        var result = AirValidator.validate(publication, options);
        var status = switch (result.status()) {
            case INVALID_IR -> LoweringResult.Status.OUTPUT_INVALID;
            case INCOMPLETE_VALIDATION -> partial&&result.unprovedOperationPreconditions().isPresent()?LoweringResult.Status.PARTIAL:LoweringResult.Status.VALIDATION_INCOMPLETE;
            case STRUCTURALLY_VALID -> LoweringResult.Status.SUCCESS;
        };
        return new OutputAssessment(status, (status == LoweringResult.Status.SUCCESS || status == LoweringResult.Status.PARTIAL) ? Optional.of(publication) : Optional.empty(), result);
    }
}
