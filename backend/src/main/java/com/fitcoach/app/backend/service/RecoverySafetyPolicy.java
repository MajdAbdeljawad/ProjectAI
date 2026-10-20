package com.fitcoach.app.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class RecoverySafetyPolicy {

    private static final Pattern CONCERNING = Pattern.compile(
            "chest pain|chest tightness|radiating arm pain|faint(?:ed|ing)?|passed out|syncope|"
                    + "severe shortness of breath|can't breathe|cannot breathe|coughing blood|"
                    + "unexplained severe pain|palpitations with dizziness|severe dizziness|"
                    + "concussion|head injury|suicidal|crushing pressure",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SIGNIFICANT_PAIN = Pattern.compile(
            "sharp pain|severe pain|injury pain|swelling|cannot walk|can't walk|pain when breathing",
            Pattern.CASE_INSENSITIVE
    );

    private RecoverySafetyPolicy() {
    }

    public static SafetyDecision evaluate(RecoveryDataset dataset) {
        List<String> flags = new ArrayList<>(dataset.getSafetyFlags() == null ? List.of() : dataset.getSafetyFlags());
        boolean concerning = dataset.isConcerningSymptomsPresent();
        boolean significantPain = dataset.isSignificantPainPresent();

        String symptomText = "";
        if (dataset.getIndicators() != null && dataset.getIndicators().getSymptoms() != null) {
            symptomText = safe(dataset.getIndicators().getSymptoms().getSymptoms()) + " "
                    + safe(dataset.getIndicators().getSymptoms().getLimitations());
        }
        if (CONCERNING.matcher(symptomText).find()) {
            concerning = true;
            flags.add("Reported symptoms include warning signs that need professional evaluation.");
        }
        if (SIGNIFICANT_PAIN.matcher(symptomText).find()) {
            significantPain = true;
            flags.add("Significant pain was reported. Training through pain is not recommended.");
        }

        Integer soreness = dataset.getIndicators() != null && dataset.getIndicators().getSubjective() != null
                ? dataset.getIndicators().getSubjective().getSorenessLevel() : null;
        if (soreness != null && soreness >= 8) {
            significantPain = true;
            flags.add("Soreness is rated 8/10 or higher.");
        }

        if (concerning) {
            return new SafetyDecision(
                    true,
                    true,
                    true,
                    significantPain,
                    flags.stream().distinct().toList(),
                    "REST",
                    "Stop training and seek appropriate professional evaluation. This app does not diagnose medical conditions.",
                    "Concerning symptoms were reported. The next action is rest and professional evaluation, not continued training."
            );
        }
        if (significantPain) {
            return new SafetyDecision(
                    true,
                    false,
                    true,
                    true,
                    flags.stream().distinct().toList(),
                    "REST",
                    "Do not train through significant pain. Choose rest or very easy mobility only if it does not increase pain, and seek professional evaluation if pain persists.",
                    "Significant pain was reported, so the next session should not proceed as planned."
            );
        }
        return new SafetyDecision(false, false, false, false, flags.stream().distinct().toList(), null, null, null);
    }

    public static String overrideAction(String recommendedAction, SafetyDecision decision) {
        if (decision.blocksTraining()) {
            return "REST";
        }
        if (decision.significantPain() && "CONTINUE_PLAN".equals(recommendedAction)) {
            return "REST";
        }
        return recommendedAction;
    }

    public static boolean mentionsConcerningSymptoms(String text) {
        return text != null && CONCERNING.matcher(text).find();
    }

    public static boolean mentionsSignificantPain(String text) {
        return text != null && SIGNIFICANT_PAIN.matcher(text).find();
    }

    private static String safe(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record SafetyDecision(
            boolean blocksTraining,
            boolean requiresProfessionalEvaluation,
            boolean concerningSymptoms,
            boolean significantPain,
            List<String> flags,
            String forcedAction,
            String safetyNotice,
            String explanation
    ) {
    }
}
