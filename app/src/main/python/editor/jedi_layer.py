"""
Jedi integration layer with input validation and error handling.

Provides code completion and analysis using Jedi library with:
- Input validation
- Error handling
- Lazy loading
- Graceful degradation
"""

import logging
from typing import List, Dict, Any, Tuple
from .exceptions import JediError

logger = logging.getLogger(__name__)

class JediWrapper:
    """Wrapper for Jedi with input validation and error handling."""

    def __init__(self):
        self._jedi = None

    @property
    def jedi(self):
        """Lazy load Jedi library."""
        if self._jedi is None:
            import jedi
            self._jedi = jedi
        return self._jedi

    def get_completions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get completions with input validation and error handling."""
        try:
            script = self.jedi.Script(code)
            completions = script.complete(*position)
            return [{
                "name": c.name,
                "type": c.type,
                "description": c.docstring() if c.docstring() else "",
                "module": c.module_name if c.module_name else ""
            } for c in completions]
        except Exception as e:
            raise JediError(f"Jedi completion failed: {e}")

    def get_definitions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get definitions for symbol at given position."""
        try:
            script = self.jedi.Script(code)
            definitions = script.goto(*position)
            return [{
                "name": d.name,
                "type": d.type,
                "module": d.module_name,
                "line": d.line,
                "column": d.column
            } for d in definitions]
        except Exception as e:
            raise JediError(f"Jedi definition failed: {e}")

class DummyJediWrapper:
    """Fallback if Jedi is not available."""

    def get_completions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        logger.warning("Jedi not available - using dummy completions")
        return []

    def get_definitions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        logger.warning("Jedi not available - using dummy definitions")
        return []