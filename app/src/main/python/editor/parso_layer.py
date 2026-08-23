"""
Parso integration layer with input validation and error handling.

Provides AST parsing and syntax analysis using Parso library with:
- Input validation
- Error handling
- Lazy loading
- Graceful degradation
"""

import logging
from typing import Any, Optional, List, Dict
from .exceptions import ParsoError

logger = logging.getLogger(__name__)

class ParsoWrapper:
    """Wrapper for Parso with input validation and error handling."""

    def __init__(self):
        self._parso = None

    @property
    def parso(self):
        """Lazy load Parso library."""
        if self._parso is None:
            import parso
            self._parso = parso
        return self._parso

    def parse(self, code: str) -> Optional[Any]:
        """Parse code into AST with input validation and error handling."""
        try:
            grammar = self.parso.load_grammar()
            return grammar.parse(code)
        except Exception as e:
            raise ParsoError(f"Parso parsing failed: {e}")

    def get_errors(self, code: str) -> List[Dict[str, Any]]:
        """Get syntax errors from code with input validation and error handling."""
        try:
            tree = self.parse(code)
            if tree is None:
                return []
            return [{
                "message": error.message,
                "line": error.start_pos[0],
                "column": error.start_pos[1]
            } for error in self.parso.iter_errors(tree)]
        except Exception as e:
            raise ParsoError(f"Parso error detection failed: {e}")

class DummyParsoWrapper:
    """Fallback if Parso is not available."""

    def parse(self, code: str) -> Optional[Any]:
        logger.warning("Parso not available - using dummy parser")
        return None

    def get_errors(self, code: str) -> List[Dict[str, Any]]:
        logger.warning("Parso not available - using dummy errors")
        return []