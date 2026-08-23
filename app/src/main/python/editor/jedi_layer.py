"""
Jedi Integration Layer

Provides code completion and analysis using Jedi library.
"""

import logging
from typing import List, Dict, Any, Tuple

logger = logging.getLogger(__name__)

class JediCompletionProvider:
    """Wrapper around Jedi library for code completion."""

    def __init__(self):
        try:
            import jedi
            self.jedi = jedi
            self.available = True
            logger.info("Jedi initialized successfully")
        except ImportError:
            self.available = False
            logger.warning("Jedi not available - completions will be limited")

    def get_completions(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get code completions at given line/column position."""
        if not self.available:
            return []

        try:
            script = self.jedi.Script(code, path="<input>")
            completions = script.complete(*position)
            
            return [{
                "name": c.name,
                "type": c.type,
                "description": c.docstring() if c.docstring() else "",
                "module": c.module_name if c.module_name else ""
            } for c in completions]
        except Exception as e:
            logger.error(f"Jedi completion error: {e}")
            return []

    def get_definition(self, code: str, position: Tuple[int, int]) -> List[Dict[str, Any]]:
        """Get definitions for symbol at given position."""
        if not self.available:
            return []

        try:
            script = self.jedi.Script(code, path="<input>")
            definitions = script.goto(*position)
            
            return [{
                "name": d.name,
                "type": d.type,
                "module": d.module_name,
                "line": d.line,
                "column": d.column
            } for d in definitions]
        except Exception as e:
            logger.error(f"Jedi definition error: {e}")
            return []