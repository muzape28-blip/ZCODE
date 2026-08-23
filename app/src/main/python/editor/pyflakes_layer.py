"""
Pyflakes Integration Layer with Lazy Loading and Error Handling

Provides static analysis and linting using Pyflakes library.
"""

import logging
import io
from typing import List, Dict, Any

logger = logging.getLogger(__name__)

class PyflakesWrapper:
    """Wrapper around Pyflakes library for static analysis."""

    def __init__(self, pyflakes_api):
        self._pyflakes = pyflakes_api

    def lint(self, code: str) -> List[Dict[str, Any]]:
        """Run static analysis on code and return issues."""
        try:
            # Create a string buffer to capture output
            buffer = io.StringIO()
            reporter = self._pyflakes.reporter.Reporter(buffer, buffer)
            
            # Run pyflakes on the code
            self._pyflakes.check(code, filename='<input>', reporter=reporter)
            
            # Parse the output
            output = buffer.getvalue()
            issues = []
            
            for line in output.splitlines():
                if ':' in line:
                    parts = line.split(':', 2)
                    if len(parts) >= 3:
                        try:
                            issues.append({
                                "line": int(parts[0]),
                                "column": int(parts[1]),
                                "message": parts[2].strip(),
                                "severity": "warning"
                            })
                        except ValueError:
                            logger.warning(f"Could not parse Pyflakes output: {line}")
            
            return issues
        except Exception as e:
            logger.error(f"Pyflakes linting error: {e}")
            return []

class DummyPyflakesWrapper:
    """Fallback if Pyflakes is not available."""

    def lint(self, code: str) -> List[Dict[str, Any]]:
        logger.warning("Pyflakes not available - using dummy linting")
        return []