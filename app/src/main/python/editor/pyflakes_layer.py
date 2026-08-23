"""
Pyflakes integration layer with input validation and error handling.

Provides static analysis and linting using Pyflakes library with:
- Input validation
- Error handling
- Lazy loading
- Graceful degradation
"""

import logging
import io
from typing import List, Dict, Any
from .exceptions import PyflakesError

logger = logging.getLogger(__name__)

class PyflakesWrapper:
    """Wrapper for Pyflakes with input validation and error handling."""

    def __init__(self):
        self._pyflakes = None

    @property
    def pyflakes(self):
        """Lazy load Pyflakes library."""
        if self._pyflakes is None:
            import pyflakes.api
            import pyflakes.reporter
            self._pyflakes = pyflakes
        return self._pyflakes

    def lint(self, code: str) -> List[Dict[str, Any]]:
        """Run static analysis on code with input validation and error handling."""
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
            raise PyflakesError(f"Pyflakes linting failed: {e}")

class DummyPyflakesWrapper:
    """Fallback if Pyflakes is not available."""

    def lint(self, code: str) -> List[Dict[str, Any]]:
        logger.warning("Pyflakes not available - using dummy linting")
        return []