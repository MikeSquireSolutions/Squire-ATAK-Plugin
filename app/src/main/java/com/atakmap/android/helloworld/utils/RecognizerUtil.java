package com.atakmap.android.squire.utils;

import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import org.apache.commons.codec.language.DoubleMetaphone;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RecognizerUtil {
    public static final String MECHANISM_OF_INJURY = "MECHANISM OF INJURY";
    public static final String INJURY = "INJURY";
    public static final String SIGNS_AND_SYMPTOMS = "SIGNS AND SYMPTOMS";
    public static final String TREATMENT = "TREATMENT";
    public static final String ZMIST = "Z MIST";
    public static final String NOTES = "NOTES";
    public static final String CONFIRM = "CONFIRM";

    static final boolean verboseLogging = false;
    static final String TAG = "Recognizer Util";

    public static String[] MIST_CHOICES = {
            ZMIST,
            NOTES,
            MECHANISM_OF_INJURY,
            INJURY,
            SIGNS_AND_SYMPTOMS,
            TREATMENT,
            CONFIRM
    };

    public static String[] MECHANISM_OF_INJURY_CHOICES = {
            "BLUNT TRAUMA",
            "PENETRATING TRAUMA",
            "GUNSHOT WOUND",
            "BURN",
            "FALL",
            "BLAST",
            "MVA",
            "CHEMICAL",
            "BIOLOGICAL",
            "RADIATION",
            "FALL",
            "CRUSH",
            "OTHER"
    };

    public static String[] INJURY_CHOICES = {
            "AMPUTATION",
            "PARTIAL AMPUTATION",
            "DEFORMITY",
            "BURN",
            "AIRWAY BURN",
            "FULL THICKNESS BURN",
            "FIRST DEGREE BURN",
            "SECOND DEGREE BURN",
            "BILATERAL FEMUR FRACTURE",
            "FEMUR FRACTURE",
            "SKULL FRACTURE",
            "BASILAR SKULL FRACTURE",
            "TRAUMATIC BRAIN INJURY",
            "DISLOCATION",
            "AVULSION",
            "FRACTURE",
            "HEMATOMA",
            "GUNSHOT WOUND",
            "FRAG WOUND",
            "SHRAPNEL WOUND",
            "STAB WOUND",
            "PUNCTURE WOUND",
            "MASSIVE HEMORRHAGE",
            "HEMORRHAGE",
            "LACERATION",
            "EVISERATION",
            "UNILATERAL PUPIL DILATION",
            "OTORRHEA",
            "RHINORRHEA"
    };

    public static String[] SIGNS_AND_SYMPTOMS_CHOICES = {
            "STABLE",
            "UNSTABLE",
            "BREATHING",
            "NOT BREATHING",
            "CONSCIOUS",
            "UNCONSCIOUS"
    };

    public static String[] TREATMENT_CHOICES = {
            "REQUIRED",
            "RENDERED",
            "RIGHT LEG",
            "LEFT LEG",
            "PELVIS",
            "GUT",
            "CHEST",
            "RIGHT ARM",
            "LEFT ARM",
            "NECK",
            "IMMOBILIZE",
            "HEAD",
            "WHOLE BLOOD TRANSFUSION",
            "SPLINT",
            "CHEST SEAL",
            "THORSACOSTOMY",
            "NEEDLE DECOMPRESSION",
            "OXYGEN",
            "AIRWAY NPA",
            "DRESSING",
            "TOURNIQUET"
    };

    public static String BRAVO = "BRAVO";
    public static String GOLF = "GOLF";
    public static String FOXTROT = "FOXTROT";
    public static String[] ZMIST_CHOICES = {
            GOLF,
            FOXTROT,
            BRAVO,
    };

    public static String[] SPECIAL_EQUIPMENT_CHOICES = {
            "NONE",
            "HOIST",
            "EXTRACTION_EQUIPMENT",
            "VENTILATOR"
    };

    public static String[] SECURITY_AT_PICKUP_CHOICES = {
            "HOT",
            "COLD"
    };

    public static String[] METHOD_OF_MARKING_CHOICES = {
            "PANELS",
            "SMOKE",
            "IR",
            "NONE"
    };

    public static String[] NATIONALITY_CHOICES = {
            "US MILITARY",
            "US CIVILIAN",
            "NON US MILITARY",
            "NON US CIVILIAN",
            "OTHER"
    };

    public static String[] NBC_CHOICES = {
            "NONE",
            "NUCLEAR",
            "BIOLOGICAL",
            "CHEMICAL"
    };

    public static String[] RADIO_FREQUENCY_CHOICES = {
            "PRIMARY SATCOM",
            "SECONDARY SATCOM",
            "TERCIARY SATCOM"
    };

    public static String[] PATIENT_STATUS_CHOICES = {
            "AMBULATORY",
            "LITTER"
    };

    public static String[] PATIENT_PRIORITY_CHOICES = {
            "CONVENIENCE",
            "ROUTINE",
            "PRIORITY",
            "URGENT SURGICAL",
            "URGENT",
    };

    public static final String LOCATION = "LOCATION";
    public static final String PATIENTS = "PATIENTS";
    public static final String NATIONALITY = "NATIONALITY";
    public static final String RADIO_FREQUENCY = "RADIO FREQUENCY";
    public static final String SPECIAL_EQUIPMENT_REQUIRED = "SPECIAL EQUIPMENT REQUIRED";
    public static final String SECURITY_AT_PICKUP_SITE = "SECURITY AT PICKUP SITE";
    public static final String METHOD_OF_MARKING = "METHOD OF MARKING";
    public static final String NBC = "NBC";

    public static final String CALL_SIGN = "CALL SIGN";
    public static final String PATIENT_STATUS = "STATUS";
    public static final String PATIENT_PRIORITY = "PRIORITY";
    public static final String NEXT_PATIENT = "NEXT PATIENT";
    public static final String PREV_PATIENT = "PREV PATIENT";

    public static final String NAME = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String MGRS = "MGRS";

    public static final String[] NINELINE_CHOICES = {
            NBC,
            METHOD_OF_MARKING,
            SECURITY_AT_PICKUP_SITE,
            SPECIAL_EQUIPMENT_REQUIRED,
            RADIO_FREQUENCY,
            NATIONALITY,
            PATIENTS,
            LOCATION,
    };

    public static String[] PATIENT_CHOICES = {
            CALL_SIGN,
            PATIENT_STATUS,
            PATIENT_PRIORITY,
            NEXT_PATIENT,
            PREV_PATIENT
    };

    public static String[] LZ_CHOICES = {
            NAME,
            DESCRIPTION,
            LOCATION,
            MGRS
    };

    // Returns tuple of <ChoiceDetected, OriginalString>
    // Original string is provided in order to calculate a remaining substr
    // of input and do more analyzing if needed.
    // e.g.
    // findBestChoice("mechanism of injury is blast", ...)
    // returns <"MECHANISM OF INJURY">, "mechanism of injury"
    // and then we can check the left-over " is blast", etc.
    //
    // TODO:    Add an argument / separate function (?) for selecting best choice that
    // TODO:    must start with the first word. i.e. it must choose the first occurence in
    // TODO:    "_Injury_ is Traumatic Brain _Injury_", leaving TBI as args.
    //
    @RequiresApi(api = Build.VERSION_CODES.N)
    public static Pair<String, String> findBestChoice(String input, String[] options) {
        DoubleMetaphone doubleMetaphone = new DoubleMetaphone();
        doubleMetaphone.setMaxCodeLen(20);
        Map<String, String> phonemes = new HashMap<>();

        for (String option : options) {
            String phonetic = doubleMetaphone.doubleMetaphone(option);
            phonemes.put(option, phonetic);

            if (verboseLogging) {
                Log.i(TAG, "Option \"" + option + "\": " + phonetic);
            }
        }

        String[] splits = input.split(" ");
        Set<String> uniqConcats = new HashSet<>();
        for (int k = 0; k < splits.length; k++) {
            // Get substring [0:k]
            StringBuilder concatenatedInput = new StringBuilder();
            for (int j = 0; j <= k; j++) {
                if (j > 0) {
                    concatenatedInput.append(" ");
                }
                concatenatedInput.append(splits[j]);
            }
            // Get substrings of substring
            String concat = concatenatedInput.toString();
            String[] innerSplit = concat.split(" ");
            for (int l = 0; l < innerSplit.length; l++) {
                StringBuilder sB = new StringBuilder();
                for (int h = l; h < innerSplit.length; h++) {
                    if (h > l) {
                        sB.append(" ");
                    }
                    sB.append(innerSplit[h]);
                }
                String finalSubStr = sB.toString();
                uniqConcats.add(finalSubStr);
            }
        }

        // We need to sort these mapping tuples
        Map<String, Pair<String, Integer>> scores = reduceAndScan(uniqConcats, options, doubleMetaphone, phonemes, 0);
        List<Pair<String, Pair<String, Integer>>> tuples = new ArrayList<>();
        for (String s : scores.keySet()) {
            Pair<String, Integer> tup = scores.get(s);
            tuples.add(Pair.of(s, tup));
        }
        tuples.sort((Pair<String, Pair<String, Integer>> q, Pair<String ,Pair<String, Integer>> p) -> {
            int i = q.getRight().getRight();
            int j = p.getRight().getRight();

            return i - j;
        });

        // Find longest String of lowest score
        // e.g. "MECHANISM OF INJURY" and "INJURY" both have a score of 0,
        // since "INJURY" is a substring. Therefore, choose MOI.
        int bestScore = 0xffff;
        Pair<String, String> bestChoice = Pair.of("", "");
        for (Pair<String, Pair<String, Integer>> tup : tuples) {
            Pair<String, Integer> inner = tup.getRight();
            String keyword = tup.getLeft();
            String original = inner.getLeft();
            int score = inner.getRight();

            if (verboseLogging) {
                Log.i(TAG, "Phrase \"" + keyword + "\" had score " + score);
            }

            if (score < bestScore) {
                bestScore = score;
                bestChoice = Pair.of(keyword, original);
            } else if (score == bestScore) {
                if (keyword.length() > bestChoice.getLeft().length()) {
                    bestChoice = Pair.of(keyword, original);
                }
            }
        }
        return bestChoice;
    }

    // Returns Map of [ChoiceDetected: {OriginalStr, Score}]
    @RequiresApi(api = Build.VERSION_CODES.N)
    static Map<String, Pair<String, Integer>> reduceAndScan(Set<String> uniqs, String[] options,
                                                            DoubleMetaphone doubleMetaphone, Map<String, String> phonemes, int confidenceLimit) {

        Map<String, Pair<String, Integer>> retVal = new HashMap<>();
        if (confidenceLimit > 2) {
            return retVal;
        }

        if (verboseLogging) {
            Log.i(TAG, "Reduce and Scan. Score limit: " + confidenceLimit);
        }

        for (String subStr : uniqs) {
            String toMatch = doubleMetaphone.doubleMetaphone(subStr);
            List<int[]> sorted = new ArrayList<>();

            if (verboseLogging) {
                Log.i(TAG, "Substring \"" + subStr + "\"= " + toMatch);
            }


            for (int i = 0; i < options.length; i++) {
                int[] tup = {levenshtein(toMatch, phonemes.get(options[i])), i};
                sorted.add(tup);
            }
            sorted.sort((int[] i, int[] j) -> i[0] - j[0]);

            for (int[] res : sorted) {
                int score = res[0];
                String phrase = options[res[1]];

                if (score > confidenceLimit) {
                    break;
                }

                if (retVal.get(phrase) != null) {
                    Pair<String, Integer> tup = retVal.get(phrase);
                    int prevScore = tup.getRight();
                    if (prevScore > score) {
                        retVal.put(phrase, Pair.of(tup.getLeft(), score));
                        if (verboseLogging) {
                            Log.i(TAG, "Phrase \"" + phrase + "\", score: " + score);
                        }
                    }
                } else {
                    retVal.put(phrase, Pair.of(subStr, score));
                    if (verboseLogging) {
                        Log.i(TAG, "Phrase \"" + phrase + "\", score: " + score);
                    }
                }
            }
        }

        Set<String> subUniqs = new HashSet<>();
        for (String s : uniqs) {
            boolean add = true;
            for (String phrase : retVal.keySet()) {
                if (s.contains(phrase)) {
                    add = false;
                    break;
                }
            }
            if (add) {
                subUniqs.add(s);
            }
        }

        Map<String, Pair<String, Integer>> recurseMap = reduceAndScan(subUniqs,
                options, doubleMetaphone, phonemes, confidenceLimit + 1);
        for (String recurseKey : recurseMap.keySet()) {
            if (retVal.containsKey(recurseKey)) {
                continue;
            }

            retVal.put(recurseKey, recurseMap.get(recurseKey));
        }

        return retVal;
    }

    private static int levenshtein(String s, String t) {
        if (s == null && t != null) {
            return t.length();
        }

        if (t == null && s != null) {
            return s.length();
        }

        s = s.toUpperCase();
        t = t.toUpperCase();
        int n = s.length();
        int m = t.length();

        int[][] d = new int[n + 1][m + 1];

        if (n == 0) {
            return m;
        }

        if (m == 0) {
            return n;
        }

        for (int i = 0; i <= n; i++) {
            d[i][0] = i;
        }

        for (int j = 0; j <= m; j++) {
            d[0][j] =  j;
        }

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                int cost = t.charAt(j - 1) == s.charAt(i - 1) ? 0 : 1;//Math.abs(s.charAt(i - 1) - t.charAt(j - 1));

                d[i][j] = Math.min(
                        Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1),
                        d[i - 1][j - 1] + cost);
            }
        }

        return d[n][m];
    }

    public static String prettify(String str) {
        StringBuilder returnVal = new StringBuilder();
        String[] splits = str.replaceAll("_", " ").split(" ");

        for (String s : splits) {
            String l = s.toLowerCase();
            returnVal.append(l.substring(0, 1).toUpperCase()).append(l.substring(1)).append(" ");
        }

        return returnVal.toString();
    }
}
