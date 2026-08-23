"""
Pyflakes Integration Layer

Provides static analysis and linting using Pyflakes library.
"""

import logging
from typing import List, Dict, Any
import io

logger = logging.getLogger(__name__)

class PyflakesLinter:
    """Wrapper around Pyflakes library for static analysis."""

    def __init__(self):
        try:
            import pyflakes.api
            import pyflakes.reporter
            self.pyflakes = pyflakes
            self.available = True
            logger.info("Pyflakes initialized successfully")
        except ImportError:
            self.available = False
            logger.warning("Pyflakes not available - linting will be limited")

    def lint(self, code: str) -> List[Dict[str, Any]]:
        """Run static analysis on code and return issues."""
        if not self.available:
            return []

        try:
            # Create a string buffer to capture output
            buffer = io.StringIO()
            reporter = self.pyflakes.reporter.Report(buffer, buffer)
            
            # Run pyflakes on the code
            self.pyflakes.api.check(code, filename='<input>', reporter=reporter)
            
            # Parse the output
            output = buffer.getvalue()
            issues = []
            
            for line in output.splitlines():
                if ':' in line:
                    parts = line.split(':', 2)
                    if len(parts) >= 3:
                        issues.append({
                            "line": int(parts[0]),
                            "column": int(parts[1]),
                            "message": parts[2].strip(),
                            "severity": "warning"
                        })
            
            return issues
        except Exception as e:
            logger.error(f"Pyflakes linting error: {e}")
            return []