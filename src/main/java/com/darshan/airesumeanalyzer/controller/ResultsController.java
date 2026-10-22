package com.darshan.airesumeanalyzer.controller;

import com.darshan.airesumeanalyzer.dto.AnalysisResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the Resume Analysis Results Dashboard.
 * Retrieves analysis data from the current user session.
 */
@Controller
public class ResultsController {

    public static final String SESSION_ANALYSIS_KEY = "analysisResult";

    @GetMapping("/results")
    public String showResults(HttpSession session, Model model) {
        model.addAttribute("appName", "AI Resume Analyzer");

        AnalysisResult result = (AnalysisResult) session.getAttribute(SESSION_ANALYSIS_KEY);

        if (result == null) {
            model.addAttribute("hasData", false);
            model.addAttribute("errorMessage", "No analysis data found or your session has expired.");
            return "results";
        }

        model.addAttribute("hasData", true);
        model.addAttribute("result", result);
        return "results";
    }
}
