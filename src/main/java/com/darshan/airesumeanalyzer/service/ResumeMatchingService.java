package com.darshan.airesumeanalyzer.service;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, rule-based resume matching and scoring engine.
 * Compares resume text against target job descriptions or produces a general audit.
 *
 * <p>Phase 4: Rule-based analysis without external AI calls or persistent storage.</p>
 */
@Service
public class ResumeMatchingService {

    public static final String DISCLAIMER_TEXT =
            "Preliminary rule-based compatibility estimate, not an official ATS score.";

    // Curated skill taxonomy with canonical display names
    private static final List<String> SKILL_TAXONOMY = List.of(
            // Languages
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "C", "Go", "Rust",
            "Kotlin", "Swift", "Ruby", "PHP", "SQL", "HTML", "CSS", "Bash", "Shell", "R", "Scala",
            // Frameworks & Libraries
            "Spring Boot", "Spring", "Hibernate", "JPA", "React", "Angular", "Vue", "Node.js",
            "Express", "Next.js", "Django", "Flask", "FastAPI", "ASP.NET", ".NET", "Bootstrap",
            "Tailwind CSS", "Tailwind", "jQuery",
            // Databases & Storage
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Oracle", "SQLite", "Cassandra",
            "Elasticsearch", "DynamoDB", "Kafka", "RabbitMQ",
            // Cloud, DevOps & Tools
            "AWS", "Azure", "GCP", "Docker", "Kubernetes", "CI/CD", "Jenkins", "Git", "GitHub",
            "GitLab", "Maven", "Gradle", "Terraform", "Ansible", "Linux", "Nginx",
            // Architecture & Testing
            "REST API", "RESTful APIs", "REST", "GraphQL", "Microservices", "Agile", "Scrum",
            "TDD", "OOP", "Design Patterns", "System Design", "JUnit", "Mockito",
            // Professional & Soft Skills
            "Leadership", "Problem Solving", "Communication", "Teamwork", "Collaboration",
            "Code Review", "Mentoring", "Analytical Skills"
    );

    // Generic headings that must never be treated as real role titles
    private static final Set<String> GENERIC_HEADINGS = Set.of(
            "about the role", "about this role", "about our role", "about the job", "about the position",
            "about us", "about the company", "about our team", "about the team", "company overview",
            "company description", "role overview", "job overview", "position overview", "overview",
            "summary", "role summary", "job summary", "position summary", "job description",
            "role description", "position description", "description", "responsibilities",
            "key responsibilities", "roles and responsibilities", "what you will do", "what you'll do",
            "what you will be doing", "what you'll be doing", "duties", "requirements",
            "qualifications", "required skills", "basic qualifications", "minimum qualifications",
            "preferred qualifications", "what you need", "what you'll need", "what you bring",
            "who you are", "who we are", "about you", "ideal candidate", "candidate profile",
            "benefits", "perks", "compensation", "salary", "how to apply", "equal opportunity employer",
            "introduction", "welcome", "notes", "details", "general technical role", "target role"
    );

    // Common role noun identifiers
    private static final Pattern ROLE_NOUN_PATTERN = Pattern.compile(
            "(?i)\\b(developer|engineer|architect|intern|internship|specialist|analyst|"
                    + "consultant|administrator|manager|lead|director|designer|devops|programmer|"
                    + "scientist|technician|associate|fullstack|full-stack|backend|frontend|sdet|sre|qa)\\b"
    );

    // Comprehensive stop words and low-value generic buzzwords ignored during keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
            // English grammatical and functional words
            "about", "above", "after", "again", "against", "all", "and", "any", "are", "aren't",
            "because", "been", "before", "being", "below", "between", "both", "but", "by", "can't",
            "cannot", "could", "couldn't", "did", "didn't", "does", "doesn't", "doing", "don't",
            "down", "during", "each", "few", "for", "from", "further", "had", "hadn't", "has",
            "hasn't", "have", "haven't", "having", "here", "how", "into", "more", "most", "mustn't",
            "myself", "off", "once", "only", "other", "ought", "our", "ours", "ourselves", "out",
            "over", "own", "same", "shan't", "she", "should", "shouldn't", "some", "such", "than",
            "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these", "they",
            "this", "those", "through", "too", "under", "until", "very", "was", "wasn't", "were",
            "weren't", "what", "when", "where", "which", "while", "who", "whom", "why", "with",
            "won't", "would", "wouldn't", "you", "your", "yours", "yourself", "yourselves",
            // Specific generic / low-value terms
            "motivated", "motivation", "eager", "learn", "learning", "collaborate", "collaboration",
            "collaborative", "application", "applications",
            // Generic buzzwords & behavioral traits
            "passionate", "passion", "communication", "interpersonal", "organizational", "detail",
            "oriented", "excellent", "strong", "good", "great", "proven", "demonstrated", "successful",
            "ability", "able", "proficient", "proficiency", "familiar", "familiarity", "knowledge",
            "understanding", "experience", "experienced", "skills", "skilled", "background",
            "qualifications", "qualified", "requirements", "required", "preferred", "preference",
            "responsibilities", "responsible", "duties", "candidate", "candidates", "applicant",
            "applicants", "person", "people", "individual", "individuals", "developer", "developers",
            "engineer", "engineers", "member", "members", "team", "teams", "company", "organization",
            "business", "role", "roles", "position", "positions", "job", "jobs", "work", "working",
            "opportunity", "environment", "culture", "industry", "day", "daily", "help", "helping",
            "build", "building", "built", "create", "creating", "develop", "developing", "deliver",
            "delivering", "support", "supporting", "maintain", "maintaining", "provide", "providing",
            "participate", "participating", "contribute", "contributing", "drive", "driving",
            "manage", "managing", "lead", "leading", "join", "joining", "seeking", "looking",
            "wanted", "must", "need", "needed", "needs", "plus", "bonus", "nice", "including",
            "includes", "well", "also", "either", "high", "level", "various", "multiple",
            "across", "within", "year", "years", "month", "months", "degree", "bachelor", "master",
            "minimum", "maximum", "hands", "closely", "effective", "effectively", "focus", "focused",
            "ideal", "wide", "range", "relevant", "equivalent", "related", "field", "based"
    );

    /**
     * Analyzes resume text against the provided job description.
     */
    public AnalysisResult analyze(String fileName, long fileSize, String resumeText, String jobDescription) {
        String cleanResume = resumeText == null ? "" : resumeText.trim();
        String cleanJobDesc = jobDescription == null ? "" : jobDescription.trim();
        boolean hasJobDesc = !cleanJobDesc.isEmpty();

        AnalysisResult result = new AnalysisResult();
        result.setId(UUID.randomUUID().toString());
        result.setFileName(fileName != null ? fileName : "resume.pdf");
        result.setFileSize(fileSize);
        result.setHasJobDescription(hasJobDesc);
        result.setDisclaimer(DISCLAIMER_TEXT);
        result.setAnalyzedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy · HH:mm")));

        // 1. Text Metrics & Section Detection
        result.setCharacterCount(cleanResume.length());
        result.setWordCount(countWords(cleanResume));
        List<String> detectedSections = detectResumeSections(cleanResume);
        result.setDetectedSections(detectedSections);

        // 2. Extract Skills from Resume
        Set<String> resumeSkills = extractSkills(cleanResume);

        // 3. Extract and Segment Job Description
        String jobTitle = determineJobTitle(cleanJobDesc);
        result.setJobTitle(jobTitle);

        Set<String> requiredSkills = new LinkedHashSet<>();
        Set<String> preferredSkills = new LinkedHashSet<>();

        if (hasJobDesc) {
            segmentJobSkills(cleanJobDesc, requiredSkills, preferredSkills);
        }

        // 4. Compare Skills
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();
        List<String> additionalSkills = new ArrayList<>();

        if (hasJobDesc) {
            Set<String> allJobSkills = new LinkedHashSet<>();
            allJobSkills.addAll(requiredSkills);
            allJobSkills.addAll(preferredSkills);

            for (String req : requiredSkills) {
                if (resumeSkills.contains(req)) {
                    matchedSkills.add(req);
                } else {
                    missingRequired.add(req);
                }
            }

            for (String pref : preferredSkills) {
                if (resumeSkills.contains(pref)) {
                    if (!matchedSkills.contains(pref)) {
                        matchedSkills.add(pref);
                    }
                } else {
                    missingPreferred.add(pref);
                }
            }

            for (String skill : resumeSkills) {
                if (!allJobSkills.contains(skill)) {
                    additionalSkills.add(skill);
                }
            }
        } else {
            // General Audit mode: all detected resume skills are considered matched/present
            matchedSkills.addAll(resumeSkills);
        }

        result.setMatchedSkills(matchedSkills);
        result.setMissingRequiredSkills(missingRequired);
        result.setMissingPreferredSkills(missingPreferred);
        result.setAdditionalSkills(additionalSkills);

        // 5. Keyword Coverage
        List<String> matchedKeywords = new ArrayList<>();
        List<String> missingKeywords = new ArrayList<>();
        int keywordCoverage = calculateKeywordCoverage(cleanResume, cleanJobDesc, matchedKeywords, missingKeywords);
        result.setKeywordCoveragePercent(keywordCoverage);
        result.setMatchedKeywords(matchedKeywords);
        result.setMissingKeywords(missingKeywords);

        // 6. Score Calculation with Component Breakdown (strictly sums to overallScore)
        Map<String, Integer> breakdown = new LinkedHashMap<>();
        int overallScore = calculateScore(hasJobDesc, requiredSkills, preferredSkills,
                matchedSkills, missingRequired, missingPreferred, additionalSkills,
                keywordCoverage, detectedSections, cleanResume, breakdown);

        result.setOverallScore(overallScore);
        result.setScoreBreakdown(breakdown);
        result.setScoreLabel(determineScoreLabel(overallScore));

        // 7. Actionable Improvement Suggestions
        List<String> suggestions = generateSuggestions(hasJobDesc, missingRequired,
                keywordCoverage, detectedSections, result.getWordCount());
        result.setSuggestions(suggestions);

        return result;
    }

    /**
     * Extracts skills from text using boundary-checked regular expressions to avoid
     * substring false positives (e.g., 'Go' matching inside 'Google', 'Java' inside 'JavaScript').
     */
    public Set<String> extractSkills(String text) {
        Set<String> found = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return found;
        }

        for (String skill : SKILL_TAXONOMY) {
            Pattern pattern = buildSkillPattern(skill);
            if (pattern.matcher(text).find()) {
                found.add(skill);
            }
        }
        return found;
    }

    /**
     * Builds a regex Pattern with strict boundaries around symbol and word characters.
     */
    public Pattern buildSkillPattern(String skill) {
        // Enforce boundary: neither preceded nor followed by alphanumeric, underscore, '+', or '#'
        return Pattern.compile(
                "(?<=^|[^a-zA-Z0-9_+#])" + Pattern.quote(skill) + "(?=[^a-zA-Z0-9_+#]|$)",
                Pattern.CASE_INSENSITIVE
        );
    }

    /**
     * Extracts the target role title intelligently from the job description.
     * Rejects generic headings like "About the Role", "Overview", or "Job Description".
     * Falls back to "Target Role Not Specified" if a role cannot be reliably determined.
     */
    public String determineJobTitle(String jobDescription) {
        if (jobDescription == null || jobDescription.isBlank()) {
            return "Target Role Not Specified";
        }

        // 1. Look for explicit title labels: "Job Title: ...", "Position: ...", "Title: ...", "Role: ..."
        Pattern labelPattern = Pattern.compile(
                "(?im)^[\\s*#-]*\\b(?:job\\s+title|position\\s+title|target\\s+role|position|title|role)\\s*[:\\-–—]\\s*([^\r\n]{3,60})$"
        );
        Matcher labelMatcher = labelPattern.matcher(jobDescription);
        if (labelMatcher.find()) {
            String candidate = cleanCandidateTitle(labelMatcher.group(1));
            if (isValidRoleTitle(candidate)) {
                return candidate;
            }
        }

        // 2. Scan lines (preferring earlier lines) for role heading
        String[] lines = jobDescription.split("\\R");
        for (int i = 0; i < Math.min(lines.length, 10); i++) {
            String candidate = cleanCandidateTitle(lines[i]);
            if (candidate.isEmpty() || candidate.endsWith(":")) {
                continue;
            }
            if (isGenericHeading(candidate)) {
                continue;
            }
            if (ROLE_NOUN_PATTERN.matcher(candidate).find() && isValidRoleTitle(candidate)) {
                return candidate;
            }
        }

        // 3. Scan for contextual hiring sentences: "seeking a ...", "looking for an? ...", "hiring a ..."
        Pattern seekingPattern = Pattern.compile(
                "(?i)(?:seeking|looking for|hiring|recruiting|need)\\s+(?:an?|our next)?\\s*([A-Za-z0-9+#/.\\- ]{2,60}?\\b(?:"
                        + "developer|engineer|architect|intern|internship|specialist|analyst|"
                        + "consultant|administrator|manager|lead|director|designer|programmer|"
                        + "scientist|technician|associate|sdet|sre)\\b(?:\\s+(?:intern|internship|lead|architect|specialist|developer|engineer))?)"
        );
        Matcher seekingMatcher = seekingPattern.matcher(jobDescription);
        if (seekingMatcher.find()) {
            String candidate = cleanCandidateTitle(seekingMatcher.group(1));
            if (isValidRoleTitle(candidate)) {
                return candidate;
            }
        }

        // 4. Fallback check for any early heading-like line under 50 chars that isn't generic
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String candidate = cleanCandidateTitle(lines[i]);
            if (candidate.length() >= 4 && candidate.length() <= 50
                    && !isGenericHeading(candidate)
                    && !candidate.contains(".")
                    && !candidate.endsWith(":")
                    && candidate.split("\\s+").length <= 6) {
                return candidate;
            }
        }

        return "Target Role Not Specified";
    }

    private String cleanCandidateTitle(String s) {
        if (s == null) return "";
        // Strip leading/trailing markdown characters, bullets, symbols, and whitespace
        String cleaned = s.replaceAll("^[\\s*#_\\-•–—]+", "").replaceAll("[\\s*#_\\-•–—,;:]+$", "").trim();
        // Remove leading generic adjectives like "an ambitious", "a talented", "experienced"
        cleaned = cleaned.replaceAll("^(?i)(?:an?\\s+)?(?:ambitious|talented|exceptional|experienced|passionate|enthusiastic|motivated|driven)\\s+", "");
        // Normalize whitespace
        return cleaned.replaceAll("\\s+", " ").trim();
    }

    private boolean isGenericHeading(String s) {
        if (s == null) return true;
        String lower = s.toLowerCase(Locale.ROOT).trim();
        if (GENERIC_HEADINGS.contains(lower)) {
            return true;
        }
        for (String heading : GENERIC_HEADINGS) {
            if (lower.startsWith(heading) || lower.endsWith(heading)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidRoleTitle(String s) {
        if (s == null || s.isBlank()) return false;
        if (isGenericHeading(s)) return false;
        int len = s.length();
        if (len < 3 || len > 60) return false;
        if (s.contains(".") || s.contains("?") || s.contains("!")) return false;
        String lower = s.toLowerCase(Locale.ROOT);
        if (lower.startsWith("we are") || lower.startsWith("you will") || lower.startsWith("looking for")) {
            return false;
        }
        return s.matches(".*[a-zA-Z].*");
    }

    /**
     * Segments job description into required and preferred skills without assuming
     * every skill in the document is mandatory.
     */
    private void segmentJobSkills(String jobDescription, Set<String> required, Set<String> preferred) {
        String lower = jobDescription.toLowerCase(Locale.ROOT);

        int requiredIdx = findRequiredSectionIndex(lower);
        int preferredIdx = findPreferredSectionIndex(lower);

        if (requiredIdx != -1 && preferredIdx != -1) {
            if (requiredIdx < preferredIdx) {
                String reqSection = jobDescription.substring(requiredIdx, preferredIdx);
                String prefSection = jobDescription.substring(preferredIdx);
                required.addAll(extractSkills(reqSection));
                preferred.addAll(extractSkills(prefSection));

                // Skills mentioned in the introductory/about section before requirements are preferred/general
                String beforeReq = jobDescription.substring(0, requiredIdx);
                preferred.addAll(extractSkills(beforeReq));
            } else {
                String prefSection = jobDescription.substring(preferredIdx, requiredIdx);
                String reqSection = jobDescription.substring(requiredIdx);
                preferred.addAll(extractSkills(prefSection));
                required.addAll(extractSkills(reqSection));

                String beforePref = jobDescription.substring(0, preferredIdx);
                preferred.addAll(extractSkills(beforePref));
            }
        } else if (requiredIdx != -1) {
            String reqSection = jobDescription.substring(requiredIdx);
            required.addAll(extractSkills(reqSection));

            // Any skills in overview/about before the requirements section are preferred, NOT mandatory
            String beforeReq = jobDescription.substring(0, requiredIdx);
            preferred.addAll(extractSkills(beforeReq));
        } else if (preferredIdx != -1) {
            String beforePref = jobDescription.substring(0, preferredIdx);
            String prefSection = jobDescription.substring(preferredIdx);
            extractSkillsWithCues(beforePref, required, preferred);
            preferred.addAll(extractSkills(prefSection));
        } else {
            // No explicit section headers ("Requirements" / "Nice to Have")
            extractSkillsWithCues(jobDescription, required, preferred);
        }

        // Avoid overlap: if a skill is in required, it takes precedence
        preferred.removeAll(required);
    }

    private void extractSkillsWithCues(String text, Set<String> required, Set<String> preferred) {
        String[] lines = text.split("\\R");
        Set<String> allSkillsInText = extractSkills(text);

        for (String skill : allSkillsInText) {
            Pattern skillPattern = buildSkillPattern(skill);
            boolean isMandatory = false;

            for (String line : lines) {
                if (skillPattern.matcher(line).find()) {
                    String lineLower = line.toLowerCase(Locale.ROOT);
                    if (containsAnyWord(lineLower, "must", "require", "required", "requirements",
                            "essential", "minimum", "need", "mandatory", "proficiency in", "strong in")) {
                        isMandatory = true;
                        break;
                    }
                }
            }

            if (isMandatory) {
                required.add(skill);
            } else {
                preferred.add(skill);
            }
        }

        // If no cues found at all, don't mark all skills as mandatory.
        // Assign the primary 1-2 skills as required and the remaining as preferred.
        if (required.isEmpty() && !allSkillsInText.isEmpty()) {
            int count = 0;
            int threshold = Math.min(2, allSkillsInText.size());
            for (String s : allSkillsInText) {
                if (count < threshold) {
                    required.add(s);
                } else {
                    preferred.add(s);
                }
                count++;
            }
        }
    }

    private int findRequiredSectionIndex(String lower) {
        String[] markers = {
                "required skills", "required qualifications", "minimum qualifications",
                "basic qualifications", "core requirements", "requirements", "must have",
                "what you'll need", "what you need", "what you bring", "mandatory"
        };
        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx != -1) return idx;
        }
        return -1;
    }

    private int findPreferredSectionIndex(String lower) {
        String[] markers = {
                "preferred qualifications", "preferred skills", "nice to have",
                "bonus points", "bonus", "desired skills", "preferred", "good to have",
                "pluses", "what sets you apart", "assets"
        };
        for (String marker : markers) {
            int idx = lower.indexOf(marker);
            if (idx != -1) return idx;
        }
        return -1;
    }

    /**
     * Calculates keyword coverage percentage between job description and resume.
     * Filters generic filler words (motivated, eager, learn, collaborate, applications, etc.)
     * and prioritizes technical skills and meaningful domain keywords.
     */
    private int calculateKeywordCoverage(String resume, String jobDescription,
                                         List<String> matched, List<String> missing) {
        if (jobDescription.isEmpty()) {
            return 100;
        }

        // 1. Collect priority technical skills from the job description
        Set<String> priorityKeywords = new LinkedHashSet<>();
        Set<String> jdSkills = extractSkills(jobDescription);
        for (String skill : jdSkills) {
            priorityKeywords.add(skill.toLowerCase(Locale.ROOT));
        }

        // 2. Extract domain keywords, strictly filtering stop words and low-value generic terms
        Set<String> domainKeywords = Arrays.stream(jobDescription.split("[^a-zA-Z0-9+#.-]+"))
                .map(String::trim)
                .map(s -> s.toLowerCase(Locale.ROOT))
                .filter(s -> s.length() >= 4 && !STOP_WORDS.contains(s) && !s.matches("\\d+"))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Combine: priority skills first, then meaningful domain terms
        Set<String> candidateKeywords = new LinkedHashSet<>(priorityKeywords);
        candidateKeywords.addAll(domainKeywords);

        if (candidateKeywords.isEmpty()) {
            return 100;
        }

        String resumeLower = resume.toLowerCase(Locale.ROOT);

        int matchCount = 0;
        int count = 0;
        for (String kw : candidateKeywords) {
            Pattern p = Pattern.compile("(?i)(?<=^|[^a-zA-Z0-9_+#])" + Pattern.quote(kw) + "(?=[^a-zA-Z0-9_+#]|$)");
            if (p.matcher(resumeLower).find()) {
                matchCount++;
                if (matched.size() < 12) {
                    matched.add(kw);
                }
            } else {
                if (missing.size() < 12) {
                    missing.add(kw);
                }
            }
            count++;
            if (count >= 25) break; // Sample top 25 significant keywords
        }

        return Math.min(100, (int) Math.round(((double) matchCount / Math.max(1, count)) * 100.0));
    }

    /**
     * Detects standard resume structural sections.
     */
    private List<String> detectResumeSections(String text) {
        List<String> sections = new ArrayList<>();
        String lower = text.toLowerCase(Locale.ROOT);

        if (containsAnyWord(lower, "experience", "work history", "employment", "professional experience")) {
            sections.add("Experience");
        }
        if (containsAnyWord(lower, "education", "academic", "university", "degree", "college")) {
            sections.add("Education");
        }
        if (containsAnyWord(lower, "skills", "technical skills", "technologies", "competencies")) {
            sections.add("Skills");
        }
        if (containsAnyWord(lower, "projects", "personal projects", "portfolio", "key projects")) {
            sections.add("Projects");
        }
        if (containsAnyWord(lower, "certifications", "certificates", "credentials", "licenses")) {
            sections.add("Certifications");
        }

        return sections;
    }

    private boolean containsAnyWord(String text, String... phrases) {
        for (String phrase : phrases) {
            Pattern p = Pattern.compile("(?i)\\b" + Pattern.quote(phrase) + "\\b");
            if (p.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    private int countWords(String text) {
        if (text.isEmpty()) return 0;
        String[] words = text.split("\\s+");
        return words.length;
    }

    /**
     * Computes the deterministic, explainable compatibility score.
     * Guarantees that component scores strictly sum to the overallScore.
     */
    private int calculateScore(boolean hasJobDesc, Set<String> requiredSkills, Set<String> preferredSkills,
                               List<String> matchedSkills, List<String> missingRequired,
                               List<String> missingPreferred, List<String> additionalSkills,
                               int keywordCoverage, List<String> detectedSections,
                               String resumeText, Map<String, Integer> breakdown) {
        if (hasJobDesc) {
            // 1. Required Skills Match (max 50 pts)
            int skillsScore;
            if (!requiredSkills.isEmpty()) {
                int matchedRequiredCount = 0;
                for (String req : requiredSkills) {
                    if (matchedSkills.contains(req)) {
                        matchedRequiredCount++;
                    }
                }
                skillsScore = (int) Math.round(((double) matchedRequiredCount / requiredSkills.size()) * 50.0);
            } else if (!preferredSkills.isEmpty()) {
                int matchedPrefCount = 0;
                for (String pref : preferredSkills) {
                    if (matchedSkills.contains(pref)) {
                        matchedPrefCount++;
                    }
                }
                skillsScore = (int) Math.round(((double) matchedPrefCount / preferredSkills.size()) * 40.0);
            } else if (!matchedSkills.isEmpty()) {
                skillsScore = Math.min(30, matchedSkills.size() * 10);
            } else {
                skillsScore = 0;
            }

            // 2. Preferred Skills & Breadth (max 15 pts)
            int preferredScore;
            if (!preferredSkills.isEmpty()) {
                int matchedPrefCount = 0;
                for (String pref : preferredSkills) {
                    if (matchedSkills.contains(pref)) {
                        matchedPrefCount++;
                    }
                }
                int prefBase = (int) Math.round(((double) matchedPrefCount / preferredSkills.size()) * 10.0);
                int breadth = Math.min(5, additionalSkills.size());
                preferredScore = Math.min(15, prefBase + breadth);
            } else {
                preferredScore = Math.min(15, additionalSkills.size() * 3);
            }

            // 3. Keyword / Domain Coverage (max 20 pts)
            int keywordScore = (int) Math.round((keywordCoverage / 100.0) * 20.0);

            // 4. Structural Completeness (max 15 pts)
            int structureScore = Math.min(15, detectedSections.size() * 3);

            // Ensure mathematical invariant: sum of components equals overallScore
            int total = skillsScore + preferredScore + keywordScore + structureScore;
            int overallScore = Math.min(100, Math.max(0, total));

            breakdown.put("Required Skills Match", skillsScore);
            breakdown.put("Preferred Skills & Breadth", preferredScore);
            breakdown.put("Keyword Coverage", keywordScore);
            breakdown.put("Resume Structure", structureScore);

            return overallScore;
        } else {
            // General Resume Audit (no job description provided)
            int skillsScore = Math.min(45, matchedSkills.size() * 5);
            int structureScore = Math.min(30, detectedSections.size() * 6);
            int contentScore = resumeText.length() > 500 ? 25 : (resumeText.length() > 200 ? 15 : 8);

            int total = skillsScore + structureScore + contentScore;
            int overallScore = Math.min(100, Math.max(0, total));

            breakdown.put("Skills Detected", skillsScore);
            breakdown.put("Resume Structure", structureScore);
            breakdown.put("Content Depth", contentScore);

            return overallScore;
        }
    }

    public String determineScoreLabel(int score) {
        if (score >= 85) return "Strong Match";
        if (score >= 70) return "Good Match";
        if (score >= 50) return "Moderate Match";
        return "Needs Alignment";
    }

    /**
     * Generates practical, actionable recommendations based on the analysis.
     */
    private List<String> generateSuggestions(boolean hasJobDesc, List<String> missingRequired,
                                             int keywordCoverage, List<String> detectedSections, int wordCount) {
        List<String> list = new ArrayList<>();

        if (hasJobDesc && !missingRequired.isEmpty()) {
            String skillsPreview = missingRequired.stream().limit(4).collect(Collectors.joining(", "));
            list.add("Target missing required skills: Explicitly highlight your experience with "
                    + skillsPreview + (missingRequired.size() > 4 ? " and other required tools." : "."));
        }

        if (hasJobDesc && keywordCoverage < 65) {
            list.add("Improve keyword alignment: Integrate more industry and domain terms from the job posting directly into your work experience bullet points.");
        }

        if (!detectedSections.contains("Projects")) {
            list.add("Add a dedicated Projects section: Describe 1-2 impactful projects to demonstrate practical application of your core technical stack.");
        }

        if (!detectedSections.contains("Certifications")) {
            list.add("Include relevant credentials: Add certifications, online specializations, or workshops to enhance your credibility.");
        }

        if (wordCount < 200) {
            list.add("Expand resume depth: Your resume appears brief. Add more context to your role responsibilities, team scale, and deliverables.");
        }

        // Always include two foundational recommendations
        list.add("Quantify your impact: Use tangible metrics (e.g., 'reduced query latency by 40%', 'delivered service for 10K+ users') to prove your results.");
        list.add("Action-oriented verbs: Begin each bullet point with decisive action verbs like Architected, Implemented, Streamlined, or Spearheaded.");

        return list;
    }
}
