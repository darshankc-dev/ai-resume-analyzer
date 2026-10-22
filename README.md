# AI Resume Analyzer

An AI-powered web application that helps job seekers evaluate their resumes against job descriptions, identify relevant skills and missing keywords, and improve their resumes for specific roles.

**Live Demo:** [Try AI Resume Analyzer](https://ai-resume-analyzer-2961.onrender.com/)

**GitHub Repository:** [darshankc-dev/ai-resume-analyzer](https://github.com/darshankc-dev/ai-resume-analyzer)

---

## Overview

AI Resume Analyzer is a web application built with Java and Spring Boot that helps users understand how closely their resumes align with a target job description.

Users can upload a PDF resume, provide a job description, and use the application to evaluate resume compatibility. The project combines resume text extraction, skill and keyword matching, and AI-assisted feedback to support the resume-improvement process.

The application is designed to make resume evaluation more accessible to students, fresh graduates, and job seekers preparing for internships and software development roles.

## Features

- **PDF Resume Upload:** Upload a text-based PDF resume through the web interface.
- **Resume Text Extraction:** Extract readable text from an uploaded resume using Apache PDFBox.
- **Job Description Matching:** Provide a target job description for role-specific resume analysis.
- **Compatibility Scoring:** Evaluate resume alignment with the target role.
- **Skill and Keyword Analysis:** Identify relevant skills and potential keyword gaps.
- **AI-Assisted Feedback:** Integrate Google Gemini to support resume analysis and improvement recommendations.
- **Web-Based Interface:** Use the application through a browser without installing a separate desktop application.
- **Live Demo:** Access the deployed application through Render.

*Note: The exact availability and behavior of every analysis feature depend on the current deployed build and its environment configuration.*

## Live Demo

Try the application here:

**[https://ai-resume-analyzer-2961.onrender.com/](https://ai-resume-analyzer-2961.onrender.com/)**

### How to Use

1. Open the live application.
2. Upload a text-based PDF resume (the interface currently indicates a maximum size of 5 MB).
3. Enter the job description you want to target.
4. Select **Analyze Resume**.
5. Review the analysis and any feedback returned by the application.

A public demo may take longer to respond after a period of inactivity because of free-tier hosting limitations. Availability also depends on the deployment and AI service configuration.

## Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Java 17 |
| Backend Framework | Spring Boot 3.2.5 |
| Web Layer | Spring Web, embedded Tomcat |
| Frontend Rendering | Thymeleaf |
| Frontend Technologies | HTML, CSS, JavaScript |
| PDF Text Extraction | Apache PDFBox 2.0.31 |
| AI Integration | Google Gen AI Java SDK |
| Build Tool | Apache Maven |
| Testing Framework | Spring Boot Starter Test |
| Containerization | Docker |
| Version Control | Git and GitHub |
| Deployment | Render |

## System Architecture

The application follows a Spring Boot web-application architecture.

```text
                 User / Browser
                       |
                       v
             HTML, CSS and JavaScript
                       |
                       v
              Spring Boot Application
                       |
             +---------+----------+
             |                    |
             v                    v
       PDF Text Extraction   Analysis Logic
         (Apache PDFBox)     and Matching
                                  |
                                  v
                         Google Gemini API
                                  |
                                  v
                         Analysis Response
                                  |
                                  v
                      Thymeleaf Results UI
```

The diagram represents the intended application flow. The exact internal call sequence depends on the current implementation.

## Project Structure

The repository contains the Maven project configuration, Docker deployment files, and application source code.

```text
ai-resume-analyzer/
├── src/
│   └── main/
│       ├── java/
│       └── resources/
├── Dockerfile
├── .dockerignore
├── .gitignore
├── pom.xml
└── sample_resume.pdf
```

The Java source and resource directories contain the application implementation. The exact package names and internal class organization can be explored in the `src/` directory.

## Getting Started

### Prerequisites

Install the following:

- Java Development Kit (JDK) 17
- Apache Maven
- Git
- A Google Gemini API key if you want to use the AI-powered analysis functionality

### 1. Clone the Repository

```bash
git clone https://github.com/darshankc-dev/ai-resume-analyzer.git
cd ai-resume-analyzer
```

### 2. Configure the Gemini API Key

Set the API key as an environment variable instead of hardcoding it in the source code.

**Windows PowerShell:**

```powershell
$env:GEMINI_API_KEY="your_gemini_api_key"
```

**macOS / Linux:**

```bash
export GEMINI_API_KEY="your_gemini_api_key"
```

If the application uses a configurable model name, set `GEMINI_MODEL` to a supported model ID as required by the current configuration.

Never commit API keys or other secrets to GitHub.

### 3. Build the Application

```bash
mvn clean package
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Alternatively, run the packaged executable JAR:

```bash
java -jar target/ai-resume-analyzer-0.0.1-SNAPSHOT.jar
```

If Maven generates a differently named JAR, use the actual filename in the `target/` directory.

### 5. Open the Application

Visit:

```text
http://localhost:8080
```

The exact local startup requirements depend on the application's current configuration and environment variables.

## Environment Configuration

| Variable | Purpose |
|---|---|
| `GEMINI_API_KEY` | Credentials for Google Gemini API access |
| `GEMINI_MODEL` | Optional model selection, if supported by the current application configuration |
| `PORT` | Port supplied by the deployment platform, if supported by the application configuration |

Keep all secrets in environment variables or the deployment provider's secret-management settings.

## Database

The Maven configuration includes Spring Data JPA and the PostgreSQL JDBC driver. However, the presence of these dependencies alone does not establish that a database is actively connected or used by the deployed application.

The project's current core workflow focuses on resume upload and analysis. Persistent storage of users, resumes, or analysis history should not be assumed unless it is implemented and configured.

## Deployment

The application is deployed on Render.

**Live URL:** [https://ai-resume-analyzer-2961.onrender.com/](https://ai-resume-analyzer-2961.onrender.com/)

The repository includes a Dockerfile for container-based deployment. Deployment requires a compatible Java runtime, correct port configuration, and environment variables for any external services.

The current hosting plan may sleep after inactivity. A successful deployment also does not guarantee that Gemini requests will succeed unless the API key, model configuration, and service limits are valid.

## Security and Privacy

- Store API keys in environment variables, not in source files.
- Do not commit `.env` files or credentials to a public repository.
- Use sample resumes when demonstrating the application publicly.
- Avoid uploading sensitive personal information unless you understand how the deployed service and external AI provider process it.
- Do not assume uploaded resumes or analysis results are permanently stored or automatically deleted without verifying the application's implementation.

## Current Scope and Limitations

- The live interface indicates support for PDF resumes up to 5 MB.
- Scanned PDFs that contain images rather than selectable text may not produce useful extracted text.
- AI-generated feedback depends on external API availability and configuration.
- Hosting cold starts or resource limits can affect response time.
- The application should be treated as a resume-improvement aid, not a guarantee of ATS success, interview selection, or employment.
- Database-backed user accounts and persistent analysis history are not documented as verified capabilities.

## Future Improvements

Potential future enhancements include:

- Resume analysis history and persistent reports
- User accounts and personalized dashboards
- More detailed skill-gap visualizations
- Additional document formats such as DOCX
- Exportable analysis reports
- Improved error handling and loading feedback
- Automated testing and continuous integration
- Accessibility and mobile usability improvements

These are potential enhancements, not claims about existing functionality.

## Author

**Darshan K C**

Computer Science and Engineering Student | Aspiring Backend Developer

Interested in Java, Spring Boot, AI-assisted applications, and backend engineering.

- **GitHub:** [@darshankc-dev](https://github.com/darshankc-dev)
- **Live Project:** [AI Resume Analyzer](https://ai-resume-analyzer-2961.onrender.com/)

---

If you find this project useful, consider giving the repository a star or sharing feedback and suggestions for improvement.
