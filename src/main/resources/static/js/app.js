/**
 * AI Resume Analyzer — Frontend Controller
 * Modern SaaS interaction logic: Drag-and-drop file upload, validation,
 * responsive progress state, and session navigation.
 */
document.addEventListener('DOMContentLoaded', () => {

    // --- DOM Elements ---
    const dropZone            = document.getElementById('drop-zone');
    const fileInput           = document.getElementById('file-input');
    const btnBrowse           = document.getElementById('btn-browse');
    const filePreview         = document.getElementById('file-preview');
    const fileName            = document.getElementById('file-name');
    const fileSize            = document.getElementById('file-size');
    const btnRemoveFile       = document.getElementById('btn-remove-file');
    const jobDescription      = document.getElementById('job-description');
    const btnAnalyze          = document.getElementById('btn-analyze');
    const btnText             = document.getElementById('btn-text');
    const spinner             = document.getElementById('spinner');
    const statusMessage       = document.getElementById('status-message');
    const statusIcon          = document.getElementById('status-icon');
    const statusText          = document.getElementById('status-text');
    const textPreview         = document.getElementById('text-preview');
    const textPreviewContent  = document.getElementById('text-preview-content');
    const btnCopy             = document.getElementById('btn-copy');

    let selectedFile = null;
    let isSubmitting = false;

    // --- Utility: Format file size ---
    function formatSize(bytes) {
        if (!bytes || bytes <= 0) return '0 B';
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    }

    // --- Utility: Update analyze button state ---
    function updateAnalyzeButton() {
        if (!btnAnalyze) return;
        btnAnalyze.disabled = !selectedFile || isSubmitting;
        if (isSubmitting) {
            btnAnalyze.setAttribute('aria-busy', 'true');
        } else {
            btnAnalyze.removeAttribute('aria-busy');
        }
    }

    // --- Utility: Show status message ---
    function showStatus(type, message) {
        if (!statusMessage || !statusText || !statusIcon) return;
        statusMessage.className = 'status-message visible ' + type;
        statusIcon.textContent = type === 'success' ? '✓' : '✕';
        statusText.textContent = message;
    }

    // --- Utility: Hide status message ---
    function hideStatus() {
        if (!statusMessage) return;
        statusMessage.className = 'status-message';
        if (statusText) statusText.textContent = '';
    }

    // --- Utility: Show text preview ---
    function showTextPreview(text) {
        if (!textPreview || !textPreviewContent) return;
        textPreviewContent.textContent = text;
        textPreview.classList.add('visible');
    }

    // --- Utility: Hide text preview ---
    function hideTextPreview() {
        if (!textPreview || !textPreviewContent) return;
        textPreview.classList.remove('visible');
        textPreviewContent.textContent = '';
    }

    // --- Reset Submission State ---
    function resetSubmissionState() {
        isSubmitting = false;
        if (btnText) btnText.textContent = 'Analyze Resume';
        if (spinner) spinner.classList.remove('visible');
        if (jobDescription) jobDescription.disabled = false;
        if (btnBrowse) btnBrowse.disabled = false;
        updateAnalyzeButton();
    }

    // --- File Selection Handler ---
    function handleFile(file) {
        if (isSubmitting) return;

        hideStatus();
        hideTextPreview();

        if (!file) return;

        // Client-side validation: must be PDF
        const isPdfType = file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');
        if (!isPdfType) {
            showStatus('error', 'Please select a valid PDF document (.pdf).');
            return;
        }

        // 5 MB max limit
        const maxBytes = 5 * 1024 * 1024;
        if (file.size > maxBytes) {
            showStatus('error', 'File size exceeds the 5 MB limit. Please choose a smaller file.');
            return;
        }

        if (file.size === 0) {
            showStatus('error', 'The selected file is empty (0 bytes). Please upload a valid resume.');
            return;
        }

        selectedFile = file;
        if (fileName) fileName.textContent = file.name;
        if (fileSize) fileSize.textContent = formatSize(file.size);
        if (filePreview) filePreview.classList.add('visible');
        updateAnalyzeButton();
    }

    // --- Remove File ---
    function removeFile() {
        if (isSubmitting) return;

        selectedFile = null;
        if (fileInput) fileInput.value = '';
        if (filePreview) filePreview.classList.remove('visible');
        hideStatus();
        hideTextPreview();
        updateAnalyzeButton();
    }

    // --- Browse Button Trigger ---
    if (btnBrowse && fileInput) {
        btnBrowse.addEventListener('click', (e) => {
            e.stopPropagation();
            if (isSubmitting) return;
            fileInput.click();
        });
    }

    // --- Drop Zone Click & Keyboard Activation ---
    if (dropZone && fileInput) {
        dropZone.addEventListener('click', (e) => {
            // Avoid double trigger if clicking directly on browse button
            if (e.target.closest('#btn-browse')) return;
            if (isSubmitting) return;
            fileInput.click();
        });

        dropZone.addEventListener('keydown', (e) => {
            if (isSubmitting) return;
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                fileInput.click();
            }
        });
    }

    // --- File Input Change ---
    if (fileInput) {
        fileInput.addEventListener('change', (e) => {
            if (isSubmitting) return;
            if (e.target.files && e.target.files.length > 0) {
                handleFile(e.target.files[0]);
            }
        });
    }

    // --- Remove Button ---
    if (btnRemoveFile) {
        btnRemoveFile.addEventListener('click', (e) => {
            e.stopPropagation();
            removeFile();
        });
    }

    // --- Drag & Drop Visuals & Handler ---
    if (dropZone) {
        ['dragenter', 'dragover'].forEach(event => {
            dropZone.addEventListener(event, (e) => {
                e.preventDefault();
                e.stopPropagation();
                if (!isSubmitting) {
                    dropZone.classList.add('drag-over');
                }
            });
        });

        ['dragleave', 'drop'].forEach(event => {
            dropZone.addEventListener(event, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropZone.classList.remove('drag-over');
            });
        });

        dropZone.addEventListener('drop', (e) => {
            if (isSubmitting) return;
            const files = e.dataTransfer ? e.dataTransfer.files : null;
            if (files && files.length > 0) {
                handleFile(files[0]);
            }
        });
    }

    // --- Copy Extracted Text to Clipboard ---
    if (btnCopy && textPreviewContent) {
        btnCopy.addEventListener('click', async () => {
            const text = textPreviewContent.textContent;
            if (!text) return;

            try {
                await navigator.clipboard.writeText(text);
                btnCopy.innerHTML = '<span>✓</span><span>Copied</span>';
                btnCopy.classList.add('copied');
                setTimeout(() => {
                    btnCopy.innerHTML = '<span>📋</span><span>Copy</span>';
                    btnCopy.classList.remove('copied');
                }, 2000);
            } catch (err) {
                // Fallback: select the text in DOM
                const range = document.createRange();
                range.selectNodeContents(textPreviewContent);
                const selection = window.getSelection();
                selection.removeAllRanges();
                selection.addRange(range);
            }
        });
    }

    // --- Analyze Resume Button (Upload + Extract + Analyze) ---
    if (btnAnalyze) {
        btnAnalyze.addEventListener('click', async () => {
            if (!selectedFile || isSubmitting) return;

            // UI: loading state & prevent duplicate submissions
            isSubmitting = true;
            btnAnalyze.disabled = true;
            btnText.textContent = 'Analyzing Resume…';
            spinner.classList.add('visible');
            if (jobDescription) jobDescription.disabled = true;
            if (btnBrowse) btnBrowse.disabled = true;
            hideStatus();
            hideTextPreview();

            // Dynamic progress messaging for smooth perceived latency
            const stepTimer1 = setTimeout(() => {
                if (isSubmitting && btnText) {
                    btnText.textContent = 'Matching Skills & Role…';
                }
            }, 3000);

            const stepTimer2 = setTimeout(() => {
                if (isSubmitting && btnText) {
                    btnText.textContent = 'Generating AI Feedback…';
                }
            }, 7500);

            const stepTimer3 = setTimeout(() => {
                if (isSubmitting && btnText) {
                    btnText.textContent = 'Polishing AI Insights…';
                }
            }, 18000);

            const formData = new FormData();
            formData.append('file', selectedFile);

            const jd = jobDescription ? jobDescription.value.trim() : '';
            formData.append('jobDescription', jd || '');

            // 45-second request timeout controller to accommodate deep AI analysis
            const abortController = new AbortController();
            const timeoutId = setTimeout(() => {
                abortController.abort();
            }, 45000);

            let isRedirecting = false;

            try {
                const response = await fetch('/api/resume/upload', {
                    method: 'POST',
                    body: formData,
                    credentials: 'same-origin',
                    signal: abortController.signal
                });

                let data = null;
                try {
                    data = await response.json();
                } catch (jsonErr) {
                    // Non-JSON response (e.g., HTTP 500 error page)
                }

                if (response.ok && data && data.success && data.redirectUrl) {
                    isRedirecting = true;
                    showStatus('success', 'Analysis complete! Opening your results dashboard…');
                    btnText.textContent = 'Opening Dashboard…';

                    if (data.extractedText) {
                        showTextPreview(data.extractedText);
                    }

                    // Navigate immediately to the results page
                    window.location.assign(data.redirectUrl);

                    // Safety fallback reset if navigation is cancelled by user
                    setTimeout(() => {
                        resetSubmissionState();
                    }, 8000);
                } else if (response.ok && data && data.success) {
                    // Fallback when redirectUrl omitted
                    showStatus('success', data.message || 'Resume text extracted successfully.');
                    if (data.extractedText) {
                        showTextPreview(data.extractedText);
                    }
                    resetSubmissionState();
                } else {
                    const message = (data && data.message)
                        ? data.message
                        : (response.status ? `Analysis failed with status ${response.status}.` : 'Failed to analyze resume.');
                    showStatus('error', message);
                    hideTextPreview();
                    resetSubmissionState();
                }
            } catch (err) {
                hideTextPreview();
                if (err.name === 'AbortError') {
                    showStatus('error', 'The analysis request timed out. Please try again.');
                } else {
                    showStatus('error', 'Connection error. Please check your network or server status and try again.');
                }
                resetSubmissionState();
            } finally {
                clearTimeout(timeoutId);
                clearTimeout(stepTimer1);
                clearTimeout(stepTimer2);
                clearTimeout(stepTimer3);

                if (!isRedirecting) {
                    resetSubmissionState();
                }
            }
        });
    }

    // Reset UI state when returning to page via browser history cache (bfcache)
    window.addEventListener('pageshow', () => {
        resetSubmissionState();
    });

    // --- Prevent default drag behaviors on window ---
    ['dragover', 'drop'].forEach(event => {
        window.addEventListener(event, (e) => e.preventDefault());
    });

    // --- Initial State ---
    resetSubmissionState();
});
